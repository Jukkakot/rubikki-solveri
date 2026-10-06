package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SolveTargetTest {

    private fun movesTo(from: Cube, to: Cube): List<Move> {
        val result = TwoPhaseSolver.solve(from, to)
        assertIs<SolveResult.Solved>(result, "from $from to $to")
        return result.moves
    }

    @Test
    fun leadsAnyCubeToAnyOtherExactly() {
        repeat(20) { seed ->
            val random = Random(seed)
            val from = Cube.solved().apply(Scramble.random(30, random))
            val to = Cube.solved().apply(Scramble.random(30, random))
            val moves = movesTo(from, to)
            assertEquals(to, from.apply(moves), "seed $seed")
            assertTrue(moves.size <= TwoPhaseSolver.MAX_LENGTH, "seed $seed: ${moves.size}")
        }
    }

    @Test
    fun leadsToEveryPatternFromAScrambleAndFromSolved() {
        val scrambled = Cube.solved().apply(Scramble.random(30, Random(7)))
        for (pattern in CubePattern.entries) {
            for (from in listOf(scrambled, Cube.solved())) {
                val to = pattern.cube(from)
                assertEquals(to, from.apply(movesTo(from, to)), "$pattern")
            }
        }
    }

    @Test
    fun worksForACubeHeldAnotherWay() {
        val turned = Cube.solved().apply("x y").apply(Scramble.random(25, Random(3)))
        val to = CubePattern.CHECKERBOARD.cube(turned)
        assertEquals(to, turned.apply(movesTo(turned, to)))
    }

    @Test
    fun solvedTargetAndSameCube() {
        val cube = Cube.solved().apply(Scramble.random(30, Random(1)))
        assertTrue(cube.apply(movesTo(cube, Cube.solved())).isSolved)
        assertEquals(emptyList(), movesTo(cube, cube))
    }

    @Test
    fun everyPatternIsAPossibleCube() {
        for (pattern in CubePattern.entries) {
            assertTrue(CubeCheck.validity(pattern.cube()).isValid, "$pattern")
            assertTrue(!pattern.cube().isSolved, "$pattern")
        }
    }

    @Test
    fun checkerboardAlternatesOnEverySide() {
        val cube = CubePattern.CHECKERBOARD.cube()
        for (face in Face.entries) {
            val centre = cube.centre(face)
            for (n in 1..9) {
                val sameAsCentre = n % 2 == 1
                assertEquals(sameAsCentre, cube.colorAt(face, n) == centre, "$face $n")
            }
        }
    }

    @Test
    fun sixSpotsHasEachCentreDifferentFromItsRing() {
        val cube = CubePattern.SIX_SPOTS.cube()
        for (face in Face.entries) {
            val ring = (1..9).filter { it != 5 }.map { cube.colorAt(face, it) }.toSet()
            assertEquals(1, ring.size, "$face")
            assertTrue(cube.centre(face) !in ring, "$face")
        }
    }

    @Test
    fun superflipFlipsEveryEdgeAndNothingElse() {
        val cube = CubePattern.SUPERFLIP.cube()
        for (face in Face.entries) {
            val centre = cube.centre(face)
            for (n in listOf(1, 3, 7, 9)) assertEquals(centre, cube.colorAt(face, n), "$face corner $n")
            for (n in listOf(2, 4, 6, 8)) assertTrue(cube.colorAt(face, n) != centre, "$face edge $n")
        }
    }

    @Test
    fun targetsSurviveTheirStringForm() {
        val targets = listOf(
            SolveTarget.Solved,
            SolveTarget.Pattern(CubePattern.TETRIS),
            SolveTarget.StageDone(Stage.WHITE_CORNERS),
            SolveTarget.Painted(CubePattern.PYTHON.cube()),
        )
        for (t in targets) assertEquals(t, SolveTarget.decode(t.encode()))
    }
}
