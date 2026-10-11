package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.WarmRefs
import org.junit.Assume.assumeTrue
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Every stills source and recording with a known cube through the scan, once per way of referencing red and
 * orange (`scan-warm-references` design 5; built for `scan-colour-spike`, whose variants A and B its findings
 * dropped). Runs only with COLOUR_SPIKE=1 (a few minutes); prints the table for the change's findings.
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

    @Test
    fun printTables() {
        assumeTrue("set COLOUR_SPIKE=1 to run", System.getenv("COLOUR_SPIKE") == "1")
        val sources = ArrayList<Triple<String, List<Picture>, String>>()
        for (source in stills) {
            val files = File("../testdata/video/${source.dir}").listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
                .let { l -> if (source.range == null) l else l.subList(source.range.first, minOf(source.range.last, l.size)) }
            val pictures = files.mapIndexed { i, file ->
                val small = scaled(ImageIO.read(file), FrameSampler.FINDER_LONG_SIDE)
                Picture(i * 100L, FaceFinder.find(argb(small), small.width, small.height).let { it.faces + it.partial }.map(FaceReading::of))
            }
            sources += Triple(source.dir.substringAfterLast('/') + (source.range?.let { ":${it.first}" } ?: ""), pictures, source.truth)
        }
        for ((name, truth) in recordings) {
            val pictures = VideoFixtures.loadRecording(name).entries.map { e ->
                when (e) {
                    is ScanRecording.Reset -> Picture(e.ms, emptyList(), reset = true)
                    is ScanRecording.Picture -> Picture(e.ms, e.found.faces)
                }
            }
            sources += Triple("rec $name", pictures, truth)
        }
        // Warmed up first: the JIT reaches every path the timing goes through.
        for ((_, pictures, _) in sources.take(3)) for (mode in WarmRefs.entries) scan(pictures, mode)
        println("source | mode | stickers right | red<->orange wrong (of warm) | finished at | wrong cube | scan ms a picture")
        for ((name, pictures, truth) in sources) for (mode in WarmRefs.entries) report(name, mode, pictures, truth)
    }

    /** The scan over [pictures] with [mode]: the first finish after the last reset before it (time from that segment's start, cube), and the mean scan time a picture. */
    private fun scan(pictures: List<Picture>, mode: WarmRefs): Pair<Pair<Long, String>?, Double> {
        var scan = VideoScan(fi.jukkakot.rubikkisolveri.cube.ColorScheme.STANDARD, mode)
        var t0: Long? = null
        var finished: Pair<Long, String>? = null
        var nanos = 0L
        var count = 0
        for (p in pictures) {
            if (p.reset) {
                scan = VideoScan(fi.jukkakot.rubikkisolveri.cube.ColorScheme.STANDARD, mode)
                t0 = null
                continue
            }
            if (t0 == null) t0 = p.ms
            val start = System.nanoTime()
            val s = scan.onFrame(p.faces, p.ms)
            nanos += System.nanoTime() - start
            count++
            if (s.finished) {
                finished = (p.ms - t0) to s.stickers.joinToString("") { it?.letter?.toString() ?: "?" }
                break
            }
        }
        return finished to nanos / 1e6 / maxOf(1, count)
    }

    private fun report(source: String, mode: WarmRefs, pictures: List<Picture>, truth: String) {
        // Stickers: matched to the true cube, named by the source's true centres (as ReadingMeasure).
        class Sample(val truth: CubeColor, val lab: Lab, val centre: Boolean)
        val samples = ArrayList<Sample>()
        for (p in pictures) for (face in p.faces) {
            val colors = ReadingMeasure.trueColors(face, truth) ?: continue
            for (n in 0 until 9) samples += Sample(colors[n], face.colors[n]!!.toLab(), n == 4)
        }
        val refs = CubeColor.entries.mapNotNull { c -> samples.filter { it.centre && it.truth == c }.map { it.lab }.takeIf { it.isNotEmpty() }?.let { c to Lab.mean(it) } }.toMap()
        val named = samples.filter { !it.centre }.map { s -> s.truth to refs.minBy { it.value.distance(s.lab) }.key }
        val right = named.count { it.first == it.second }.toDouble() / maxOf(1, named.size)
        val warm = named.filter { it.first == CubeColor.RED || it.first == CubeColor.ORANGE }
        val warmWrong = warm.count { (t, n) -> (t == CubeColor.RED && n == CubeColor.ORANGE) || (t == CubeColor.ORANGE && n == CubeColor.RED) }.toDouble() / maxOf(1, warm.size)
        val (finished, ms) = scan(pictures, mode)
        println(
            "$source | $mode | %.1f %% | %.1f %% | %s | %s | %.2f".format(
                right * 100, warmWrong * 100, finished?.first?.let { "%.1f s".format(it / 1000.0) } ?: "never",
                if (finished != null && finished.second != truth) "WRONG ${finished.second}" else "-", ms,
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
