package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceLattice
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import org.junit.Assume.assumeTrue
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.ln
import kotlin.test.Test

/**
 * `scan-false-faces-finish` 1.1: per source, the lattices whose reading fits no true face (fewer than
 * 6 of the 8 outer stickers in any face and turn) and, for the stills, every found lattice's flatness
 * numbers ([flatness]) of the true faces against those that read like a lattice over the cube's edge. Runs only with FALSE_FACES=1; writes
 * one line per lattice to `build/false-faces.csv` for a closer look.
 */
class FalseFacesHarness {
    private class Source(val dir: String, val range: IntRange?, val truth: String)

    private val stills = listOf(
        Source("2026-10-05/stills/20261005_151828", null, VideoFixtures.TRUTH),
        Source("2026-10-05/stills/20261005_151903", null, VideoFixtures.TRUTH),
        Source("2026-10-05/stills/20261005_213729", null, VideoFixtures.EVENING_TRUTH),
        Source("2026-10-05/stills/20261005_213817", null, VideoFixtures.EVENING_TRUTH),
        Source("2026-10-05/stills/20261005_213850", null, VideoFixtures.EVENING_TRUTH),
        Source("2026-10-05/stills/20261005_213929", null, VideoFixtures.EVENING_TRUTH),
        Source("2026-10-07/stills/20261007_152753", null, VideoFixtures.TRUTH_1007),
        Source("2026-10-07/stills/20261007_202058", null, VideoFixtures.STRIPED_TRUTH),
        Source("2026-10-07/stills/20261007_202156", null, VideoFixtures.STRIPED_TRUTH),
        Source("2026-10-07/stills/20261007_202318", null, VideoFixtures.STRIPED_U2_TRUTH),
        Source("2026-10-07/stills/20261007_202403", null, VideoFixtures.STRIPED_U2_TRUTH),
        Source("2026-10-07/stills/web_181940", null, VideoFixtures.STRIPED_TRUTH),
        Source("2026-10-08/stills/web_084657", 66..455, VideoFixtures.STRIPED_TRUTH),
        Source("2026-10-08c/stills/web_121505", 66..373, VideoFixtures.TRUTH_1008C),
        Source("2026-10-08c/stills/web_121505", 448..751, VideoFixtures.TRUTH_1008C),
    )

    private val recordings = FinishDelayHarness.RECORDINGS + listOf(
        "web_20261011_100403" to ColourSpikeHarness.TRUTH_1011,
        "web_20261011_101618" to ColourSpikeHarness.TRUTH_1011,
        "web_20261012_100233" to VideoFixtures.STRIPED_TRUTH,
    )

    @Test
    fun printNumbers() {
        assumeTrue("set FALSE_FACES=1 to run", System.getenv("FALSE_FACES") == "1")
        val csv = StringBuilder("source,frame,stickers,fit,edge,steps,sizes,bend,meanbend,true,pts\n")
        println("source | lattices | fitting no true face | read like over an edge | true faces (7+ of 8): steps, sizes, bend at 99 % | over an edge: the same, median")
        for (source in stills) {
            val files = File("../testdata/video/${source.dir}").listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
                .let { l -> if (source.range == null) l else l.subList(source.range.first, minOf(source.range.last, l.size)) }
            val name = source.dir.substringAfterLast('/') + (source.range?.let { ":${it.first}" } ?: "")
            val trueFlat = ArrayList<List<Double>>()
            val edgeFlat = ArrayList<List<Double>>()
            var all = 0
            var wrong = 0
            for (file in files) {
                val small = scaled(ImageIO.read(file), FrameSampler.FINDER_LONG_SIDE)
                val argb = small.getRGB(0, 0, small.width, small.height, null, 0, small.width)
                val found = FaceFinder.find(argb, small.width, small.height)
                for (l in found.faces + found.partial) {
                    val fit = fit(l.stickers.map { it?.color }, source.truth)
                    val flat = flatness(l)
                    val isTrue = fit >= 6
                    val edge = fit < 6 && overEdge(l.stickers.map { it?.color }, source.truth)
                    all++
                    if (!isTrue) wrong++
                    if (fit >= 7) trueFlat += flat
                    if (edge) edgeFlat += flat
                    csv.append("$name,${file.name},${l.hits},$fit,$edge,%.3f,%.3f,%.3f,%.3f,$isTrue,${l.stickers.joinToString(";") { b -> b?.let { "%.1f %.1f".format(java.util.Locale.ROOT, it.centre.x, it.centre.y) } ?: "-" }}\n".format(java.util.Locale.ROOT, flat[0], flat[1], flat[2], flat[3]))
                }
            }
            fun at(list: List<List<Double>>, i: Int, q: Double) = list.map { it[i] }.sorted().let { l -> if (l.isEmpty()) 0.0 else l[((l.size - 1) * q).toInt()] }
            fun row(list: List<List<Double>>, q: Double) = (0 until 3).joinToString(" ") { "%.2f".format(at(list, it, q)) }
            println("$name | $all | $wrong | ${edgeFlat.size} | ${row(trueFlat, 0.99)} | ${row(edgeFlat, 0.5)}")
        }
        for ((name, truth) in recordings) {
            var all = 0
            var wrong = 0
            var edge = 0
            for (e in VideoFixtures.loadRecording(name).entries) {
                if (e !is ScanRecording.Picture) continue
                for (f in e.found.faces) {
                    all++
                    if (fit(f.colors, truth) < 6) {
                        wrong++
                        if (overEdge(f.colors, truth)) edge++
                    }
                }
            }
            println("rec $name | $all | $wrong | $edge | - | -")
        }
        File("build/false-faces.csv").writeText(csv.toString())
    }

