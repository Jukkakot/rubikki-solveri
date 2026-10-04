package fi.jukkakot.rubikkisolveri.cube.scan

import kotlin.math.cbrt
import kotlin.math.pow

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

    /**
     * The grid square as seen on screen, [size]×[size] pixels row by row as ARGB (nearest pixel),
     * for a picture of what was sampled.
     */
    fun picture(frame: RgbaFrame, size: Int = PICTURE_SIZE): IntArray {
        val cw = frame.cropRight - frame.cropLeft
        val ch = frame.cropBottom - frame.cropTop
        val side = GRID_SIZE * minOf(cw, ch)
        val left = frame.cropLeft + (cw - side) / 2
        val top = frame.cropTop + (ch - side) / 2
        return IntArray(size * size) { i ->
            val (fx, fy) = toFrame((i % size + 0.5f) / size, (i / size + 0.5f) / size, frame.rotation)
            val x = (left + fx * side).toInt().coerceIn(0, frame.width - 1)
            val y = (top + fy * side).toInt().coerceIn(0, frame.height - 1)
            val p = frame.pixel(x, y)
            (0xff shl 24) or (p.r shl 16) or (p.g shl 8) or p.b
        }
    }

    const val PICTURE_SIZE = 120

    /** A cell looks like a sticker when its middle is this much lighter (Lab L) than its gap. */
    const val MIN_GAP_CONTRAST = 15.0

    /** At least this many of the nine cells must look like stickers. */
    const val MIN_STICKER_CELLS = 6

    /**
     * Per cell of a grid [picture] ([size]×[size] ARGB): the median lightness of the cell's middle
     * minus the darkest tenth of its outer edge. Stickers on a cube have dark gaps around them; a
     * desk or a sheet of paper does not, whatever its colour.
     */
    fun gapContrast(picture: IntArray, size: Int = PICTURE_SIZE): List<Double> {
        // Runs on every camera frame: primitive arrays only, lightness through a lookup table.
        val cell = size / 3
        val m0 = cell * 3 / 10
        val m1 = cell - m0
        val edge = cell / 5
        val mid = DoubleArray((m1 - m0) * (m1 - m0))
        val ring = DoubleArray(cell * cell)
        return (0 until 9).map { i ->
            val left = (i % 3) * cell
            val top = (i / 3) * cell
            var nm = 0
            var nr = 0
            for (y in 0 until cell) for (x in 0 until cell) {
                val inMiddle = x in m0 until m1 && y in m0 until m1
                val onEdge = x < edge || y < edge || x >= cell - edge || y >= cell - edge
                if (!inMiddle && !onEdge) continue
                val l = lightness(picture[(top + y) * size + left + x])
                if (inMiddle) mid[nm++] = l else ring[nr++] = l
            }
            mid.sort(0, nm)
            ring.sort(0, nr)
            mid[nm / 2] - ring[nr / 10]
        }
    }

    /** sRGB channel value to linear light. */
    private val LINEAR = DoubleArray(256) { c ->
        val v = c / 255.0
        if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    /** CIE L* of an ARGB pixel (same as [Rgb.toLab]'s `l`). */
    fun lightness(argb: Int): Double {
        val y = 0.2126 * LINEAR[(argb shr 16) and 0xff] + 0.7152 * LINEAR[(argb shr 8) and 0xff] + 0.0722 * LINEAR[argb and 0xff]
        val f = if (y > 216.0 / 24389) cbrt(y) else (24389.0 / 27 * y + 16) / 116
        return 116 * f - 16
    }

    /** The grid [picture] shows cube stickers: enough cells have a dark gap around a lighter middle. */
    fun looksLikeCube(picture: IntArray, size: Int = PICTURE_SIZE): Boolean =
        gapContrast(picture, size).count { it >= MIN_GAP_CONTRAST } >= MIN_STICKER_CELLS

    fun median(values: List<Int>): Int = values.sorted()[values.size / 2]
}
