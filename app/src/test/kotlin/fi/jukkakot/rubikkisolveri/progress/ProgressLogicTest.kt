package fi.jukkakot.rubikkisolveri.progress

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgressLogicTest {
    private fun s(seconds: Double, penalty: Penalty = Penalty.NONE) = TimedResult((seconds * 1000).toLong(), penalty)

    @Test
    fun averageOfFive() {
        val results = listOf(s(10.0), s(12.0), s(11.0), s(30.0), s(9.0))
        assertEquals(Average.Time(11_000), SolveStats.averageOf(5, results))
        assertEquals(Average.NotEnough, SolveStats.averageOf(12, results))
        assertEquals(9_000L, SolveStats.best(results))
        assertEquals(14_400L, SolveStats.mean(results))
    }

    @Test
    fun oneDnfIsTheWorst() {
        val results = listOf(s(10.0), s(12.0, Penalty.DNF), s(11.0), s(13.0), s(9.0))
        assertEquals(Average.Time(11_333), SolveStats.averageOf(5, results))
        assertEquals(9_000L, SolveStats.best(results))
    }

    @Test
    fun twoDnfs() {
        val results = listOf(s(10.0, Penalty.DNF), s(12.0, Penalty.DNF), s(11.0), s(13.0), s(9.0))
        assertEquals(Average.Dnf, SolveStats.averageOf(5, results))
    }

    @Test
    fun plusTwo() {
        assertEquals(14_340L, s(12.34, Penalty.PLUS_TWO).effective)
        assertNull(s(12.34, Penalty.DNF).effective)
        assertEquals("12.34", SolveStats.format(12_345))
        assertEquals("1:02.50", SolveStats.format(62_509))
        assertEquals("0.00", SolveStats.format(0))
    }

    @Test
    fun holdReleaseStop() {
        var t = 0L
        val timer = TimerState { t }
        timer.press()
        t = 300; timer.tick()
        assertEquals(TimerState.Phase.HOLDING, timer.phase)
        t = 600; timer.tick()
        assertEquals(TimerState.Phase.READY, timer.phase)
        timer.release()
        assertEquals(TimerState.Phase.RUNNING, timer.phase)
        t = 600 + 12_340
        assertEquals(12_340L, timer.display())
        assertEquals(12_340L, timer.press())
        assertEquals(TimerState.Phase.STOPPED, timer.phase)
        timer.release()
        assertEquals(TimerState.Phase.STOPPED, timer.phase, "lifting the stopping finger does not start again")
    }

    @Test
    fun releasedTooEarly() {
        var t = 0L
        val timer = TimerState { t }
        timer.press()
        t = 200
        timer.release()
        assertEquals(TimerState.Phase.IDLE, timer.phase)
    }
}
