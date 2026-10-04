/*
 * Kotlin port of min2phase by Chen Shuang, MIT licence (see Util.kt and LICENSE).
 * Ported: the standard search (`solution` without OPTIMAL_SOLUTION). Not ported, as the app does
 * not use them: `next`, the optimal search and the table file I/O.
 */
package fi.jukkakot.rubikkisolveri.cube.solve.min2phase

import fi.jukkakot.rubikkisolveri.cube.solve.withSolverLock
import kotlin.math.abs

internal class Search {
    private val move = IntArray(31)

    private val nodeUD = Array(21) { CoordCube() }

    private var selfSym = 0L
    private var conjMask = 0
    private var urfIdx = 0
    private var length1 = 0
    private var depth1 = 0
    private var maxDep2 = 0
    private var solLen = 0
    private var solution: Util.Solution? = null
    private var probe = 0L
    private var probeMax = 0L
    private var probeMin = 0L
    private var verbose = 0
    private var valid1 = 0
    private var allowShorter = false
    private val cc = CubieCube()
    private val urfCubieCube = Array(6) { CubieCube() }
    private val urfCoordCube = Array(6) { CoordCube() }
    private val phase1Cubie = Array(21) { CubieCube() }

    private val preMoveCubes = arrayOfNulls<CubieCube>(MAX_PRE_MOVES + 1).also {
        for (i in 0 until MAX_PRE_MOVES) it[i + 1] = CubieCube()
    }
    private val preMoves = IntArray(MAX_PRE_MOVES)
    private var preMoveLen = 0
    private var maxPreMoves = 0

    private var isRec = false

    /**
     * The solution of [facelets] (URFDLB order) or "Error n" (1: colours, 2: edges, 3: flip,
     * 4: corners, 5: twist, 6: parity, 7: no solution within [maxDepth], 8: probe limit).
     */
    fun solution(facelets: String, maxDepth: Int, probeMax: Long, probeMin: Long, verbose: Int): String {
        require(verbose and OPTIMAL_SOLUTION == 0) { "OPTIMAL_SOLUTION is not ported" }
        val check = verify(facelets)
        if (check != 0) {
            return "Error " + abs(check)
        }
        this.solLen = maxDepth + 1
        this.probe = 0
        this.probeMax = probeMax
        this.probeMin = minOf(probeMin, probeMax)
        this.verbose = verbose
        this.solution = null
        this.isRec = false

        CoordCube.init(false)
        initSearch()

        return search()
    }

    private fun initSearch() {
        conjMask = (if (TRY_INVERSE) 0 else 0x38) or (if (TRY_THREE_AXES) 0 else 0x36)
        selfSym = cc.selfSymmetry()
        conjMask = conjMask or if (((selfSym shr 16) and 0xffff) != 0L) 0x12 else 0
        conjMask = conjMask or if (((selfSym shr 32) and 0xffff) != 0L) 0x24 else 0
        conjMask = conjMask or if (((selfSym shr 48) and 0xffff) != 0L) 0x38 else 0
        selfSym = selfSym and 0xffffffffffffL
        maxPreMoves = if (conjMask > 7) 0 else MAX_PRE_MOVES

        for (i in 0 until 6) {
            urfCubieCube[i].copy(cc)
            urfCoordCube[i].setWithPrun(urfCubieCube[i], 20)
            cc.URFConjugate()
            if (i % 3 == 2) {
                cc.invCubieCube()
            }
        }
    }

    fun verify(facelets: String): Int {
        var count = 0x000000
        val f = ByteArray(54)
        if (facelets.length < 54) return -1
        val center = charArrayOf(
            facelets[Util.U5], facelets[Util.R5], facelets[Util.F5],
            facelets[Util.D5], facelets[Util.L5], facelets[Util.B5],
        ).concatToString()
        for (i in 0 until 54) {
            f[i] = center.indexOf(facelets[i]).toByte()
            if (f[i].toInt() == -1) {
                return -1
            }
            count += 1 shl (f[i].toInt() shl 2)
        }
        if (count != 0x999999) {
            return -1
        }
        Util.toCubieCube(f, cc)
        return cc.verify()
    }

