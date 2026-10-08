package fi.jukkakot.rubikkisolveri.ui.scan

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.CameraSettings
import fi.jukkakot.rubikkisolveri.cube.scan.ExposureSteps
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

/** Camera frames this far apart are a stall worth logging. */
private const val CAMERA_STALL_MILLIS = 300L

@androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
private fun lockExposure(controller: LifecycleCameraController, lock: Boolean) {
    val control = controller.cameraControl ?: return
    try {
        val options = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_LOCK, lock)
            .setCaptureRequestOption(CaptureRequest.CONTROL_AWB_LOCK, lock)
            .build()
        Camera2CameraControl.from(control).setCaptureRequestOptions(options)
        AppLog.info(Evt.SCAN_LOCK, null, "lock" to lock)
    } catch (e: Exception) {
        AppLog.logger.error(Evt.SCAN_ERROR, e, "exposure lock failed")
    }
}

/** The analysis frame's size, turn and visible part: where an upright picture's point lies in the camera's frame. */
private class FrameGeometry(val width: Int, val height: Int, val rotation: Int, val left: Int, val top: Int, val right: Int, val bottom: Int) {
    fun share(p: Point): Point = FrameSampler.toFrameShare(p, width, height, rotation, left, top, right, bottom)
}

/**
 * The camera's metering and focus at [settings]' points (`camera-exposure` design 3): one
 * `FocusMeteringAction` without auto-cancel, focus alone where the two differ. No point: the camera's
 * own choice over the whole picture.
 */
private fun meterAt(controller: LifecycleCameraController, settings: CameraSettings, geometry: FrameGeometry?) {
    val control = controller.cameraControl ?: return
    val meter = settings.meter
    val focus = settings.focus ?: meter
    try {
        if (geometry == null || meter == null || focus == null) {
            control.cancelFocusAndMetering()
            return
        }
        // The points are shares of the whole sensor-oriented frame.
        val factory = SurfaceOrientedMeteringPointFactory(1f, 1f)
        fun point(p: Point) = geometry.share(p).let { factory.createPoint(it.x.toFloat(), it.y.toFloat()) }
        val action = if (meter == focus) {
            FocusMeteringAction.Builder(point(meter), FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB)
        } else {
            FocusMeteringAction.Builder(point(focus), FocusMeteringAction.FLAG_AF)
                .addPoint(point(meter), FocusMeteringAction.FLAG_AE or FocusMeteringAction.FLAG_AWB)
        }.disableAutoCancel().build()
        control.startFocusAndMetering(action)
    } catch (e: Exception) {
        AppLog.logger.error(Evt.SCAN_ERROR, e, "metering point failed")
    }
}

/** The camera set [darker] steps below its own exposure, as far as it allows (`camera-exposure` design 2). */
private fun setDarker(controller: LifecycleCameraController, darker: Int) {
    val control = controller.cameraControl ?: return
    val state = controller.cameraInfo?.exposureState ?: return
    if (!state.isExposureCompensationSupported) return
    val index = ExposureSteps.index(darker, state.exposureCompensationStep.toDouble(), state.exposureCompensationRange.lower)
    if (index == state.exposureCompensationIndex) return
    try {
        control.setExposureCompensationIndex(index)
    } catch (e: Exception) {
        AppLog.logger.error(Evt.SCAN_ERROR, e, "exposure compensation failed")
    }
}

/** How many steps darker the camera allows. */
private fun maxDarker(info: CameraInfo): Int {
    val state = info.exposureState
    if (!state.isExposureCompensationSupported) return 0
    return ExposureSteps.maxDarker(state.exposureCompensationStep.toDouble(), state.exposureCompensationRange.lower)
}

/** What the camera can do, for the `scan.camera` line (`camera-exposure` design 6). */
@androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
private fun cameraAbilities(info: CameraInfo): List<Pair<String, Any?>> {
    val exposure = info.exposureState
    val fields = mutableListOf<Pair<String, Any?>>(
        "compensation" to if (exposure.isExposureCompensationSupported) "${exposure.exposureCompensationRange.lower}..${exposure.exposureCompensationRange.upper}" else "none",
        "stepEv" to (exposure.exposureCompensationStep.toDouble() * 100).toInt() / 100.0,
        "darkerSteps" to maxDarker(info),
        "torch" to info.hasFlashUnit(),
    )
    try {
        val camera2 = Camera2CameraInfo.from(info)
        val af = camera2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
        fields.add(0, "id" to camera2.cameraId)
        fields += "focus" to (af?.joinToString(",") { afMode(it) } ?: "none")
        fields += "aeRegions" to (camera2.getCameraCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) ?: 0)
        fields += "afRegions" to (camera2.getCameraCharacteristic(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) ?: 0)
        fields += "lock" to (camera2.getCameraCharacteristic(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) ?: false)
    } catch (e: Exception) {
        AppLog.logger.error(Evt.SCAN_ERROR, e, "camera characteristics")
    }
    return fields
}

private fun afMode(mode: Int): String = when (mode) {
    CameraMetadata.CONTROL_AF_MODE_OFF -> "off"
    CameraMetadata.CONTROL_AF_MODE_AUTO -> "auto"
    CameraMetadata.CONTROL_AF_MODE_MACRO -> "macro"
    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "video"
    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "picture"
    CameraMetadata.CONTROL_AF_MODE_EDOF -> "edof"
    else -> mode.toString()
}

