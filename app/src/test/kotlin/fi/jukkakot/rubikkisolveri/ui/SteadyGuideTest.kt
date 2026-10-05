package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Sequences
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.guide.GUIDE_CUBE_TAG
import fi.jukkakot.rubikkisolveri.ui.guide.GuideCube
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod
import fi.jukkakot.rubikkisolveri.ui.solve.SolvePlan
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The guide cube's one view: never turning by itself, the reset button after a drag, in both methods. */
@RunWith(RobolectricTestRunner::class)
class SteadyGuideTest {
    @get:Rule
    val compose = createComposeRule()

    private val moves = Notation.parse("R B L D")
    private val start = Cube.solved().apply(Sequences.inverse(moves))
    private lateinit var state: StepperState
    private val view = CubeViewState(CubeScene.DEFAULT_VIEW)

    private fun guide() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                state = rememberStepperState(start, moves)
                GuideCube(state, mirror = true, viewState = view)
            }
        }
        compose.mainClock.advanceTimeBy(1000)
    }

    private fun step() {
        compose.runOnIdle { state.done() }
        compose.mainClock.advanceTimeBy(3000)
    }

    private fun resetShown() = compose.onAllNodesWithContentDescription("Palauta asento").fetchSemanticsNodes().isNotEmpty()

    @Test
    fun theViewStaysOverRBLAndD() {
        guide()
        repeat(3) {
            step()
            compose.runOnIdle { assertEquals(CubeScene.DEFAULT_VIEW, view.rotation) }
        }
        compose.runOnIdle { assertEquals(3, state.index) }
        assertTrue(!resetShown())
    }

    @Test
    fun dragShowsResetAndResetHidesIt() {
        guide()
        compose.onNodeWithTag(GUIDE_CUBE_TAG).performTouchInput { swipeLeft() }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame() // nothing else animates now: one more frame shows the button
        assertTrue(resetShown())
        step()
        assertTrue(resetShown(), "the next move keeps the user's view")
        compose.onNodeWithContentDescription("Palauta asento").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertTrue(view.isAt(CubeScene.DEFAULT_VIEW)) }
        assertTrue(!resetShown())
    }

    @Test
    fun learnMethodHasTheResetButton() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    start, onBack = {}, onHome = {}, planner = { _, _ -> SolvePlan.Ready(moves, null) },
                    initialMethod = SolveMethod.LEARN,
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Siirto 1/4").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(GUIDE_CUBE_TAG).performTouchInput { swipeLeft() }
        compose.waitUntil(5_000) { resetShown() }
    }
}
