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
 * cube; [demo] plays the current move and returns the cube to before it.
 */
@Stable
class StepperState(
    val start: Cube,
    val moves: List<Move>,
    initialIndex: Int,
    val animator: CubeAnimator,
    private val scope: CoroutineScope,
    private val onDemoEnd: () -> Unit = {},
) {
    var index by mutableIntStateOf(initialIndex)
        private set

    val current: Move? get() = moves.getOrNull(index)
    val isFinished: Boolean get() = index >= moves.size

    fun cubeAt(i: Int): Cube = start.apply(moves.take(i))

    /** A demo may still be running or waiting: start from the real state of this step. */
    private fun settle() {
        if (animator.target != cubeAt(index) || animator.pending > 0) animator.snapTo(cubeAt(index))
    }

    fun done() {
        val move = current ?: return
        settle()
        animator.play(move)
        index++
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
        scope.launch {
            snapshotFlow { animator.pending }.first { it == 0 }
            onDemoEnd()
            delay(DEMO_PAUSE_MS)
            if (index == shownAt && animator.pending == 0) animator.snapTo(cubeAt(index))
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
        const val DEMO_PAUSE_MS = 700L
        const val AUTO_DEMO_DELAY_MS = 500L
    }
}

@Composable
fun rememberStepperState(start: Cube, moves: List<Move>, onDemoEnd: () -> Unit = {}): StepperState {
    var savedIndex by rememberSaveable { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val animator = rememberCubeAnimator(start.apply(moves.take(savedIndex)))
    val state = remember { StepperState(start, moves, savedIndex, animator, scope, onDemoEnd) }
    LaunchedEffect(state.index) {
        savedIndex = state.index
        state.autoDemo()
    }
    return state
}
