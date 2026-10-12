package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.WarmRefs
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `scan-false-faces-finish` 3.2: the striped cube's mirror (every side half round) fits the readings of
 * the stills of [VideoFixtures.STRIPED_TABLE] as well as the true cube while its sides are seen one at a
 * time; with CENTRES references the scan settled each side's turn alone by the best cube and finished with
 * the mirror at 18 s. It waits for a picture that tells them apart and finishes with the true cube.
 */
class MirrorFinishTest {
    @Test
    fun stripedTableDoesNotFinishAsItsMirror() {
        val files = File("../testdata/video/2026-10-07/stills/${VideoFixtures.STRIPED_TABLE}").listFiles { f -> f.name.endsWith(".jpg") }!!.sortedBy { it.name }
        val scan = VideoScan(ColorScheme.STANDARD, WarmRefs.CENTRES)
        var cube: String? = null
        for ((i, file) in files.withIndex()) {
            val image = scaled(ImageIO.read(file), FrameSampler.FINDER_LONG_SIDE)
            val argb = image.getRGB(0, 0, image.width, image.height, null, 0, image.width)
            val faces = FaceFinder.find(argb, image.width, image.height).let { it.faces + it.partial }.map(FaceReading::of)
            val s = scan.onFrame(faces, i * 100L)
            if (s.finished) {
                cube = s.stickers.joinToString("") { it?.letter?.toString() ?: "?" }
                break
            }
        }
        assertTrue(cube != null, "never finished")
        assertEquals(VideoFixtures.STRIPED_TRUTH, cube)
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
