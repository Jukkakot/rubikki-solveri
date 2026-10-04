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
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.guide.GUIDE_CUBE_TAG
import fi.jukkakot.rubikkisolveri.ui.guide.GuideCube
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The fast method's steady guide cube: no swinging view, the mirror, the reset button. */
@RunWith(RobolectricTestRunner::class)
class SteadyGuideTest {
    @get:Rule
    val compose = createComposeRule()

    private val start = Cube.solved().apply("B' R'")
    private lateinit var state: StepperState
    private val view = CubeViewState(CubeScene.DEFAULT_VIEW)

    private fun guide(steady: Boolean) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                state = rememberStepperState(start, Notation.parse("R B"))
                GuideCube(state, steady = steady, mirror = steady, viewState = view)
            }
        }
        compose.mainClock.advanceTimeBy(1000)
    }

    private fun stepToBack() {
        compose.runOnIdle { state.done() }
        compose.mainClock.advanceTimeBy(3000)
    }

    private fun shown(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    private fun resetShown() = compose.onAllNodesWithContentDescription("Palauta asento").fetchSemanticsNodes().isNotEmpty()

    @Test
    fun fastMethodKeepsTheViewAndShowsTheMirror() {
        guide(steady = true)
        val before = view.rotation
        stepToBack()
        compose.runOnIdle {
            assertEquals(1, state.index)
            assertEquals(before, view.rotation)
        }
        assertTrue(shown("Peili"))
        assertTrue(!resetShown())
    }

    @Test
    fun learnMethodSwingsAndHasNoMirror() {
        guide(steady = false)
        val before = view.rotation
        stepToBack()
        compose.runOnIdle { assertNotEquals(before, view.rotation) }
        assertTrue(!shown("Peili"))
    }

    @Test
    fun dragShowsResetAndResetHidesIt() {
        guide(steady = true)
        compose.onNodeWithTag(GUIDE_CUBE_TAG).performTouchInput { swipeLeft() }
        compose.mainClock.advanceTimeBy(500)
        assertTrue(resetShown())
        stepToBack()
        assertTrue(resetShown(), "the next move keeps the user's view")
        compose.onNodeWithContentDescription("Palauta asento").performClick()
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertTrue(view.isAt(CubeScene.DEFAULT_VIEW)) }
        assertTrue(!resetShown())
    }

    @Test
    fun solveScreenFastMethodShowsTheMirror() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(Cube.solved().apply("R"), onBack = {}, onHome = {}, planner = INLINE_PLANNER)
            }
        }
        compose.waitUntil(10_000) { shown("Siirto 1/1") }
        assertTrue(shown("Peili"))
    }
}
