package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import fi.jukkakot.rubikkisolveri.ui.home.HOME_CUBE_TAG
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
import fi.jukkakot.rubikkisolveri.ui.nav.VideoScanRoute
import fi.jukkakot.rubikkisolveri.ui.nav.TargetRoute
import androidx.navigation.toRoute
import fi.jukkakot.rubikkisolveri.ui.nav.LessonsRoute
import fi.jukkakot.rubikkisolveri.ui.nav.TimerRoute
import fi.jukkakot.rubikkisolveri.ui.nav.FreeCubeRoute
import fi.jukkakot.rubikkisolveri.progress.InMemoryProgressRepository
import fi.jukkakot.rubikkisolveri.progress.ProgressRepository
import kotlinx.coroutines.runBlocking
import fi.jukkakot.rubikkisolveri.ui.nav.RubikkiNavHost
import fi.jukkakot.rubikkisolveri.ui.nav.SettingsRoute
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import fi.jukkakot.rubikkisolveri.ui.theme.ForcedDark
import fi.jukkakot.rubikkisolveri.ui.theme.fredoka
import fi.jukkakot.rubikkisolveri.ui.theme.KarkkiShapes
import fi.jukkakot.rubikkisolveri.ui.theme.nunito
import androidx.compose.material3.Shapes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.Typography
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

    private fun start(crashedLastTime: Boolean = false, progress: ProgressRepository = InMemoryProgressRepository()) {
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
                        homeSpin = false,
                        progress = progress,
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
        compose.onNodeWithText("Versio test").assertExists()
        assertTrue(isOn(HomeRoute))
    }

    @Test
    fun openLessons() {
        start()
        compose.onNodeWithText("Opettele").performClick()
        assertTrue(isOn(LessonsRoute))
    }

    @Test
    fun openTheTimer() {
        start()
        compose.onNodeWithText("Ajanotto").performClick()
        assertTrue(isOn(TimerRoute))
    }

    @Test
    fun openTheFreeCube() {
        start()
        compose.onNodeWithText("Vapaa").performClick()
        assertTrue(isOn(FreeCubeRoute()))
    }

    @Test
    fun noSolveSummaryOnHome() {
        val progress = InMemoryProgressRepository()
        start(progress = progress)
        runBlocking { progress.addTimed(42_310, "R U") }
        compose.waitForIdle()
        compose.onNodeWithText("ratkaisu", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Paras", substring = true).assertDoesNotExist()
    }

    @Test
    fun tapTheCubeToScan() {
        start()
        compose.onNodeWithTag(HOME_CUBE_TAG).performClick()
        assertTrue(isOn(VideoScanRoute()))
    }

    @Test
    fun dragTheCubeDoesNotScan() {
        start()
        compose.onNodeWithTag(HOME_CUBE_TAG).performTouchInput { swipeLeft() }
        assertTrue(isOn(HomeRoute))
    }

    @Test
    fun theScanStartsAsVideo() {
        start()
        compose.onNodeWithContentDescription("Skannaa kuutio").performClick()
        assertTrue(isOn(VideoScanRoute()))
    }

    @Test
    fun backFromAScannedSolutionStartsANewScan() {
        start()
        compose.runOnUiThread {
            nav.navigate(VideoScanRoute())
            nav.navigate(SolveRoute(fi.jukkakot.rubikkisolveri.cube.Cube.solved().apply("R U").toColorString(), fromScan = true))
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Aloita").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Aloita").performClick()
        compose.onNodeWithContentDescription("Takaisin").performClick()
        compose.onNodeWithText("Aloita").assertIsDisplayed()
        compose.onNodeWithContentDescription("Takaisin").performClick()
        assertTrue(isOn(VideoScanRoute()))
    }

    private fun chooseCheckerboardFromHome() {
        compose.onNodeWithText("Kuviot").performClick()
        compose.onNodeWithText("Shakkilauta").performScrollTo().performClick()
        compose.onNodeWithText("Valitse").performClick()
        compose.onNodeWithText("Mistä aloitetaan?").assertIsDisplayed()
    }

    @Test
    fun aPatternFromHomeOnASolvedCube() {
        start()
        chooseCheckerboardFromHome()
        compose.onNodeWithText("Kuutio on jo ratkaistu").performClick()
        assertTrue(isOn(SolveRoute("x")))
        assertEquals("p:CHECKERBOARD", nav.currentBackStackEntry!!.toRoute<SolveRoute>().target)
    }

    @Test
    fun aPatternFromHomeScansFirstAndBackReturnsToThePicker() {
        start()
        chooseCheckerboardFromHome()
        compose.onNodeWithText("Skannaa kuutio").performClick()
        assertTrue(isOn(VideoScanRoute()))
        assertEquals("p:CHECKERBOARD", nav.currentBackStackEntry!!.toRoute<VideoScanRoute>().target)
        compose.runOnUiThread { nav.popBackStack() }
        assertTrue(isOn(TargetRoute("x")))
        compose.onNodeWithText("Mistä aloitetaan?").assertDoesNotExist()
    }

    @Test
    fun openManualInput() {
        start()
        compose.onNodeWithText("Käsin").performClick()
        assertTrue(isOn(ManualInputRoute()))
        compose.onNodeWithText("Etupuoli (1/6)").assertIsDisplayed()
    }

    @Test
    fun validCubeOpensItsSolution() {
        start()
        compose.onNodeWithText("Käsin").performClick()
        compose.onNodeWithContentDescription("Lisää").performClick()
        compose.onNodeWithText("Täytä ratkaistuna").performClick()
        compose.onNodeWithContentDescription("Tarkista").performClick()
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
    fun aboutShowsTheLicences() {
        start()
        compose.onNodeWithContentDescription("Asetukset").performClick()
        compose.onNodeWithText("Tietoja").performScrollTo().performClick()
        // At a glance: no licence text until the button is tapped.
        compose.onNodeWithText("min2phase (kaksivaiheinen ratkaisija)", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Avoimen lähdekoodin lisenssit").performClick()
        compose.onNodeWithText("min2phase (kaksivaiheinen ratkaisija)", substring = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Copyright (c) 2023 Chen Shuang", substring = true).performScrollTo().assertExists()
        compose.onNodeWithText("SIL OPEN FONT LICENSE", substring = true).performScrollTo().assertExists()
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
        compose.onNodeWithText("app.start ver=test").assertIsDisplayed()
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

    @Test
    fun karkkiFontsAndShapesInBothModes() {
        var dark by mutableStateOf(false)
        var typography: Typography? = null
        var shapes: Shapes? = null
        var headings: FontFamily? = null
        var body: FontFamily? = null
        compose.setContent {
            RubikkiTheme(systemDark = dark, dynamicColor = false) {
                typography = MaterialTheme.typography
                shapes = MaterialTheme.shapes
                headings = fredoka()
                body = nunito()
            }
        }
        for (mode in listOf(false, true)) {
            dark = mode
            compose.waitForIdle()
            assertEquals(headings, typography!!.headlineSmall.fontFamily)
            assertEquals(headings, typography!!.titleLarge.fontFamily)
            assertEquals(body, typography!!.bodyLarge.fontFamily)
            assertEquals(body, typography!!.labelLarge.fontFamily)
            assertEquals(KarkkiShapes, shapes)
        }
    }

    @Test
    fun scanIsDarkInALightApp() {
        var app = -1f
        var scan = -1f
        compose.setContent {
            RubikkiTheme(mode = ThemeMode.LIGHT, dynamicColor = false) {
                app = MaterialTheme.colorScheme.background.luminance()
                ForcedDark { scan = MaterialTheme.colorScheme.background.luminance() }
            }
        }
        compose.waitForIdle()
        assertTrue(app > 0.8f)
        assertTrue(scan < 0.2f)
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
                    AppActions(ThemeMode.SYSTEM, {}, AppLanguage.ENGLISH, {}, { emptyList() }, {}, {}, false, {}, "test", homeSpin = false),
                )
            }
        }
        compose.onNodeWithContentDescription("Scan the cube").assertIsDisplayed()
    }
}
