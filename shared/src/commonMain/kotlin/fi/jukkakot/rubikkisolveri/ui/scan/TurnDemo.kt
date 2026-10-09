package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.Quat
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3
import fi.jukkakot.rubikkisolveri.ui.cube3d.holdFor
import kotlin.math.acos

/**
 * The small cube's repeating turn (`scan-feedback` design 5, `scan-side-balls`): from [from] to [to]
 * (whole-cube views, as [fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState.rotation]). [side] is the
 * next side's colour, which it turns towards the camera; null for the plain tilt to a corner view.
 * [colors] are its stickers (null grey), [needed] the stickers that blink.
 */
data class TurnDemo(
    val side: CubeColor?,
    val from: Quat,
    val to: Quat,
    val colors: List<CubeColor?> = DEMO_COLORS,
    val needed: Set<Int> = emptySet(),
)

/** Nothing new read this long: the small cube shows how to turn. */
const val DEMO_IDLE_MILLIS = 2_000L

/**
 * The turn to show for [state] after [sinceProgress] ms without anything new read; null while there
 * is progress or the scan is complete. With a next side ([VideoScanState.nextSide]): the known
 * stickers in colour (the rest grey, centres coloured), from the cube's hold (or face-on when the scan
 * does not know it) to that side facing the camera by the shortest whole-cube turn, its stickers still
 * needed blinking. Otherwise from face-on to the corner view. [previous] stays while the side stays,
 * so the loop does not jump as the real cube moves.
 */
fun turnDemo(state: VideoScanState, sinceProgress: Long, previous: TurnDemo? = null): TurnDemo? {
    if (state.complete || sinceProgress < DEMO_IDLE_MILLIS) return null
    val next = state.nextSide ?: return TurnDemo(null, Quat.IDENTITY, CubeScene.DEFAULT_VIEW)
    val color = ColorScheme.STANDARD[next]
    if (previous != null && previous.side == color) return previous
    val hold = state.orientation?.let(::holdFor) ?: state.pose?.let(::holdFor) ?: Quat.IDENTITY
    val normal = hold.rotate(V3.of(next.normal))
    val colors = state.stickers.mapIndexed { i, c -> c ?: DEMO_COLORS[i] }
    return TurnDemo(color, hold, (towardsCamera(normal) * hold).normalized(), colors, neededAt(state, next))
}

/** The stickers of [side] (not its centre) the scan does not know or that are not part of the clear cube yet: what its view is needed for. */
fun neededAt(state: VideoScanState, side: Face): Set<Int> =
    (0 until 9).map { side.ordinal * 9 + it }.filter { it % 9 != 4 && (state.stickers[it] == null || it !in state.clear) }.toSet()

/** The shortest rotation taking direction [n] (camera space, unit) to the camera (+z); half round about the up axis when [n] points away. */
private fun towardsCamera(n: V3): Quat {
    val z = V3(0f, 0f, 1f)
    val axis = n cross z
    val angle = acos((n dot z).coerceIn(-1f, 1f))
    val len = axis.length
    if (len < 1e-4f) return if (n.z > 0) Quat.IDENTITY else Quat.axisAngle(V3(0f, 1f, 0f), angle)
    return Quat.axisAngle(axis * (1f / len), angle)
}

/** The demo cube's stickers where nothing is known: grey, the six centres in their colours, so its hold reads at a glance. */
val DEMO_COLORS: List<CubeColor?> = List(54) { i -> if (i % 9 == 4) ColorScheme.STANDARD[Face.entries[i / 9]] else null }
