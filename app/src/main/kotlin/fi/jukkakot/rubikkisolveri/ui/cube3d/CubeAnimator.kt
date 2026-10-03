package fi.jukkakot.rubikkisolveri.ui.cube3d

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.provider.Settings
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Move
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays moves on a cube one after another. [cube] is the state before the step in progress
 * ([move], [progress] 0..1); when a step finishes it is applied to [cube]. A half turn plays as two
 * quarter steps, so [move] is then the quarter move. [durationScale] is the phone's animator
 * scale: 0 applies moves at once.
 */
@Stable
class CubeAnimator(initial: Cube, private val scope: CoroutineScope, private val durationScale: Float = 1f) {
    /** True when the phone's animations are off: moves apply at once. */
    val isInstant: Boolean get() = durationScale == 0f

    var cube: Cube by mutableStateOf(initial)
        private set
    var move: Move? by mutableStateOf(null)
        private set
    private val animatable = Animatable(0f)
    val progress: Float get() = animatable.value

    /** Moves waiting or playing. */
    var pending: Int by mutableIntStateOf(0)
        private set

    /** The cube after every queued move. */
    var target: Cube by mutableStateOf(initial)
        private set

    /** Called when the first quarter step of a half turn ends; the cube then shows that state. */
    var onHalfway: ((Move) -> Unit)? = null

    private var generation = 0
    private val queue = Channel<Pair<Int, Pair<Move, Float>>>(Channel.UNLIMITED)

    init {
        scope.launch {
            for ((gen, item) in queue) {
                if (gen != generation) continue
                val (next, speed) = item
                val millis = (QUARTER_MS * durationScale / speed).toInt()
                // A half turn plays as two quarter steps the same way with a pause between.
                val steps = if (millis > 0 && next.quarterTurns == 2) List(2) { Move(next.layer, 1) } else listOf(next)
                for ((i, step) in steps.withIndex()) {
                    if (i > 0) delay((STEP_PAUSE_MS * durationScale / speed).toLong())
                    if (gen != generation) break
                    if (millis > 0) {
                        move = step
                        animatable.snapTo(0f)
                        animatable.animateTo(1f, tween(millis, easing = FastOutSlowInEasing))
                    }
                    if (gen != generation) break
                    cube = cube.apply(step)
                    move = null
                    animatable.snapTo(0f)
                    if (i < steps.lastIndex) onHalfway?.invoke(next)
                }
                if (gen == generation) pending--
            }
        }
    }

    fun play(move: Move, speed: Float = 1f) = play(listOf(move), speed)

    fun play(moves: List<Move>, speed: Float = 1f) {
        for (m in moves) {
            pending++
            target = target.apply(m)
            queue.trySend(generation to (m to speed))
        }
    }

    /** Drops queued moves and shows [cube] at once. */
    fun snapTo(cube: Cube) {
        generation++
        pending = 0
        move = null
        this.cube = cube
        target = cube
        scope.launch { animatable.snapTo(0f) }
    }

    companion object {
        const val QUARTER_MS = 300
        const val STEP_PAUSE_MS = 250
    }
}

@Composable
fun rememberCubeAnimator(initial: Cube): CubeAnimator {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    return remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        CubeAnimator(initial, scope, scale)
    }
}
