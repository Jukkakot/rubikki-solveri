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
import kotlinx.coroutines.launch

/**
 * Plays moves on a cube one after another. [cube] is the state before the move in progress
 * ([move], [progress] 0..1); when a move finishes it is applied to [cube]. [durationScale] is the
 * phone's animator scale: 0 applies moves at once.
 */
@Stable
class CubeAnimator(initial: Cube, private val scope: CoroutineScope, private val durationScale: Float = 1f) {
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

    private var generation = 0
    private val queue = Channel<Pair<Int, Pair<Move, Float>>>(Channel.UNLIMITED)

    init {
        scope.launch {
            for ((gen, item) in queue) {
                if (gen != generation) continue
                val (next, speed) = item
                val millis = ((if (next.quarterTurns == 2) HALF_MS else QUARTER_MS) * durationScale / speed).toInt()
                if (millis > 0) {
                    move = next
                    animatable.snapTo(0f)
                    animatable.animateTo(1f, tween(millis, easing = FastOutSlowInEasing))
                }
                if (gen == generation) {
                    cube = cube.apply(next)
                    move = null
                    animatable.snapTo(0f)
                    pending--
                }
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
        const val HALF_MS = 450
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
