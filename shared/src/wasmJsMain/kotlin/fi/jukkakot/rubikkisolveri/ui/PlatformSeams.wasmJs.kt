package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.text.intl.Locale
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.TimeSource
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// The browser has no Material You: the Karkki scheme is always used.
@Composable
actual fun platformColorScheme(dark: Boolean, dynamic: Boolean): ColorScheme? = null

// The page has no status bar of its own.
@Composable
actual fun LightStatusBarIcons(off: Boolean) = Unit

@Composable
actual fun animationScale(): Float = remember { if (BrowserHooks.reducedMotion()) 0f else 1f }

private val start = TimeSource.Monotonic.markNow()

actual fun elapsedMillis(): Long = start.elapsedNow().inWholeMilliseconds

actual fun argbToImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(argb.size * 4)
    for (i in argb.indices) {
        val c = argb[i]
        bytes[i * 4] = (c and 0xFF).toByte()
        bytes[i * 4 + 1] = (c shr 8 and 0xFF).toByte()
        bytes[i * 4 + 2] = (c shr 16 and 0xFF).toByte()
        bytes[i * 4 + 3] = (c ushr 24).toByte()
    }
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.UNPREMUL))
    bitmap.installPixels(bytes)
    return bitmap.asComposeImageBitmap()
}

actual object LocalFormats {
    actual fun shortDateTime(epochMillis: Long, language: String): String =
        BrowserHooks.formatDateTime(epochMillis, language, false)

    actual fun timeOrDateTime(epochMillis: Long, language: String): String {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(zone).date
        val day = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(zone).date
        return BrowserHooks.formatDateTime(epochMillis, language, day == today)
    }
}

@Composable
actual fun currentLanguage(): String = Locale.current.toLanguageTag()

@Composable
actual fun KeepScreenOn() {
    DisposableEffect(Unit) {
        BrowserHooks.keepScreenOn(true)
        onDispose { BrowserHooks.keepScreenOn(false) }
    }
}