    /** The most of the eight outer stickers of [colors] (null: not found) that agree with a face of [truth] in some turn, red and orange alike. */
    private fun fit(colors: List<Rgb?>, truth: String): Int {
        val names = colors.map { it?.let(ColorClassifier::live) }
        fun same(a: CubeColor?, b: CubeColor) = a == b || (a.isWarm() && b.isWarm())
        var best = 0
        for (face in truth.chunked(9)) for (k in 0 until 4) {
            val turned = RotationSearch.turned(names, k)
            best = maxOf(best, (0 until 9).count { it != 4 && same(turned[it], CubeColor.fromLetter(face[it])) })
        }
        return best
    }

    /**
     * How far [lattice] is from lying on one plane, the largest over its rows and columns of three
     * stickers: the jump between the two half-steps |ln(d01 / d12)|, between the two sticker size ratios
     * |ln(a0 / a1) - ln(a1 / a2)| (areas), and the bend (sine of the angle between the half-steps); with
     * the mean bend of the three parallel lines (the larger of the two directions).
     */
    private fun flatness(lattice: FaceLattice): List<Double> {
        val s = lattice.stickers
        val lines = listOf(listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8))
        var steps = 0.0
        var sizes = 0.0
        var bend = 0.0
        var meanBend = 0.0
        val bends = Array(2) { ArrayList<Double>() }
        for ((li, line) in lines.withIndex()) {
            val a = s[line[0]] ?: continue
            val b = s[line[1]] ?: continue
            val c = s[line[2]] ?: continue
            val p = b.centre - a.centre
            val q = c.centre - b.centre
            if (p.length <= 0.0 || q.length <= 0.0) continue
            steps = maxOf(steps, abs(ln(p.length / q.length)))
            sizes = maxOf(sizes, abs(ln(a.area.toDouble() / b.area) - ln(b.area.toDouble() / c.area)))
            val sin = p.cross(q) / p.length / q.length
            bend = maxOf(bend, abs(sin))
            bends[li / 3] += sin
        }
        for (list in bends) if (list.size >= 2) meanBend = maxOf(meanBend, abs(list.average()))
        return listOf(steps, sizes, bend, meanBend)
    }

    /**
     * Whether [colors] read like a lattice over the cube's edge: some face of [truth] in some turn agrees
     * on the middle row and one outer row (at least five of those six) but on at most one of the other
     * outer row's three stickers.
     */
    private fun overEdge(colors: List<Rgb?>, truth: String): Boolean {
        val names = colors.map { it?.let(ColorClassifier::live) }
        fun same(a: CubeColor?, b: CubeColor) = a == b || (a.isWarm() && b.isWarm())
        for (face in truth.chunked(9)) for (k in 0 until 4) {
            val turned = RotationSearch.turned(names, k)
            fun agree(range: IntRange) = range.count { same(turned[it], CubeColor.fromLetter(face[it])) }
            val top = agree(0..2)
            val mid = agree(3..5)
            val bottom = agree(6..8)
            if (mid + top >= 5 && bottom <= 1 && turned.subList(6, 9).count { it != null } >= 2) return true
            if (mid + bottom >= 5 && top <= 1 && turned.subList(0, 3).count { it != null } >= 2) return true
        }
        return false
    }

    private fun CubeColor?.isWarm() = this == CubeColor.RED || this == CubeColor.ORANGE

    private fun scaled(image: BufferedImage, longSide: Int): BufferedImage {
        val k = longSide.toDouble() / maxOf(image.width, image.height)
        if (k >= 1.0) return image
        val w = (image.width * k).toInt()
        val h = (image.height * k).toInt()
        return BufferedImage(w, h, BufferedImage.TYPE_INT_RGB).also { out ->
            val g = out.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.drawImage(image, 0, 0, w, h, null)
            g.dispose()
        }
    }
}
