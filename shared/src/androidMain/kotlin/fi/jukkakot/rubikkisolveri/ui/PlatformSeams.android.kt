package fi.jukkakot.rubikkisolveri.ui

import android.app.Activity
import android.graphics.Bitmap
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import fi.jukkakot.rubikkisolveri.log.LogTime
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlin.time.toKotlinInstant

@Composable
actual fun platformColorScheme(dark: Boolean, dynamic: Boolean): ColorScheme? {
    if (!dynamic) return null
    val context = LocalContext.current
    return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
actual fun LightStatusBarIcons(off: Boolean) {
    // The app's own choice comes back on leaving.
    val view = LocalView.current
    DisposableEffect(view, off) {
        val window = if (off) null else (view.context as? Activity)?.window
        val bars = window?.let { WindowCompat.getInsetsController(it, view) }
        val before = bars?.isAppearanceLightStatusBars
        bars?.isAppearanceLightStatusBars = false
        onDispose { if (before != null) bars.isAppearanceLightStatusBars = before }
    }
}

@Composable
actual fun animationScale(): Float {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
}

actual fun elapsedMillis(): Long = SystemClock.elapsedRealtime()

actual fun argbToImageBitmap(argb: IntArray, width: Int, height: Int): ImageBitmap =
    Bitmap.createBitmap(argb, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()

actual object LocalFormats {
    actual fun shortDateTime(epochMillis: Long, language: String): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.forLanguageTag(language)).format(Date(epochMillis))

    actual fun timeOrDateTime(epochMillis: Long, language: String): String {
        val zone = ZoneId.systemDefault()
        return LogTime.format(Instant.ofEpochMilli(epochMillis).toKotlinInstant(), LocalDate.now(zone), zone, Locale.forLanguageTag(language))
    }
}

@Composable
actual fun currentLanguage(): String = LocalConfiguration.current.locales[0].toLanguageTag()
