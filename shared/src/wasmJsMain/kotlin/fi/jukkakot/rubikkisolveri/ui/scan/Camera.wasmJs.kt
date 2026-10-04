package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

@Composable
actual fun CameraPreview(
    torch: Boolean,
    onSamples: (List<Rgb>) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier,
    lockExposure: Boolean,
    onPicture: ((IntArray) -> Unit)?,
    onTorchAvailable: (Boolean) -> Unit,
): Unit = TODO("web: getUserMedia")

@Composable
actual fun CameraPermissionGate(
    alternative: Pair<String, () -> Unit>,
    denied: @Composable (@Composable () -> Unit) -> Unit,
    content: @Composable () -> Unit,
): Unit = TODO("web: getUserMedia")
