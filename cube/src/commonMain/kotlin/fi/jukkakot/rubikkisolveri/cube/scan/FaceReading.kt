package fi.jukkakot.rubikkisolveri.cube.scan

import kotlin.math.abs

/**
 * One face found in a frame: the nine readings in reading order (row by row as seen, [u] along
 * a row to the right, [v] down, not mirrored; see [FaceLattice]; null where a sticker was not found,
 * e.g. under a finger) and where the face lies in the frame ([centre] and the steps [u], [v] between
 * neighbouring stickers, in frame pixels).
 */
data class FaceReading(val colors: List<Rgb?>, val centre: Point, val u: Point, val v: Point) {
    init {
        require(colors.size == 9)
    }

    /** All nine stickers found. */
    val isFull: Boolean get() = colors.all { it != null }

    /** Size of one sticker cell in the frame (pixels²); the largest face is the one most towards the camera. */
    val area: Double get() = abs(u.cross(v))

    /** The face's outline in the frame, clockwise from the top left corner. */
    val outline: List<Point>
        get() = listOf(
            centre - u * 1.5 - v * 1.5,
            centre + u * 1.5 - v * 1.5,
            centre + u * 1.5 + v * 1.5,
            centre - u * 1.5 + v * 1.5,
        )

    /**
     * The side of this face (0 top, 1 right, 2 bottom, 3 left, in reading order) that [other], seen
     * in the same frame, lies across: its centre is about two to four steps away on that side and
     * not far off to the side. Null when [other] does not sit like a neighbouring face (a corner view).
     */
    fun sideTowards(other: FaceReading): Int? {
        val d = other.centre - centre
        val det = u.cross(v)
        if (det == 0.0) return null
        val a = d.cross(v) / det
        val b = u.cross(d) / det
        val (major, minor) = if (abs(a) > abs(b)) a to b else b to a
        if (abs(major) !in NEIGHBOUR_STEPS || abs(minor) > NEIGHBOUR_SIDEWAYS * abs(major)) return null
        return if (abs(a) > abs(b)) (if (a > 0) 1 else 3) else (if (b > 0) 2 else 0)
    }

    companion object {
        /** How far (in steps) a neighbouring face's centre lies across the shared edge. */
        val NEIGHBOUR_STEPS = 1.8..4.5

        /** How far off to the side it may lie, as a share of [NEIGHBOUR_STEPS]. */
        const val NEIGHBOUR_SIDEWAYS = 0.5

        fun of(lattice: FaceLattice): FaceReading = FaceReading(lattice.stickers.map { it?.color }, lattice.centre, lattice.u, lattice.v)
    }
}
