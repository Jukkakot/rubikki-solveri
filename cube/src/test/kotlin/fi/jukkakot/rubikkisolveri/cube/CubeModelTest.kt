package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.CubeColor.BLUE
import fi.jukkakot.rubikkisolveri.cube.CubeColor.GREEN
import fi.jukkakot.rubikkisolveri.cube.CubeColor.ORANGE
import fi.jukkakot.rubikkisolveri.cube.CubeColor.RED
import fi.jukkakot.rubikkisolveri.cube.CubeColor.WHITE
import fi.jukkakot.rubikkisolveri.cube.CubeColor.YELLOW
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CubeModelTest {
    private val solved = Cube.solved()

    // --- Cube state

    @Test
    fun solvedCube() {
        assertTrue(solved.isSolved)
        assertEquals(WHITE, solved.centre(Face.U))
        assertEquals(GREEN, solved.centre(Face.F))
        assertEquals(RED, solved.centre(Face.R))
        assertEquals(ORANGE, solved.centre(Face.L))
        assertEquals(BLUE, solved.centre(Face.B))
        assertEquals(YELLOW, solved.centre(Face.D))
        for (face in Face.entries) assertTrue((1..9).all { solved.colorAt(face, it) == solved.centre(face) })
        assertEquals("UUUUUUUUURRRRRRRRRFFFFFFFFFDDDDDDDDDLLLLLLLLLBBBBBBBBB", solved.toFaceletString())
    }

    @Test
    fun geometryMatchesTheStandardNet() {
        fun s(name: String) = Stickers.index(Face.valueOf(name.take(1)), name.drop(1).toInt())
        val corners = listOf(
            "U9 R1 F3", "U7 F1 L3", "U1 L1 B3", "U3 B1 R3", "D3 F9 R7", "D1 L9 F7", "D7 B9 L7", "D9 R9 B7",
        )
        for ((corner, names) in Corner.entries.zip(corners)) {
            assertEquals(names.split(" ").map(::s), corner.stickers, corner.name)
        }
        val edges = listOf(
            "U6 R2", "U8 F2", "U4 L2", "U2 B2", "D6 R8", "D2 F8", "D4 L8", "D8 B8", "F6 R4", "F4 L6", "B6 L4", "B4 R6",
        )
        for ((edge, names) in Edge.entries.zip(edges)) {
            assertEquals(names.split(" ").map(::s), edge.stickers, edge.name)
        }
    }

    // --- Moves

    @Test
    fun fourQuarterTurns() {
        val scrambled = solved.apply(Scramble.random(30, Random(1)))
        for (move in Move.ALL.filter { it.quarterTurns == 1 }) {
            assertEquals(scrambled, scrambled.apply(listOf(move, move, move, move)), move.toString())
        }
    }

    @Test
    fun everyMoveIsAPermutationAndPrimeUndoesIt() {
        for (move in Move.ALL) {
            assertEquals((0 until 54).toSet(), move.permutation.toSet(), move.toString())
            assertEquals(solved, solved.apply(move).apply(move.inverse), move.toString())
        }
        assertEquals(solved.apply("R R"), solved.apply("R2"))
        assertEquals(solved.apply("R R R"), solved.apply("R'"))
    }

    @Test
    fun knownCycle() {
        val sexy = Notation.parse("R U R' U'")
        var cube = solved
        repeat(5) {
            cube = cube.apply(sexy)
            assertFalse(cube.isSolved)
        }
        assertTrue(cube.apply(sexy).isSolved)
    }

    @Test
    fun knownOrders() {
        fun order(text: String): Int {
            val moves = Notation.parse(text)
            var cube = solved.apply(moves)
            var n = 1
            while (cube != solved) {
                cube = cube.apply(moves)
                n++
            }
            return n
        }
        assertEquals(105, order("R U"))
        assertEquals(63, order("R U'"))
        assertEquals(6, order("R2 U2"))
        assertEquals(4, order("M"))
    }

    @Test
    fun turnDirection() {
        val u = solved.apply("U")
        assertEquals(listOf(RED, RED, RED), (1..3).map { u.colorAt(Face.F, it) })
        assertEquals(listOf(GREEN, GREEN, GREEN), (1..3).map { u.colorAt(Face.L, it) })
        val r = solved.apply("R")
        assertEquals(listOf(GREEN, GREEN, GREEN), listOf(3, 6, 9).map { r.colorAt(Face.U, it) })
        val f = solved.apply("F")
        assertEquals(listOf(WHITE, WHITE, WHITE), listOf(1, 4, 7).map { f.colorAt(Face.R, it) })
        val d = solved.apply("D")
        assertEquals(listOf(ORANGE, ORANGE, ORANGE), (7..9).map { d.colorAt(Face.F, it) })
        val l = solved.apply("L")
        assertEquals(listOf(WHITE, WHITE, WHITE), listOf(1, 4, 7).map { l.colorAt(Face.F, it) })
        assertEquals(listOf(BLUE, BLUE, BLUE), listOf(1, 4, 7).map { l.colorAt(Face.U, it) })
        val b = solved.apply("B")
        assertEquals(listOf(RED, RED, RED), (1..3).map { b.colorAt(Face.U, it) })
    }

    @Test
    fun slicesAndWideTurnsMatchTheirDefinitions() {
        val scrambled = solved.apply(Scramble.random(20, Random(2)))
        fun same(a: String, b: String) = assertEquals(scrambled.apply(b), scrambled.apply(a), "$a = $b")
        same("M", "R L' x'")
        same("E", "U D' y'")
        same("S", "F' B z")
        same("r", "R M'")
        same("Rw", "R M'")
        same("u", "U E'")
        same("f", "F S")
        same("l", "L M")
        same("d", "D E")
        same("b", "B S'")
        same("x", "R M' L'")
        same("y", "U E' D'")
        same("z", "F S B'")
    }

    @Test
    fun rotationKeepsSolved() {
        val x = solved.apply("x")
        assertTrue(x.isSolved)
        assertEquals(GREEN, x.centre(Face.U))
        assertEquals(YELLOW, x.centre(Face.F))
        val y = solved.apply("y")
        assertEquals(RED, y.centre(Face.F))
        val z = solved.apply("z")
        assertEquals(ORANGE, z.centre(Face.U))
    }

    // --- Pieces, cross-checked against the published two-phase cubie tables for R and F

    @Test
    fun pieceViewOfRAndF() {
        val r = CubeCheck.pieces(solved.apply("R"))!!
        assertEquals("DFR UFL ULB URF DRB DLF DBL UBR", r.cornerPermutation.joinToString(" "))
        assertEquals(listOf(2, 0, 0, 1, 1, 0, 0, 2), r.cornerTwist)
        assertEquals("FR UF UL UB BR DF DL DB DR FL BL UR", r.edgePermutation.joinToString(" "))
        assertEquals(List(12) { 0 }, r.edgeFlip)

        val f = CubeCheck.pieces(solved.apply("F"))!!
        assertEquals("UFL DLF ULB UBR URF DFR DBL DRB", f.cornerPermutation.joinToString(" "))
        assertEquals(listOf(1, 2, 0, 0, 2, 1, 0, 0), f.cornerTwist)
        assertEquals("UR FL UL UB DR FR DL DB UF DF BL BR", f.edgePermutation.joinToString(" "))
        assertEquals(listOf(0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0), f.edgeFlip)

        assertTrue(CubeCheck.pieces(solved)!!.isSolved)
        assertTrue(CubeCheck.pieces(solved.apply("x y"))!!.isSolved, "pieces are read through the centres")
    }

    // --- Notation

    @Test
    fun parseAndPrint() {
        val moves = Notation.parse("R U2 R' u x' M2")
        assertEquals(6, moves.size)
        assertEquals("R U2 R' u x' M2", Notation.format(moves))
        assertEquals(Notation.parse("r U'"), Notation.parse("  Rw   U’ "))
        assertEquals(Notation.parse("R2"), Notation.parse("R2'"))
        assertEquals(emptyList(), Notation.parse(""))
    }

    @Test
    fun badToken() {
        val error = assertFailsWith<NotationException> { Notation.parse("R Q U") }
        assertEquals("Q", error.token)
        assertEquals(2, error.position)
        assertFailsWith<NotationException> { Notation.parse("R3") }
        assertFailsWith<NotationException> { Notation.parse("Mw") }
    }

    @Test
    fun inverseUndoes() {
        val random = Random(3)
        repeat(100) {
            val moves = List(30) { Move.ALL[random.nextInt(Move.ALL.size)] }
            assertTrue(solved.apply(moves).apply(Sequences.inverse(moves)).isSolved)
        }
    }

    @Test
    fun mergeAndCancel() {
        assertEquals("R2 L", Notation.format(Sequences.simplify(Notation.parse("R R U U' F2 F2 L"))))
        assertEquals("", Notation.format(Sequences.simplify(Notation.parse("R U U' R'"))))
        assertEquals("R'", Notation.format(Sequences.simplify(Notation.parse("R2 R"))))
        val random = Random(4)
        repeat(100) {
            val moves = List(20) { Move.ALL[random.nextInt(Move.ALL.size)] }
            assertEquals(solved.apply(moves), solved.apply(Sequences.simplify(moves)))
        }
    }

    // --- Scramble

    @Test
    fun scrambleShape() {
        repeat(50) { seed ->
            val moves = Scramble.random(25, Random(seed))
            assertEquals(25, moves.size)
            assertTrue(moves.all { it.layer.kind == Layer.Kind.FACE })
            assertTrue(moves.zipWithNext().none { (a, b) -> a.layer == b.layer })
            assertTrue(moves.windowed(3).none { w -> w.map { it.layer.axisIndex }.toSet().size == 1 })
        }
    }
}
