package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face

/**
 * The video scan's side row (`scan-side-balls`): the cube's six sides, each named by its centre's
 * colour, so all six can be shown before anything is read.
 */
object ScanSides {
    /** The row's order: white, red, green, yellow, orange, blue (opposite sides apart; [Face.entries] URFDLB). */
    val ROW: List<Face> = Face.entries

    /** [side]'s centre colour: its ball's colour and its name in the status line. */
    fun color(side: Face, scheme: ColorScheme = ColorScheme.STANDARD): CubeColor = scheme[side]

    /** [side]'s nine stickers (net order). */
    fun stickersOf(side: Face): List<Int> = (0 until 9).map { side.ordinal * 9 + it }

    /**
     * The sides done in [state]: those whose nine stickers are all part of the clear cube
     * ([VideoScanState.clear]); all of them once the scan is complete, never all six before. When
     * every side would count but the cube is not complete yet, the one with the open doubt stays
     * undone ([held]).
     */
    fun done(state: VideoScanState, previous: Face? = null): Set<Face> {
        if (state.complete || state.finished) return ROW.toSet()
        val done = ROW.filter { f -> stickersOf(f).all { it in state.clear } }.toSet()
        return if (done.size < ROW.size) done else done - held(state, previous)
    }

    /**
     * The side kept undone while only the complete flag is missing: the one with the most open doubt
     * ([NextSide.score]), [previous] on a tie (so the pulse does not jump), else the earlier one in the row.
     */
    private fun held(state: VideoScanState, previous: Face?): Face {
        val scores = ROW.associateWith { NextSide.score(state, it) }
        val top = scores.values.max()
        return previous?.takeIf { scores[it] == top } ?: ROW.first { scores[it] == top }
    }

    /** [sides] as a 6-bit mask in [ROW] order (the browser's state text). */
    fun mask(sides: Set<Face>): Int = ROW.indices.filter { ROW[it] in sides }.sumOf { 1 shl it }

    /** The sides of [mask]. */
    fun ofMask(mask: Int): Set<Face> = ROW.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
}

/**
 * The side worth showing next (`scan-side-balls`): among the sides not done, one never read first,
 * else the one whose showing would settle the most, held steady.
 */
object NextSide {
    /** A side whose turn is still open counts this many stickers. */
    const val OPEN_TURN = 3

    /** Another side takes over from the previous choice only when it scores at least this many times more. */
    const val SWITCH = 1.5

    /**
     * The next side for [state]: among the sides not done, the unread ones ([VideoScanState.readSides])
     * if any, else all of them; [previous] kept while it is among those and no other scores [SWITCH]
     * times more; null once every side is done. Ties go to the earlier side in the row.
     */
    fun choose(state: VideoScanState, previous: Face?, scheme: ColorScheme = ColorScheme.STANDARD): Face? {
        val done = ScanSides.done(state, previous)
        val open = ScanSides.ROW.filter { it !in done }
        if (open.isEmpty()) return null
        val unread = open.filter { scheme[it] !in state.readSides }
        val candidates = unread.ifEmpty { open }
        val scores = candidates.associateWith { score(state, it) }
        val best = candidates.maxBy { scores.getValue(it) }
        val kept = previous?.takeIf { it in scores } ?: return best
        val (b, k) = scores.getValue(best) to scores.getValue(kept)
        return if (b > k && b >= SWITCH * k) best else kept
    }

    /** What showing [side] would settle: its stickers not part of the clear cube, plus [OPEN_TURN] if its turn is open. */
    fun score(state: VideoScanState, side: Face): Int =
        ScanSides.stickersOf(side).count { it !in state.clear } + if (side in state.openTurns) OPEN_TURN else 0
}
