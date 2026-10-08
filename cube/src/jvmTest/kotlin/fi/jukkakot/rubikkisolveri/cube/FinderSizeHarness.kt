package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * `scan-speed-up-2` design 1: the finder on the committed stills scaled to several long sides — full
 * faces per picture, finder time, and the whole rules scan's result where the cube is known. Runs only
 * with SIZE_HARNESS=1 (about a minute).
 */
class FinderSizeHarness {
    private val videos = listOf(
        Triple("2026-10-05/stills/20261005_151828", null, VideoFixtures.TRUTH),
        Triple("2026-10-07/stills/web_181940", null, VideoFixtures.STRIPED_TRUTH),
        Triple("2026-10-07/stills/20261007_202058", null, VideoFixtures.STRIPED_TRUTH),
        Triple("2026-10-08/stills/web_084657", 66..455, VideoFixtures.STRIPED_TRUTH),
        Triple("2026-10-08c/stills/web_121505", 66..373, VideoFixtures.TRUTH_1008C),
    )

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

    @Test
    fun printNumbers() {
        assumeTrue(System.getenv("SIZE_HARNESS") == "1")
        val sizes = listOf(640, 480, 360, 300, 240)
        println("video | long side: full faces/picture, finder ms, scan (frame clear, right)")
        for ((dir, range, truth) in videos) {
            val files = File("../testdata/video/$dir").listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
                .let { l -> if (range == null) l else l.subList(range.first, minOf(range.last, l.size)) }
            val row = StringBuilder(dir.substringAfterLast('/'))
            for (size in sizes) {
                files.take(5).forEach { find(scaled(ImageIO.read(it), size)) }
                var ms = 0.0
                var full = 0
                val scan = VideoScan(engine = ScanEngine.RULES)
                var clearAt = -1
                for ((i, file) in files.withIndex()) {
                    val p = scaled(ImageIO.read(file), size)
                    val start = System.nanoTime()
                    val faces = find(p)
                    ms += (System.nanoTime() - start) / 1e6
                    full += faces.count { it.isFull }
                    val s = scan.onFrame(faces, i * 100L)
                    if (clearAt < 0 && s.complete) clearAt = i
                }
                val cube = scan.state.stickers.joinToString("") { it?.letter?.toString() ?: "?" }
                val right = clearAt >= 0 && (0 until 4).any { cube == truth } // the scan's net order
                row.append(" | $size: ${"%.2f".format(full.toDouble() / files.size)} ${"%.1f".format(ms / files.size)} ms ${if (clearAt >= 0) "clear@$clearAt ${if (right) "ok" else "WRONG"}" else "-"}")
            }
            println(row)
        }
    }

    private fun find(image: BufferedImage): List<FaceReading> {
        val argb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
        return FaceFinder.find(argb, image.width, image.height).let { it.faces + it.partial }.map(FaceReading::of)
    }
}
