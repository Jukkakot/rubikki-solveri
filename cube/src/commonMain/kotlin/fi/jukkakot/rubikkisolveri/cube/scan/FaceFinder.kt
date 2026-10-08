package fi.jukkakot.rubikkisolveri.cube.scan

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt

/** A point in frame pixels. */
data class Point(val x: Double, val y: Double) {
    operator fun plus(o: Point) = Point(x + o.x, y + o.y)
    operator fun minus(o: Point) = Point(x - o.x, y - o.y)
    operator fun times(k: Double) = Point(x * k, y * k)
    val length: Double get() = hypot(x, y)
    fun cross(o: Point): Double = x * o.y - y * o.x
}

/**
 * A sticker-like patch of a frame: [centre], pixel [area], [aspect] (long / short axis), median
 * [color] and the covariance of its pixels ([xx], [yy], [xy]).
 */
data class Blob(
    val centre: Point,
    val area: Int,
    val aspect: Double,
    val color: Rgb,
    val xx: Double = 0.0,
    val yy: Double = 0.0,
    val xy: Double = 0.0,
) {
    /** Standard deviation of the pixels along the unit vector [n]. */
    fun spread(n: Point): Double = sqrt(n.x * n.x * xx + 2 * n.x * n.y * xy + n.y * n.y * yy)
}

/**
 * A 3×3 face found in a frame: [stickers] row by row as seen ([u] points along a row, [v] down the
 * columns, the face not mirrored). [hits] is how many of the nine places held a sticker; a full
 * face has nine, [stickers] then has no nulls.
 */
data class FaceLattice(val stickers: List<Blob?>, val u: Point, val v: Point, val error: Double) {
    val hits: Int get() = stickers.count { it != null }
    val isFull: Boolean get() = hits == 9
    val centre: Point get() = stickers[4]!!.centre

    /** The nine colours as seen (only for a full face). */
    val colors: List<Rgb> get() = stickers.map { it!!.color }

    /**
     * For each pair of neighbouring stickers, measured across the step (sideways to the other axis):
     * how much of the distance between their centres the two span, and the smaller one's size over
     * the larger one's. On one face both stay about the same also in perspective; across the cube's
     * edge the next face is seen flatter, so the pair spans less and differs in size.
     */
    private val pairs: List<Pair<Double, Double>> by lazy {
        val result = ArrayList<Pair<Double, Double>>()
        val nu = Point(-u.y, u.x) * (1 / u.length)
        val nv = Point(-v.y, v.x) * (1 / v.length)
        fun pair(a: Int, b: Int, n: Point) {
            val sa = stickers[a] ?: return
            val sb = stickers[b] ?: return
            val d = abs((sb.centre - sa.centre).x * n.x + (sb.centre - sa.centre).y * n.y)
            val ka = sa.spread(n)
            val kb = sb.spread(n)
            if (d > 0) result += sqrt(12.0) * (ka + kb) / 2 / d to minOf(ka, kb) / maxOf(ka, kb)
        }
        for (row in 0 until 3) for (col in 0 until 2) pair(row * 3 + col, row * 3 + col + 1, nv)
        for (col in 0 until 3) for (row in 0 until 2) pair(row * 3 + col, (row + 1) * 3 + col, nu)
        result
    }

    /** The smallest share of a step a pair of neighbours spans. */
    val span: Double get() = pairs.minOfOrNull { it.first } ?: 0.0

    /** The smallest size ratio of a pair of neighbours. */
    val likeness: Double get() = pairs.minOfOrNull { it.second } ?: 0.0

    /**
     * The steps' length over the cell's size, (|u| + |v|) / √|u × v|: 2 for a square, more when a
     * step runs along a diagonal (a lattice sheared onto the next face). For the same stickers the
     * true lattice is the most compact.
     */
    val compactness: Double get() = (u.length + v.length) / sqrt(abs(u.cross(v)))

    /** Ordering of competing lattices: the higher, the more the stickers look like one face. */
    val quality: Double get() = span + likeness / 2 - compactness * compactWeight

    companion object {
        /** How much [compactness] counts in [quality]. */
        var compactWeight = 0.3
    }
}

/**
 * What [FaceFinder.find] saw in one frame: the full faces, the faces missing one or two stickers,
 * and every lattice considered (the best per centre sticker, for tuning).
 */
data class FinderResult(
    val blobs: List<Blob>,
    val faces: List<FaceLattice>,
    val partial: List<FaceLattice>,
    val candidates: List<FaceLattice> = emptyList(),
)

