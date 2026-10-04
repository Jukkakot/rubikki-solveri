package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [net] (54 in net order) as the camera sees it when face f is held turned turns[f] quarter turns. */
fun <T> asSeen(net: List<T>, turns: Map<Face, Int>): List<T> = Face.entries.flatMap { face ->
    RotationSearch.turned(net.subList(face.ordinal * 9, face.ordinal * 9 + 9), 4 - (turns[face] ?: 0))
}

class RotationSearchTest {
    private val turns = mapOf(Face.U to 1, Face.R to 0, Face.F to 3, Face.D to 2, Face.L to 1, Face.B to 2)

    @Test
    fun turningIsAQuarterClockwise() {
        // 0 1 2 / 3 4 5 / 6 7 8 turned clockwise: the left column becomes the top row.
        assertEquals(listOf(6, 3, 0, 7, 4, 1, 8, 5, 2), RotationSearch.turned((0 until 9).toList(), 1))
        assertEquals((0 until 9).toList(), RotationSearch.turned(RotationSearch.turned((0 until 9).toList(), 3), 1))
    }

    @Test
    fun scrambledCubeWithTurnedFacesIsRestored() {
        for (seed in 0 until 10) {
            val cube = Cube.solved().apply(Scramble.random(25, Random(seed)))
            val result = RotationSearch.search(asSeen(cube.toList(), turns))
            assertEquals(cube.toList(), result.colors, "seed $seed")
            assertEquals(turns, result.rotations, "seed $seed")
            assertTrue(result.validity.isValid)
            assertTrue(result.ambiguous.isEmpty(), "seed $seed ${result.ambiguous}")
        }
    }

    @Test
    fun phoneScanWithTurnedFaces() {
        val samples = Face.entries.flatMap { face -> PHONE_SCAN.getValue(face).split(",").map { Rgb.fromHex(it) } }
        val classified = ColorClassifier.classify(asSeen(samples, turns))
        val result = RotationSearch.search(classified.colors)
        assertEquals(PHONE_SCAN_COLORS, result.colors.joinToString("") { it.letter.toString() })
        // The source maps every sticker back to its reading (the all-white top may come out turned any way).
        assertEquals(result.colors, result.source.map { classified.colors[it] })
        assertEquals(turns - Face.U, result.rotations - Face.U)
    }

    @Test
    fun misreadStickerStillGivesTheRotations() {
        val cube = Cube.solved().apply(Scramble.random(25, Random(42)))
        val a = Stickers.index(Face.F, 1)
        val b = (0 until 54).first { it % 9 != 4 && it / 9 != Face.F.ordinal && cube[it] != cube[a] }
        val misread = cube.with(a, cube[b]).with(b, cube[a])
        val result = RotationSearch.search(asSeen(misread.toList(), turns))
        assertEquals(turns, result.rotations)
        assertEquals(misread.toList(), result.colors)
        assertFalse(result.validity.isValid)
    }

    /**
     * The [wrong] face's readings were confirmed as its opposite face: the two blocks are exchanged
     * and the two colours named the wrong way round everywhere. Returns the colours and readings.
     */
    private fun mislabelled(cube: Cube, wrong: Face): Pair<List<CubeColor>, List<Rgb>> {
        val other = wrong.opposite
        val rename = mapOf(cube.centre(wrong) to cube.centre(other), cube.centre(other) to cube.centre(wrong))
        fun <T> blocks(list: List<T>) = Face.entries.flatMap { face ->
            val block = when (face) {
                wrong -> other
                other -> wrong
                else -> face
            }
            list.subList(block.ordinal * 9, block.ordinal * 9 + 9)
        }
        val seen = asSeen(cube.toList(), turns)
        return blocks(seen.map { rename[it] ?: it }) to blocks(seen.map { ColorClassifier.DEFAULT_PALETTE.getValue(it) })
    }

    @Test
    fun redOrangeLabelSwapIsUndone() {
        val cube = Cube.solved().apply(Scramble.random(25, Random(7)))
        val (colors, samples) = mislabelled(cube, Face.R)
        val result = RotationSearch.search(colors, samples = samples)
        assertEquals(cube.toList(), result.colors)
        assertEquals(Face.L, result.from[Face.R])
        assertEquals(Face.R, result.from[Face.L])
        assertEquals(Face.U, result.from[Face.U])
    }

    @Test
    fun whiteYellowLabelSwapIsUndone() {
        val cube = Cube.solved().apply(Scramble.random(25, Random(8)))
        val (colors, samples) = mislabelled(cube, Face.U)
        val result = RotationSearch.search(colors, samples = samples)
        assertEquals(cube.toList(), result.colors)
        assertEquals(Face.D, result.from[Face.U])
    }

    @Test
    fun solvedCubeIsNotAmbiguous() {
        val result = RotationSearch.search(Cube.solved().toList())
        assertTrue(result.validity.isValid)
        assertTrue(result.ambiguous.isEmpty())
        assertTrue(result.rotations.values.all { it == 0 })
    }

    companion object {
        /** The first fully successful phone scan (see [PhoneScanTest]), net order. */
        val PHONE_SCAN = mapOf(
            Face.U to "97b4cd,9bb8d1,97b6cc,95adc7,9ab6d3,99b5cc,8ba4bc,93adc6,91acc1",
            Face.R to "94293a,9a2031,951625,8f0518,99061b,961a2a,98bf48,8e0919,a2cb51",
            Face.F to "0ead68,e85d43,04ab57,04a653,04ae56,04a95e,049d48,a1c955,0360a8",
            Face.D to "963043,0ab25f,961424,035fab,add94e,a9d25d,03589f,a2ca50,03a357",
            Face.L to "db5f4b,0dac5d,e35939,a2c955,e35533,e25a3e,97bd4a,d74b2b,a0c75c",
            Face.B to "036ab2,0369b5,0363af,035da9,0360b0,e45639,d6472a,035aa5,da4f35",
        )
        const val PHONE_SCAN_COLORS = "WWWWWWWWWRRRRRRYRYGOGGGGGYBRGRBYYBYGOGOYOOYOYBBBBBOOBO"
    }
}
