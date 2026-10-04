package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Real data from the user's phone (Galaxy S24, 2026-10-03): grid pictures and readings. */
class PhoneScanTest {
    private fun picture(name: String): IntArray {
        val image = ImageIO.read(javaClass.getResource("/scan/$name.png")!!)
        val size = FrameSampler.PICTURE_SIZE
        assertEquals(size, image.width)
        return IntArray(size * size) { image.getRGB(it % size, it / size) }
    }

    @Test
    fun deskIsNotACube() {
        assertFalse(FrameSampler.looksLikeCube(picture("desk")))
    }

    @Test
    fun everyFaceOfTheCubeIsACube() {
        // Including the white face, which reads grey-blue.
        for (face in listOf("F", "R", "B", "L", "U", "D")) {
            val contrast = FrameSampler.gapContrast(picture(face))
            assertTrue(contrast.all { it >= FrameSampler.MIN_GAP_CONTRAST }, "$face $contrast")
        }
    }

    /** The six accepted faces of the first fully successful phone scan (scan.face lines). */
    private val firstScan = mapOf(
        Face.U to "97b4cd,9bb8d1,97b6cc,95adc7,9ab6d3,99b5cc,8ba4bc,93adc6,91acc1",
        Face.R to "94293a,9a2031,951625,8f0518,99061b,961a2a,98bf48,8e0919,a2cb51",
        Face.F to "0ead68,e85d43,04ab57,04a653,04ae56,04a95e,049d48,a1c955,0360a8",
        Face.D to "963043,0ab25f,961424,035fab,add94e,a9d25d,03589f,a2ca50,03a357",
        Face.L to "db5f4b,0dac5d,e35939,a2c955,e35533,e25a3e,97bd4a,d74b2b,a0c75c",
        Face.B to "036ab2,0369b5,0363af,035da9,0360b0,e45639,d6472a,035aa5,da4f35",
    )

    @Test
    fun firstSuccessfulScanStaysRight() {
        val samples = Face.entries.flatMap { face -> firstScan.getValue(face).split(",").map { Rgb.fromHex(it) } }
        val result = ColorClassifier.classify(samples)
        assertEquals(
            "WWWWWWWWWRRRRRRYRYGOGGGGGYBRGRBYYBYGOGOYOOYOYBBBBBOOBO",
            result.colors.joinToString("") { it.letter.toString() },
        )
        assertTrue(result.uncertain().isEmpty())
        assertTrue(CubeCheck.validity(CubeEditor(result.colors).toCube()!!).isValid)
    }
}
