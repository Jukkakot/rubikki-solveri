package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.jukkakot.rubikkisolveri.cube.scan.ArgbImage
import fi.jukkakot.rubikkisolveri.cube.scan.CameraSettings
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/**
 * The browser functions the shared screens need. All JavaScript lives in the `web` module's
 * `platform.mjs`; `web` fills these in at start, before the first frame.
 */
object BrowserHooks {
    /** `prefers-reduced-motion: reduce`. */
    var reducedMotion: () -> Boolean = { false }

    /** `Intl.DateTimeFormat`: [timeOnly] gives just the time. */
    var formatDateTime: (epochMillis: Long, language: String, timeOnly: Boolean) -> String =
        { millis, _, _ -> millis.toString() }

    /** The browser camera: see `CameraPreview`. */
    var cameraPreview: @Composable (CameraArgs) -> Unit = {}

    /** Asks for the camera; see `CameraPermissionGate`. */
    var cameraGate: @Composable (alternative: Pair<String, () -> Unit>, denied: @Composable (@Composable () -> Unit) -> Unit, content: @Composable () -> Unit) -> Unit =
        { _, _, content -> content() }
}

/** The arguments of `CameraPreview`, passed on to the browser camera. */
class CameraArgs(
    val torch: Boolean,
    val onSamples: (List<Rgb>) -> Unit,
    val onError: (Throwable) -> Unit,
    val modifier: Modifier,
    val exposure: CameraSettings,
    val onPicture: ((IntArray) -> Unit)?,
    val onTorchAvailable: (Boolean) -> Unit,
    val onImage: ((ArgbImage) -> Unit)? = null,
    val onMaxDarker: (Int) -> Unit = {},
)
