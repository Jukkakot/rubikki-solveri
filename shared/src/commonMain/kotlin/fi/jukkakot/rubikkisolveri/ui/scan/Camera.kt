package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/**
 * The back camera's preview. Every analysed frame is read through the grid and its nine readings
 * passed to [onSamples] (possibly on a background thread); [onPicture] gets the grid picture of the
 * same frame. [lockExposure] holds the current exposure and white balance where the camera can.
 * [onTorchAvailable] says whether the camera has a torch the app can switch.
 */
@Composable
expect fun CameraPreview(
    torch: Boolean,
    onSamples: (List<Rgb>) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    lockExposure: Boolean = false,
    onPicture: ((IntArray) -> Unit)? = null,
    onTorchAvailable: (Boolean) -> Unit = {},
)

/**
 * Shows [content] once the camera may be used. Otherwise asks for the permission (once
 * automatically) and explains it, inside [denied]'s frame, with [alternative] as a way out.
 */
@Composable
expect fun CameraPermissionGate(
    alternative: Pair<String, () -> Unit>,
    denied: @Composable (@Composable () -> Unit) -> Unit = { it() },
    content: @Composable () -> Unit,
)
