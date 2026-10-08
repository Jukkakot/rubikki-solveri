package fi.jukkakot.rubikkisolveri.ui.scan

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import fi.jukkakot.rubikkisolveri.cube.scan.ArgbImage
import fi.jukkakot.rubikkisolveri.cube.scan.CameraSettings
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/**
 * The back camera's preview. Every analysed frame is read through the grid and its nine readings
 * passed to [onSamples] (possibly on a background thread); [onPicture] gets the grid picture of the
 * same frame. [exposure] says where the camera measures light and focuses (shares of [onImage]'s
 * upright picture), how many steps darker it is set and whether exposure and white balance are held,
 * each where the camera can. [onTorchAvailable] says whether the camera has a torch the app can
 * switch. [onImage] gets, about fifteen times a second, the whole visible picture upright
 * (`FrameSampler.upright`) for the video scan, on the phone with the picture itself to show
 * ([ScanImage.picture], `scan-read-picture-android`). [onMaxDarker] gets how many steps darker the camera
 * can be set once it is open (0 = it cannot). Where the platform finds the video scan's faces
 * itself (the browser's worker), they go to [onFaces] instead of [onImage], and [onWorker] says
 * whether that works (true once it runs, false when the pictures go to [onImage] after all).
 */
@Composable
expect fun CameraPreview(
    torch: Boolean,
    onSamples: (List<Rgb>) -> Unit,
    onError: (Throwable) -> Unit,
    modifier: Modifier = Modifier,
    exposure: CameraSettings = CameraSettings.FREE,
    onPicture: ((IntArray) -> Unit)? = null,
    onTorchAvailable: (Boolean) -> Unit = {},
    onImage: ((ScanImage) -> Unit)? = null,
    onMaxDarker: (Int) -> Unit = {},
    onFaces: ((FoundFaces) -> Unit)? = null,
    onWorker: (Boolean) -> Unit = {},
)

/**
 * One picture for the video scan: [image] to find faces in, and where the platform makes it, the same
 * picture upright to show in the camera's place ([picture], filling the box; made in [pictureMs]).
 */
class ScanImage(val image: ArgbImage, val picture: ImageBitmap? = null, val pictureMs: Double = 0.0)

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
