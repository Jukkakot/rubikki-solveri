package fi.jukkakot.rubikkisolveri.ui.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState
import kotlin.math.exp

/**
 * The grey veil over one sticker still needed, on the camera picture (picture pixels): centred at
 * [centre], its sides along [u] and [v] (one sticker step each). [key] stays the same for the same
 * sticker from frame to frame, so its position can glide.
 */
data class PaintTile(val key: Int, val centre: Point, val u: Point, val v: Point)

/**
 * The small dot over one known sticker in its read colour (`scan-steady-progress`): centred at
 * [centre], [u] and [v] its sticker steps. [key] is the key its veil had, so a sticker becoming
 * known glides on.
 */
data class PaintDot(val key: Int, val centre: Point, val u: Point, val v: Point, val color: CubeColor)

/** A side's centre and its sticker steps: where a finished side's tick goes. */
data class PaintTick(val centre: Point, val u: Point, val v: Point)

/**
 * What to paint over one camera picture (`scan-paint-calm`, `scan-steady-progress`): a veil over
 * every sticker still needed ([tiles]), a small dot in its read colour on every known one ([dots]),
 * the [outlines] and [ticks] of confirmed sides, and a dim outline round each other face found
 * ([found]; four corners each).
 */
data class ScanPaint(
    val tiles: List<PaintTile>,
    val outlines: List<List<Point>>,
    val ticks: List<PaintTick> = emptyList(),
    val found: List<List<Point>> = emptyList(),
    val dots: List<PaintDot> = emptyList(),
) {
    companion object {
        val EMPTY = ScanPaint(emptyList(), emptyList())

        /**
         * The paint for [state]: the stickers still needed on every face found in this frame and on
         * every other side of the projection turned towards the camera. A side the rest of the cube
         * confirms gets an outline and a tick; a face found otherwise a dim outline.
         */
        fun of(state: VideoScanState): ScanPaint {
            val tiles = ArrayList<PaintTile>()
            val dots = ArrayList<PaintDot>()
            val outlines = ArrayList<List<Point>>()
            val ticks = ArrayList<PaintTick>()
            val found = ArrayList<List<Point>>()
            val foundSides = HashSet<Face>()
            state.found.forEachIndexed { f, face ->
                val r = face.reading
                // The side this face is, once its centre is named: the centre's colour tells it.
                val side = face.names[4]?.let { c -> Face.entries.firstOrNull { ColorScheme.STANDARD[it] == c } }
                if (side != null) foundSides += side
                for (n in 0 until 9) {
                    val centre = r.centre + r.u * (n % 3 - 1.0) + r.v * (n / 3 - 1.0)
                    val key = if (side != null) FOUND_KEY + side.ordinal * 9 + n else LOOSE_KEY + f * 9 + n
                    val known = face.known[n]?.takeIf { face.recognised[n] }
                    if (known != null) dots += PaintDot(key, centre, r.u, r.v, known) else if (!face.recognised[n]) tiles += PaintTile(key, centre, r.u, r.v)
                }
                val corners = listOf(-1.5 to -1.5, 1.5 to -1.5, 1.5 to 1.5, -1.5 to 1.5).map { (a, b) -> r.centre + r.u * a + r.v * b }
                if (side != null && side in state.confirmed) {
                    outlines += corners
                    ticks += PaintTick(r.centre, r.u, r.v)
                } else {
                    found += corners
                }
            }
            val projection = state.projection ?: return ScanPaint(tiles, outlines, ticks, found, dots)
            for (side in projection.facing) {
                if (side in foundSides) continue
                val p = { n: Int -> projection.points[side.ordinal * 9 + n] }
                // A side seen at an angle is narrower: its veils follow its own sticker spacing.
                val u = (p(5) - p(3)) * 0.5
                val v = (p(7) - p(1)) * 0.5
                for (n in 0 until 9) {
                    val i = side.ordinal * 9 + n
                    val known = state.stickers[i]
                    if (known == null) tiles += PaintTile(i, p(n), u, v) else dots += PaintDot(i, p(n), u, v, known)
                }
                if (side in state.confirmed) {
                    outlines += listOf(0, 2, 8, 6).map { n -> p(4) + (p(n) - p(4)) * 1.5 }
                    ticks += PaintTick(p(4), u, v)
                }
            }
            return ScanPaint(tiles, outlines, ticks, found, dots)
        }

        /** Keys of a found face's stickers (by side and reading order) and of a face whose side is not named. */
        private const val FOUND_KEY = 1_000
        private const val LOOSE_KEY = 2_000
    }
}

/**
 * Whether the marks show while the cube moves (`scan-paint-calm`): hidden while the cube's centre in
 * the picture moves faster than [MOVING_SIDES_PER_SECOND] side widths a second, shown again once it
 * has stayed slower for [REST_MILLIS].
 */
class MotionFade {
    private var last: Point? = null
    private var lastAt = 0L
    private var restSince: Long? = Long.MIN_VALUE / 2 // rested since long ago: the marks show from the first picture

    /** The cube's centre [centre] (null: not known) in a picture at [nowMillis], one side [side] pixels wide: whether the marks show. */
    fun step(centre: Point?, side: Double, nowMillis: Long): Boolean {
        val before = last
        val dt = nowMillis - lastAt
        if (centre != null) {
            last = centre
            lastAt = nowMillis
        }
        if (centre != null && before != null && dt > 0 && side > 0) {
            val speed = (centre - before).length / side / (dt / 1000.0)
            if (speed > MOVING_SIDES_PER_SECOND) restSince = null else if (restSince == null) restSince = nowMillis
        }
        return restSince?.let { nowMillis - it >= REST_MILLIS } ?: false
    }
}

/** Faster than this many side widths a second, the cube counts as moving: a hand holding it stays under it (`scan-steady-progress`). */
const val MOVING_SIDES_PER_SECOND = 1.0

/** The marks come back after the cube has rested this long. */
const val REST_MILLIS = 300L

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
