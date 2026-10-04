package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

// The browser has no Material You: the Karkki scheme is always used.
@Composable
actual fun platformColorScheme(dark: Boolean, dynamic: Boolean): ColorScheme? = null

// The page has no status bar of its own.
@Composable
actual fun LightStatusBarIcons(off: Boolean) = Unit

@Composable
actual fun animationScale(): Float = 1f

actual fun elapsedMillis(): Long = TODO("web: performance.now()")

actual fun argbToImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap = TODO("web: skia bitmap")

actual object LocalFormats {
    actual fun shortDateTime(epochMillis: Long, language: String): String = TODO("web: Intl")
    actual fun timeOrDateTime(epochMillis: Long, language: String): String = TODO("web: Intl")
}

@Composable
actual fun currentLanguage(): String = "fi"
