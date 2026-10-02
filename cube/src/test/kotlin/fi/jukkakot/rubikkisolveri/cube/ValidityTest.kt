package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.CubeColor.BLUE
import fi.jukkakot.rubikkisolveri.cube.CubeColor.GREEN
import fi.jukkakot.rubikkisolveri.cube.CubeColor.RED
import fi.jukkakot.rubikkisolveri.cube.CubeColor.WHITE
import fi.jukkakot.rubikkisolveri.cube.CubeColor.YELLOW
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ValidityTest {
    private val solved = Cube.solved()

    private fun Cube.paint(vararg changes: Pair<Int, CubeColor>): Cube =
        changes.fold(this) { cube, (index, color) -> cube.with(index, color) }

    private fun s(face: Face, n: Int) = Stickers.index(face, n)

    @Test
    fun solvedAndScrambledCubesAreValid() {
        assertEquals(Validity.Valid, CubeCheck.validity(solved))
        repeat(1000) { seed ->
            val cube = solved.apply(Scramble.random(25, Random(seed)))
            assertEquals(Validity.Valid, CubeCheck.validity(cube), "seed $seed")
        }
        assertEquals(Validity.Valid, CubeCheck.validity(solved.apply("x y' M E S r d")))
    }

    @Test
    fun wrongColourCount() {
        val cube = solved.paint(s(Face.U, 1) to YELLOW)
        assertEquals(Validity.WrongColorCount(mapOf(WHITE to 8, YELLOW to 10)), CubeCheck.validity(cube))
    }

    @Test
    fun badCentres() {
        val swappedWhiteYellow = solved.paint(s(Face.U, 5) to YELLOW, s(Face.D, 5) to WHITE)
        assertIs<Validity.BadCentres>(CubeCheck.validity(swappedWhiteYellow))
        val mirrored = solved.paint(s(Face.R, 5) to solved.centre(Face.L), s(Face.L, 5) to solved.centre(Face.R))
        assertIs<Validity.BadCentres>(CubeCheck.validity(mirrored))
    }

    @Test
    fun impossiblePiece() {
        // URF (white, red, green) gets yellow instead of green; D2 gets the green to keep counts.
        val cube = solved.paint(s(Face.F, 3) to YELLOW, s(Face.D, 2) to GREEN)
        assertEquals(listOf(WHITE, RED, YELLOW), Corner.URF.stickers.map { cube[it] })
        assertEquals(Validity.ImpossiblePiece(Corner.URF.stickers), CubeCheck.validity(cube))
    }

    @Test
    fun duplicatePiece() {
        // The white-red-green corner also at UBR (U3 B1 R3 read as U R F), and the UF edge
        // painted as the UB edge so the colour counts stay right.
        val cube = solved.paint(
            s(Face.U, 3) to WHITE, s(Face.B, 1) to RED, s(Face.R, 3) to GREEN,
            s(Face.F, 2) to BLUE,
        )
        assertEquals(Validity.DuplicatePiece(Corner.URF.stickers + Corner.UBR.stickers), CubeCheck.validity(cube))
    }

    @Test
    fun twistedCorner() {
        val urf = Corner.URF.stickers
        val cube = solved.paint(urf[0] to solved[urf[2]], urf[1] to solved[urf[0]], urf[2] to solved[urf[1]])
        assertEquals(Validity.TwistedCorner, CubeCheck.validity(cube))
        assertEquals(Validity.TwistedCorner, CubeCheck.validity(cube.apply("R U F2 D' L")))
    }

    @Test
    fun flippedEdge() {
        val uf = Edge.UF.stickers
        val cube = solved.paint(uf[0] to solved[uf[1]], uf[1] to solved[uf[0]])
        assertEquals(Validity.FlippedEdge, CubeCheck.validity(cube))
        assertEquals(Validity.FlippedEdge, CubeCheck.validity(cube.apply("B2 L U' R")))
    }

    @Test
    fun swappedPieces() {
        val uf = Edge.UF.stickers
        val ur = Edge.UR.stickers
        val cube = solved.paint(uf[0] to solved[ur[0]], uf[1] to solved[ur[1]], ur[0] to solved[uf[0]], ur[1] to solved[uf[1]])
        assertEquals(Validity.SwappedPieces, CubeCheck.validity(cube))
        assertEquals(Validity.SwappedPieces, CubeCheck.validity(cube.apply("F R2 D")))
    }

    @Test
    fun parityOfPermutations() {
        assertEquals(0, CubeCheck.parity(listOf(0, 1, 2)))
        assertEquals(1, CubeCheck.parity(listOf(1, 0, 2)))
        assertEquals(0, CubeCheck.parity(listOf(1, 2, 0)))
    }

    @Test
    fun twentyFourOrientations() {
        assertEquals(24, CubeCheck.centreArrangements(ColorScheme.STANDARD).size)
    }
}
