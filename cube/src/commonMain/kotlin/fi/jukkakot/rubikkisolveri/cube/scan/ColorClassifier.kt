package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import kotlin.math.exp

/** Colours for all 54 stickers and how sure each one is (0 = a coin toss, 1 = certain). */
data class Classification(val colors: List<CubeColor>, val confidence: List<Double>) {
    fun uncertain(threshold: Double = ColorClassifier.UNCERTAIN_BELOW): Set<Int> =
        confidence.indices.filter { confidence[it] < threshold }.toSet()
}

object ColorClassifier {
    const val UNCERTAIN_BELOW = 0.12

    /** Typical readings of a stickerless cube in daylight; used only for the live preview. */
    val DEFAULT_PALETTE: Map<CubeColor, Rgb> = mapOf(
        CubeColor.WHITE to Rgb(225, 225, 225),
        CubeColor.YELLOW to Rgb(220, 210, 40),
        CubeColor.GREEN to Rgb(30, 170, 80),
        CubeColor.BLUE to Rgb(20, 80, 190),
        CubeColor.RED to Rgb(190, 25, 35),
        CubeColor.ORANGE to Rgb(240, 100, 20),
    )

    private val defaultLabs = DEFAULT_PALETTE.mapValues { it.value.toLab() }

    /**
     * References for the live reading: the mean of the readings [known] for a colour (the cube's own
     * centres and the user's corrections), the default palette for colours not seen yet.
     */
    fun references(known: Map<CubeColor, List<Lab>> = emptyMap()): Map<CubeColor, Lab> =
        defaultLabs.mapValues { (color, default) -> known[color]?.takeIf { it.isNotEmpty() }?.let(Lab::mean) ?: default }

    /** Quick reading of one cell (live preview, review and centre check). */
    fun live(rgb: Rgb, refs: Map<CubeColor, Lab> = defaultLabs): CubeColor = ranked(rgb, refs).first()

    /** All colours, the closest to [rgb] first. */
    fun ranked(rgb: Rgb, refs: Map<CubeColor, Lab> = defaultLabs): List<CubeColor> {
        val lab = rgb.toLab()
        return refs.entries.sortedBy { it.value.distance(lab) }.map { it.key }
    }

    /**
     * How well [lab] fits each colour (by ordinal), the shares summing to 1: a Gaussian of its
     * distance to each reference in [refs] with width [width]. A reading halfway between two colours
     * gives each about half (`video-scan-light`).
     */
    fun shares(lab: Lab, refs: Map<CubeColor, Lab>, width: Double = SHARE_WIDTH): DoubleArray {
        val d2 = DoubleArray(6) { c -> refs.getValue(CubeColor.entries[c]).distance(lab).let { it * it } }
        val least = d2.min()
        val w = DoubleArray(6) { c -> exp(-(d2[c] - least) / (2 * width * width)) }
        val sum = w.sum()
        // Shares too small to matter are dropped, so a clear reading is one whole vote.
        for (c in w.indices) if (w[c] < MIN_SHARE * sum) w[c] = 0.0
        val kept = w.sum()
        return DoubleArray(6) { w[it] / kept }
    }

    /** A share below this is dropped (and the rest scaled up). */
    const val MIN_SHARE = 0.05

    /**
     * [shares]' width (Lab distance): a reading this far from a colour's reference keeps 61 % of the
     * weight of one right on it. Readings spread about 5 around their own colour in good light; the
     * test videos clear as fast from 3 to 5 (`video-scan-light` findings).
     */
    const val SHARE_WIDTH = 4.0

    /** Most a reading is brightened by [scaled] (so noise in near-black is not blown up). */
    const val MAX_GAIN = 6.0

    /** [rgb] brightened so its brightest channel is 255 (gain at most [MAX_GAIN]): its colour without its brightness. */
    fun scaled(rgb: Rgb): Rgb {
        val top = maxOf(rgb.r, rgb.g, rgb.b)
        val gain = if (top == 0) 1.0 else minOf(255.0 / top, MAX_GAIN)
        fun ch(v: Int) = (v * gain).toInt().coerceIn(0, 255)
        return Rgb(ch(rgb.r), ch(rgb.g), ch(rgb.b))
    }