    private fun phase1PreMoves(maxl: Int, lm0: Int, cc: CubieCube, ssym: Int): Int {
        preMoveLen = maxPreMoves - maxl
        if (if (isRec) depth1 == length1 - preMoveLen else (preMoveLen == 0 || ((0x36FB7 shr lm0) and 1) == 0)) {
            depth1 = length1 - preMoveLen
            phase1Cubie[0] = cc
            allowShorter = depth1 == MIN_P1LENGTH_PRE && preMoveLen != 0

            if (nodeUD[depth1 + 1].setWithPrun(cc, depth1) &&
                phase1(nodeUD[depth1 + 1], ssym, depth1, -1) == 0
            ) {
                return 0
            }
        }

        if (maxl == 0 || preMoveLen + MIN_P1LENGTH_PRE >= length1) {
            return 1
        }

        var skipMoves = CubieCube.getSkipMoves(ssym.toLong())
        if (maxl == 1 || preMoveLen + 1 + MIN_P1LENGTH_PRE >= length1) { // last pre move
            skipMoves = skipMoves or 0x36FB7 // 11 0110 1111 1011 0111
        }

        val lm = lm0 / 3 * 3
        var m = 0
        while (m < 18) {
            run body@{
                if (m == lm || m == lm - 9 || m == lm + 9) {
                    m += 2
                    return@body
                }
                if (isRec && m != preMoves[maxPreMoves - maxl] || (skipMoves and (1 shl m)) != 0) {
                    return@body
                }
                val target = preMoveCubes[maxl]!!
                CubieCube.CornMult(CubieCube.move(m), cc, target)
                CubieCube.EdgeMult(CubieCube.move(m), cc, target)
                preMoves[maxPreMoves - maxl] = m
                val ret = phase1PreMoves(maxl - 1, m, target, ssym and CubieCube.moveCubeSym[m].toInt())
                if (ret == 0) {
                    return 0
                }
            }
            m++
        }
        return 1
    }

    private fun search(): String {
        length1 = if (isRec) length1 else 0
        while (length1 < solLen) {
            maxDep2 = minOf(MAX_DEPTH2, solLen - length1 - 1)
            urfIdx = if (isRec) urfIdx else 0
            while (urfIdx < 6) {
                if ((conjMask and (1 shl urfIdx)) == 0) {
                    if (phase1PreMoves(maxPreMoves, -30, urfCubieCube[urfIdx], (selfSym and 0xffff).toInt()) == 0) {
                        return solution?.toString() ?: "Error 8"
                    }
                }
                urfIdx++
            }
            length1++
        }
        return solution?.toString() ?: "Error 7"
    }

    /**
     * @return 0: found or probe limit exceeded, 1: at least 1 + maxDep2 moves away (try next
     * power), 2: at least 2 + maxDep2 moves away (try next axis)
     */
    private fun initPhase2Pre(): Int {
        isRec = false
        if (probe >= (if (solution == null) probeMax else probeMin)) {
            return 0
        }
        ++probe

        for (i in valid1 until depth1) {
            CubieCube.CornMult(phase1Cubie[i], CubieCube.move(move[i]), phase1Cubie[i + 1])
            CubieCube.EdgeMult(phase1Cubie[i], CubieCube.move(move[i]), phase1Cubie[i + 1])
        }
        valid1 = depth1

        var p2corn = phase1Cubie[depth1].getCPermSym()
        var p2csym = p2corn and 0xf
        p2corn = p2corn shr 4
        var p2edge = phase1Cubie[depth1].getEPermSym()
        var p2esym = p2edge and 0xf
        p2edge = p2edge shr 4
        var p2mid = phase1Cubie[depth1].getMPerm()
        var edgei = CubieCube.getPermSymInv(p2edge, p2esym, false)
        var corni = CubieCube.getPermSymInv(p2corn, p2csym, true)

        val lastMove = if (depth1 == 0) -1 else move[depth1 - 1]
        val lastPre = if (preMoveLen == 0) -1 else preMoves[preMoveLen - 1]

        var ret = 0
        val p2switchMax = (if (preMoveLen == 0) 1 else 2) * (if (depth1 == 0) 1 else 2)
        var p2switchMask = (1 shl p2switchMax) - 1
        var p2switch = 0
        while (p2switch < p2switchMax) {
            // 0 normal; 1 lastmove; 2 lastmove + premove; 3 premove
            if (((p2switchMask shr p2switch) and 1) != 0) {
                p2switchMask = p2switchMask and (1 shl p2switch).inv()
                ret = initPhase2(p2corn, p2csym, p2edge, p2esym, p2mid, edgei, corni)
                if (ret == 0 || ret > 2) {
                    break
                } else if (ret == 2) {
                    p2switchMask = p2switchMask and (0x4 shl p2switch) // 0->2; 1=>3; 2=>N/A
                }
            }
            if (p2switchMask == 0) {
                break
            }
            if ((p2switch and 1) == 0 && depth1 > 0) {
                val m = Util.std2ud[lastMove / 3 * 3 + 1]
                move[depth1 - 1] = Util.ud2std[m] * 2 - move[depth1 - 1]

                p2mid = CoordCube.MPermMove[p2mid][m]
                p2corn = CoordCube.CPermMove[p2corn][CubieCube.SymMoveUD[p2csym][m]]
                p2csym = CubieCube.SymMult[p2corn and 0xf][p2csym]
                p2corn = p2corn shr 4
                p2edge = CoordCube.EPermMove[p2edge][CubieCube.SymMoveUD[p2esym][m]]
                p2esym = CubieCube.SymMult[p2edge and 0xf][p2esym]
                p2edge = p2edge shr 4
                corni = CubieCube.getPermSymInv(p2corn, p2csym, true)
                edgei = CubieCube.getPermSymInv(p2edge, p2esym, false)
            } else if (preMoveLen > 0) {
                val m = Util.std2ud[lastPre / 3 * 3 + 1]
                preMoves[preMoveLen - 1] = Util.ud2std[m] * 2 - preMoves[preMoveLen - 1]

                p2mid = CubieCube.MPermInv[CoordCube.MPermMove[CubieCube.MPermInv[p2mid].toInt()][m]].toInt()
                p2corn = CoordCube.CPermMove[corni shr 4][CubieCube.SymMoveUD[corni and 0xf][m]]
                corni = (p2corn and 0xf.inv()) or CubieCube.SymMult[p2corn and 0xf][corni and 0xf]
                p2corn = CubieCube.getPermSymInv(corni shr 4, corni and 0xf, true)
                p2csym = p2corn and 0xf
                p2corn = p2corn shr 4
                p2edge = CoordCube.EPermMove[edgei shr 4][CubieCube.SymMoveUD[edgei and 0xf][m]]
                edgei = (p2edge and 0xf.inv()) or CubieCube.SymMult[p2edge and 0xf][edgei and 0xf]
                p2edge = CubieCube.getPermSymInv(edgei shr 4, edgei and 0xf, false)
                p2esym = p2edge and 0xf
                p2edge = p2edge shr 4
            }
            p2switch++
        }
        if (depth1 > 0) {
            move[depth1 - 1] = lastMove
        }
        if (preMoveLen > 0) {
            preMoves[preMoveLen - 1] = lastPre
        }
        return if (ret == 0) 0 else 2
    }

