package fi.jukkakot.rubikkisolveri.cube.solve.min2phase

import kotlin.random.Random
import kotlin.random.asJavaRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.measureTime

/**
 * The Kotlin port must behave exactly like the Java original (kept in jvmTest/java as the
 * oracle): same random cubes from the same random source and the same solution strings.
 */
class Min2phasePortTest {
    private val javaSearch = cs.min2phase.Search()
    private val kotlinSearch = Search()

    init {
        val java = measureTime { cs.min2phase.Search.init() }
        val kotlin = measureTime { Search.init() }
        println("min2phase tables: Java $java, Kotlin $kotlin")
    }

    @Test
    fun randomCubesAreTheSameForTheSameSource() {
        for (seed in 0 until 100) {
            assertEquals(
                cs.min2phase.Tools.randomCube(Random(seed).asJavaRandom()),
                Tools.randomCube(Random(seed)),
                "seed $seed",
            )
        }
    }

    @Test
    fun solutionsMatchTheJavaOriginal() {
        // The app's parameters (TwoPhaseSolver): 21 moves, 100 000 probes, then 1 000 more probes
        // for a shorter solution; and without the extra probes (scrambles).
        for (seed in 0 until 1000) {
            val cube = cs.min2phase.Tools.randomCube(Random(10_000 + seed).asJavaRandom())
            val minProbes = if (seed % 2 == 0) 1_000L else 0L
            assertEquals(
                javaSearch.solution(cube, 21, 100_000, minProbes, 0),
                kotlinSearch.solution(cube, 21, 100_000, minProbes, 0),
                "cube $cube",
            )
        }
    }

    @Test
    fun specialPositionsMatch() {
        val scrambles = listOf(
            "",
            "R",
            "U2 D2 F2 B2 L2 R2", // checkerboard: highly symmetric
            "R L U2 R' L' U2", // symmetric
            "F U' F2 D' B U R' F' L D' R' U' L U B' D2 R' F U2 D2",
        )
        val cubes = scrambles.map { cs.min2phase.Tools.fromScramble(it) } + cs.min2phase.Tools.superFlip()
        for (cube in cubes) {
            for (verbose in listOf(0, Search.USE_SEPARATOR, Search.INVERSE_SOLUTION, Search.APPEND_LENGTH)) {
                assertEquals(
                    javaSearch.solution(cube, 21, 100_000, 1_000, verbose),
                    kotlinSearch.solution(cube, 21, 100_000, 1_000, verbose),
                    "cube $cube verbose $verbose",
                )
            }
        }
    }

    @Test
    fun fromScrambleMatches() {
        val moves = intArrayOf(0, 4, 8, 9, 13, 17, 2, 6, 10, 15)
        assertEquals(cs.min2phase.Tools.fromScramble(moves), Tools.fromScramble(moves))
    }

    @Test
    fun invalidCubesGiveTheSameErrors() {
        val solved = cs.min2phase.Tools.fromScramble("")
        val swapped = solved.substring(0, 1) + solved[9] + solved.substring(2, 9) + solved[1] + solved.substring(10)
        val twisted = cs.min2phase.Tools.fromScramble("R").let { it.substring(0, 8) + it[9] + it.substring(9) }
        for (cube in listOf(swapped, twisted, "UUU", solved.replace('U', 'X'))) {
            assertEquals(
                javaSearch.solution(cube, 21, 100_000, 1_000, 0),
                kotlinSearch.solution(cube, 21, 100_000, 1_000, 0),
                "cube $cube",
            )
        }
    }
}
