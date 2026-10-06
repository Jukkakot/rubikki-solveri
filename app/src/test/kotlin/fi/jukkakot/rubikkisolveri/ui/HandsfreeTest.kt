package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.ui.guide.HandsfreeSpeed
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HandsfreeTest {
    @get:Rule
    val compose = createComposeRule()

    private val start = Cube.solved().apply("F")
    private lateinit var state: StepperState
    private var speed by mutableStateOf<HandsfreeSpeed?>(HandsfreeSpeed.NORMAL)
    private var warnings = 0
    private var advances = 0

    // A quarter turn's demo: 0.5 s wait + 0.3 s turn, and a frame or two.
    private val DEMO = 900L

    private fun stepper(moves: String) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            state = rememberStepperState(
                start, Notation.parse(moves),
                handsfree = speed,
                onHandsfreeWarn = { warnings++ },
                onHandsfreeAdvance = { advances++ },
            )
        }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun waitsPerSpeedAndHalfTurnsGetMore() {
        val quarter = Notation.parse("R")[0]
        val half = Notation.parse("R2")[0]
        assertEquals(listOf(4_000L, 2_400L, 1_200L), HandsfreeSpeed.entries.map { it.waitMs(quarter) })
        HandsfreeSpeed.entries.forEach { assertTrue(it.waitMs(half) > it.waitMs(quarter), "$it half turn") }
        assertEquals(HandsfreeSpeed.NORMAL, HandsfreeSpeed.fromName(null))
    }

    @Test
    fun movesOnAfterTheTimeWithAWarningFirst() {
        stepper("F' U R")
        compose.mainClock.advanceTimeBy(DEMO + HandsfreeSpeed.NORMAL.quarterMs - 1_000)
        compose.runOnIdle {
            assertEquals(0, state.index, "still on the first move")
            assertEquals(0, warnings)
        }
        compose.mainClock.advanceTimeBy(700)
        compose.runOnIdle {
            assertEquals(1, warnings, "warned shortly before the end")
            assertEquals(0, state.index)
        }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle {
            assertEquals(1, state.index)
            assertEquals(1, advances)
        }
    }

    @Test
    fun halfTurnWaitsLonger() {
        stepper("F2 U")
        compose.mainClock.advanceTimeBy(DEMO + HandsfreeSpeed.NORMAL.quarterMs + 300)
        compose.runOnIdle { assertEquals(0, state.index, "a half turn is not over at a quarter turn's time") }
        compose.mainClock.advanceTimeBy(HandsfreeSpeed.NORMAL.quarterMs)
        compose.runOnIdle { assertEquals(1, state.index) }
    }

    @Test
    fun stopKeepsTheMoveAndNeverRunsPastTheEnd() {
        stepper("F' U")
        compose.mainClock.advanceTimeBy(DEMO + 1_000)
        compose.runOnIdle { speed = null }
        compose.mainClock.advanceTimeBy(20_000)
        compose.runOnIdle { assertEquals(0, state.index, "stopped: stays on the same move") }
        compose.runOnIdle { speed = HandsfreeSpeed.FAST }
        compose.mainClock.advanceTimeBy(20_000)
        compose.runOnIdle {
            assertEquals(2, state.index)
            assertTrue(state.isFinished)
            assertEquals(2, advances, "no advance past the end")
        }
    }
}