    private fun initPhase2(p2corn: Int, p2csym: Int, p2edge: Int, p2esym: Int, p2mid: Int, edgei: Int, corni: Int): Int {
        val prun = maxOf(
            CoordCube.getPruning(
                CoordCube.EPermCCombPPrun,
                (edgei shr 4) * CoordCube.N_COMB +
                    CoordCube.CCombPConj[CubieCube.Perm2CombP[corni shr 4].toInt() and 0xff][CubieCube.SymMultInv[edgei and 0xf][corni and 0xf]],
            ),
            maxOf(
                CoordCube.getPruning(
                    CoordCube.EPermCCombPPrun,
                    p2edge * CoordCube.N_COMB +
                        CoordCube.CCombPConj[CubieCube.Perm2CombP[p2corn].toInt() and 0xff][CubieCube.SymMultInv[p2esym][p2csym]],
                ),
                CoordCube.getPruning(CoordCube.MCPermPrun, p2corn * CoordCube.N_MPERM + CoordCube.MPermConj[p2mid][p2csym]),
            ),
        )

        if (prun > maxDep2) {
            return prun - maxDep2
        }

        var depth2 = maxDep2
        while (depth2 >= prun) {
            val ret = phase2(p2edge, p2esym, p2corn, p2csym, p2mid, depth2, depth1, 10)
            if (ret < 0) {
                break
            }
            depth2 -= ret
            solLen = 0
            val sol = Util.Solution()
            solution = sol
            sol.setArgs(verbose, urfIdx, depth1)
            for (i in 0 until depth1 + depth2) {
                sol.appendSolMove(move[i])
            }
            for (i in preMoveLen - 1 downTo 0) {
                sol.appendSolMove(preMoves[i])
            }
            solLen = sol.length
            depth2--
        }

        if (depth2 != maxDep2) { // At least one solution has been found.
            maxDep2 = minOf(MAX_DEPTH2, solLen - length1 - 1)
            return if (probe >= probeMin) 0 else 1
        }
        return 1
    }

