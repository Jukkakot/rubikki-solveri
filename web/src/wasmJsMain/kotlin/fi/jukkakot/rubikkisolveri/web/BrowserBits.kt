package fi.jukkakot.rubikkisolveri.web

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Vibration where the browser has it (Chrome on Android); silent elsewhere (Safari, desktops). */
object BrowserHaptics : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        vibrate(if (hapticFeedbackType == HapticFeedbackType.Confirm || hapticFeedbackType == HapticFeedbackType.LongPress) 20 else 8)
    }
}

/** A square ARGB picture as base64 PNG (for the stored scan pictures). */
@OptIn(ExperimentalEncodingApi::class)
fun encodePngBase64(argb: IntArray, size: Int): String {
    val bytes = ByteArray(argb.size * 4)
    for (i in argb.indices) {
        val c = argb[i]
        bytes[i * 4] = (c and 0xFF).toByte()
        bytes[i * 4 + 1] = (c shr 8 and 0xFF).toByte()
        bytes[i * 4 + 2] = (c shr 16 and 0xFF).toByte()
        bytes[i * 4 + 3] = (c ushr 24).toByte()
    }
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(size, size, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(bytes)
    val png = Image.makeFromBitmap(bitmap).encodeToData(EncodedImageFormat.PNG)?.bytes ?: ByteArray(0)
    return Base64.encode(png)
}