    private val scaledDefaults = DEFAULT_PALETTE.mapValues { scaled(it.value).toLab() }

    /**
     * Distance of [rgb] from each colour regardless of brightness ([scaled] on both sides). References
     * are the readings [known] for a colour (the cube's own centres), the default palette otherwise.
     * Used for naming centres, where a dim light must not make a dark colour look white.
     */
    fun centreDistances(rgb: Rgb, known: Map<CubeColor, Rgb> = emptyMap()): Map<CubeColor, Double> {
        val lab = scaled(rgb).toLab()
        return scaledDefaults.mapValues { (color, default) -> (known[color]?.let { scaled(it).toLab() } ?: default).distance(lab) }
    }

    /** All colours, the closest to the centre reading [rgb] first ([centreDistances]). */
    fun rankedCentre(rgb: Rgb, known: Map<CubeColor, Rgb> = emptyMap()): List<CubeColor> =
        centreDistances(rgb, known).entries.sortedBy { it.value }.map { it.key }

    /**
     * Ways to name six centre readings [centres] (one per captured face) with the six colours, the
     * best fit first ([centreDistances] to the default palette, summed), at most [limit]. A naming
     * lists the colour of each centre in order. [preferred] (the names given while scanning) wins a tie.
     */
    fun centreNamings(centres: List<Rgb>, preferred: List<CubeColor>? = null, limit: Int = 12): List<List<CubeColor>> {
        require(centres.size == 6)
        val colors = CubeColor.entries
        val cost = centres.mapIndexed { i, rgb ->
            val d = centreDistances(rgb)
            DoubleArray(6) { c -> d.getValue(colors[c]) + if (preferred != null && preferred[i] != colors[c]) TIE else 0.0 }
        }
        val all = ArrayList<Pair<Double, IntArray>>(720)
        fun permute(perm: IntArray, k: Int) {
            if (k == 6) {
                all += perm.indices.sumOf { cost[it][perm[it]] } to perm.copyOf()
                return
            }
            for (i in k until 6) {
                perm[k] = perm[i].also { perm[i] = perm[k] }
                permute(perm, k + 1)
                perm[k] = perm[i].also { perm[i] = perm[k] }
            }
        }
        permute(IntArray(6) { it }, 0)
        return all.sortedBy { it.first }.take(limit).map { (_, perm) -> perm.map { colors[it] } }
    }

    private const val TIE = 1e-6

    /**
     * Classifies all 54 readings (URFDLB order) by the cube's own centres: every colour gets
     * exactly nine stickers (balanced assignment), references refined to the mean of their nine.
     * The centres keep the holding position's colours and seed the references.
     */
    fun classify(samples: List<Rgb>, scheme: ColorScheme = ColorScheme.STANDARD): Classification {
        require(samples.size == Stickers.COUNT)
        val labs = samples.map { it.toLab() }
        val colors = CubeColor.entries
        val centreColor = Face.entries.associate { Stickers.centre(it) to scheme[it] }
        var refs: Map<CubeColor, Lab> = centreColor.entries.groupBy({ it.value }, { labs[it.key] }).mapValues { Lab.mean(it.value) }
        var assigned = List(Stickers.COUNT) { CubeColor.WHITE }
        repeat(3) {
            val cost = Array(Stickers.COUNT) { i ->
                DoubleArray(Stickers.COUNT) { slot ->
                    val color = colors[slot / 9]
                    val centre = centreColor[i]
                    when {
                        centre != null -> if (centre == color) 0.0 else BIG
                        else -> labs[i].distance(refs.getValue(color))
                    }
                }
            }
            val slotOf = Hungarian.solve(cost)
            assigned = slotOf.map { colors[it / 9] }
            refs = colors.associateWith { color -> Lab.mean(labs.filterIndexed { i, _ -> assigned[i] == color }) }
        }
        val confidence = labs.mapIndexed { i, lab ->
            if (i in centreColor) {
                1.0
            } else {
                val sorted = refs.entries.sortedBy { it.value.distance(lab) }
                if (sorted[0].key != assigned[i]) {
                    0.0
                } else {
                    val d1 = sorted[0].value.distance(lab)
                    val d2 = sorted[1].value.distance(lab)
                    if (d1 + d2 == 0.0) 0.0 else (d2 - d1) / (d1 + d2)
                }
            }
        }
        return Classification(assigned, confidence)
    }

