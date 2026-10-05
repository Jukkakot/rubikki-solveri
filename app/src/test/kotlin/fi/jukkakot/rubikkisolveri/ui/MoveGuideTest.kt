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
    private val AUTO_DEMO = StepperState.AUTO_DEMO_DELAY_MS

    private fun stepper() {
        compose.mainClock.autoAdvance = false
        compose.setContent { state = rememberStepperState(start, moves, onDemoTick = { demos++ }) }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun automaticDemo() {
        stepper()
        // First the cube before the move (with its arrow) for a moment, then the demo.
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle {
            assertEquals(null, state.animator.move)
            assertEquals(start, state.animator.cube)
        }
        compose.mainClock.advanceTimeBy(350)
        compose.runOnIdle { assertEquals(moves[0], state.animator.move) }
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
        }
    }

    @Test
    fun doneDuringTheDemoLandsOnTheNextCube() {
        stepper()
        compose.mainClock.advanceTimeBy(AUTO_DEMO + 150)
        compose.runOnIdle {
            state.done()
            assertEquals(0, state.animator.pending)
            assertEquals(start.apply(moves.take(1)), state.animator.cube)
        }
    }

    @Test
    fun theDemoRepeatsAfterThreeSeconds() {
        stepper()
        compose.mainClock.advanceTimeBy(1_500)
        compose.runOnIdle { assertEquals(1, demos) }
        compose.mainClock.advanceTimeBy(StepperState.REPEAT_MS + 500)
        compose.runOnIdle {
            assertEquals(2, demos, "played again")
            assertEquals(start.apply(moves.take(1)), state.animator.cube)
        }
    }

    @Test
    fun showPlaysAfterATapDuringATurn() {
        stepper()
        // The automatic demo is turning: tap show again, then show plays a full turn.
        compose.mainClock.advanceTimeBy(AUTO_DEMO + 150)
        compose.runOnIdle {
            assertEquals(moves[0], state.animator.move, "the demo is turning")
            state.demo()
        }
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { assertEquals(moves[0], state.animator.move, "show turns at once") }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle {
            assertEquals(0, state.animator.pending)
            assertEquals(start.apply(moves.take(1)), state.animator.cube)
            // And later moves still play.
            state.done()
        }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle() // the next step's demo starts from here
        compose.mainClock.advanceTimeBy(AUTO_DEMO + 150)
        compose.runOnIdle { assertEquals(moves[1], state.animator.move, "the next demo turns") }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle { assertEquals(start.apply(moves.take(2)), state.animator.cube) }
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