    /**
     * @return 0: found or probe limit exceeded, 1: try next power, 2: try next axis
     */
    private fun phase1(node: CoordCube, ssym: Int, maxl: Int, lm: Int): Int {
        if (node.prun == 0 && maxl < 5) {
            return if (allowShorter || maxl == 0) {
                depth1 -= maxl
                val ret = initPhase2Pre()
                depth1 += maxl
                ret
            } else {
                1
            }
        }

        val skipMoves = CubieCube.getSkipMoves(ssym.toLong())

        var axis = 0
        while (axis < 18) {
            if (axis == lm || axis == lm - 9) {
                axis += 3
                continue
            }
            for (power in 0 until 3) {
                val m = axis + power

                if (isRec && m != move[depth1 - maxl] ||
                    skipMoves != 0 && (skipMoves and (1 shl m)) != 0
                ) {
                    continue
                }

                var prun = nodeUD[maxl].doMovePrun(node, m, true)
                if (prun > maxl) {
                    break
                } else if (prun == maxl) {
                    continue
                }

                if (USE_CONJ_PRUN) {
                    prun = nodeUD[maxl].doMovePrunConj(node, m)
                    if (prun > maxl) {
                        break
                    } else if (prun == maxl) {
                        continue
                    }
                }

                move[depth1 - maxl] = m
                valid1 = minOf(valid1, depth1 - maxl)
                val ret = phase1(nodeUD[maxl], ssym and CubieCube.moveCubeSym[m].toInt(), maxl - 1, axis)
                if (ret == 0) {
                    return 0
                } else if (ret >= 2) {
                    break
                }
            }
            axis += 3
        }
        return 1
    }

    /**
     * -1: no solution found; X: solution with X moves shorter than expectation, so the length of
     * the solution is depth - X.
     */
    private fun phase2(edge: Int, esym: Int, corn: Int, csym: Int, mid: Int, maxl: Int, depth: Int, lm: Int): Int {
        if (edge == 0 && corn == 0 && mid == 0) {
            return maxl
        }
        val moveMask = Util.ckmv2bit[lm]
        var m = 0
        while (m < 10) {
            run body@{
                if (((moveMask shr m) and 1) != 0) {
                    m += (0x42 shr m) and 3
                    return@body
                }
                val midx = CoordCube.MPermMove[mid][m]
                var cornx = CoordCube.CPermMove[corn][CubieCube.SymMoveUD[csym][m]]
                val csymx = CubieCube.SymMult[cornx and 0xf][csym]
                cornx = cornx shr 4
                var edgex = CoordCube.EPermMove[edge][CubieCube.SymMoveUD[esym][m]]
                val esymx = CubieCube.SymMult[edgex and 0xf][esym]
                edgex = edgex shr 4
                val edgei = CubieCube.getPermSymInv(edgex, esymx, false)
                val corni = CubieCube.getPermSymInv(cornx, csymx, true)

                var prun = CoordCube.getPruning(
                    CoordCube.EPermCCombPPrun,
                    (edgei shr 4) * CoordCube.N_COMB +
                        CoordCube.CCombPConj[CubieCube.Perm2CombP[corni shr 4].toInt() and 0xff][CubieCube.SymMultInv[edgei and 0xf][corni and 0xf]],
                )
                if (prun > maxl + 1) {
                    return maxl - prun + 1
                } else if (prun >= maxl) {
                    m += (0x42 shr m) and 3 and (maxl - prun)
                    return@body
                }
                prun = maxOf(
                    CoordCube.getPruning(CoordCube.MCPermPrun, cornx * CoordCube.N_MPERM + CoordCube.MPermConj[midx][csymx]),
                    CoordCube.getPruning(
                        CoordCube.EPermCCombPPrun,
                        edgex * CoordCube.N_COMB +
                            CoordCube.CCombPConj[CubieCube.Perm2CombP[cornx].toInt() and 0xff][CubieCube.SymMultInv[esymx][csymx]],
                    ),
                )
                if (prun >= maxl) {
                    m += (0x42 shr m) and 3 and (maxl - prun)
                    return@body
                }
                val ret = phase2(edgex, esymx, cornx, csymx, midx, maxl - 1, depth + 1, m)
                if (ret >= 0) {
                    move[depth] = Util.ud2std[m]
                    return ret
                }
                if (ret < -2) {
                    m = 10 // break
                    return@body
                }
                if (ret < -1) {
                    m += (0x42 shr m) and 3
                }
            }
            m++
        }
        return -1
    }

    companion object {
        const val USE_TWIST_FLIP_PRUN = true

        // Options for research purpose.
        const val MAX_PRE_MOVES = 20
        const val TRY_INVERSE = true
        const val TRY_THREE_AXES = true

        const val USE_COMBP_PRUN = USE_TWIST_FLIP_PRUN
        const val USE_CONJ_PRUN = USE_TWIST_FLIP_PRUN
        const val MIN_P1LENGTH_PRE = 7
        const val MAX_DEPTH2 = 12

        var inited = false
            private set

        const val USE_SEPARATOR = 0x1
        const val INVERSE_SOLUTION = 0x2
        const val APPEND_LENGTH = 0x4
        const val OPTIMAL_SOLUTION = 0x8

        private val lock = Any()

        fun isInited(): Boolean = inited

        fun init() {
            withSolverLock(lock) {
                CoordCube.init(true)
                inited = true
            }
        }
    }
}