    /**
     * Classifies a rescanned [face] ([samples], net order) against the rest of this cube: the
     * references are the mean of the other faces' [readings] per colour, labelled by [colors] (so the
     * user's fixes count). The rescan has its own exposure, so its readings are first scaled by how
     * much brighter the face's centre read in [readings]. Each sticker takes the nearest colour; the
     * centre keeps the holding position's. Returns nine colours and confidences.
     */
    fun classifyFace(
        face: Face,
        samples: List<Rgb>,
        colors: List<CubeColor?>,
        readings: List<Rgb>,
        scheme: ColorScheme = ColorScheme.STANDARD,
    ): Classification {
        require(samples.size == 9 && colors.size == Stickers.COUNT && readings.size == Stickers.COUNT)
        val others = (0 until Stickers.COUNT).filter { it / 9 != face.ordinal }
        val refs = CubeColor.entries.mapNotNull { color ->
            others.filter { colors[it] == color }.takeIf { it.isNotEmpty() }?.let { idx -> color to Lab.mean(idx.map { readings[it].toLab() }) }
        }.toMap().ifEmpty { references() }
        // Exposure acts as a gain: scale the rescan so its centre is as bright as in the first scan.
        val before = readings[Stickers.centre(face)].let { it.r + it.g + it.b }
        val now = samples[4].let { it.r + it.g + it.b }
        val gain = if (now == 0) 1.0 else before.toDouble() / now
        fun scaled(v: Int) = (v * gain).toInt().coerceIn(0, 255)
        val labs = samples.map { Rgb(scaled(it.r), scaled(it.g), scaled(it.b)).toLab() }
        val result = labs.mapIndexed { n, lab ->
            if (n == 4) {
                scheme[face] to 1.0
            } else {
                val sorted = refs.entries.sortedBy { it.value.distance(lab) }
                val d1 = sorted[0].value.distance(lab)
                val d2 = sorted.getOrNull(1)?.value?.distance(lab) ?: d1
                sorted[0].key to if (d1 + d2 == 0.0) 0.0 else (d2 - d1) / (d1 + d2)
            }
        }
        return Classification(result.map { it.first }, result.map { it.second })
    }

    private const val BIG = 1e9
}

/** Minimum-cost perfect assignment for a square cost matrix (Kuhn–Munkres, O(n³)). */
object Hungarian {
    /** For each row, the column it is assigned to. */
    fun solve(cost: Array<DoubleArray>): IntArray {
        val n = cost.size
        val u = DoubleArray(n + 1)
        val v = DoubleArray(n + 1)
        val p = IntArray(n + 1)
        val way = IntArray(n + 1)
        for (i in 1..n) {
            p[0] = i
            var j0 = 0
            val minv = DoubleArray(n + 1) { Double.POSITIVE_INFINITY }
            val used = BooleanArray(n + 1)
            do {
                used[j0] = true
                val i0 = p[j0]
                var delta = Double.POSITIVE_INFINITY
                var j1 = 0
                for (j in 1..n) {
                    if (!used[j]) {
                        val cur = cost[i0 - 1][j - 1] - u[i0] - v[j]
                        if (cur < minv[j]) {
                            minv[j] = cur
                            way[j] = j0
                        }
                        if (minv[j] < delta) {
                            delta = minv[j]
                            j1 = j
                        }
                    }
                }
                for (j in 0..n) {
                    if (used[j]) {
                        u[p[j]] += delta
                        v[j] -= delta
                    } else {
                        minv[j] -= delta
                    }
                }
                j0 = j1
            } while (p[j0] != 0)
            do {
                val j1 = way[j0]
                p[j0] = p[j1]
                j0 = j1
            } while (j0 != 0)
        }
        val rowToCol = IntArray(n)
        for (j in 1..n) if (p[j] != 0) rowToCol[p[j] - 1] = j - 1
        return rowToCol
    }
}
