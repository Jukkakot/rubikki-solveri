package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.Quat
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3
import fi.jukkakot.rubikkisolveri.ui.cube3d.holdFor
import kotlin.math.acos

/**
 * The small cube's repeating turn (`scan-feedback` design 5, `scan-next-view` design 5): from [from]
 * to [to] (whole-cube views, as [fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState.rotation]).
 * [side] is the unread side it turns towards the camera; [corner] the next corner it turns towards
 * the camera once every side is read; both null for the tilt to a corner view. [colors] are its
 * stickers (null grey), [needed] the stickers that blink.
 */
data class TurnDemo(
    val side: CubeColor?,
    val from: Quat,
    val to: Quat,
    val corner: Corner? = null,
    val colors: List<CubeColor?> = DEMO_COLORS,
    val needed: Set<Int> = emptySet(),
)

/** Nothing new read this long: the small cube shows how to turn. */
const val DEMO_IDLE_MILLIS = 2_000L

/** The sides in the order the demo asks for them while some are unread: opposite sides apart. */
val SIDE_ORDER = listOf(CubeColor.WHITE, CubeColor.RED, CubeColor.GREEN, CubeColor.YELLOW, CubeColor.ORANGE, CubeColor.BLUE)

/**
 * The turn to show for [state] after [sinceProgress] ms without anything new read; null while there
 * is progress or the scan is complete. While a side is unread (the first in [SIDE_ORDER]) and the
 * cube's hold is known: grey with coloured centres, from the hold to that side facing the camera, the
 * shortest whole-cube turn. Once every side is read: the known stickers in colour, from the hold (or
 * face-on) to the next corner facing the camera, its needed stickers blinking. Otherwise from face-on
 * to the corner view. [previous] stays while the side or corner to show stays, so the loop does not
 * jump as the real cube moves.
 */
fun turnDemo(state: VideoScanState, sinceProgress: Long, previous: TurnDemo? = null): TurnDemo? {
    if (state.complete || sinceProgress < DEMO_IDLE_MILLIS) return null
    val unread = SIDE_ORDER.firstOrNull { it !in state.readSides }
    val hold = state.orientation?.let(::holdFor) ?: state.pose?.let(::holdFor)
    val side = unread?.takeIf { hold != null }
    val corner = state.nextCorner.takeIf { unread == null }
    if (previous != null && previous.side == side && previous.corner == corner) return previous
    if (corner != null) {
        val start = hold ?: Quat.IDENTITY
        val towards = corner.faces.fold(V3.ZERO) { v, f -> v + V3.of(f.normal) }.normalized()
        return TurnDemo(null, start, (towardsCamera(start.rotate(towards)) * start).normalized(), corner, state.stickers.mapIndexed { i, c -> c ?: DEMO_COLORS[i] }, neededAt(state, corner))
    }
    if (side == null || hold == null) return TurnDemo(null, Quat.IDENTITY, CubeScene.DEFAULT_VIEW)
    val normal = hold.rotate(V3.of(ColorScheme.STANDARD.faceOf(side).normal))
    return TurnDemo(side, hold, (towardsCamera(normal) * hold).normalized())
}

/** The stickers of [corner]'s three sides that the scan does not know yet: what its view is needed for. */
fun neededAt(state: VideoScanState, corner: Corner): Set<Int> =
    corner.faces.flatMap { f -> (0 until 9).map { f.ordinal * 9 + it } }.filter { it % 9 != 4 && state.stickers[it] == null }.toSet()

/** The shortest rotation taking direction [n] (camera space, unit) to the camera (+z); half round about the up axis when [n] points away. */
private fun towardsCamera(n: V3): Quat {
    val z = V3(0f, 0f, 1f)
    val axis = n cross z
    val angle = acos((n dot z).coerceIn(-1f, 1f))
    val len = axis.length
    if (len < 1e-4f) return if (n.z > 0) Quat.IDENTITY else Quat.axisAngle(V3(0f, 1f, 0f), angle)
    return Quat.axisAngle(axis * (1f / len), angle)
}

/** The demo cube's stickers while sides are unread: grey, the six centres in their colours, so its hold reads at a glance. */
val DEMO_COLORS: List<CubeColor?> = List(54) { i -> if (i % 9 == 4) ColorScheme.STANDARD[Face.entries[i / 9]] else null }
