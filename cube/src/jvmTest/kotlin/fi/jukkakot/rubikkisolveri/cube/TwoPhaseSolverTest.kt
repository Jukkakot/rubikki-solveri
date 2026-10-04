package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TwoPhaseSolverTest {

    private fun solved(cube: Cube): List<Move> {
        val result = TwoPhaseSolver.solve(cube)
        assertIs<SolveResult.Solved>(result, "for $cube")
        return result.moves
    }

    @Test
    fun solvesAnyScrambledCubeInAtMost21Moves() {
        TwoPhaseSolver.warmUp()
        var total = 0L
        var lengths = 0
        repeat(50) { seed ->
            val cube = Cube.solved().apply(Scramble.random(30, Random(seed)))
            val start = System.nanoTime()
            val moves = solved(cube)
            lengths += moves.size
            total += System.nanoTime() - start
            assertTrue(cube.apply(moves).isSolved, "seed $seed")
            assertTrue(moves.size <= TwoPhaseSolver.MAX_LENGTH, "seed $seed: ${moves.size}")
            assertTrue(moves.all { it.layer.kind == Layer.Kind.FACE })
        }
        assertTrue(total / 50 / 1_000_000 < 300, "average ${total / 50 / 1_000_000} ms")
        assertTrue(lengths / 50.0 <= 19.5, "average length ${lengths / 50.0}")
        println("two-phase: average ${lengths / 50.0} moves, ${total / 50 / 1_000_000} ms")
    }

    @Test
    fun solvesTheCubeHowEverItIsHeld() {
        val cube = Cube.solved().apply("x y R U R' F2 D L2 z")
        assertTrue(cube.apply(solved(cube)).isSolved)
    }

    @Test
    fun superflip() {
        val superflip = Cube.solved().apply("U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2")
        val moves = solved(superflip)
        assertTrue(superflip.apply(moves).isSolved)
        assertTrue(moves.size in 20..21, "superflip needs at least 20 moves; found ${moves.size}")
    }

    @Test
    fun alreadySolved() {
        assertEquals(emptyList(), solved(Cube.solved()))
    }

    @Test
    fun invalidCubeIsReported() {
        val uf = Edge.UF.stickers
        val solved = Cube.solved()
        val flipped = solved.with(uf[0], solved[uf[1]]).with(uf[1], solved[uf[0]])
        assertEquals(SolveResult.Invalid(Validity.FlippedEdge), TwoPhaseSolver.solve(flipped))
    }

    @Test
    fun randomStateScrambleIsSolvable() {
        val scramble = TwoPhaseSolver.randomStateScramble(Random(7))
        val cube = Cube.solved().apply(scramble)
        assertTrue(!cube.isSolved)
        assertTrue(cube.apply(solved(cube)).isSolved)
    }
}
