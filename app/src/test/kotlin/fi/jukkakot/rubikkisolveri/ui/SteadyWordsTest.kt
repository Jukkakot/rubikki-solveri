package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.Strings
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.ui.common.MoveWords
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The move words (as seen in the holding view) checked against the cube model. */
class SteadyWordsTest {
    /** Where the sticker at index [from] of a solved cube ends up after [move]. */
    private fun follow(from: Int, move: String): Int {
        val solved = Cube.solved()
        val marked = solved.with(from, if (solved[from] == CubeColor.YELLOW) CubeColor.WHITE else CubeColor.YELLOW)
        val m = Notation.parseMove(move)!!
        val a = solved.apply(m).toList()
        val b = marked.apply(m).toList()
        return a.indices.single { a[it] != b[it] }
    }

    private fun sentence(move: String): String = MoveWords.parts(Notation.parseMove(move)!!)!!.first.key

    @Test
    fun directionsMatchTheCube() {
        // Front top-middle edge for the top, front middle row's ends for the sides, front
        // bottom-middle for the bottom, the top's back edge for the back's top row.
        val cases = listOf(
            Triple("U", Stickers.index(Face.F, 2), "move_top_left" to Face.L),
            Triple("U'", Stickers.index(Face.F, 2), "move_top_right" to Face.R),
            Triple("D", Stickers.index(Face.F, 8), "move_bottom_right" to Face.R),
            Triple("D'", Stickers.index(Face.F, 8), "move_bottom_left" to Face.L),
            Triple("R", Stickers.index(Face.F, 6), "move_right_up" to Face.U),
            Triple("R'", Stickers.index(Face.F, 6), "move_right_down" to Face.D),
            Triple("L", Stickers.index(Face.F, 4), "move_left_down" to Face.D),
            Triple("L'", Stickers.index(Face.F, 4), "move_left_up" to Face.U),
            Triple("B", Stickers.index(Face.U, 2), "move_back_left" to Face.L),
            Triple("B'", Stickers.index(Face.U, 2), "move_back_right" to Face.R),
        )
        for ((move, from, expected) in cases) {
            val (key, face) = expected
            assertEquals(key, sentence(move), move)
            val to = follow(from, move)
            assertTrue(to in Stickers.index(face, 1)..Stickers.index(face, 9), "$move moves the sticker to $face")
        }
        // Front clockwise as seen from the front: top-middle goes to right-middle.
        assertEquals("move_front_cw", sentence("F"))
        assertEquals(Stickers.index(Face.F, 6), follow(Stickers.index(Face.F, 2), "F"))
        assertEquals("move_front_ccw", sentence("F'"))
        assertEquals(Stickers.index(Face.F, 4), follow(Stickers.index(Face.F, 2), "F'"))
    }

    @Test
    fun sentencesInBothLanguages() {
        val words = { language: String, move: String ->
            MoveWords.describe(Notation.parseMove(move)!!, { id, args -> Strings.get(language, id.key, *args) })
        }
        assertEquals("Turn the top layer to the right.", words("en", "U'"))
        assertEquals("Käännä takapuolta niin, että sen ylärivi liikkuu vasemmalle.", words("fi", "B"))
        assertEquals("Turn the top layer half a turn.", words("en", "U2"))
        val all = listOf("U", "D", "R", "L", "F", "B").flatMap { f -> listOf(f, "$f'", "${f}2") }
        for (language in listOf("fi", "en")) {
            val texts = all.map { words(language, it) }
            assertEquals(18, texts.toSet().size, language)
            assertTrue(texts.none { it.contains('%') })
        }
    }
}
