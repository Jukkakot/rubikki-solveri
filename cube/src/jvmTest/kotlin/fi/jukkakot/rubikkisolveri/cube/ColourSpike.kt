package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

/** `scan-colour-spike`: the two ways of reading colours compared with today's. */
object ColourSpike {
    /**
     * Idea A (design 1): [face], found in a picture [scale] times smaller than the [full] ARGB picture
     * ([width]×[height]), with each found sticker read again from [full] at its lattice point: a
     * square half a step wide, trimmed mean per channel.
     */
    fun reread(face: FaceReading, full: IntArray, width: Int, height: Int, scale: Double): FaceReading {
        val steps = maxOf(3, ceil(0.5 * maxOf(hypot(face.u.x, face.u.y), hypot(face.v.x, face.v.y)) * scale).toInt())
        val colors = face.colors.mapIndexed { n, old ->
            if (old == null) return@mapIndexed null
            val at = face.centre + face.u * ((n % 3) - 1.0) + face.v * ((n / 3) - 1.0)
            val rs = ArrayList<Int>()
            val gs = ArrayList<Int>()
            val bs = ArrayList<Int>()
            for (i in 0 until steps) for (j in 0 until steps) {
                val s = -0.25 + 0.5 * i / (steps - 1)
                val t = -0.25 + 0.5 * j / (steps - 1)
                val p = at + face.u * s + face.v * t
                val x = (p.x * scale).toInt().coerceIn(0, width - 1)
                val y = (p.y * scale).toInt().coerceIn(0, height - 1)
                val argb = full[y * width + x]
                rs += (argb shr 16) and 0xff
                gs += (argb shr 8) and 0xff
                bs += argb and 0xff
            }
            Rgb(FrameSampler.trimmedMean(rs), FrameSampler.trimmedMean(gs), FrameSampler.trimmedMean(bs))
        }
        return face.copy(colors = colors)
    }

    /** Hue in degrees, −180..180 with red at 0. */
    fun hue(c: Rgb): Double {
        val max = maxOf(c.r, c.g, c.b)
        val min = minOf(c.r, c.g, c.b)
        if (max == min) return 0.0
        val d = (max - min).toDouble()
        val h = when (max) {
            c.r -> 60 * ((c.g - c.b) / d)
            c.g -> 60 * ((c.b - c.r) / d + 2)
            else -> 60 * ((c.r - c.g) / d + 4)
        }
        return if (h > 180) h - 360 else h
    }

    /** A warm reading (design 2): clearly coloured and between −30° and 45° hue. */
    fun isWarm(c: Rgb): Boolean = c.toLab().let { l -> hypot(l.a, l.b) >= FrameSampler.MIN_COLOUR_CHROMA } && hue(c) in -30.0..45.0

    /**
     * Idea B (design 2): the warm readings of the scan so far split by hue in two (1-D 2-means on a
     * half-degree histogram, red the lower group); once each group has [MIN_GROUP] readings and their
     * mean hues are [MIN_APART] degrees apart, each warm reading is replaced by its group's mean
     * colour at its own brightness (largest channel); with [margin] > 0 only readings at least that share
     * of the two means' distance away from the split.
     */
    class WarmSplit(private val margin: Double = 0.0) {
        private val counts = IntArray(BINS)
        private val sums = Array(BINS) { LongArray(3) }

        /** The split hue and both groups' mean colours, or null while the start condition does not hold. */
        var split: Triple<Double, Rgb, Rgb>? = null
            private set

        fun apply(faces: List<FaceReading>): List<FaceReading> {
            for (f in faces) for (c in f.colors) if (c != null && isWarm(c)) add(c)
            split = compute()
            val s = split ?: return faces
            val gap = margin * (hue(s.third) - hue(s.second))
            return faces.map { f ->
                f.copy(colors = f.colors.map { c -> if (c != null && isWarm(c) && kotlin.math.abs(hue(c) - s.first) >= gap) pulled(c, if (hue(c) < s.first) s.second else s.third) else c })
            }
        }

        private fun add(c: Rgb) {
            val b = bin(hue(c))
            counts[b]++
            sums[b][0] += c.r.toLong()
            sums[b][1] += c.g.toLong()
            sums[b][2] += c.b.toLong()
        }

        private fun compute(): Triple<Double, Rgb, Rgb>? {
            val total = counts.sum()
            if (total < 2 * MIN_GROUP) return null
            // Start the two means at the quartiles.
            var lo = quantile(0.25)
            var hi = quantile(0.75)
            var cut = (lo + hi) / 2
            repeat(20) {
                var n0 = 0L
                var s0 = 0.0
                var n1 = 0L
                var s1 = 0.0
                for (b in 0 until BINS) {
                    val h = centreOf(b)
                    if (h < cut) {
                        n0 += counts[b]
                        s0 += h * counts[b]
                    } else {
                        n1 += counts[b]
                        s1 += h * counts[b]
                    }
                }
                if (n0 == 0L || n1 == 0L) return null
                lo = s0 / n0
                hi = s1 / n1
                cut = (lo + hi) / 2
            }
            val below = (0 until BINS).filter { centreOf(it) < cut }
            val above = (0 until BINS).filter { centreOf(it) >= cut }
            val n0 = below.sumOf { counts[it] }
            val n1 = above.sumOf { counts[it] }
            if (n0 < MIN_GROUP || n1 < MIN_GROUP || hi - lo < MIN_APART) return null
            return Triple(cut, mean(below), mean(above))
        }

        private fun mean(bins: List<Int>): Rgb {
            val n = bins.sumOf { counts[it] }.toDouble()
            return Rgb((bins.sumOf { sums[it][0] } / n).roundToInt(), (bins.sumOf { sums[it][1] } / n).roundToInt(), (bins.sumOf { sums[it][2] } / n).roundToInt())
        }

        private fun quantile(q: Double): Double {
            val target = counts.sum() * q
            var acc = 0
            for (b in 0 until BINS) {
                acc += counts[b]
                if (acc >= target) return centreOf(b)
            }
            return centreOf(BINS - 1)
        }

        companion object {
            const val MIN_GROUP = 9
            const val MIN_APART = 4.0
            private const val FROM = -30.0
            private const val STEP = 0.5
            private const val BINS = 150
            private fun bin(h: Double) = ((h - FROM) / STEP).toInt().coerceIn(0, BINS - 1)
            private fun centreOf(b: Int) = FROM + (b + 0.5) * STEP

            /** [target]'s colour at [c]'s brightness. */
            fun pulled(c: Rgb, target: Rgb): Rgb {
                val k = maxOf(c.r, c.g, c.b).toDouble() / maxOf(1, maxOf(target.r, target.g, target.b))
                fun ch(v: Int) = (v * k).roundToInt().coerceIn(0, 255)
                return Rgb(ch(target.r), ch(target.g), ch(target.b))
            }
        }
    }
}
