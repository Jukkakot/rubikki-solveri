package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Point
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** `scan-colour-spike`: the two variants do what the design says. */
class ColourSpikeTest {
    private val painted = listOf(
        Rgb(190, 25, 35), Rgb(240, 100, 20), Rgb(250, 250, 250),
        Rgb(20, 160, 60), Rgb(250, 220, 30), Rgb(20, 60, 200),
        Rgb(240, 100, 20), Rgb(190, 25, 35), Rgb(250, 250, 250),
    )

    /** A 300×300 picture: nine 80 px stickers on a black ground, 100 px apart, the middle one at (150, 150). */
    private fun grid(): IntArray = IntArray(300 * 300) { i ->
        val x = i % 300
        val y = i / 300
        val col = x / 100
        val row = y / 100
        val inSticker = x % 100 in 10 until 90 && y % 100 in 10 until 90
        if (!inSticker) 0xff000000.toInt() else painted[row * 3 + col].let { (0xff shl 24) or (it.r shl 16) or (it.g shl 8) or it.b }
    }

    @Test
    fun rereadTakesEachStickerFromTheFullPicture() {
        // Found in a picture three times smaller (100 px): centre (50, 50), 33 px steps, colours blended.
        val found = FaceReading(List(9) { Rgb(120, 60, 60) }.let { it.toMutableList<Rgb?>().also { l -> l[8] = null } }, Point(50.0, 50.0), Point(100 / 3.0, 0.0), Point(0.0, 100 / 3.0))
        val again = ColourSpike.reread(found, grid(), 300, 300, 3.0)
        for (n in 0 until 8) assertEquals(painted[n], again.colors[n], "sticker $n")
        assertNull(again.colors[8], "a sticker not found stays not found")
        assertEquals(found.centre, again.centre)
    }

    private fun face(vararg colors: Rgb) = FaceReading(colors.toList(), Point(0.0, 0.0), Point(1.0, 0.0), Point(0.0, 1.0))

    @Test
    fun warmSplitSeparatesRedFromAReddishOrange() {
        val red = Rgb(200, 30, 40) // hue ≈ −3°
        val orange = Rgb(220, 60, 30) // hue ≈ 9°, nearer red than the palette's orange
        val blue = Rgb(20, 60, 200)
        val split = ColourSpike.WarmSplit()
        // Too few warm readings: nothing changes.
        val first = face(red, red, red, orange, orange, orange, blue, blue, blue)
        assertEquals(first, split.apply(listOf(first)).single())
        assertNull(split.split)
        repeat(3) { split.apply(listOf(first)) }
        val s = assertNotNull(split.split)
        assertTrue(ColourSpike.hue(red) < s.first && ColourSpike.hue(orange) > s.first, "split at ${s.first}")
        // A darker orange stays orange at its own brightness; blue is untouched.
        val dark = Rgb(110, 33, 16)
        val out = split.apply(listOf(face(dark, red, red, orange, orange, orange, blue, blue, blue))).single()
        assertEquals(110, maxOf(out.colors[0]!!.r, out.colors[0]!!.g, out.colors[0]!!.b))
        assertTrue(ColourSpike.hue(out.colors[0]!!) > s.first)
        assertEquals(blue, out.colors[6])
    }

    @Test
    fun warmSplitWaitsForTwoGroupsApart() {
        val split = ColourSpike.WarmSplit()
        val red = Rgb(200, 30, 40)
        repeat(10) { split.apply(listOf(face(red, red, red, red, red, red, red, red, red))) }
        assertNull(split.split, "one colour only")
    }
}
