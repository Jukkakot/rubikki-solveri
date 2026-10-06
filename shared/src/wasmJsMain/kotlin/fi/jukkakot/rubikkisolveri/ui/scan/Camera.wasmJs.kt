package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.jukkakot.rubikkisolveri.cube.scan.ArgbImage
import fi.jukkakot.rubikkisolveri.cube.scan.CameraSettings
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.ui.BrowserHooks
import fi.jukkakot.rubikkisolveri.ui.CameraArgs

@Composable
actual fun CameraPreview(
    torch: Boolean,
    onSamples: (List<Rgb>) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    exposure: CameraSettings,
    onPicture: ((IntArray) -> Unit)?,
    onTorchAvailable: (Boolean) -> Unit,
    onImage: ((ArgbImage) -> Unit)?,
    onMaxDarker: (Int) -> Unit,
) = BrowserHooks.cameraPreview(CameraArgs(torch, onSamples, onError, modifier, exposure, onPicture, onTorchAvailable, onImage, onMaxDarker))

@Composable
actual fun CameraPermissionGate(
    alternative: Pair<String, () -> Unit>,
    denied: @Composable (@Composable () -> Unit) -> Unit,
    content: @Composable () -> Unit,
) = BrowserHooks.cameraGate(alternative, denied, content)