/**
 * Finds cube faces anywhere in a frame, also seen at an angle, without a fixed grid: sticker-like
 * pixels (not dark plastic) grow into blobs of one colour; blobs of a plausible size and shape are
 * stickers; nine stickers whose centres fit a 3×3 lattice are a face. Pure Kotlin, so it runs on the
 * phone, in the browser and in JVM tests. Spike code (`video-scan-spike`): thresholds tuned on the
 * test videos of 2026-10-05.
 */
object FaceFinder {
    /** A pixel whose brightest channel is below this is dark plastic (or shadow). */
    var darkBelow = 70

    /** Neighbouring pixels join one blob when their colours differ by at most this (sum over RGB). */
    var joinWithin = 36

    /** ... and differ from the blob's mean colour by at most this (sum over RGB). */
    var meanWithin = 60

    /** Smallest sticker, as a share of the frame's shorter side squared. */
    var minAreaShare = 0.0012

    /** Largest sticker, as a share of the frame's shorter side squared. */
    var maxAreaShare = 0.12

    /** Longest sticker, long axis over short axis (a face seen at a steep angle is flat). */
    var maxAspect = 4.0

    /** A sticker fills its fitted rectangle about this well (area over 12·√(λ1·λ2)). */
    var minFill = 0.75
    var maxFill = 1.2

    /** A predicted lattice place takes a blob within this share of a step sideways (and at the corners). */
    var tolerance = 0.3

    /** ... and within this share of a step along the step (perspective makes the far step shorter). */
    var stretch = 0.45

    /** Least [FaceLattice.span] of a face (lower: a row of the next face across the cube's edge). */
    var minSpan = 0.6

    /** Neighbours of a centre considered for the two step vectors. */
    private const val NEIGHBOURS = 10

    fun find(argb: IntArray, width: Int, height: Int): FinderResult {
        val blobs = blobs(argb, width, height)
        val candidates = blobs.indices.mapNotNull { lattice(blobs, it) }
        val faces = pick(candidates.filter { it.isFull })
        val used = faces.flatMap { f -> f.stickers }.toSet()
        val partial = pick(candidates.filter { it.hits in 7..8 && it.stickers[4] !in used })
        return FinderResult(blobs, faces, partial, candidates)
    }

    /**
     * Sticker-like blobs of [argb] ([width]×[height]). With [debug] (one per pixel), each pixel gets
     * why its blob was kept or not: 0 dark, 1 too big, 2 too small, 3 wrong shape, 4 a sticker.
     */
    fun blobs(argb: IntArray, width: Int, height: Int, debug: IntArray? = null): List<Blob> {
        val n = width * height
        val r = IntArray(n)
        val g = IntArray(n)
        val b = IntArray(n)
        val dark = BooleanArray(n)
        for (i in 0 until n) {
            val p = argb[i]
            r[i] = (p shr 16) and 0xff
            g[i] = (p shr 8) and 0xff
            b[i] = p and 0xff
            dark[i] = maxOf(r[i], g[i], b[i]) < darkBelow
        }
        val short = minOf(width, height).toDouble()
        val minArea = (minAreaShare * short * short).toInt()
        val maxArea = (maxAreaShare * short * short).toInt()
        val label = BooleanArray(n)
        val queue = IntArray(n)
        val found = ArrayList<Blob>()
        for (start in 0 until n) {
            if (dark[start] || label[start]) continue
            label[start] = true
            var head = 0
            var tail = 0
            queue[tail++] = start
            var tooBig = false
            var sr = r[start]
            var sg = g[start]
            var sb = b[start]
            while (head < tail) {
                val i = queue[head++]
                val x = i % width
                val y = i / width
                // The four neighbours left, right, up and down (a loop, not a local function: it runs for every pixel).
                for (d in 0 until 4) {
                    val j = when (d) {
                        0 -> if (x > 0) i - 1 else continue
                        1 -> if (x < width - 1) i + 1 else continue
                        2 -> if (y > 0) i - width else continue
                        else -> if (y < height - 1) i + width else continue
                    }
                    if (label[j] || dark[j]) continue
                    if (abs(r[i] - r[j]) + abs(g[i] - g[j]) + abs(b[i] - b[j]) > joinWithin) continue
                    // Also close to the blob's mean, so a slow drift cannot carry it into the background.
                    if (abs(sr - r[j] * tail) + abs(sg - g[j] * tail) + abs(sb - b[j] * tail) > meanWithin * tail) continue
                    label[j] = true
                    queue[tail++] = j
                    sr += r[j]
                    sg += g[j]
                    sb += b[j]
                }
                if (tail > maxArea) tooBig = true
            }
            val area = tail
            fun mark(code: Int) {
                if (debug != null) for (k in 0 until area) debug[queue[k]] = code
            }
            if (tooBig || area < minArea) {
                mark(if (tooBig) 1 else 2)
                continue
            }
            var sx = 0.0
            var sy = 0.0
            for (k in 0 until area) {
                sx += queue[k] % width
                sy += queue[k] / width
            }
            val cx = sx / area
            val cy = sy / area
            var xx = 0.0
            var yy = 0.0
            var xy = 0.0
            for (k in 0 until area) {
                val dx = queue[k] % width - cx
                val dy = queue[k] / width - cy
                xx += dx * dx
                yy += dy * dy
                xy += dx * dy
            }
            xx /= area
            yy /= area
            xy /= area
            // Eigenvalues of the covariance: a w×h rectangle gives w²/12 and h²/12.
            val mid = (xx + yy) / 2
            val spread = sqrt(((xx - yy) / 2).let { it * it } + xy * xy)
            val l1 = mid + spread
            val l2 = mid - spread
            if (l2 <= 0.0) {
                mark(3)
                continue
            }
            val aspect = sqrt(l1 / l2)
            val fill = area / (12 * sqrt(l1 * l2))
            if (aspect > maxAspect || fill < minFill || fill > maxFill) {
                mark(3)
                continue
            }
            mark(4)
            found += Blob(Point(cx, cy), area, aspect, medianColor(queue, area, r, g, b), xx, yy, xy)
        }
        return found
    }

