package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import fi.jukkakot.rubikkisolveri.settings.AppLanguage
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import fi.jukkakot.rubikkisolveri.ui.nav.AppActions
import fi.jukkakot.rubikkisolveri.ui.nav.HomeRoute
import fi.jukkakot.rubikkisolveri.ui.nav.LogRoute
import fi.jukkakot.rubikkisolveri.ui.nav.ManualInputRoute
import fi.jukkakot.rubikkisolveri.ui.nav.SolveRoute
import fi.jukkakot.rubikkisolveri.ui.nav.ScanRoute
import fi.jukkakot.rubikkisolveri.ui.nav.LessonsRoute
import fi.jukkakot.rubikkisolveri.ui.nav.RubikkiNavHost
import fi.jukkakot.rubikkisolveri.ui.nav.SettingsRoute
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ShellTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var nav: TestNavHostController
    private val chosenLanguages = mutableListOf<AppLanguage>()
    private val chosenThemes = mutableListOf<ThemeMode>()
    private val notationChoices = mutableListOf<Boolean>()
    private var logLines = mutableListOf("2026-10-02T12:00:00Z INFO app.start ver=test")

    private fun start(crashedLastTime: Boolean = false) {
        compose.setContent {
            nav = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            RubikkiTheme(dynamicColor = false) {
                RubikkiNavHost(
                    nav,
                    AppActions(
                        themeMode = ThemeMode.SYSTEM,
                        onThemeMode = { chosenThemes += it },
                        language = AppLanguage.FINNISH,
                        onLanguage = { chosenLanguages += it },
                        readLog = { logLines.toList() },
                        clearLog = { logLines.clear() },
                        shareLog = {},
                        crashedLastTime = crashedLastTime,
                        onCrashNoticeShown = {},
                        version = "test",
                        onShowNotation = { notationChoices += it },
                    ),
                )
            }
        }
    }

    private fun isOn(route: Any): Boolean {
        compose.waitForIdle()
        return nav.currentDestination?.hasRoute(route::class) == true
    }

    @Test
    fun appStartsOnHome() {
        start()
        compose.onNodeWithText("Rubikki Solveri").assertIsDisplayed()
        assertTrue(isOn(HomeRoute))
    }

    @Test
    fun openLessons() {
        start()
        compose.onNodeWithText("Opettele ratkaisemaan").performClick()
        assertTrue(isOn(LessonsRoute))
    }

    @Test
    fun openTheScan() {
        start()
        compose.onNodeWithText("Skannaa kuutio").performClick()
        assertTrue(isOn(ScanRoute))
    }

    @Test
    fun openManualInput() {
        start()
        compose.onNodeWithText("Syötä värit käsin").performClick()
        assertTrue(isOn(ManualInputRoute()))
        compose.onNodeWithText("Etupuoli (1/6)").assertIsDisplayed()
    }

    @Test
    fun validCubeOpensItsSolution() {
        start()
        compose.onNodeWithText("Syötä värit käsin").performClick()
        compose.onNodeWithContentDescription("Lisää").performClick()
        compose.onNodeWithText("Täytä ratkaistuna").performClick()
        compose.onNodeWithText("Tarkista").performScrollTo().performClick()
        assertTrue(isOn(SolveRoute("x")))
    }

    @Test
    fun openAndLeaveSettings() {
        start()
        compose.onNodeWithContentDescription("Asetukset").performClick()
        assertTrue(isOn(SettingsRoute))
        compose.onNodeWithContentDescription("Takaisin").performClick()
        assertTrue(isOn(HomeRoute))
    }

    @Test
    fun switchToEnglish() {
        start()
        compose.onNodeWithContentDescription("Asetukset").performClick()
        compose.onNodeWithText("English").performClick()
        assertEquals(listOf(AppLanguage.ENGLISH), chosenLanguages)
    }

    @Test
    fun forcedLightIsChosenInSettings() {
        start()
        compose.onNodeWithContentDescription("Asetukset").performClick()
        compose.onNodeWithText("Vaalea").performClick()
        assertEquals(listOf(ThemeMode.LIGHT), chosenThemes)
    }

    @Test
    fun notationSwitch() {
        start()
        compose.onNodeWithContentDescription("Asetukset").performClick()
        compose.onNodeWithText("Näytä siirtomerkinnät").performScrollTo().performClick()
        assertEquals(listOf(true), notationChoices)
    }

    @Test
    fun clearTheLog() {
        start()
        compose.onNodeWithContentDescription("Asetukset").performClick()
        compose.onNodeWithText("Loki").performScrollTo().performClick()
        assertTrue(isOn(LogRoute))
        compose.onNodeWithText("2026-10-02T12:00:00Z INFO app.start ver=test").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tyhjennä").performClick()
        compose.onNodeWithText("Loki on tyhjä.").assertIsDisplayed()
        assertTrue(logLines.isEmpty())
    }

    @Test
    fun crashThenRestartOffersTheLog() {
        start(crashedLastTime = true)
        compose.onNodeWithText("Sovellus kaatui viime kerralla.").assertIsDisplayed()
        compose.onNodeWithText("Näytä loki").performClick()
        compose.waitForIdle()
        assertTrue(isOn(LogRoute))
    }
}

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun backgroundLuminance(mode: ThemeMode, systemDark: Boolean): Float {
        var luminance = -1f
        compose.setContent {
            RubikkiTheme(mode = mode, systemDark = systemDark, dynamicColor = false) {
                luminance = MaterialTheme.colorScheme.background.luminance()
            }
        }
        compose.waitForIdle()
        return luminance
    }

    @Test
    fun followThePhone() {
        assertTrue(backgroundLuminance(ThemeMode.SYSTEM, systemDark = true) < 0.2f)
    }

    @Test
    fun forcedLight() {
        assertTrue(backgroundLuminance(ThemeMode.LIGHT, systemDark = true) > 0.8f)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class EnglishTextsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun homeIsInEnglish() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                val nav = TestNavHostController(LocalContext.current).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }
                RubikkiNavHost(
                    nav,
                    AppActions(ThemeMode.SYSTEM, {}, AppLanguage.ENGLISH, {}, { emptyList() }, {}, {}, false, {}, "test"),
                )
            }
        }
        compose.onNodeWithText("Scan the cube").assertIsDisplayed()
    }
}
