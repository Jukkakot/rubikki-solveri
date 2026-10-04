package fi.jukkakot.rubikkisolveri.web

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.res.Res
import fi.jukkakot.rubikkisolveri.res.camera_denied_web
import fi.jukkakot.rubikkisolveri.res.scan_permission_allow
import fi.jukkakot.rubikkisolveri.res.scan_permission_title
import fi.jukkakot.rubikkisolveri.ui.BrowserHooks
import fi.jukkakot.rubikkisolveri.ui.CameraArgs
import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import fi.jukkakot.rubikkisolveri.ui.scan.coverCrop
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.khronos.webgl.toByteArray

/** The preview's long side in pixels: lower it if the phone shows fewer than 10 frames a second. */
const val PREVIEW_LONG_SIDE = 360

/** The grid square's size for reading colours (the grid's cells ≈ 63 px, like the phone's analysis). */
const val ANALYSIS_SIZE = 264

/** Frames are read at most this often (about 15 a second). */
private const val FRAME_MILLIS = 66L

/** Camera frames this far apart are a stall worth logging (as on the phone). */
private const val CAMERA_STALL_MILLIS = 300L

/** Connects the shared scan screens to the browser camera. */
fun installCamera() {
    BrowserHooks.cameraGate = { alternative, denied, content -> WebCameraGate(alternative, denied, content) }
    BrowserHooks.cameraPreview = { args -> WebCameraPreview(args) }
}

private sealed interface Access {
    data object Asking : Access
    data object Granted : Access
    data class Refused(val error: String) : Access
}

/**
 * Opens the camera when the scan opens, so the browser asks for it there. Blocked → explains where
 * to allow it, with the way out; any other failure (no camera) shows the content, whose preview
 * reports the error.
 */
@Composable
private fun WebCameraGate(
    alternative: Pair<String, () -> Unit>,
    denied: @Composable (@Composable () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    var access by remember { mutableStateOf<Access>(Access.Asking) }
    var attempt by remember { mutableIntStateOf(0) }
    DisposableEffect(attempt) {
        var live = true
        cameraAcquire { status ->
            if (live) access = if (status == "ok") Access.Granted else Access.Refused(status)
            AppLog.info(Evt.SCAN_PERMISSION, null, "granted" to (status == "ok"), "status" to status, "camera" to cameraInfo().ifEmpty { null })
        }
        onDispose {
            live = false
            cameraRelease()
        }
    }
    when (val a = access) {
        Access.Asking -> denied { Box(Modifier.fillMaxSize()) }
        Access.Granted -> content()
        is Access.Refused -> if (a.error == "NotAllowedError" || a.error == "SecurityError") {
            denied {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(Res.string.scan_permission_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(Res.string.camera_denied_web))
                    Button(onClick = { access = Access.Asking; attempt++ }) { Text(stringResource(Res.string.scan_permission_allow)) }
                    TextButton(onClick = alternative.second) { Text(alternative.first) }
                }
            }
        } else {
            content()
        }
    }
}

@Composable
private fun WebCameraPreview(args: CameraArgs) {
    val current by rememberUpdatedState(args)
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var box by remember { mutableStateOf(IntSize.Zero) }
    var running by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        var live = true
        cameraAcquire { status ->
            if (!live) return@cameraAcquire
            if (status == "ok") {
                running = true
                current.onTorchAvailable(cameraTorchSupported())
            } else {
                AppLog.info(Evt.SCAN_ERROR, "camera: $status")
                current.onError(IllegalStateException(status))
            }
        }
        onDispose {
            live = false
            cameraRelease()
        }
    }
    LaunchedEffect(running, args.torch) { if (running) cameraSetTorch(args.torch) }
    LaunchedEffect(running, args.lockExposure) {
        if (running && args.lockExposure) AppLog.info(Evt.SCAN_LOCK, null, "lock" to cameraLockExposure(true))
        else if (running) cameraLockExposure(false)
    }
    LaunchedEffect(running, box) {
        if (!running || box.width == 0 || box.height == 0) return@LaunchedEffect
        var lastRead = 0L
        var lastFrame = 0L
        while (true) {
            val now = withFrameMillis { elapsedMillis() }
            if (now - lastRead < FRAME_MILLIS) continue
            val vw = cameraVideoWidth()
            val vh = cameraVideoHeight()
            if (vw == 0 || vh == 0) continue
            val crop = coverCrop(vw, vh, box.width, box.height)
            if (!cameraGrab(crop.x, crop.y, crop.width, crop.height, PREVIEW_LONG_SIDE, ANALYSIS_SIZE)) continue
            lastRead = now
            if (lastFrame > 0 && now - lastFrame >= CAMERA_STALL_MILLIS) {
                AppLog.info(Evt.SCAN_STALL, null, "where" to "camera", "ms" to now - lastFrame)
            }
            lastFrame = now
            try {
                val frame = RgbaFrame(ANALYSIS_SIZE, ANALYSIS_SIZE, ANALYSIS_SIZE * 4, cameraAnalysisData().toByteArray(), 0)
                // The picture first, so it belongs to the same frame as the readings.
                current.onPicture?.invoke(FrameSampler.picture(frame))
                current.onSamples(FrameSampler.sample(frame))
                image = rgbaBitmap(cameraPreviewData().toByteArray(), cameraPreviewWidth(), cameraPreviewHeight())
            } catch (e: Throwable) {
                AppLog.logger.error(Evt.SCAN_ERROR, e)
            }
        }
    }
    Box(args.modifier.clipToBounds().onSizeChanged { box = it }) {
        image?.let { Image(it, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

private fun rgbaBitmap(bytes: ByteArray, width: Int, height: Int): ImageBitmap {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(bytes)
    return bitmap.asComposeImageBitmap()
}