    /**
     * The sticker's colour: the per-channel median of its pixels after leaving out glare, the pixels
     * much brighter and much greyer than the darker half of the blob (a lamp's reflection). A blob
     * whose darker half is itself near grey (a white sticker) keeps all its pixels.
     */
    internal fun medianColor(pixels: IntArray, count: Int, r: IntArray, g: IntArray, b: IntArray): Rgb {
        fun bright(i: Int) = maxOf(r[i], g[i], b[i])
        fun saturation(i: Int) = bright(i).let { top -> if (top == 0) 0.0 else (top - minOf(r[i], g[i], b[i])).toDouble() / top }
        // Sorted by brightness, ties in pixel order (as a stable sort): brightness and place packed in one Long.
        val keys = LongArray(count) { k -> (bright(pixels[k]).toLong() shl 32) or k.toLong() }
        keys.sort()
        val byBrightness = IntArray(count) { pixels[(keys[it] and 0xffffffffL).toInt()] }
        val darkerSize = (count + 1) / 2
        val baseSaturation = DoubleArray(darkerSize) { saturation(byBrightness[it]) }.also { it.sort() }[darkerSize / 2]
        val baseBright = bright(byBrightness[darkerSize / 2])
        val kept = if (baseSaturation < GLARE_MIN_SATURATION) {
            byBrightness
        } else {
            byBrightness.filter { bright(it) <= baseBright * GLARE_BRIGHTER || saturation(it) >= baseSaturation * GLARE_GREYER }.toIntArray()
        }
        fun median(ch: IntArray): Int {
            val values = IntArray(kept.size) { ch[kept[it]] }
            values.sort()
            return values[kept.size / 2]
        }
        return Rgb(median(r), median(g), median(b))
    }

    /** A blob whose darker half is less saturated than this is white (or grey): no glare is left out. */
    const val GLARE_MIN_SATURATION = 0.25

    /** Glare is brighter than the darker half's median by this factor … */
    const val GLARE_BRIGHTER = 1.1

    /** … and less saturated than this share of the darker half's. */
    const val GLARE_GREYER = 0.6

    /** The best lattice with blob [c] in the middle (the most stickers, then the best [FaceLattice.quality]), or null. */
    private fun lattice(blobs: List<Blob>, c: Int): FaceLattice? {
        val centre = blobs[c]
        val reach = 4 * sqrt(centre.area.toDouble()) * sqrt(centre.aspect)
        val near = blobs.indices.filter { it != c && (blobs[it].centre - centre.centre).length < reach }
            .sortedBy { (blobs[it].centre - centre.centre).length }
            .take(NEIGHBOURS)
        val d2 = DoubleArray(blobs.size) { (blobs[it].centre - centre.centre).let { d -> d.x * d.x + d.y * d.y } }
        var best: FaceLattice? = null
        for (ai in near.indices) for (bi in ai + 1 until near.size) {
            val u = blobs[near[ai]].centre - centre.centre
            val v = blobs[near[bi]].centre - centre.centre
            val lu = u.length
            val lv = v.length
            if (abs(u.cross(v)) < 0.4 * lu * lv) continue
            if (lu > 3 * lv || lv > 3 * lu) continue
            // Only blobs a lattice place could take (each place within 1 + 3 × the larger tolerance steps of the centre along u and v).
            val r = (1 + 3 * maxOf(stretch, tolerance)) * (lu + lv)
            val within = blobs.indices.filter { it != c && d2[it] <= r * r }
            val candidate = fit(blobs, c, u, v, within) ?: continue
            if (best == null || candidate.hits > best.hits || (candidate.hits == best.hits && candidate.quality > best.quality)) best = candidate
        }
        return best?.takeIf { it.hits >= 7 }
    }

