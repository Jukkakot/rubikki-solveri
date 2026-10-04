/*
 * Kotlin port of min2phase by Chen Shuang, MIT licence (see Util.kt and LICENSE).
 */
package fi.jukkakot.rubikkisolveri.cube.solve.min2phase

internal class CubieCube() {
    val ca = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
    val ea = byteArrayOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22)
    private var temps: CubieCube? = null

    constructor(cperm: Int, twist: Int, eperm: Int, flip: Int) : this() {
        setCPerm(cperm)
        setTwist(twist)
        Util.setNPerm(ea, eperm, 12, true)
        setFlip(flip)
    }

    constructor(c: CubieCube) : this() {
        copy(c)
    }

    fun copy(c: CubieCube) {
        for (i in 0 until 8) ca[i] = c.ca[i]
        for (i in 0 until 12) ea[i] = c.ea[i]
    }

    fun invCubieCube() {
        val t = temps ?: CubieCube().also { temps = it }
        for (edge in 0 until 12) {
            val e = ea[edge].toInt()
            t.ea[e shr 1] = ((edge shl 1) or (e and 1)).toByte()
        }
        for (corn in 0 until 8) {
            val c = ca[corn].toInt()
            t.ca[c and 0x7] = (corn or ((0x20 shr (c shr 3)) and 0x18)).toByte()
        }
        copy(t)
    }

    /** this = S_urf^-1 * this * S_urf. */
    fun URFConjugate() {
        val t = temps ?: CubieCube().also { temps = it }
        CornMult(urf2, this, t)
        CornMult(t, urf1, this)
        EdgeMult(urf2, this, t)
        EdgeMult(t, urf1, this)
    }

    fun getFlip(): Int {
        var idx = 0
        for (i in 0 until 11) {
            idx = (idx shl 1) or (ea[i].toInt() and 1)
        }
        return idx
    }

    fun setFlip(idx0: Int) {
        var idx = idx0
        var parity = 0
        for (i in 10 downTo 0) {
            val v = idx and 1
            parity = parity xor v
            ea[i] = ((ea[i].toInt() and 1.inv()) or v).toByte()
            idx = idx shr 1
        }
        ea[11] = ((ea[11].toInt() and 1.inv()) or parity).toByte()
    }

    fun getFlipSym(): Int = FlipR2S[getFlip()]

    fun getTwist(): Int {
        var idx = 0
        for (i in 0 until 7) {
            idx += (idx shl 1) + (ca[i].toInt() shr 3)
        }
        return idx
    }

    fun setTwist(idx0: Int) {
        var idx = idx0
        var twst = 15
        for (i in 6 downTo 0) {
            val v = idx % 3
            twst -= v
            ca[i] = ((ca[i].toInt() and 0x7) or (v shl 3)).toByte()
            idx /= 3
        }
        ca[7] = ((ca[7].toInt() and 0x7) or ((twst % 3) shl 3)).toByte()
    }

    fun getTwistSym(): Int = TwistR2S[getTwist()]

    fun getUDSlice(): Int = 494 - Util.getComb(ea, 8, true)

    fun setUDSlice(idx: Int) {
        Util.setComb(ea, 494 - idx, 8, true)
    }

    fun getCPerm(): Int = Util.getNPerm(ca, 8, false)

    fun setCPerm(idx: Int) {
        Util.setNPerm(ca, idx, 8, false)
    }

    fun getCPermSym(): Int = ESym2CSym(EPermR2S[getCPerm()])

    fun getEPerm(): Int = Util.getNPerm(ea, 8, true)

    fun setEPerm(idx: Int) {
        Util.setNPerm(ea, idx, 8, true)
    }

    fun getEPermSym(): Int = EPermR2S[getEPerm()]

    fun getMPerm(): Int = Util.getNPerm(ea, 12, true) % 24

    fun setMPerm(idx: Int) {
        Util.setNPerm(ea, idx, 12, true)
    }

    fun getCComb(): Int = Util.getComb(ca, 0, false)

    fun setCComb(idx: Int) {
        Util.setComb(ca, idx, 0, false)
    }

    /**
     * 0: solvable, -2: not all 12 edges exactly once, -3: flip error, -4: not all corners exactly
     * once, -5: twist error, -6: parity error.
     */
    fun verify(): Int {
        var sum = 0
        var edgeMask = 0
        for (e in 0 until 12) {
            edgeMask = edgeMask or (1 shl (ea[e].toInt() shr 1))
            sum = sum xor (ea[e].toInt() and 1)
        }
        if (edgeMask != 0xfff) return -2
        if (sum != 0) return -3
        var cornMask = 0
        sum = 0
        for (c in 0 until 8) {
            cornMask = cornMask or (1 shl (ca[c].toInt() and 7))
            sum += ca[c].toInt() shr 3
        }
        if (cornMask != 0xff) return -4
        if (sum % 3 != 0) return -5
        if ((Util.getNParity(Util.getNPerm(ea, 12, true), 12) xor Util.getNParity(getCPerm(), 8)) != 0) return -6
        return 0
    }

    fun selfSymmetry(): Long {
        val c = CubieCube(this)
        val d = CubieCube()
        val cperm = c.getCPermSym() shr 4
        var sym = 0L
        for (urfInv in 0 until 6) {
            val cpermx = c.getCPermSym() shr 4
            if (cperm == cpermx) {
                for (i in 0 until 16) {
                    CornConjugate(c, SymMultInv[0][i], d)
                    if (d.ca.contentEquals(ca)) {
                        EdgeConjugate(c, SymMultInv[0][i], d)
                        if (d.ea.contentEquals(ea)) {
                            sym = sym or (1L shl minOf((urfInv shl 4) or i, 48))
                        }
                    }
                }
            }
            c.URFConjugate()
            if (urfInv % 3 == 2) {
                c.invCubieCube()
            }
        }
        return sym
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0 until 8) sb.append("|" + (ca[i].toInt() and 7) + " " + (ca[i].toInt() shr 3))
        sb.append("\n")
        for (i in 0 until 12) sb.append("|" + (ea[i].toInt() shr 1) + " " + (ea[i].toInt() and 1))
        return sb.toString()
    }

    @Suppress("FunctionName")
    companion object {
        /** 16 symmetries generated by S_F2, S_U4 and S_LR2. */
        val CubeSym = arrayOfNulls<CubieCube>(16)

        /** 18 move cubes. */
        val moveCube = arrayOfNulls<CubieCube>(18)

        val moveCubeSym = LongArray(18)
        val firstMoveSym = IntArray(48)

        val SymMult = Array(16) { IntArray(16) }
        val SymMultInv = Array(16) { IntArray(16) }
        val SymMove = Array(16) { IntArray(18) }
        val Sym8Move = IntArray(8 * 18)
        val SymMoveUD = Array(16) { IntArray(18) }

        // ClassIndexToRepresentantArrays (char[] in the Java original; values < 65536).
        val FlipS2R = IntArray(CoordCube.N_FLIP_SYM)
        val TwistS2R = IntArray(CoordCube.N_TWIST_SYM)
        val EPermS2R = IntArray(CoordCube.N_PERM_SYM)
        val Perm2CombP = ByteArray(CoordCube.N_PERM_SYM)
        val PermInvEdgeSym = IntArray(CoordCube.N_PERM_SYM)
        val MPermInv = ByteArray(CoordCube.N_MPERM)

        const val SYM_E2C_MAGIC = 0x00DDDD00

        fun ESym2CSym(idx: Int): Int = idx xor ((SYM_E2C_MAGIC shr ((idx and 0xf) shl 1)) and 3)

        // Raw-Coordnate to Sym-Coordnate, only for speeding up initialization.
        val FlipR2S = IntArray(CoordCube.N_FLIP)
        val TwistR2S = IntArray(CoordCube.N_TWIST)
        val EPermR2S = IntArray(CoordCube.N_PERM)
        val FlipS2RF = IntArray(CoordCube.N_FLIP_SYM * 8)

        var SymStateTwist = IntArray(0)
        var SymStateFlip = IntArray(0)
        var SymStatePerm = IntArray(0)

        val urf1 = CubieCube(2531, 1373, 67026819, 1367)
        val urf2 = CubieCube(2089, 1906, 322752913, 2040)
        val urfMove = arrayOf(
            intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17),
            intArrayOf(6, 7, 8, 0, 1, 2, 3, 4, 5, 15, 16, 17, 9, 10, 11, 12, 13, 14),
            intArrayOf(3, 4, 5, 6, 7, 8, 0, 1, 2, 12, 13, 14, 15, 16, 17, 9, 10, 11),
            intArrayOf(2, 1, 0, 5, 4, 3, 8, 7, 6, 11, 10, 9, 14, 13, 12, 17, 16, 15),
            intArrayOf(8, 7, 6, 2, 1, 0, 5, 4, 3, 17, 16, 15, 11, 10, 9, 14, 13, 12),
            intArrayOf(5, 4, 3, 8, 7, 6, 2, 1, 0, 14, 13, 12, 17, 16, 15, 11, 10, 9),
        )

        fun move(i: Int): CubieCube = moveCube[i]!!

        fun sym(i: Int): CubieCube = CubeSym[i]!!

        /** prod = a * b, Corner Only. */
        fun CornMult(a: CubieCube, b: CubieCube, prod: CubieCube) {
            for (corn in 0 until 8) {
                val bc = b.ca[corn].toInt()
                val ac = a.ca[bc and 7].toInt()
                val oriA = ac shr 3
                val oriB = bc shr 3
                prod.ca[corn] = ((ac and 7) or (((oriA + oriB) % 3) shl 3)).toByte()
            }
        }

        /** prod = a * b, Corner Only. With mirrored cases considered. */
        fun CornMultFull(a: CubieCube, b: CubieCube, prod: CubieCube) {
            for (corn in 0 until 8) {
                val bc = b.ca[corn].toInt()
                val ac = a.ca[bc and 7].toInt()
                val oriA = ac shr 3
                val oriB = bc shr 3
                var ori = oriA + (if (oriA < 3) oriB else 6 - oriB)
                ori = ori % 3 + (if ((oriA < 3) == (oriB < 3)) 0 else 3)
                prod.ca[corn] = ((ac and 7) or (ori shl 3)).toByte()
            }
        }

        /** prod = a * b, Edge Only. */
        fun EdgeMult(a: CubieCube, b: CubieCube, prod: CubieCube) {
            for (ed in 0 until 12) {
                val be = b.ea[ed].toInt()
                prod.ea[ed] = (a.ea[be shr 1].toInt() xor (be and 1)).toByte()
            }
        }

        /** b = S_idx^-1 * a * S_idx, Corner Only. */
        fun CornConjugate(a: CubieCube, idx: Int, b: CubieCube) {
            val sinv = sym(SymMultInv[0][idx])
            val s = sym(idx)
            for (corn in 0 until 8) {
                val acs = a.ca[s.ca[corn].toInt() and 7].toInt()
                val sv = sinv.ca[acs and 7].toInt()
                val oriA = sv shr 3
                val oriB = acs shr 3
                val ori = if (oriA < 3) oriB else (3 - oriB) % 3
                b.ca[corn] = ((sv and 7) or (ori shl 3)).toByte()
            }
        }

        /** b = S_idx^-1 * a * S_idx, Edge Only. */
        fun EdgeConjugate(a: CubieCube, idx: Int, b: CubieCube) {
            val sinv = sym(SymMultInv[0][idx])
            val s = sym(idx)
            for (ed in 0 until 12) {
                val se = s.ea[ed].toInt()
                val ase = a.ea[se shr 1].toInt()
                b.ea[ed] = (sinv.ea[ase shr 1].toInt() xor (ase and 1) xor (se and 1)).toByte()
            }
        }

        fun getPermSymInv(idx: Int, sym: Int, isCorner: Boolean): Int {
            var idxi = PermInvEdgeSym[idx]
            if (isCorner) {
                idxi = ESym2CSym(idxi)
            }
            return (idxi and 0xfff0) or SymMult[idxi and 0xf][sym]
        }

        fun getSkipMoves(ssym0: Long): Int {
            var ssym = ssym0
            var ret = 0
            var i = 1
            while (true) {
                ssym = ssym shr 1
                if (ssym == 0L) break
                if ((ssym and 1L) == 1L) {
                    ret = ret or firstMoveSym[i]
                }
                i++
            }
            return ret
        }

        fun initMove() {
            moveCube[0] = CubieCube(15120, 0, 119750400, 0)
            moveCube[3] = CubieCube(21021, 1494, 323403417, 0)
            moveCube[6] = CubieCube(8064, 1236, 29441808, 550)
            moveCube[9] = CubieCube(9, 0, 5880, 0)
            moveCube[12] = CubieCube(1230, 412, 2949660, 0)
            moveCube[15] = CubieCube(224, 137, 328552, 137)
            for (a in 0 until 18 step 3) {
                for (p in 0 until 2) {
                    val next = CubieCube()
                    moveCube[a + p + 1] = next
                    EdgeMult(move(a + p), move(a), next)
                    CornMult(move(a + p), move(a), next)
                }
            }
        }

        fun initSym() {
            var c = CubieCube()
            var d = CubieCube()
            var t: CubieCube

            val f2 = CubieCube(28783, 0, 259268407, 0)
            val u4 = CubieCube(15138, 0, 119765538, 7)
            val lr2 = CubieCube(5167, 0, 83473207, 0)
            for (i in 0 until 8) {
                lr2.ca[i] = (lr2.ca[i].toInt() or (3 shl 3)).toByte()
            }

            for (i in 0 until 16) {
                CubeSym[i] = CubieCube(c)
                CornMultFull(c, u4, d)
                EdgeMult(c, u4, d)
                t = d; d = c; c = t
                if (i % 4 == 3) {
                    CornMultFull(c, lr2, d)
                    EdgeMult(c, lr2, d)
                    t = d; d = c; c = t
                }
                if (i % 8 == 7) {
                    CornMultFull(c, f2, d)
                    EdgeMult(c, f2, d)
                    t = d; d = c; c = t
                }
            }
            for (i in 0 until 16) {
                for (j in 0 until 16) {
                    CornMultFull(sym(i), sym(j), c)
                    for (k in 0 until 16) {
                        if (sym(k).ca.contentEquals(c.ca)) {
                            SymMult[i][j] = k
                            SymMultInv[k][j] = i
                            break
                        }
                    }
                }
            }
            for (j in 0 until 18) {
                for (s in 0 until 16) {
                    CornConjugate(move(j), SymMultInv[0][s], c)
                    for (m in 0 until 18) {
                        if (move(m).ca.contentEquals(c.ca)) {
                            SymMove[s][j] = m
                            SymMoveUD[s][Util.std2ud[j]] = Util.std2ud[m]
                            break
                        }
                    }
                    if (s % 2 == 0) {
                        Sym8Move[(j shl 3) or (s shr 1)] = SymMove[s][j]
                    }
                }
            }

            for (i in 0 until 18) {
                moveCubeSym[i] = move(i).selfSymmetry()
                var j = i
                for (s in 0 until 48) {
                    if (SymMove[s % 16][j] < i) {
                        firstMoveSym[s] = firstMoveSym[s] or (1 shl i)
                    }
                    if (s % 16 == 15) {
                        j = urfMove[2][j]
                    }
                }
            }
        }

        fun initSym2Raw(nRaw: Int, sym2Raw: IntArray, raw2Sym: IntArray, symState: IntArray, coord: Int): Int {
            val c = CubieCube()
            val d = CubieCube()
            var count = 0
            var idx = 0
            val symInc = if (coord >= 2) 1 else 2
            val isEdge = coord != 1

            for (i in 0 until nRaw) {
                if (raw2Sym[i] != 0) continue
                when (coord) {
                    0 -> c.setFlip(i)
                    1 -> c.setTwist(i)
                    2 -> c.setEPerm(i)
                }
                for (s in 0 until 16 step symInc) {
                    if (isEdge) EdgeConjugate(c, s, d) else CornConjugate(c, s, d)
                    when (coord) {
                        0 -> idx = d.getFlip()
                        1 -> idx = d.getTwist()
                        2 -> idx = d.getEPerm()
                    }
                    if (coord == 0 && Search.USE_TWIST_FLIP_PRUN) {
                        FlipS2RF[(count shl 3) or (s shr 1)] = idx
                    }
                    if (idx == i) {
                        symState[count] = (symState[count] or (1 shl (s / symInc))) and 0xffff
                    }
                    val symIdx = ((count shl 4) or s) / symInc
                    raw2Sym[idx] = symIdx and 0xffff
                }
                sym2Raw[count++] = i
            }
            return count
        }

        fun initFlipSym2Raw() {
            SymStateFlip = IntArray(CoordCube.N_FLIP_SYM)
            initSym2Raw(CoordCube.N_FLIP, FlipS2R, FlipR2S, SymStateFlip, 0)
        }

        fun initTwistSym2Raw() {
            SymStateTwist = IntArray(CoordCube.N_TWIST_SYM)
            initSym2Raw(CoordCube.N_TWIST, TwistS2R, TwistR2S, SymStateTwist, 1)
        }

        fun initPermSym2Raw() {
            SymStatePerm = IntArray(CoordCube.N_PERM_SYM)
            initSym2Raw(CoordCube.N_PERM, EPermS2R, EPermR2S, SymStatePerm, 2)
            val cc = CubieCube()
            for (i in 0 until CoordCube.N_PERM_SYM) {
                cc.setEPerm(EPermS2R[i])
                Perm2CombP[i] = (Util.getComb(cc.ea, 0, true) +
                    (if (Search.USE_COMBP_PRUN) Util.getNParity(EPermS2R[i], 8) * 70 else 0)).toByte()
                cc.invCubieCube()
                PermInvEdgeSym[i] = cc.getEPermSym()
            }
            for (i in 0 until CoordCube.N_MPERM) {
                cc.setMPerm(i)
                cc.invCubieCube()
                MPermInv[i] = cc.getMPerm().toByte()
            }
        }

        init {
            initMove()
            initSym()
        }
    }
}
