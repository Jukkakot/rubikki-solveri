package fi.jukkakot.rubikkisolveri.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import fi.jukkakot.rubikkisolveri.ui.LightStatusBarIcons
import fi.jukkakot.rubikkisolveri.ui.platformColorScheme
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

// Fallback schemes for previews and tests: the tonal palette Android derives from its fallback seed
// #1B6EF3 (what a black wallpaper gives). On the phone the dynamic Material You colours win.
private val LightFallback = lightColorScheme(
    primary = Color(0xFF415F91),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF284777),
    secondary = Color(0xFF565F71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF3E4759),
    tertiary = Color(0xFF705575),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFAD8FD),
    onTertiaryContainer = Color(0xFF573E5C),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE0E2EC),
    onSurfaceVariant = Color(0xFF44474E),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3FA),
    surfaceContainer = Color(0xFFEDEDF4),
    surfaceContainerHigh = Color(0xFFE7E8EE),
    surfaceContainerHighest = Color(0xFFE2E2E9),
)

private val DarkFallback = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    onPrimary = Color(0xFF0A305F),
    primaryContainer = Color(0xFF284777),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDDBCE0),
    onTertiary = Color(0xFF3F2844),
    tertiaryContainer = Color(0xFF573E5C),
    onTertiaryContainer = Color(0xFFFAD8FD),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474E),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474E),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF191C20),
    surfaceContainer = Color(0xFF1D2024),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33353A),
)

// Both fonts are variable (SIL OFL, bundled in composeResources/font); each weight picks its point on the axis.
@OptIn(ExperimentalTextApi::class)
@Composable
private fun variable(res: FontResource, vararg weights: FontWeight) = FontFamily(
    weights.map { Font(res, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight))) },
)

private val WEIGHTS = arrayOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** Chunky rounded headings (display, headline, title). */
@Composable
fun fredoka(): FontFamily = variable(Res.font.fredoka, *WEIGHTS)

/** Rounded text (body, label). */
@Composable
fun nunito(): FontFamily = variable(Res.font.nunito, *WEIGHTS)

@Composable
fun karkkiTypography(): Typography {
    val heading = fredoka()
    val body = nunito()
    fun TextStyle.heading() = copy(fontFamily = heading, fontWeight = FontWeight.SemiBold)
    fun TextStyle.text(weight: FontWeight? = fontWeight) = copy(fontFamily = body, fontWeight = weight)
    return Typography().run {
        copy(
            displayLarge = displayLarge.heading(),
            displayMedium = displayMedium.heading(),
            displaySmall = displaySmall.heading(),
            headlineLarge = headlineLarge.heading(),
            headlineMedium = headlineMedium.heading(),
            headlineSmall = headlineSmall.heading(),
            titleLarge = titleLarge.heading(),
            titleMedium = titleMedium.heading(),
            titleSmall = titleSmall.heading(),
            bodyLarge = bodyLarge.text(),
            bodyMedium = bodyMedium.text(),
            bodySmall = bodySmall.text(),
            labelLarge = labelLarge.text(FontWeight.Bold),
            labelMedium = labelMedium.text(FontWeight.Bold),
            labelSmall = labelSmall.text(FontWeight.Bold),
        )
    }
}

/** The M3 scale raised for the soft Karkki look; buttons stay pills. */
val KarkkiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
    val scheme: ColorScheme = platformColorScheme(dark, dynamicColor) ?: if (dark) DarkFallback else LightFallback
    CompositionLocalProvider(LocalDynamicColor provides dynamicColor) {
        MaterialTheme(colorScheme = scheme, typography = karkkiTypography(), shapes = KarkkiShapes, content = content)
    }
}

/** Whether the theme uses the phone's dynamic colours, so a forced-dark part matches the rest. */
private val LocalDynamicColor = staticCompositionLocalOf { true }

/**
 * [content] in the dark variant of the current theme (the scan screens: the camera picture and the
 * sticker colours stand out on dark). Leaving it returns to the app theme.
 */
@Composable
fun ForcedDark(content: @Composable () -> Unit) {
    // Light status-bar icons on the dark screen.
    LightStatusBarIcons()
    RubikkiTheme(mode = ThemeMode.DARK, dynamicColor = LocalDynamicColor.current) {
        Surface(color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground, content = content)
    }
}

/** [content] forced dark when [dark], else in the app theme. */
@Composable
fun DarkIf(dark: Boolean, content: @Composable () -> Unit) {
    if (dark) ForcedDark(content) else content()
}
