package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.math.exp

/**
 * One sticker's paint on the camera picture (picture pixels): centred at [centre], its sides along
 * [u] and [v] (one sticker step each), in [color] (null: still needed, drawn grey). [key] stays the
 * same for the same sticker from frame to frame, so its position can glide.
 */
data class PaintTile(val key: Int, val centre: Point, val u: Point, val v: Point, val color: CubeColor?)

/** What to paint over one camera picture: the [tiles], the [outlines] of confirmed sides (four corners each). */
data class ScanPaint(val tiles: List<PaintTile>, val outlines: List<List<Point>>) {
    companion object {
        val EMPTY = ScanPaint(emptyList(), emptyList())

        /**
         * The paint for [state]: every face found in this frame in the colours it was read as (solid
         * once known), and every other side of the projection turned towards the camera in its known
         * colour or grey while needed. A side the rest of the cube confirms gets an outline.
         */
        fun of(state: VideoScanState): ScanPaint {
            val tiles = ArrayList<PaintTile>()
            val outlines = ArrayList<List<Point>>()
            val foundSides = HashSet<Face>()
            state.found.forEachIndexed { f, face ->
                val r = face.reading
                // The side this face is, once its centre is named: the centre's colour tells it.
                val side = face.names[4]?.let { c -> Face.entries.firstOrNull { ColorScheme.STANDARD[it] == c } }
                if (side != null) foundSides += side
                for (n in 0 until 9) {
                    val name = face.names[n] ?: continue
                    val centre = r.centre + r.u * (n % 3 - 1.0) + r.v * (n / 3 - 1.0)
                    val key = if (side != null) FOUND_KEY + side.ordinal * 9 + n else LOOSE_KEY + f * 9 + n
                    tiles += PaintTile(key, centre, r.u, r.v, name.takeIf { face.recognised[n] })
                }
                if (side != null && side in state.confirmed) {
                    outlines += listOf(-1.5 to -1.5, 1.5 to -1.5, 1.5 to 1.5, -1.5 to 1.5).map { (a, b) -> r.centre + r.u * a + r.v * b }
                }
            }
            val projection = state.projection ?: return ScanPaint(tiles, outlines)
            for (side in projection.facing) {
                if (side in foundSides) continue
                val p = { n: Int -> projection.points[side.ordinal * 9 + n] }
                // A side seen at an angle is narrower: its tiles follow its own sticker spacing.
                val u = (p(5) - p(3)) * 0.5
                val v = (p(7) - p(1)) * 0.5
                for (n in 0 until 9) {
                    val i = side.ordinal * 9 + n
                    tiles += PaintTile(i, p(n), u, v, state.stickers[i])
                }
                if (side in state.confirmed) outlines += listOf(0, 2, 8, 6).map { n -> p(4) + (p(n) - p(4)) * 1.5 }
            }
            return ScanPaint(tiles, outlines)
        }

        /** Keys of a found face's stickers (by side and reading order) and of a face whose side is not named. */
        private const val FOUND_KEY = 1_000
        private const val LOOSE_KEY = 2_000
    }
}

/**
 * Positions that glide towards where they should be at the display's rate: each step moves a point
 * part of the way, by `1 - e^(-dt/τ)` of what is left ([tauMillis] = τ); a point new, or further
 * than its snap distance from where it should be (the cube turned to another side), jumps at once.
 */
class Glide(private val tauMillis: Float = GLIDE_TAU_MILLIS) {
    private val at = HashMap<Int, Point>()

    /** Moves every point towards its [targets] position over [dtMillis]; points not in [targets] are forgotten. */
    fun step(targets: Map<Int, Point>, dtMillis: Float, snap: (Int) -> Double): Map<Int, Point> {
        val k = 1.0 - exp(-dtMillis / tauMillis).toDouble()
        val next = HashMap<Int, Point>(targets.size)
        for ((key, target) in targets) {
            val now = at[key]
            next[key] = if (now == null || (target - now).length > snap(key)) target else now + (target - now) * k
        }
        at.clear()
        at.putAll(next)
        return next
    }
}

/** How quickly the paint follows the cube (`scan-paint` design 2). */
const val GLIDE_TAU_MILLIS = 60f

/** The paint fades from this age of the projection and is gone at [PAINT_GONE_MILLIS]. */
const val PAINT_FADE_MILLIS = 600L
const val PAINT_GONE_MILLIS = 1_200L

/** How strongly the projection's paint shows at [ageMillis]: 1, fading to 0 between the two limits. */
fun paintAlpha(ageMillis: Long): Float = when {
    ageMillis <= PAINT_FADE_MILLIS -> 1f
    ageMillis >= PAINT_GONE_MILLIS -> 0f
    else -> 1f - (ageMillis - PAINT_FADE_MILLIS).toFloat() / (PAINT_GONE_MILLIS - PAINT_FADE_MILLIS)
}
