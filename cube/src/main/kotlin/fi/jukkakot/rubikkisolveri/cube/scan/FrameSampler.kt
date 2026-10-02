package fi.jukkakot.rubikkisolveri.cube.scan

/**
 * One camera frame as RGBA bytes. [cropLeft]..[cropBottom] is the part visible on screen and
 * [rotation] (0, 90, 180, 270) the clockwise turn that makes the frame upright on screen.
 */
class RgbaFrame(
    val width: Int,
    val height: Int,
    val rowStride: Int,
    val bytes: ByteArray,
    val rotation: Int,
    val cropLeft: Int = 0,
    val cropTop: Int = 0,
    val cropRight: Int = width,
    val cropBottom: Int = height,
) {
    init {
        require(rotation in setOf(0, 90, 180, 270)) { "rotation $rotation" }
    }

    fun pixel(x: Int, y: Int): Rgb {
        val i = y * rowStride + x * 4
        return Rgb(bytes[i].toInt() and 0xff, bytes[i + 1].toInt() and 0xff, bytes[i + 2].toInt() and 0xff)
    }
}

/** Where the 3×3 grid is and how a frame is read through it. */
object FrameSampler {
    /** The grid is a centred square this share of the visible area's shorter side. */
    const val GRID_SIZE = 0.72f

    /** Middle part of each cell that is read (avoids the gaps between stickers). */
    private const val CELL_MIDDLE = 0.4f

    /**
     * Maps a point of the grid square as seen on screen ([u] right, [v] down, 0..1) to the same
     * point in the unrotated frame's square (0..1).
     */
    fun toFrame(u: Float, v: Float, rotation: Int): Pair<Float, Float> = when (rotation) {
        0 -> u to v
        90 -> v to 1 - u
        180 -> 1 - u to 1 - v
        else -> 1 - v to u
    }

    /** The nine readings of the grid, row by row as seen on screen. */
    fun sample(frame: RgbaFrame): List<Rgb> {
        val cw = frame.cropRight - frame.cropLeft
        val ch = frame.cropBottom - frame.cropTop
        val side = GRID_SIZE * minOf(cw, ch)
        val left = frame.cropLeft + (cw - side) / 2
        val top = frame.cropTop + (ch - side) / 2
        val margin = (1 - CELL_MIDDLE) / 2
        return (0 until 9).map { cell ->
            val row = cell / 3
            val col = cell % 3
            val (x0, y0) = toFrame((col + margin) / 3, (row + margin) / 3, frame.rotation)
            val (x1, y1) = toFrame((col + 1 - margin) / 3, (row + 1 - margin) / 3, frame.rotation)
            val xa = (left + minOf(x0, x1) * side).toInt().coerceIn(0, frame.width - 1)
            val xb = (left + maxOf(x0, x1) * side).toInt().coerceIn(0, frame.width - 1)
            val ya = (top + minOf(y0, y1) * side).toInt().coerceIn(0, frame.height - 1)
            val yb = (top + maxOf(y0, y1) * side).toInt().coerceIn(0, frame.height - 1)
            val rs = ArrayList<Int>()
            val gs = ArrayList<Int>()
            val bs = ArrayList<Int>()
            for (y in ya..yb step 2) for (x in xa..xb step 2) {
                val p = frame.pixel(x, y)
                rs += p.r
                gs += p.g
                bs += p.b
            }
            Rgb(median(rs), median(gs), median(bs))
        }
    }

    fun median(values: List<Int>): Int = values.sorted()[values.size / 2]
}
