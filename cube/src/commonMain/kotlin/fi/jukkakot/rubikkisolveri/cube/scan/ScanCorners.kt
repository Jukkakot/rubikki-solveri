package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge

/**
 * The video scan's corner row (`scan-next-view`): the cube's eight corner positions, named by the
 * centres that meet there, so all eight can be shown before anything is read.
 */
object ScanCorners {
    /**
     * The row's order: the four white-side corners, then the four yellow-side ones, each going round
     * the same way ([Corner.entries]), so the row reads the same in every scan.
     */
    val ROW: List<Corner> = Corner.entries

    /**
     * [corner]'s colours as its picture shows it: the white or yellow side on top, then the side on
     * the right and the one on the left, seen from outside with that side up ([Corner.faces] is
     * clockwise round the corner).
     */
    fun colors(corner: Corner, scheme: ColorScheme = ColorScheme.STANDARD): List<CubeColor> = corner.faces.map { scheme[it] }

    /** [corner]'s stickers and those of the three edges that meet at it: what a read corner stands for. */
    fun stickersOf(corner: Corner): List<Int> =
        corner.stickers + Edge.entries.filter { e -> corner.faces.containsAll(e.faces) }.flatMap { it.stickers }

    /**
     * The corners read in [state] (`scan-corner-ticks`): those whose [stickersOf] are all part of the
     * clear cube ([VideoScanState.clear]); all of them once the scan is complete, never all eight
     * before. When every corner would count but the cube is not complete yet, the one touching the
     * open doubt stays unread ([held]).
     */
    fun read(state: VideoScanState, previous: Corner? = null): Set<Corner> {
        if (state.complete || state.finished) return ROW.toSet()
        val read = ROW.filter { c -> stickersOf(c).all { it in state.clear } }.toSet()
        return if (read.size < ROW.size) read else read - held(state, previous)
    }

    /**
     * The corner kept unread while only the complete flag is missing: the one whose sides hold the
     * most doubt ([NextCorner.score]: unclear stickers, open turns), [previous] on a tie (so the
     * pulse does not jump), else the earlier one in the row.
     */
    private fun held(state: VideoScanState, previous: Corner?): Corner {
        val scores = ROW.associateWith { NextCorner.score(state, it) }
        val top = scores.values.max()
        return previous?.takeIf { scores[it] == top } ?: ROW.first { scores[it] == top }
    }

    /** [corners] as an 8-bit mask in [ROW] order (the browser's state text). */
    fun mask(corners: Set<Corner>): Int = ROW.indices.filter { ROW[it] in corners }.sumOf { 1 shl it }

    /** The corners of [mask]. */
    fun ofMask(mask: Int): Set<Corner> = ROW.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
}

/**
 * The corner worth showing next (`scan-next-view` design 3): among the unread corners, the one whose
 * view (its three sides) would settle the most.
 */
object NextCorner {
    /** A side whose turn is still open counts this many stickers. */
    const val OPEN_TURN = 3

    /** Another corner takes over from the previous choice only when it scores at least this many times more. */
    const val SWITCH = 1.5

    /**
     * The next corner for [state], keeping [previous] while it is unread and no other corner scores
     * [SWITCH] times more; null once every corner is read. Ties go to the earlier corner in the row.
     */
    fun choose(state: VideoScanState, previous: Corner?): Corner? {
        val read = ScanCorners.read(state, previous)
        val unread = ScanCorners.ROW.filter { it !in read }
        if (unread.isEmpty()) return null
        val scores = unread.associateWith { score(state, it) }
        val best = unread.maxBy { scores.getValue(it) }
        val kept = previous?.takeIf { it in scores } ?: return best
        return if (scores.getValue(best) >= SWITCH * scores.getValue(kept)) best else kept
    }

    /**
     * What [corner]'s view would settle: its three sides' stickers that are unknown or in doubt (on a
     * side not confirmed), plus [OPEN_TURN] for each of those sides whose turn is still open.
     */
    fun score(state: VideoScanState, corner: Corner): Int = corner.faces.sumOf { face ->
        val doubt = (0 until 9).count { n -> state.stickers.getOrNull(face.ordinal * 9 + n) == null || face !in state.confirmed }
        doubt + if (face in state.openTurns) OPEN_TURN else 0
    }
}
