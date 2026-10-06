package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceCodec
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals

class FaceCodecTest {
    @Test
    fun facesComeBackAsTheyWent() {
        val full = FaceReading(List(9) { Rgb(it * 20, 255 - it, 7) }, Point(180.25, 320.0), Point(40.125, -1.5), Point(0.333, 41.0))
        val partial = full.copy(colors = full.colors.toMutableList().also { it[1] = null; it[8] = null }, centre = Point(10.0, 12.0))
        val sent = FaceCodec.Found(listOf(full, partial), 360, 640, 23)
        val back = FaceCodec.decode(FaceCodec.encode(sent))
        assertEquals(listOf(full, partial), back.faces)
        assertEquals(360, back.width)
        assertEquals(640, back.height)
        assertEquals(23, back.finderMs)
    }

    @Test
    fun noFaces() {
        val back = FaceCodec.decode(FaceCodec.encode(FaceCodec.Found(emptyList(), 360, 203, 0)))
        assertEquals(emptyList(), back.faces)
        assertEquals(203, back.height)
    }
}
