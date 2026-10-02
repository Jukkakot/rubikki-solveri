package fi.jukkakot.rubikkisolveri.ui.scan

import android.util.Size
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import java.util.concurrent.Executors

/**
 * The back camera's preview. Every analysed frame is read through the grid and its nine readings
 * passed to [onSamples] (on a background thread). The controller aligns analysis with the visible
 * preview, so the frame's crop rect is exactly what the user sees.
 */
@Composable
fun CameraPreview(torch: Boolean, onSamples: (List<Rgb>) -> Unit, onError: (Throwable) -> Unit, modifier: Modifier = Modifier) {
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
        controller.setImageAnalysisAnalyzer(executor) { image: ImageProxy ->
            try {
                val plane = image.planes[0]
                val bytes = plane.buffer
                if (buffer.size != bytes.remaining()) buffer = ByteArray(bytes.remaining())
                bytes.get(buffer)
                val crop = image.cropRect
                val frame = RgbaFrame(
                    image.width, image.height, plane.rowStride, buffer, image.imageInfo.rotationDegrees,
                    crop.left, crop.top, crop.right, crop.bottom,
                )
                onSamples(FrameSampler.sample(frame))
            } catch (e: Exception) {
                AppLog.logger.error(Evt.SCAN_ERROR, e)
            } finally {
                image.close()
            }
        }
        try {
            controller.bindToLifecycle(lifecycleOwner)
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
    AndroidView(
        factory = { PreviewView(it).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            this.controller = controller
        } },
        modifier = modifier,
    )
}
