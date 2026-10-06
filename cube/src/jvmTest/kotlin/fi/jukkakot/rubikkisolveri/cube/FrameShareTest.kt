package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.RgbaFrame
import kotlin.test.Test
import kotlin.test.assertTrue

/** The metering point goes back to the camera frame exactly where [FrameSampler.upright] took that pixel from. */
class FrameShareTest {
    @Test
    fun uprightPointsMapBackToTheirFramePixel() {
        val w = 200
        val h = 120
        // Each pixel carries its own position: red = x, green = y.
        val bytes = ByteArray(w * h * 4) { i ->
            val p = i / 4
            when (i % 4) {
                0 -> (p % w).toByte()
                1 -> (p / w).toByte()
                else -> 0
            }
        }
        for (rotation in listOf(0, 90, 180, 270)) {
            val frame = RgbaFrame(w, h, w * 4, bytes, rotation, cropLeft = 20, cropTop = 10, cropRight = 180, cropBottom = 110)
            val image = FrameSampler.upright(frame, shortSide = 1_000)
            for ((i, j) in listOf(0 to 0, image.width / 3 to image.height / 2, image.width - 1 to image.height - 1)) {
                val share = Point((i + 0.5) / image.width, (j + 0.5) / image.height)
                val back = FrameSampler.toFrameShare(share, w, h, rotation, 20, 10, 180, 110)
                val argb = image.argb[j * image.width + i]
                val x = (argb shr 16) and 0xff
                val y = (argb shr 8) and 0xff
                assertTrue(kotlin.math.abs(back.x * w - (x + 0.5)) <= 1.0, "rotation $rotation x: $back vs $x")
                assertTrue(kotlin.math.abs(back.y * h - (y + 0.5)) <= 1.0, "rotation $rotation y: $back vs $y")
            }
        }
    }
}