/**
 * The analysis frame's visible part ([crop] of the RGBA [rgba], [rowStride] bytes a row) turned upright
 * by [rotation] degrees: the very picture the video scan reads, to show in the camera's place
 * (`scan-read-picture-android`). A copy: the buffer is reused for the next frame.
 */
private fun uprightBitmap(rgba: ByteArray, width: Int, height: Int, rowStride: Int, crop: Rect, rotation: Int): Bitmap {
    val full = Bitmap.createBitmap(rowStride / 4, height, Bitmap.Config.ARGB_8888)
    full.copyPixelsFromBuffer(ByteBuffer.wrap(rgba, 0, rowStride * height))
    val right = minOf(crop.right, width)
    val turn = Matrix().apply { postRotate(rotation.toFloat()) }
    return Bitmap.createBitmap(full, crop.left, crop.top, right - crop.left, crop.bottom - crop.top, turn, false)
}

@Composable
actual fun CameraPreview(
    torch: Boolean,
    onSamples: (List<Rgb>) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    exposure: CameraSettings,
    onPicture: ((IntArray) -> Unit)?,
    onTorchAvailable: (Boolean) -> Unit,
    onImage: ((ScanImage) -> Unit)?,
    onMaxDarker: (Int) -> Unit,
    onFaces: ((FoundFaces) -> Unit)?,
    onWorker: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val geometry = remember { AtomicReference<FrameGeometry?>(null) }
    // The camera's abilities, logged with the analysis size at the first frame.
    val abilities = remember { AtomicReference<List<Pair<String, Any?>>?>(null) }
    var ready by remember { mutableStateOf(false) }
    val controller = remember {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(LifecycleCameraController.IMAGE_ANALYSIS)
            imageAnalysisOutputImageFormat = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
            imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(Size(640, 480), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER),
                )
                .build()
        }
    }
    DisposableEffect(controller) {
        var buffer = ByteArray(0)
        var lastFrame = 0L
        controller.setImageAnalysisAnalyzer(executor) { image: ImageProxy ->
            try {
                val now = System.nanoTime() / 1_000_000
                if (lastFrame > 0 && now - lastFrame >= CAMERA_STALL_MILLIS) {
                    AppLog.info(Evt.SCAN_STALL, null, "where" to "camera", "ms" to now - lastFrame)
                }
                lastFrame = now
                val plane = image.planes[0]
                val bytes = plane.buffer
                if (buffer.size != bytes.remaining()) buffer = ByteArray(bytes.remaining())
                bytes.get(buffer)
                val crop = image.cropRect
                val rotation = image.imageInfo.rotationDegrees
                if (geometry.get() == null) {
                    abilities.getAndSet(null)?.let {
                        AppLog.info(Evt.SCAN_CAMERA, null, *it.toTypedArray(), "size" to "${image.width}x${image.height}")
                    }
                }
                geometry.set(FrameGeometry(image.width, image.height, rotation, crop.left, crop.top, crop.right, crop.bottom))
                val frame = RgbaFrame(
                    image.width, image.height, plane.rowStride, buffer, rotation,
                    crop.left, crop.top, crop.right, crop.bottom,
                )
                // The picture first, so it belongs to the same frame as the readings.
                onPicture?.invoke(FrameSampler.picture(frame))
                onSamples(FrameSampler.sample(frame))
                // Every camera picture goes to the video scan; its finder keeps only the newest it can take.
                if (onImage != null) {
                    val start = System.nanoTime()
                    val picture = uprightBitmap(buffer, image.width, image.height, plane.rowStride, crop, rotation)
                    val pictureMs = (System.nanoTime() - start) / 1e6
                    onImage(ScanImage(FrameSampler.upright(frame), picture.asImageBitmap(), pictureMs))
                }
            } catch (e: Exception) {
                AppLog.logger.error(Evt.SCAN_ERROR, e)
            } finally {
                image.close()
            }
        }
        try {
            controller.bindToLifecycle(lifecycleOwner)
            controller.initializationFuture.addListener(
                {
                    val info = controller.cameraInfo
                    onTorchAvailable(info?.hasFlashUnit() ?: false)
                    if (info != null) {
                        abilities.set(cameraAbilities(info))
                        onMaxDarker(maxDarker(info))
                    }
                    ready = true
                },
                ContextCompat.getMainExecutor(context),
            )
        } catch (e: Exception) {
            AppLog.logger.error(Evt.SCAN_ERROR, e, "bind failed")
            onError(e)
        }
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            executor.shutdown()
        }
    }
    LaunchedEffect(torch) { runCatching { controller.enableTorch(torch) } }
    LaunchedEffect(ready, exposure.lock) { if (ready || !exposure.lock) lockExposure(controller, exposure.lock) }
    LaunchedEffect(ready, exposure.darker) { if (ready) setDarker(controller, exposure.darker) }
    LaunchedEffect(ready, exposure.meter, exposure.focus) {
        if (ready && (exposure.meter != null || exposure.focus != null)) meterAt(controller, exposure, geometry.get())
    }
    AndroidView(
        factory = { PreviewView(it).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            this.controller = controller
        } },
        modifier = modifier,
    )
}