    /**
     * Fits a lattice with steps [u0] and [v0] around blob [c]: the four edge places are predicted
     * from the steps, the corners from the edges found (a small perspective correction).
     */
    private fun fit(blobs: List<Blob>, c: Int, u0: Point, v0: Point, within: List<Int>): FaceLattice? {
        val o = blobs[c].centre
        val det = u0.cross(v0)
        var error = 0.0
        val taken = BooleanArray(blobs.size)
        taken[c] = true
        // The blob nearest to [p] in lattice steps, within [alongU] steps along u and [alongV] along v.
        fun nearest(p: Point, alongU: Double, alongV: Double): Int? {
            var best = -1
            // Squared distances compared (hypot is slow and this runs about a thousand times a frame).
            var bestD2 = 1.0
            for (i in within) {
                if (taken[i]) continue
                val q = blobs[i].centre
                val dx = q.x - p.x
                val dy = q.y - p.y
                val a = (dx * v0.y - dy * v0.x) / det / alongU
                val b = (u0.x * dy - u0.y * dx) / det / alongV
                val dist2 = a * a + b * b
                if (dist2 < bestD2) {
                    best = i
                    bestD2 = dist2
                }
            }
            if (best < 0) return null
            taken[best] = true
            error += sqrt(bestD2)
            return best
        }
        // Steps as predicted; an edge found replaces its prediction for the corners next to it.
        val up = nearest(o + u0, stretch, tolerance)
        val um = nearest(o - u0, stretch, tolerance)
        val vp = nearest(o + v0, tolerance, stretch)
        val vm = nearest(o - v0, tolerance, stretch)
        // Three edges missing leave at most six of nine.
        if (listOf(up, um, vp, vm).count { it == null } >= 3) return null
        fun at(i: Int?, fallback: Point) = i?.let { blobs[it].centre } ?: fallback
        val eu = mapOf(1 to at(up, o + u0), -1 to at(um, o - u0))
        val ev = mapOf(1 to at(vp, o + v0), -1 to at(vm, o - v0))
        val grid = HashMap<Pair<Int, Int>, Int?>()
        grid[0 to 0] = c
        grid[1 to 0] = up
        grid[-1 to 0] = um
        grid[0 to 1] = vp
        grid[0 to -1] = vm
        for (i in listOf(-1, 1)) for (j in listOf(-1, 1)) {
            grid[i to j] = nearest(eu.getValue(i) + ev.getValue(j) - o, tolerance, tolerance)
        }
        val hits = grid.values.count { it != null }
        if (hits < 7) return null
        val areas = grid.values.filterNotNull().map { blobs[it].area }.sorted()
        val median = areas[areas.size / 2]
        if (areas.first() * 4 < median || areas.last() > median * 4) return null
        // Reading order: u along a row to the right, v down, not mirrored (u × v > 0 with y down).
        var u = u0
        var v = v0
        var flipU = false
        var flipV = false
        var swap = false
        if (abs(v.x) > abs(u.x)) {
            u = v0.also { v = u0 }
            swap = true
        }
        if (u.x < 0) {
            u = u * -1.0
            flipU = true
        }
        if (u.cross(v) < 0) {
            v = v * -1.0
            flipV = true
        }
        val stickers = (0 until 9).map { n ->
            var i = n % 3 - 1
            var j = n / 3 - 1
            if (flipU) i = -i
            if (flipV) j = -j
            val key = if (swap) j to i else i to j
            grid[key]?.let { blobs[it] }
        }
        return FaceLattice(stickers, u, v, error / hits).takeIf { it.span >= minSpan }
    }

    /** Greedy choice of lattices sharing no sticker, the most stickers and the best [FaceLattice.quality] first. */
    private fun pick(candidates: List<FaceLattice>): List<FaceLattice> {
        val used = HashSet<Blob>()
        val chosen = ArrayList<FaceLattice>()
        for (l in candidates.sortedWith(compareByDescending<FaceLattice> { it.hits }.thenByDescending { it.quality })) {
            val mine = l.stickers.filterNotNull()
            if (mine.any { it in used }) continue
            used += mine
            chosen += l
        }
        return chosen
    }
}
