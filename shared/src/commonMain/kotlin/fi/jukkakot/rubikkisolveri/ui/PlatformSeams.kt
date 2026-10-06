package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

// The few things each platform does its own way; everything else is common code.

/** The device's own colours (Material You), or null where there are none: the Karkki scheme is used. */
@Composable
expect fun platformColorScheme(dark: Boolean, dynamic: Boolean): ColorScheme?

/** Light status-bar icons while shown (the forced-dark scan screens); [off] leaves them as they are. */
@Composable
expect fun LightStatusBarIcons(off: Boolean = false)

/** The device's animation speed factor: 0 = animations off, 1 = normal. */
@Composable
expect fun animationScale(): Float

/** A monotonic clock in milliseconds for the timer. */
expect fun elapsedMillis(): Long

/** A square or rectangular ARGB picture (as `FrameSampler.picture` gives) for `Image`. */
expect fun argbToImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap

/** Dates and times as the user's language writes them. */
expect object LocalFormats {
    /** Short date and time, for history rows. */
    fun shortDateTime(epochMillis: Long, language: String): String

    /** Only the time for today's log lines, otherwise date and time. */
    fun timeOrDateTime(epochMillis: Long, language: String): String
}

/** The language the texts are shown in now ("fi", "en"). */
@Composable
expect fun currentLanguage(): String

/** Keeps the screen from turning off while this is in the composition. */
@Composable
expect fun KeepScreenOn()
