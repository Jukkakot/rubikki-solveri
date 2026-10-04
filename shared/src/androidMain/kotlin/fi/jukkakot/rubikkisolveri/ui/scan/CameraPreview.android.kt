package fi.jukkakot.rubikkisolveri.ui.scan

import android.hardware.camera2.CaptureRequest
import android.util.Size
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import java.util.concurrent.Executors

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

@Composable
actual fun CameraPreview(
    torch: Boolean,
    onSamples: (List<Rgb>) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    lockExposure: Boolean,
    onPicture: ((IntArray) -> Unit)?,
    onTorchAvailable: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
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
                val frame = RgbaFrame(
                    image.width, image.height, plane.rowStride, buffer, image.imageInfo.rotationDegrees,
                    crop.left, crop.top, crop.right, crop.bottom,
                )
                // The picture first, so it belongs to the same frame as the readings.
                onPicture?.invoke(FrameSampler.picture(frame))
                onSamples(FrameSampler.sample(frame))
            } catch (e: Exception) {
                AppLog.logger.error(Evt.SCAN_ERROR, e)
            } finally {
                image.close()
            }
        }
        try {
            controller.bindToLifecycle(lifecycleOwner)
            controller.initializationFuture.addListener(
                { onTorchAvailable(controller.cameraInfo?.hasFlashUnit() ?: false) },
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
    LaunchedEffect(lockExposure) { lockExposure(controller, lockExposure) }
    AndroidView(
        factory = { PreviewView(it).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            this.controller = controller
        } },
        modifier = modifier,
    )
}
