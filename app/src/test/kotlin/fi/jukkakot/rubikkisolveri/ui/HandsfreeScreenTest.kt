package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.guide.GUIDE_CUBE_TAG
import fi.jukkakot.rubikkisolveri.ui.guide.HandsfreeSpeed
import fi.jukkakot.rubikkisolveri.ui.solve.HANDSFREE_STOP_TAG
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HandsfreeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var saved: HandsfreeSpeed? = null

    private fun solve() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    Cube.solved().apply("R U L"), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
                    onHandsfreeSpeed = { saved = it },
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Siirto 1/3").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun tapOnTheCubeConfirms() {
        solve()
        compose.onNodeWithTag(GUIDE_CUBE_TAG).performClick()
        compose.onNodeWithText("Siirto 2/3").assertIsDisplayed()
    }

    @Test
    fun playAdvancesAndATouchStopsIt() {
        solve()
        compose.mainClock.autoAdvance = false
        // ▶ starts at once, with no ready prompt.
        compose.onNodeWithContentDescription("Handsfree").performClick()
        compose.onNodeWithText("Valmis").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(HandsfreeSpeed.NORMAL.quarterMs + 1_500)
        compose.onNodeWithText("Siirto 2/3").assertIsDisplayed()
        // A touch anywhere stops it on the same move and brings ▶ back.
        compose.onNodeWithTag(HANDSFREE_STOP_TAG).performTouchInput { down(center); up() }
        compose.mainClock.advanceTimeBy(20_000)
        compose.onNodeWithText("Siirto 2/3").assertIsDisplayed()
        compose.onNodeWithTag(HANDSFREE_STOP_TAG).assertDoesNotExist()
        compose.onNodeWithContentDescription("Handsfree").assertIsDisplayed()
    }

    @Test
    fun theTimeStartsWithTheMove() {
        solve()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("Handsfree").performClick()
        // The time runs during the demo (half a second's pause and a 0.3 s turn): the move is done
        // after its own time, not after the demo plus its time (2.9 s).
        var waited = 0L
        while (waited < 5_000 && compose.onAllNodesWithText("Siirto 2/3").fetchSemanticsNodes().isEmpty()) {
            compose.mainClock.advanceTimeBy(100)
            waited += 100
        }
        assertTrue(waited in HandsfreeSpeed.NORMAL.quarterMs - 200..HandsfreeSpeed.NORMAL.quarterMs + 300, "moved on after $waited ms")
    }

    @Test
    fun startScreenHandsfreeWithItsSpeed() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    Cube.solved().apply("R U L"), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
                    onHandsfreeSpeed = { saved = it }, startScreen = true,
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Aloita").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Nopea").performClick()
        assertEquals(HandsfreeSpeed.FAST, saved, "the chosen speed is remembered")
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Handsfree").performClick()
        compose.mainClock.advanceTimeBy(HandsfreeSpeed.FAST.quarterMs + 1_000)
        compose.onNodeWithText("Siirto 2/3").assertExists()
    }
}
