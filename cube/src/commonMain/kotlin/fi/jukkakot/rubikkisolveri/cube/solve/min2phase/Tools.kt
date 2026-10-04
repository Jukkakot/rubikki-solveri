/*
 * Kotlin port of min2phase by Chen Shuang, MIT licence (see Util.kt and LICENSE).
 * Ported: the random-state generator for fully random cubes and fromScramble.
 */
package fi.jukkakot.rubikkisolveri.cube.solve.min2phase

import kotlin.random.Random

internal object Tools {
    /**
     * A uniformly random solvable cube in URFDLB facelet order. Draws from [gen] in the same order
     * as the Java original's `randomCube(Random)`, so the same random source gives the same cube.
     */
    fun randomCube(gen: Random): String {
        // randomState(STATE_RANDOM, STATE_RANDOM, STATE_RANDOM, STATE_RANDOM): ep is random, so
        // the corner permutation is drawn first and the edge permutation matches its parity.
        val cpVal = gen.nextInt(40320)
        val parity = Util.getNParity(cpVal, 8)
        var epVal: Int
        do {
            epVal = gen.nextInt(479001600)
        } while (Util.getNParity(epVal, 12) != parity)
        val twist = gen.nextInt(2187)
        val flip = gen.nextInt(2048)
        return Util.toFaceCube(CubieCube(cpVal, twist, epVal, flip))
    }

    fun fromScramble(scramble: IntArray): String {
        var c1 = CubieCube()
        var c2 = CubieCube()
        for (m in scramble) {
            CubieCube.CornMult(c1, CubieCube.move(m), c2)
            CubieCube.EdgeMult(c1, CubieCube.move(m), c2)
            val tmp = c1
            c1 = c2
            c2 = tmp
        }
        return Util.toFaceCube(c1)
    }
}
