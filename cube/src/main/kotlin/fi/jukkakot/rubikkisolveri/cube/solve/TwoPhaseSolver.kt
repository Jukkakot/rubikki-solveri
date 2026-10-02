package fi.jukkakot.rubikkisolveri.cube.solve

import cs.min2phase.Search
import cs.min2phase.Tools
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Sequences
import fi.jukkakot.rubikkisolveri.cube.Validity
import kotlin.random.Random
import kotlin.random.asJavaRandom

sealed interface SolveResult {
    /** [moves] solve the cube (face turns only, as the cube is held). */
    data class Solved(val moves: List<Move>, val millis: Long) : SolveResult

    /** The cube cannot be solved, for [reason]. */
    data class Invalid(val reason: Validity) : SolveResult

    /** The search gave up (should not happen with the default limits). */
    data class Failed(val message: String) : SolveResult
}

/**
 * Shortest practical solutions with the two-phase algorithm (min2phase, vendored under its MIT
 * licence in `cs.min2phase`). The tables take a moment to build: call [warmUp] early on a
 * background thread; [solve] waits for it.
 */
object TwoPhaseSolver {
    /** Longest solution accepted; 21 is found almost at once for any cube. */
    const val MAX_LENGTH = 21
    private const val PROBE_LIMIT = 100_000L
    private const val MIN_PROBES = 0L
    private const val IMPROVE_PROBES = 1_000L

    fun warmUp() {
        if (!Search.isInited()) Search.init()
    }

    /**
     * Solves [cube]. The search tries at least [IMPROVE_PROBES] second-phase probes for a shorter
     * solution after the first one: measured on a desktop, 18.9 moves on average in about 25 ms
     * (without it 20.8 moves in 1 ms; 5000 probes only reach 18.8 in 135 ms).
     */
    fun solve(cube: Cube): SolveResult {
        val validity = CubeCheck.validity(cube)
        if (!validity.isValid) return SolveResult.Invalid(validity)
        val start = System.nanoTime()
        warmUp()
        if (cube.isSolved) return SolveResult.Solved(emptyList(), 0)
        val text = Search().solution(cube.toFaceletString(), MAX_LENGTH, PROBE_LIMIT, IMPROVE_PROBES, 0)
        val millis = (System.nanoTime() - start) / 1_000_000
        if (text.startsWith("Error")) return SolveResult.Failed(text)
        return SolveResult.Solved(Notation.parse(text), millis)
    }

    /**
     * A scramble that leads to a uniformly random cube state (the standard competition way): the
     * inverse of a solution of a random state.
     */
    fun randomStateScramble(random: Random = Random.Default): List<Move> {
        warmUp()
        Tools.setRandomSource(random.asJavaRandom())
        val state = Tools.randomCube()
        val text = Search().solution(state, MAX_LENGTH, PROBE_LIMIT, MIN_PROBES, 0)
        return Sequences.inverse(Notation.parse(text))
    }
}
