package fi.jukkakot.rubikkisolveri.ui.guide

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeAnimator
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Steps through [moves] starting from [start]: [index] moves are done. The [animator] shows the
 * cube; [demo] plays the current move from before it and leaves the cube after it, as the user's
 * cube will be once turned. [done] does not replay the turn: the cube jumps to the next step and
 * [nods] (or, after the last move, [celebrations]) counts up for the view to react.
 */
@Stable
class StepperState(
    val start: Cube,
    val moves: List<Move>,
    initialIndex: Int,
    val animator: CubeAnimator,
    private val scope: CoroutineScope,
    private val onDemoTick: () -> Unit = {},
) {
    var index by mutableIntStateOf(initialIndex)
        private set

    /** Counts steps reached by [done] (not the last): the cube nods for each. */
    var nods by mutableIntStateOf(0)
        private set

    /** Counts finishes reached by [done]: the solved cube celebrates. */
    var celebrations by mutableIntStateOf(0)
        private set

    val current: Move? get() = moves.getOrNull(index)
    val isFinished: Boolean get() = index >= moves.size

    fun cubeAt(i: Int): Cube = start.apply(moves.take(i))

    /** True while a demo plays: its half turn ticks after the first quarter step. */
    private var demoing = false

    init {
        animator.onHalfway = { if (demoing) onDemoTick() }
    }

    /** A demo may still be running or waiting: start from the real state of this step. */
    private fun settle() {
        demoing = false
        if (animator.target != cubeAt(index) || animator.pending > 0) animator.snapTo(cubeAt(index))
    }

    fun done() {
        if (current == null) return
        demoing = false
        val next = cubeAt(index + 1)
        if (animator.target != next || animator.pending > 0) animator.snapTo(next)
        index++
        if (isFinished) celebrations++ else nods++
    }

    fun back() {
        if (index == 0) return
        settle()
        index--
        animator.play(moves[index].inverse)
    }

    fun demo() {
        val move = current ?: return
        settle()
        val shownAt = index
        animator.play(move)
        demoing = true
        scope.launch {
            snapshotFlow { animator.pending }.first { it == 0 }
            if (index != shownAt || !demoing) return@launch
            demoing = false
            onDemoTick()
        }
    }

    /** Waits for the cube to settle on the new step, then demos it once. */
    suspend fun autoDemo() {
        if (isFinished || animator.isInstant) return
        val at = index
        snapshotFlow { animator.pending }.first { it == 0 }
        delay(AUTO_DEMO_DELAY_MS)
        if (index == at && animator.pending == 0 && animator.cube == cubeAt(at)) demo()
    }

    companion object {
        const val AUTO_DEMO_DELAY_MS = 1_500L
    }
}

@Composable
fun rememberStepperState(start: Cube, moves: List<Move>, onDemoTick: () -> Unit = {}): StepperState {
    var savedIndex by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val animator = rememberCubeAnimator(start.apply(moves.take(savedIndex)))
    val state = remember { StepperState(start, moves, savedIndex, animator, scope, onDemoTick) }
    LaunchedEffect(state.index) {
        savedIndex = state.index
        state.autoDemo()
    }
    return state
}
