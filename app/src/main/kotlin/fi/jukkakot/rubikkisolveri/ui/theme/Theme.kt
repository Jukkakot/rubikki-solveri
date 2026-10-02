package fi.jukkakot.rubikkisolveri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import fi.jukkakot.rubikkisolveri.settings.ThemeMode

// Fallback schemes for previews and tests; on the phone (Android 12+) the dynamic Material You colours win.
private val LightFallback = lightColorScheme(
    primary = Color(0xFF2B5EA7),
    secondary = Color(0xFF55606F),
    tertiary = Color(0xFF2E7D4F),
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    secondary = Color(0xFFBDC7DA),
    tertiary = Color(0xFF8FD6A6),
)

fun isDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun RubikkiTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    systemDark: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val dark = isDark(mode, systemDark)
    val context = LocalContext.current
    val scheme: ColorScheme = when {
        dynamicColor ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkFallback
        else -> LightFallback
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
