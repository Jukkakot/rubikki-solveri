package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.WarmRefs
import org.junit.Assume.assumeTrue
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * `scan-false-faces-finish` 3.1: the stills of `202156` through the scan with CENTRES references, which
 * finishes with every side half round; at the finish, how each face's turn was settled and what the
 * half-round cube costs. Runs only with MIRROR=1.
 */
class MirrorFinishHarness {
    @Test
    fun printFinish() {
        assumeTrue("set MIRROR=1 to run", System.getenv("MIRROR") == "1")
        val dir = System.getenv("MIRROR_DIR") ?: "2026-10-07/stills/20261007_202156"
        val mode = WarmRefs.valueOf(System.getenv("MIRROR_MODE") ?: "CENTRES")
        val files = File("../testdata/video/$dir").listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
        val scan = VideoScan(ColorScheme.STANDARD, mode)
        for ((i, file) in files.withIndex()) {
            val small = scaled(ImageIO.read(file), FrameSampler.FINDER_LONG_SIDE)
            val argb = small.getRGB(0, 0, small.width, small.height, null, 0, small.width)
            val faces = FaceFinder.find(argb, small.width, small.height).let { it.faces + it.partial }.map(FaceReading::of)
            val s = scan.onFrame(faces, i * 100L)
            if (i % 20 == 0 || s.finished) println("${file.name}\n${scan.finishDetail}")
            if (s.finished) {
                println("finished ${s.stickers.joinToString("") { it?.letter?.toString() ?: "?" }}")
                break
            }
        }
    }

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
