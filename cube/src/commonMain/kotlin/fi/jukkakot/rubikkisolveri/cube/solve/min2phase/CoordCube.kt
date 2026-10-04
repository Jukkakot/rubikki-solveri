/*
 * Kotlin port of min2phase by Chen Shuang, MIT licence (see Util.kt and LICENSE).
 */
package fi.jukkakot.rubikkisolveri.cube.solve.min2phase

import fi.jukkakot.rubikkisolveri.cube.solve.withSolverLock

internal class CoordCube {
    var twist = 0
    var tsym = 0
    var flip = 0
    var fsym = 0
    var slice = 0
    var prun = 0

    var twistc = 0
    var flipc = 0

    fun set(node: CoordCube) {
        twist = node.twist
        tsym = node.tsym
        flip = node.flip
        fsym = node.fsym
        slice = node.slice
        prun = node.prun
        if (Search.USE_CONJ_PRUN) {
            twistc = node.twistc
            flipc = node.flipc
        }
    }

    fun calcPruning(@Suppress("UNUSED_PARAMETER") isPhase1: Boolean) {
        prun = maxOf(
            maxOf(
                getPruning(UDSliceTwistPrun, twist * N_SLICE + UDSliceConj[slice][tsym]),
                getPruning(UDSliceFlipPrun, flip * N_SLICE + UDSliceConj[slice][fsym]),
            ),
            maxOf(
                if (Search.USE_CONJ_PRUN) {
                    getPruning(TwistFlipPrun, ((twistc shr 3) shl 11) or CubieCube.FlipS2RF[flipc xor (twistc and 7)])
                } else {
                    0
                },
                if (Search.USE_TWIST_FLIP_PRUN) {
                    getPruning(TwistFlipPrun, (twist shl 11) or CubieCube.FlipS2RF[(flip shl 3) or (fsym xor tsym)])
                } else {
                    0
                },
            ),
        )
    }

    fun setWithPrun(cc: CubieCube, depth: Int): Boolean {
        twist = cc.getTwistSym()
        flip = cc.getFlipSym()
        tsym = twist and 7
        twist = twist shr 3

        prun = if (Search.USE_TWIST_FLIP_PRUN) {
            getPruning(TwistFlipPrun, (twist shl 11) or CubieCube.FlipS2RF[flip xor tsym])
        } else {
            0
        }
        if (prun > depth) return false

        fsym = flip and 7
        flip = flip shr 3

        slice = cc.getUDSlice()
        prun = maxOf(
            prun,
            maxOf(
                getPruning(UDSliceTwistPrun, twist * N_SLICE + UDSliceConj[slice][tsym]),
                getPruning(UDSliceFlipPrun, flip * N_SLICE + UDSliceConj[slice][fsym]),
            ),
        )
        if (prun > depth) return false

        if (Search.USE_CONJ_PRUN) {
            val pc = CubieCube()
            CubieCube.CornConjugate(cc, 1, pc)
            CubieCube.EdgeConjugate(cc, 1, pc)
            twistc = pc.getTwistSym()
            flipc = pc.getFlipSym()
            prun = maxOf(
                prun,
                getPruning(TwistFlipPrun, ((twistc shr 3) shl 11) or CubieCube.FlipS2RF[flipc xor (twistc and 7)]),
            )
        }
        return prun <= depth
    }

    /** @return pruning value */
    fun doMovePrun(cc: CoordCube, m: Int, @Suppress("UNUSED_PARAMETER") isPhase1: Boolean): Int {
        slice = UDSliceMove[cc.slice][m]

        flip = FlipMove[cc.flip][CubieCube.Sym8Move[(m shl 3) or cc.fsym]]
        fsym = (flip and 7) xor cc.fsym
        flip = flip shr 3

        twist = TwistMove[cc.twist][CubieCube.Sym8Move[(m shl 3) or cc.tsym]]
        tsym = (twist and 7) xor cc.tsym
        twist = twist shr 3

        prun = maxOf(
            maxOf(
                getPruning(UDSliceTwistPrun, twist * N_SLICE + UDSliceConj[slice][tsym]),
                getPruning(UDSliceFlipPrun, flip * N_SLICE + UDSliceConj[slice][fsym]),
            ),
            if (Search.USE_TWIST_FLIP_PRUN) {
                getPruning(TwistFlipPrun, (twist shl 11) or CubieCube.FlipS2RF[(flip shl 3) or (fsym xor tsym)])
            } else {
                0
            },
        )
        return prun
    }

