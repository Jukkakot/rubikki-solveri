package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * `scan-colour-spike` design 3: today's colours against idea A (re-read from the full still) and idea
 * B (red and orange split by the scan's own warm readings) on every stills source and recording with a
 * known cube. Runs only with COLOUR_SPIKE=1 (a few minutes); prints the tables for `findings.md`.
 */
class ColourSpikeHarness {
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
        "web_20261011_100403" to TRUTH_1011,
        "web_20261011_101618" to TRUTH_1011,
    )

    /** One picture: when it was taken and the faces found in it. */
    private class Picture(val ms: Long, val faces: List<FaceReading>, val reset: Boolean = false)

    private class Row(val right: Double, val warmWrong: Double, val finishedMs: Long?, val wrongCube: Boolean)

    @Test
    fun printTables() {
        assumeTrue("set COLOUR_SPIKE=1 to run", System.getenv("COLOUR_SPIKE") == "1")
        println("source | variant | stickers right | red<->orange wrong (of warm) | finished at | wrong cube")
        var rereadNanos = 0L
        var rereadPictures = 0
        for (source in stills) {
            val files = File("../testdata/video/${source.dir}").listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
                .let { l -> if (source.range == null) l else l.subList(source.range.first, minOf(source.range.last, l.size)) }
            val today = ArrayList<Picture>()
            val a = ArrayList<Picture>()
            for ((i, file) in files.withIndex()) {
                val image = ImageIO.read(file)
                val small = scaled(image, FrameSampler.FINDER_LONG_SIDE)
                val faces = FaceFinder.find(argb(small), small.width, small.height).let { it.faces + it.partial }.map(FaceReading::of)
                val full = argb(image)
                val start = System.nanoTime()
                val again = faces.map { ColourSpike.reread(it, full, image.width, image.height, image.width.toDouble() / small.width) }
                rereadNanos += System.nanoTime() - start
                rereadPictures++
                today += Picture(i * 100L, faces)
                a += Picture(i * 100L, again)
            }
            val name = source.dir.substringAfterLast('/') + (source.range?.let { ":${it.first}" } ?: "")
            report(name, "today", today, today, source.truth)
            report(name, "A", today, a, source.truth)
            report(name, "B", today, split(today), source.truth)
            report(name, "A+B", today, split(a), source.truth)
            report(name, "B2", today, split(today, 0.25), source.truth)
        }
        println("A: re-read %.2f ms a picture (JVM, 360×640 stills)".format(rereadNanos / 1e6 / rereadPictures))
        for ((name, truth) in recordings) {
            val pictures = VideoFixtures.loadRecording(name).entries.map { e ->
                when (e) {
                    is ScanRecording.Reset -> Picture(e.ms, emptyList(), reset = true)
                    is ScanRecording.Picture -> Picture(e.ms, e.found.faces)
                }
            }
            report(name, "today", pictures, pictures, truth)
            report(name, "B", pictures, split(pictures), truth)
            report(name, "B2", pictures, split(pictures, 0.25), truth)
        }
    }

    /** Idea B over [pictures] (B2: only readings clear of the split by [margin]), a fresh split at each reset. */
    private fun split(pictures: List<Picture>, margin: Double = 0.0): List<Picture> {
        var warm = ColourSpike.WarmSplit(margin)
        return pictures.map { p ->
            if (p.reset) {
                warm = ColourSpike.WarmSplit(margin)
                p
            } else {
                Picture(p.ms, warm.apply(p.faces))
            }
        }
    }

    private fun report(source: String, variant: String, base: List<Picture>, pictures: List<Picture>, truth: String) {
        // Stickers: matched to the true cube on today's reading, named by the source's true centres (as ReadingMeasure).
        class Sample(val truth: CubeColor, val lab: Lab, val centre: Boolean)
        val samples = ArrayList<Sample>()
        for ((b, p) in base.zip(pictures)) for ((face, read) in b.faces.zip(p.faces)) {
            val colors = ReadingMeasure.trueColors(face, truth) ?: continue
            for (n in 0 until 9) samples += Sample(colors[n], read.colors[n]!!.toLab(), n == 4)
        }
        val refs = CubeColor.entries.mapNotNull { c -> samples.filter { it.centre && it.truth == c }.map { it.lab }.takeIf { it.isNotEmpty() }?.let { c to Lab.mean(it) } }.toMap()
        val named = samples.filter { !it.centre }.map { s -> s.truth to refs.minBy { it.value.distance(s.lab) }.key }
        val right = named.count { it.first == it.second }.toDouble() / maxOf(1, named.size)
        val warm = named.filter { it.first == CubeColor.RED || it.first == CubeColor.ORANGE }
        val warmWrong = warm.count { (t, n) -> (t == CubeColor.RED && n == CubeColor.ORANGE) || (t == CubeColor.ORANGE && n == CubeColor.RED) }.toDouble() / maxOf(1, warm.size)
        // The scan: the first finished picture after the last reset before it, its time from that segment's start.
        var scan = VideoScan()
        var t0: Long? = null
        var finished: Pair<Long, String>? = null
        for (p in pictures) {
            if (p.reset) {
                scan = VideoScan()
                t0 = null
                continue
            }
            if (t0 == null) t0 = p.ms
            val s = scan.onFrame(p.faces, p.ms)
            if (s.finished) {
                finished = (p.ms - t0) to s.stickers.joinToString("") { it?.letter?.toString() ?: "?" }
                break
            }
        }
        val row = Row(right, warmWrong, finished?.first, finished != null && finished.second != truth)
        println(
            "$source | $variant | %.1f %% | %.1f %% | %s | %s".format(
                row.right * 100, row.warmWrong * 100, row.finishedMs?.let { "%.1f s".format(it / 1000.0) } ?: "never",
                if (row.wrongCube) "WRONG ${finished!!.second}" else "-",
            ),
        )
    }

    private fun argb(image: BufferedImage): IntArray = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)

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

    companion object {
        /** The cube of the browser scans of 2026-10-11 10:04–10:16 (the scans in between finished with it). */
        const val TRUTH_1011 = "WBBRWGWWYORRYRRGYOBRGOGOOWYYGRWYGGYWGBRWOGRBBYOOYBOBBW"
    }
}
