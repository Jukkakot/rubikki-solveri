package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers

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

    /** Quick reading of one cell against the default palette (live preview and centre check). */
    fun live(rgb: Rgb): CubeColor {
        val lab = rgb.toLab()
        return defaultLabs.minBy { it.value.distance(lab) }.key
    }

    /**
     * Classifies all 54 readings (URFDLB order) by the cube's own centres: every colour gets
     * exactly nine stickers (balanced assignment), references refined to the mean of their nine.
     * The centres keep the holding position's colours.
     */
    fun classify(samples: List<Rgb>, scheme: ColorScheme = ColorScheme.STANDARD): Classification {
        require(samples.size == Stickers.COUNT)
        val labs = samples.map { it.toLab() }
        val colors = CubeColor.entries
        val centreColor = Face.entries.associate { Stickers.centre(it) to scheme[it] }
        var refs: Map<CubeColor, Lab> = Face.entries.associate { scheme[it] to labs[Stickers.centre(it)] }
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
