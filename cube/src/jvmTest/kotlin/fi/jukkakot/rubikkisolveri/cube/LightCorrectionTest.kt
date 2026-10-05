package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LightCorrectionTest {
    private val colors = listOf(
        CubeColor.WHITE, CubeColor.RED, CubeColor.ORANGE,
        CubeColor.YELLOW, CubeColor.WHITE, CubeColor.GREEN,
        CubeColor.BLUE, CubeColor.RED, CubeColor.ORANGE,
    )

    private fun face(tint: (Rgb) -> Rgb) =
        FaceReading(colors.map { tint(ColorClassifier.DEFAULT_PALETTE.getValue(it)) }, Point(100.0, 100.0), Point(30.0, 0.0), Point(0.0, 30.0))

    @Test
    fun aFrameTintedWarmReadsLikeTheUntintedOne() {
        fun warm(c: Rgb) = Rgb(minOf(255, (c.r * 1.1).toInt()), c.g, (c.b * 0.7).toInt())
        val plain = face { it }
        val tinted = face(::warm)
        val light = LightCorrection()
        light.update(LightCorrection.likelyWhites(listOf(tinted)))
        val corrected = light.apply(tinted)
        for (n in 0 until 9) {
            val c = corrected.colors[n]!!
            assertEquals(colors[n], ColorClassifier.live(c), "sticker $n read as ${c.toHex()}")
            assertTrue(c.toLab().distance(plain.colors[n]!!.toLab()) < 8.0, "sticker $n: ${c.toHex()} vs ${plain.colors[n]!!.toHex()}")
        }
        // Untinted, the correction changes next to nothing.
        val again = LightCorrection().apply { update(LightCorrection.likelyWhites(listOf(plain))) }
        assertTrue(again.gains.all { it in 0.99..1.01 })
    }
}