    fun doMovePrunConj(cc: CoordCube, m0: Int): Int {
        val m = CubieCube.SymMove[3][m0]
        flipc = FlipMove[cc.flipc shr 3][CubieCube.Sym8Move[(m shl 3) or (cc.flipc and 7)]] xor (cc.flipc and 7)
        twistc = TwistMove[cc.twistc shr 3][CubieCube.Sym8Move[(m shl 3) or (cc.twistc and 7)]] xor (cc.twistc and 7)
        return getPruning(TwistFlipPrun, ((twistc shr 3) shl 11) or CubieCube.FlipS2RF[flipc xor (twistc and 7)])
    }

    @Suppress("FunctionName")
    companion object {
        const val N_MOVES = 18
        const val N_MOVES2 = 10

        const val N_SLICE = 495
        const val N_TWIST = 2187
        const val N_TWIST_SYM = 324
        const val N_FLIP = 2048
        const val N_FLIP_SYM = 336
        const val N_PERM = 40320
        const val N_PERM_SYM = 2768
        const val N_MPERM = 24
        const val N_COMB = 140 // Search.USE_COMBP_PRUN
        const val P2_PARITY_MOVE = 0xA5 // Search.USE_COMBP_PRUN

        // XMove = Move Table, XPrun = Pruning Table, XConj = Conjugate Table (char in Java).

        // phase1
        val UDSliceMove = Array(N_SLICE) { IntArray(N_MOVES) }
        val TwistMove = Array(N_TWIST_SYM) { IntArray(N_MOVES) }
        val FlipMove = Array(N_FLIP_SYM) { IntArray(N_MOVES) }
        val UDSliceConj = Array(N_SLICE) { IntArray(8) }
        val UDSliceTwistPrun = IntArray(N_SLICE * N_TWIST_SYM / 8 + 1)
        val UDSliceFlipPrun = IntArray(N_SLICE * N_FLIP_SYM / 8 + 1)
        val TwistFlipPrun = IntArray(N_FLIP * N_TWIST_SYM / 8 + 1)

        // phase2
        val CPermMove = Array(N_PERM_SYM) { IntArray(N_MOVES2) }
        val EPermMove = Array(N_PERM_SYM) { IntArray(N_MOVES2) }
        val MPermMove = Array(N_MPERM) { IntArray(N_MOVES2) }
        val MPermConj = Array(N_MPERM) { IntArray(16) }
        val CCombPMove = Array(N_COMB) { IntArray(N_MOVES2) }
        val CCombPConj = Array(N_COMB) { IntArray(16) }
        val MCPermPrun = IntArray(N_MPERM * N_PERM_SYM / 8 + 1)
        val EPermCCombPPrun = IntArray(N_COMB * N_PERM_SYM / 8 + 1)

        /** 0: not initialized, 1: partially initialized, 2: finished. */
        var initLevel = 0

        private val lock = Any()

        fun init(fullInit: Boolean) {
            withSolverLock(lock) {
                if (initLevel == 2 || initLevel == 1 && !fullInit) return@withSolverLock
                if (initLevel == 0) {
                    CubieCube.initPermSym2Raw()
                    initCPermMove()
                    initEPermMove()
                    initMPermMoveConj()
                    initCombPMoveConj()

                    CubieCube.initFlipSym2Raw()
                    CubieCube.initTwistSym2Raw()
                    initFlipMove()
                    initTwistMove()
                    initUDSliceMoveConj()
                }
                initMCPermPrun(fullInit)
                initPermCombPPrun(fullInit)
                initSliceTwistPrun(fullInit)
                initSliceFlipPrun(fullInit)
                if (Search.USE_TWIST_FLIP_PRUN) {
                    initTwistFlipPrun(fullInit)
                }
                initLevel = if (fullInit) 2 else 1
            }
        }

        fun setPruning(table: IntArray, index: Int, value: Int) {
            table[index shr 3] = table[index shr 3] xor (value shl (index shl 2)) // index << 2 <=> (index & 7) << 2
        }

        fun getPruning(table: IntArray, index: Int): Int =
            (table[index shr 3] shr (index shl 2)) and 0xf // index << 2 <=> (index & 7) << 2

        fun initUDSliceMoveConj() {
            val c = CubieCube()
            val d = CubieCube()
            for (i in 0 until N_SLICE) {
                c.setUDSlice(i)
                for (j in 0 until N_MOVES step 3) {
                    CubieCube.EdgeMult(c, CubieCube.move(j), d)
                    UDSliceMove[i][j] = d.getUDSlice()
                }
                for (j in 0 until 16 step 2) {
                    CubieCube.EdgeConjugate(c, CubieCube.SymMultInv[0][j], d)
                    UDSliceConj[i][j shr 1] = d.getUDSlice()
                }
            }
            for (i in 0 until N_SLICE) {
                for (j in 0 until N_MOVES step 3) {
                    var udslice = UDSliceMove[i][j]
                    for (k in 1 until 3) {
                        udslice = UDSliceMove[udslice][j]
                        UDSliceMove[i][j + k] = udslice
                    }
                }
            }
        }

        fun initFlipMove() {
            val c = CubieCube()
            val d = CubieCube()
            for (i in 0 until N_FLIP_SYM) {
                c.setFlip(CubieCube.FlipS2R[i])
                for (j in 0 until N_MOVES) {
                    CubieCube.EdgeMult(c, CubieCube.move(j), d)
                    FlipMove[i][j] = d.getFlipSym()
                }
            }
        }

        fun initTwistMove() {
            val c = CubieCube()
            val d = CubieCube()
            for (i in 0 until N_TWIST_SYM) {
                c.setTwist(CubieCube.TwistS2R[i])
                for (j in 0 until N_MOVES) {
                    CubieCube.CornMult(c, CubieCube.move(j), d)
                    TwistMove[i][j] = d.getTwistSym()
                }
            }
        }

        fun initCPermMove() {
            val c = CubieCube()
            val d = CubieCube()
            for (i in 0 until N_PERM_SYM) {
                c.setCPerm(CubieCube.EPermS2R[i])
                for (j in 0 until N_MOVES2) {
                    CubieCube.CornMult(c, CubieCube.move(Util.ud2std[j]), d)
                    CPermMove[i][j] = d.getCPermSym()
                }
            }
        }

        fun initEPermMove() {
            val c = CubieCube()
            val d = CubieCube()
            for (i in 0 until N_PERM_SYM) {
                c.setEPerm(CubieCube.EPermS2R[i])
                for (j in 0 until N_MOVES2) {
                    CubieCube.EdgeMult(c, CubieCube.move(Util.ud2std[j]), d)
                    EPermMove[i][j] = d.getEPermSym()
                }
            }
        }

        fun initMPermMoveConj() {
            val c = CubieCube()
            val d = CubieCube()
            for (i in 0 until N_MPERM) {
                c.setMPerm(i)
                for (j in 0 until N_MOVES2) {
                    CubieCube.EdgeMult(c, CubieCube.move(Util.ud2std[j]), d)
                    MPermMove[i][j] = d.getMPerm()
                }
                for (j in 0 until 16) {
                    CubieCube.EdgeConjugate(c, CubieCube.SymMultInv[0][j], d)
                    MPermConj[i][j] = d.getMPerm()
                }
            }
        }

        fun initCombPMoveConj() {
            val c = CubieCube()
            val d = CubieCube()
            for (i in 0 until N_COMB) {
                c.setCComb(i % 70)
                for (j in 0 until N_MOVES2) {
                    CubieCube.CornMult(c, CubieCube.move(Util.ud2std[j]), d)
                    CCombPMove[i][j] = (d.getCComb() + 70 * (((P2_PARITY_MOVE shr j) and 1) xor (i / 70))) and 0xffff
                }
                for (j in 0 until 16) {
                    CubieCube.CornConjugate(c, CubieCube.SymMultInv[0][j], d)
                    CCombPConj[i][j] = (d.getCComb() + 70 * (i / 70)) and 0xffff
                }
            }
        }

        fun hasZero(v: Int): Boolean = ((v - 0x11111111) and v.inv() and 0x88888888.toInt()) != 0

        //          |   4 bits  |   4 bits  |   4 bits  |  2 bits | 1b |  1b |   4 bits  |
        // PrunFlag: | MIN_DEPTH | MAX_DEPTH | INV_DEPTH | Padding | P2 | E2C | SYM_SHIFT |
        fun initRawSymPrun(
            prunTable: IntArray,
            rawMove: Array<IntArray>?,
            rawConj: Array<IntArray>?,
            symMove: Array<IntArray>,
            symStateArr: IntArray,
            prunFlag: Int,
            fullInit: Boolean,
        ) {
            val symShift = prunFlag and 0xf
            val symE2cMagic = if (((prunFlag shr 4) and 1) == 1) CubieCube.SYM_E2C_MAGIC else 0x00000000
            val isPhase2 = ((prunFlag shr 5) and 1) == 1
            val invDepth = (prunFlag shr 8) and 0xf
            val maxDepth = (prunFlag shr 12) and 0xf
            val minDepth = (prunFlag shr 16) and 0xf
            val searchDepth = if (fullInit) maxDepth else minDepth

            val symMask = (1 shl symShift) - 1
            val isTfp = rawMove == null
            val nRaw = if (isTfp) N_FLIP else rawMove!!.size
            val nSize = nRaw * symMove.size
            val nMoves = if (isPhase2) 10 else 18
            val nextAxisMagic = if (nMoves == 10) 0x42 else 0x92492

            var depth = getPruning(prunTable, nSize) - 1

            if (depth == -1) {
                for (i in 0 until nSize / 8 + 1) {
                    prunTable[i] = 0x11111111
                }
                setPruning(prunTable, 0, 0 xor 1)
                depth = 0
            }

            while (depth < searchDepth) {
                val mask = ((depth + 1) * 0x11111111) xor -1
                for (i in prunTable.indices) {
                    var v = prunTable[i] xor mask
                    v = v and (v shr 1)
                    prunTable[i] += v and (v shr 2) and 0x11111111
                }

                val inv = depth > invDepth
                val select = if (inv) depth + 2 else depth
                val selArrMask = select * 0x11111111
                val check = if (inv) depth else depth + 2
                depth++
                val xorVal = depth xor (depth + 1)
                var v = 0
                var i = 0
                while (i < nSize) {
                    // Java: for (...; i++, val >>= 4) with `continue` running the update too.
                    run body@{
                        if ((i and 7) == 0) {
                            v = prunTable[i shr 3]
                            if (!hasZero(v xor selArrMask)) {
                                i += 7
                                return@body
                            }
                        }
                        if ((v and 0xf) != select) return@body
                        val raw = i % nRaw
                        val sym = i / nRaw
                        var flip = 0
                        var fsym = 0
                        if (isTfp) {
                            flip = CubieCube.FlipR2S[raw]
                            fsym = flip and 7
                            flip = flip shr 3
                        }

                        var m = 0
                        while (m < nMoves) {
                            run move@{
                                var symx = symMove[sym][m]
                                val rawx: Int = if (isTfp) {
                                    CubieCube.FlipS2RF[
                                        FlipMove[flip][CubieCube.Sym8Move[(m shl 3) or fsym]] xor fsym xor (symx and symMask),
                                    ]
                                } else {
                                    rawConj!![rawMove!![raw][m]][symx and symMask]
                                }
                                symx = symx shr symShift
                                val idx = symx * nRaw + rawx
                                val prun = getPruning(prunTable, idx)
                                if (prun != check) {
                                    if (prun < depth - 1) {
                                        m += (nextAxisMagic shr m) and 3
                                    }
                                    return@move
                                }
                                if (inv) {
                                    setPruning(prunTable, i, xorVal)
                                    m = nMoves // break
                                    return@move
                                }
                                setPruning(prunTable, idx, xorVal)
                                var j = 1
                                var symState = symStateArr[symx]
                                while (true) {
                                    symState = symState shr 1
                                    if (symState == 0) break
                                    if ((symState and 1) == 1) {
                                        var idxx = symx * nRaw
                                        idxx += if (isTfp) {
                                            CubieCube.FlipS2RF[CubieCube.FlipR2S[rawx] xor j]
                                        } else {
                                            rawConj!![rawx][j xor ((symE2cMagic shr (j shl 1)) and 3)]
                                        }
                                        if (getPruning(prunTable, idxx) == check) {
                                            setPruning(prunTable, idxx, xorVal)
                                        }
                                    }
                                    j++
                                }
                            }
                            m++
                        }
                    }
                    i++
                    v = v shr 4
                }
            }
        }

        fun initTwistFlipPrun(fullInit: Boolean) {
            initRawSymPrun(TwistFlipPrun, null, null, TwistMove, CubieCube.SymStateTwist, 0x19603, fullInit)
        }

        fun initSliceTwistPrun(fullInit: Boolean) {
            initRawSymPrun(UDSliceTwistPrun, UDSliceMove, UDSliceConj, TwistMove, CubieCube.SymStateTwist, 0x69603, fullInit)
        }

        fun initSliceFlipPrun(fullInit: Boolean) {
            initRawSymPrun(UDSliceFlipPrun, UDSliceMove, UDSliceConj, FlipMove, CubieCube.SymStateFlip, 0x69603, fullInit)
        }

        fun initMCPermPrun(fullInit: Boolean) {
            initRawSymPrun(MCPermPrun, MPermMove, MPermConj, CPermMove, CubieCube.SymStatePerm, 0x8ea34, fullInit)
        }

        fun initPermCombPPrun(fullInit: Boolean) {
            initRawSymPrun(EPermCCombPPrun, CCombPMove, CCombPConj, EPermMove, CubieCube.SymStatePerm, 0x7d824, fullInit)
        }
    }
}
