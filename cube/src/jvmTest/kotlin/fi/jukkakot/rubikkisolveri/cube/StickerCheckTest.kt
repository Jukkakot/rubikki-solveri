package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The per-cell sticker check, on grid pictures built from cell readings (median RGB of each cell's
 * middle) of the user's shared pictures, 2026-10-04 (Galaxy S24, Samsung Internet).
 */
class StickerCheckTest {
    private val size = FrameSampler.PICTURE_SIZE
    private val cell = size / 3
    private val gap = 0xff101010.toInt()

    private fun argb(hex: String) = (0xff shl 24) or hex.toInt(16)

    /** A cell filled by [fill] (x, y inside the cell to ARGB), with a dark gap round it. */
    private fun picture(cells: List<(Int, Int) -> Int>): IntArray = IntArray(size * size) { i ->
        val x = i % size
        val y = i / size
        val cx = x % cell
        val cy = y % cell
        if (cx < 3 || cy < 3 || cx >= cell - 3 || cy >= cell - 3) gap else cells[(y / cell) * 3 + x / cell](cx, cy)
    }

    private fun flat(hex: String): (Int, Int) -> Int = argb(hex).let { c -> { _, _ -> c } }

    private fun face(hexes: String) = picture(hexes.split(",").map { flat(it) })

    /** A pattern of two colours in 4-pixel squares. */
    private fun checker(a: String, b: String): (Int, Int) -> Int = { x, y -> if ((x / 4 + y / 4) % 2 == 0) argb(a) else argb(b) }

    @Test
    fun realFacesPass() {
        val faces = listOf(
            "f64202,a8cb18,005193,00aa30,dd3400,008629,8b0000,710000,c52800", // dark reds
            "007eda,00d053,ff4b00,d5f528,ff4300,9e0006,ff4702,00b036,810003",
            "c9dcf1,c6daed,ff4500,ff4100,0067bc,0064b2,c6e320,970000,df3300", // daylight whites
            "b5cddd,fe4100,00579d,00bb3b,910000,00922a,00b635,870000,8b9fab",
            "007ad3,00cd47,bed4e7,ff3e00,a20000,930209,b2c4d4,00a934,00952a",
            "838e91,838e91,838e91,838e91,838e91,838e91,838e91,838e91,838e91", // evening white
            "97b4cd,9bb8d1,97b6cc,95adc7,9ab6d3,99b5cc,8ba4bc,93adc6,91acc1", // grey-blue white, 2026-10-03
            "94293a,9a2031,951625,8f0518,99061b,961a2a,98bf48,8e0919,a2cb51",
        )
        for (hexes in faces) {
            val check = FrameSampler.check(face(hexes))
            assertEquals(List(9) { true }, check.stickerCells, hexes)
            assertTrue(check.looksLikeCube, hexes)
        }
    }

    @Test
    fun roomColoursFail() {
        // Black, grey-brown, beige and dark olive cells of the room and blanket pictures.
        for (hex in listOf("0a0b10", "000000", "6e5b47", "897059", "b17f6f", "7a5c52", "284723")) {
            val cells = FrameSampler.stickerCells(face(List(9) { "ff4500" }.toMutableList().also { it[4] = hex }.joinToString(",")))
            assertFalse(cells[4], hex)
            assertEquals(8, cells.count { it }, hex)
        }
    }

    @Test
    fun yellowMiddleWithPatternedNeighboursFails() {
        val blanket = picture(List(9) { if (it == 4) flat("d1aa31") else checker("7a5c52", "bf993e") })
        val check = FrameSampler.check(blanket)
        assertEquals(List(9) { it == 4 }, check.stickerCells)
        assertFalse(check.looksLikeCube)
    }

    @Test
    fun cellOnAGapFails() {
        // Red on the left, green on the right, the dark gap between them through the middle.
        val straddle: (Int, Int) -> Int = { x, _ ->
            when {
                x < cell / 2 - 3 -> argb("ff4500")
                x < cell / 2 + 3 -> gap
                else -> argb("00aa30")
            }
        }
        val check = FrameSampler.check(picture(List(9) { if (it == 7) straddle else flat("0067bc") }))
        assertEquals(List(9) { it != 7 }, check.stickerCells)
        assertFalse(check.looksLikeCube)
    }

    @Test
    fun aPictureIsCheckedQuickly() {
        val picture = face("f64202,a8cb18,005193,00aa30,dd3400,008629,8b0000,710000,c52800")
        repeat(200) { FrameSampler.check(picture) }
        val runs = 200
        val start = System.nanoTime()
        repeat(runs) { FrameSampler.check(picture) }
        val millis = (System.nanoTime() - start) / 1e6 / runs
        assertTrue(millis < 2.0, "$millis ms")
    }
}
