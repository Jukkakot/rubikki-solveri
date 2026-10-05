package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MoveGuideTest {
    @get:Rule
    val compose = createComposeRule()

    private val start = Cube.solved().apply("F")
    private val moves = Notation.parse("F' U R")
    private lateinit var state: StepperState
    private var demos = 0

    private fun stepper() {
        compose.mainClock.autoAdvance = false
        compose.setContent { state = rememberStepperState(start, moves, onDemoTick = { demos++ }) }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun automaticDemo() {
        stepper()
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle { assertEquals(moves[0], state.animator.move ?: moves[0]) }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            assertEquals(1, demos)
            assertEquals(start.apply(moves.take(1)), state.animator.cube, "the cube stays after the move")
            assertEquals(0, state.animator.pending)
        }
    }

    @Test
    fun doneAfterTheDemoDoesNotTurnAgain() {
        stepper()
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            state.done()
            assertEquals(1, state.index)
            assertEquals(null, state.animator.move, "no turn is replayed")
            assertEquals(0, state.animator.pending)
            assertEquals(start.apply(moves.take(1)), state.animator.cube)
            assertEquals(1, state.nods)
        }
    }

    @Test
    fun doneDuringTheDemoLandsOnTheNextCube() {
        stepper()
        compose.mainClock.advanceTimeBy(650)
        compose.runOnIdle {
            state.done()
            assertEquals(0, state.animator.pending)
            assertEquals(start.apply(moves.take(1)), state.animator.cube)
        }
    }

    @Test
    fun doneThenBack() {
        stepper()
        compose.runOnIdle { state.done() }
        compose.mainClock.advanceTimeBy(5000)
        compose.runOnIdle {
            assertEquals(1, state.index)
            state.back()
            assertEquals(start.apply(moves.take(1)), state.animator.cube, "back starts from before the current move")
            assertEquals(1, state.animator.pending, "and animates the undo")
            assertEquals(1, state.nods, "back does not nod")
        }
        compose.mainClock.advanceTimeBy(5000)
        compose.runOnIdle {
            assertEquals(0, state.index)
            assertEquals(start.apply(moves.take(1)), state.animator.cube, "then move 1 demos again and stays after it")
        }
    }

    @Test
    fun theLastDoneCelebratesInsteadOfNodding() {
        stepper()
        compose.runOnIdle { repeat(moves.size) { state.done() } }
        compose.runOnIdle {
            assertEquals(true, state.isFinished)
            assertEquals(moves.size - 1, state.nods)
            assertEquals(1, state.celebrations)
        }
        compose.mainClock.advanceTimeBy(3000)
    }

    @Test
    fun notationOn() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(Cube.solved().apply("R"), onBack = {}, onHome = {}, showNotation = true, planner = INLINE_PLANNER)
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Siirto 1/1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("R'").performScrollTo().assertIsDisplayed()
    }
}
