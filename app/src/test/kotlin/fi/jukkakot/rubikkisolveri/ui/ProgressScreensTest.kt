package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.progress.InMemoryProgressRepository
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonsScreen
import fi.jukkakot.rubikkisolveri.ui.progress.HistoryScreen
import fi.jukkakot.rubikkisolveri.ui.progress.TimerScreen
import fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ProgressScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val repo = InMemoryProgressRepository()

    @Test
    fun newScrambleAndStats() {
        // Hold/release/stop is covered by TimerState's unit tests; a running timer asks for every
        // frame, which the Compose test clock cannot idle on.
        val scrambles = ArrayDeque(listOf("R U F", "L D B", "F2 R'"))
        runBlocking {
            for (ms in listOf(10_000L, 12_000L, 11_000L, 30_000L, 9_000L)) repo.addTimed(ms, "x")
        }
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                TimerScreen(repo, onBack = {}, onHistory = {}, onGuidedScramble = {},
                    scrambles = { Notation.parse(scrambles.removeFirst()) })
            }
        }
        compose.onNodeWithText("R U F").assertIsDisplayed()
        compose.onNodeWithText("Uusi sekoitus").performClick()
        compose.onNodeWithText("L D B").assertIsDisplayed()
        compose.onNodeWithText("Ka5").performScrollTo()
        compose.onNodeWithText("11.00").assertExists()
        compose.onNodeWithText("9.00").assertExists()
    }

    @Test
    fun guidedSolveRecorded() {
        var recorded: Triple<SolveMethod, Int, Long>? = null
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(Cube.solved().apply("R"), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
                    onFinished = { m, n, ms -> recorded = Triple(m, n, ms) })
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tein sen").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Tein sen").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(SolveMethod.FAST, recorded?.first)
        assertEquals(1, recorded?.second)
    }

    @Test
    fun historyShowsGuidedSolves() {
        runBlocking { repo.addGuided("LEARN", 150, 600_000) }
        compose.setContent { RubikkiTheme(dynamicColor = false) { HistoryScreen(repo, onBack = {}) } }
        compose.onNodeWithText("Opettele vaiheittain · 150 siirtoa · 10:00.00").assertIsDisplayed()
    }

    @Test
    fun practisedTwice() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) { LessonsScreen(onOpen = {}, onBack = {}, practiceCounts = mapOf(3 to 2)) }
        }
        compose.onNodeWithText("harjoiteltu 2×", substring = true).assertIsDisplayed()
    }
}
