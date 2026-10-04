package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.progress.InMemoryProgressRepository
import fi.jukkakot.rubikkisolveri.ui.free.FreeCubeScreen
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonScreen
import fi.jukkakot.rubikkisolveri.ui.progress.TimerScreen
import fi.jukkakot.rubikkisolveri.ui.scan.ScanContent
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each screen with actions shows its main action without scrolling in a short browser window. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "fi-w411dp-h560dp-xxhdpi")
class FitScreensSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { RubikkiTheme(dynamicColor = false) { content() } }
        compose.waitForIdle()
    }

    @Test
    fun freeCube() {
        show { FreeCubeScreen(start = Cube.solved().apply("R U"), onBack = {}) }
        compose.onNodeWithText("Ratkaise").assertIsDisplayed()
    }

    @Test
    fun scan() {
        show { ScanContent(emptyFlow(), torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = {}, preview = {}) }
        compose.onNodeWithContentDescription("Ota kuva").assertIsDisplayed()
    }

    @Test
    fun timer() {
        show {
            TimerScreen(InMemoryProgressRepository(), onBack = {}, onHistory = {}, onGuidedScramble = {}, scrambles = { Notation.parse("R U") })
        }
        compose.onNodeWithContentDescription("Ajastin").assertIsDisplayed()
        compose.onNodeWithText("Ratkaisuja").assertIsDisplayed()
    }

    @Test
    fun lesson() {
        show { LessonScreen(3, onBack = {}, onPractice = {}, onFreeCube = {}, initialPage = 2) }
        compose.onNodeWithText("Toista sarja").assertIsDisplayed()
    }
}
