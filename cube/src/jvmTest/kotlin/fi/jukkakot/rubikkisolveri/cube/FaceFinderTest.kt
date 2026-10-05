package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceFinder
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FaceFinderTest {
    private val width = 360
    private val height = 640
    private val table = 0xffc8b496.toInt()
    private val plastic = 0xff141414.toInt()
    private val nine = listOf(0xffe6e6e6, 0xffdcd228, 0xff1eaa50, 0xff1450be, 0xffbe1923, 0xfff06414, 0xff1eaa50, 0xffe6e6e6, 0xff1450be)
        .map { it.toInt() }

    /** A frame with one face whose corner is at [origin] and whose rows and columns step by [u] and [v] (one sticker each). */
    private fun frame(origin: Point, u: Point, v: Point): IntArray {
        val det = u.cross(v)
        return IntArray(width * height) { p ->
            val d = Point((p % width).toDouble(), (p / width).toDouble()) - origin
            // Face coordinates (s along u, t along v) of the pixel.
            val s = d.cross(v) / det
            val t = u.cross(d) / det
            when {
                s < -0.05 || s >= 3.05 || t < -0.05 || t >= 3.05 -> table
                else -> {
                    val fs = s - floor(s)
                    val ft = t - floor(t)
                    val cell = floor(t).toInt().coerceIn(0, 2) * 3 + floor(s).toInt().coerceIn(0, 2)
                    if (s in 0.0..3.0 && t in 0.0..3.0 && fs in 0.1..0.9 && ft in 0.1..0.9) nine[cell] else plastic
                }
            }
        }
    }

    private fun Rgb.argb() = (0xff shl 24) or (r shl 16) or (g shl 8) or b

    @Test
    fun drawnFaceGivesNineStickers() {
        val blobs = FaceFinder.blobs(frame(Point(80.0, 200.0), Point(60.0, 0.0), Point(0.0, 60.0)), width, height)
        assertEquals(9, blobs.size)
    }

    @Test
    fun straightFaceIsFoundInReadingOrder() {
        val result = FaceFinder.find(frame(Point(80.0, 200.0), Point(60.0, 0.0), Point(0.0, 60.0)), width, height)
        assertEquals(1, result.faces.size)
        assertEquals(nine, result.faces[0].colors.map { it.argb() })
    }

    @Test
    fun shearedFaceIsFound() {
        // Seen at an angle: rows tilt down to the right, columns lean and are foreshortened.
        val result = FaceFinder.find(frame(Point(60.0, 220.0), Point(55.0, 18.0), Point(-22.0, 34.0)), width, height)
        assertEquals(1, result.faces.size)
        assertEquals(nine, result.faces[0].colors.map { it.argb() })
    }

    @Test
    fun turnedFaceReadsAsATurnOfTheSameFace() {
        // Turned a quarter clockwise in the picture: rows step down, columns step left.
        val result = FaceFinder.find(frame(Point(260.0, 200.0), Point(0.0, 60.0), Point(-60.0, 0.0)), width, height)
        assertEquals(1, result.faces.size)
        val turned = listOf(6, 3, 0, 7, 4, 1, 8, 5, 2).map { nine[it] }
        assertEquals(turned, result.faces[0].colors.map { it.argb() })
    }

    @Test
    fun scanPictureGivesOneFace() {
        // A grid picture of the guided scan (committed test material), the face filling it.
        val file = File("../testdata/video/2026-10-05/scan-pictures/share2057364588164787508.png")
        val image = ImageIO.read(file)
        val argb = IntArray(image.width * image.height) { image.getRGB(it % image.width, it / image.width) }
        val result = FaceFinder.find(argb, image.width, image.height)
        assertEquals(1, result.faces.size, "blobs ${result.blobs.size}")
    }

    /** The colour [FaceFinder] gives a blob of [pixels]. */
    private fun colorOf(pixels: List<Rgb>): Rgb =
        FaceFinder.medianColor(IntArray(pixels.size) { it }, pixels.size, IntArray(pixels.size) { pixels[it].r }, IntArray(pixels.size) { pixels[it].g }, IntArray(pixels.size) { pixels[it].b })

    @Test
    fun aBrightWashedOutPatchDoesNotChangeAStickersColour() {
        val red = List(45) { Rgb(186 + it % 9, 22 + it % 7, 30 + it % 5) }
        // A lamp's reflection over more than half the sticker: from pale red to nearly white.
        val glare = List(55) { k -> Rgb(minOf(255, 225 + k), 120 + 2 * k, 120 + 2 * k) }
        val color = colorOf(red + glare)
        assertEquals(CubeColor.RED, ColorClassifier.live(color), color.toHex())
        assertTrue(color.g < 40, color.toHex())
        // A white sticker with a brighter patch keeps it (all its pixels are near grey).
        val white = colorOf(List(60) { Rgb(200, 200, 195) } + List(40) { Rgb(250, 250, 250) })
        assertEquals(Rgb(200, 200, 195), white)
    }
}
