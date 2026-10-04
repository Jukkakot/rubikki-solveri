/*
 * Kotlin port of min2phase (https://github.com/cs0x7f/min2phase, commit
 * 4d183b9eff8119cac72bc50ef35a7d8990740e06) by Chen Shuang, used under its MIT licence (see
 * LICENSE in this folder). Same algorithm and tables as the Java original, which stays in
 * cube/src/jvmTest/java as the oracle for Min2phasePortTest.
 */
package fi.jukkakot.rubikkisolveri.cube.solve.min2phase

internal object Util {
    // Moves
    const val Ux1 = 0
    const val Ux2 = 1
    const val Ux3 = 2
    const val Rx1 = 3
    const val Rx2 = 4
    const val Rx3 = 5
    const val Fx1 = 6
    const val Fx2 = 7
    const val Fx3 = 8
    const val Dx1 = 9
    const val Dx2 = 10
    const val Dx3 = 11
    const val Lx1 = 12
    const val Lx2 = 13
    const val Lx3 = 14
    const val Bx1 = 15
    const val Bx2 = 16
    const val Bx3 = 17

    // Facelets (only those used by name)
    const val U1 = 0; const val U2 = 1; const val U3 = 2; const val U4 = 3; const val U5 = 4
    const val U6 = 5; const val U7 = 6; const val U8 = 7; const val U9 = 8
    const val R1 = 9; const val R2 = 10; const val R3 = 11; const val R4 = 12; const val R5 = 13
    const val R6 = 14; const val R7 = 15; const val R8 = 16; const val R9 = 17
    const val F1 = 18; const val F2 = 19; const val F3 = 20; const val F4 = 21; const val F5 = 22
    const val F6 = 23; const val F7 = 24; const val F8 = 25; const val F9 = 26
    const val D1 = 27; const val D2 = 28; const val D3 = 29; const val D4 = 30; const val D5 = 31
    const val D6 = 32; const val D7 = 33; const val D8 = 34; const val D9 = 35
    const val L1 = 36; const val L2 = 37; const val L3 = 38; const val L4 = 39; const val L5 = 40
    const val L6 = 41; const val L7 = 42; const val L8 = 43; const val L9 = 44
    const val B1 = 45; const val B2 = 46; const val B3 = 47; const val B4 = 48; const val B5 = 49
    const val B6 = 50; const val B7 = 51; const val B8 = 52; const val B9 = 53

    // Colors
    const val U = 0
    const val R = 1
    const val F = 2
    const val D = 3
    const val L = 4
    const val B = 5

    val cornerFacelet = arrayOf(
        intArrayOf(U9, R1, F3), intArrayOf(U7, F1, L3), intArrayOf(U1, L1, B3), intArrayOf(U3, B1, R3),
        intArrayOf(D3, F9, R7), intArrayOf(D1, L9, F7), intArrayOf(D7, B9, L7), intArrayOf(D9, R9, B7),
    )
    val edgeFacelet = arrayOf(
        intArrayOf(U6, R2), intArrayOf(U8, F2), intArrayOf(U4, L2), intArrayOf(U2, B2), intArrayOf(D6, R8), intArrayOf(D2, F8),
        intArrayOf(D4, L8), intArrayOf(D8, B8), intArrayOf(F6, R4), intArrayOf(F4, L6), intArrayOf(B6, L4), intArrayOf(B4, R6),
    )

    val Cnk = Array(13) { IntArray(13) }
    val move2str = arrayOf(
        "U ", "U2", "U'", "R ", "R2", "R'", "F ", "F2", "F'",
        "D ", "D2", "D'", "L ", "L2", "L'", "B ", "B2", "B'",
    )
    val ud2std = intArrayOf(Ux1, Ux2, Ux3, Rx2, Fx2, Dx1, Dx2, Dx3, Lx2, Bx2, Rx1, Rx3, Fx1, Fx3, Lx1, Lx3, Bx1, Bx3)
    val std2ud = IntArray(18)
    val ckmv2bit = IntArray(11)

    init {
        for (i in 0 until 18) {
            std2ud[ud2std[i]] = i
        }
        for (i in 0 until 10) {
            val ix = ud2std[i] / 3
            ckmv2bit[i] = 0
            for (j in 0 until 10) {
                val jx = ud2std[j] / 3
                val bit = if ((ix == jx) || ((ix % 3 == jx % 3) && (ix >= jx))) 1 else 0
                ckmv2bit[i] = ckmv2bit[i] or (bit shl j)
            }
        }
        ckmv2bit[10] = 0
        for (i in 0 until 13) {
            Cnk[i][0] = 1
            Cnk[i][i] = 1
            for (j in 1 until i) {
                Cnk[i][j] = Cnk[i - 1][j - 1] + Cnk[i - 1][j]
            }
        }
    }

    class Solution {
        var length = 0
        var depth1 = 0
        var verbose = 0
        var urfIdx = 0
        val moves = IntArray(31)

        fun setArgs(verbose: Int, urfIdx: Int, depth1: Int) {
            this.verbose = verbose
            this.urfIdx = urfIdx
            this.depth1 = depth1
        }

        fun appendSolMove(curMove: Int) {
            if (length == 0) {
                moves[length++] = curMove
                return
            }
            val axisCur = curMove / 3
            val axisLast = moves[length - 1] / 3
            if (axisCur == axisLast) {
                val pow = (curMove % 3 + moves[length - 1] % 3 + 1) % 4
                if (pow == 3) {
                    length--
                } else {
                    moves[length - 1] = axisCur * 3 + pow
                }
                return
            }
            if (length > 1 && axisCur % 3 == axisLast % 3 && axisCur == moves[length - 2] / 3) {
                val pow = (curMove % 3 + moves[length - 2] % 3 + 1) % 4
                if (pow == 3) {
                    moves[length - 2] = moves[length - 1]
                    length--
                } else {
                    moves[length - 2] = axisCur * 3 + pow
                }
                return
            }
            moves[length++] = curMove
        }

        override fun toString(): String {
            val sb = StringBuilder()
            val urf = if ((verbose and Search.INVERSE_SOLUTION) != 0) (urfIdx + 3) % 6 else urfIdx
            if (urf < 3) {
                for (s in 0 until length) {
                    if ((verbose and Search.USE_SEPARATOR) != 0 && s == depth1) {
                        sb.append(".  ")
                    }
                    sb.append(move2str[CubieCube.urfMove[urf][moves[s]]]).append(' ')
                }
            } else {
                for (s in length - 1 downTo 0) {
                    sb.append(move2str[CubieCube.urfMove[urf][moves[s]]]).append(' ')
                    if ((verbose and Search.USE_SEPARATOR) != 0 && s == depth1) {
                        sb.append(".  ")
                    }
                }
            }
            if ((verbose and Search.APPEND_LENGTH) != 0) {
                sb.append("(").append(length).append("f)")
            }
            return sb.toString()
        }
    }

    fun toCubieCube(f: ByteArray, ccRet: CubieCube) {
        for (i in 0 until 8) ccRet.ca[i] = 0
        for (i in 0 until 12) ccRet.ea[i] = 0
        for (i in 0 until 8) {
            var ori = 0
            while (ori < 3) {
                val c = f[cornerFacelet[i][ori]].toInt()
                if (c == U || c == D) break
                ori++
            }
            val col1 = f[cornerFacelet[i][(ori + 1) % 3]].toInt()
            val col2 = f[cornerFacelet[i][(ori + 2) % 3]].toInt()
            for (j in 0 until 8) {
                if (col1 == cornerFacelet[j][1] / 9 && col2 == cornerFacelet[j][2] / 9) {
                    ccRet.ca[i] = (((ori % 3) shl 3) or j).toByte()
                    break
                }
            }
        }
        for (i in 0 until 12) {
            for (j in 0 until 12) {
                if (f[edgeFacelet[i][0]].toInt() == edgeFacelet[j][0] / 9 &&
                    f[edgeFacelet[i][1]].toInt() == edgeFacelet[j][1] / 9
                ) {
                    ccRet.ea[i] = (j shl 1).toByte()
                    break
                }
                if (f[edgeFacelet[i][0]].toInt() == edgeFacelet[j][1] / 9 &&
                    f[edgeFacelet[i][1]].toInt() == edgeFacelet[j][0] / 9
                ) {
                    ccRet.ea[i] = ((j shl 1) or 1).toByte()
                    break
                }
            }
        }
    }

    fun toFaceCube(cc: CubieCube): String {
        val ts = charArrayOf('U', 'R', 'F', 'D', 'L', 'B')
        val f = CharArray(54) { ts[it / 9] }
        for (c in 0 until 8) {
            val j = cc.ca[c].toInt() and 0x7
            val ori = cc.ca[c].toInt() shr 3
            for (n in 0 until 3) {
                f[cornerFacelet[c][(n + ori) % 3]] = ts[cornerFacelet[j][n] / 9]
            }
        }
        for (e in 0 until 12) {
            val j = cc.ea[e].toInt() shr 1
            val ori = cc.ea[e].toInt() and 1
            for (n in 0 until 2) {
                f[edgeFacelet[e][(n + ori) % 2]] = ts[edgeFacelet[j][n] / 9]
            }
        }
        return f.concatToString()
    }

    fun getNParity(idx0: Int, n: Int): Int {
        var idx = idx0
        var p = 0
        for (i in n - 2 downTo 0) {
            p = p xor (idx % (n - i))
            idx /= (n - i)
        }
        return p and 1
    }

    fun setVal(val0: Int, value: Int, isEdge: Boolean): Byte =
        (if (isEdge) (value shl 1) or (val0 and 1) else value or (val0 and 7.inv())).toByte()

    fun getVal(val0: Int, isEdge: Boolean): Int = if (isEdge) val0 shr 1 else val0 and 7

    /** 0xFEDCBA9876543210 as a signed Long, as in the Java original. */
    private const val NIBBLES = -0x123456789abcdf0L

    fun setNPerm(arr: ByteArray, idx0: Int, n: Int, isEdge: Boolean) {
        var idx = idx0
        var value = NIBBLES
        var extract = 0L
        for (p in 2..n) {
            extract = (extract shl 4) or (idx % p).toLong()
            idx /= p
        }
        for (i in 0 until n - 1) {
            val v = (extract.toInt() and 0xf) shl 2
            extract = extract shr 4
            arr[i] = setVal(arr[i].toInt(), ((value shr v) and 0xf).toInt(), isEdge)
            val m = (1L shl v) - 1
            value = (value and m) or ((value shr 4) and m.inv())
        }
        arr[n - 1] = setVal(arr[n - 1].toInt(), (value and 0xf).toInt(), isEdge)
    }

    fun getNPerm(arr: ByteArray, n: Int, isEdge: Boolean): Int {
        var idx = 0
        var value = NIBBLES
        for (i in 0 until n - 1) {
            val v = getVal(arr[i].toInt(), isEdge) shl 2
            idx = (n - i) * idx + ((value shr v) and 0xf).toInt()
            value -= 0x1111111111111110L shl v
        }
        return idx
    }

    fun getComb(arr: ByteArray, mask: Int, isEdge: Boolean): Int {
        val end = arr.size - 1
        var idxC = 0
        var r = 4
        for (i in end downTo 0) {
            val perm = getVal(arr[i].toInt(), isEdge)
            if ((perm and 0xc) == mask) {
                idxC += Cnk[i][r--]
            }
        }
        return idxC
    }

    fun setComb(arr: ByteArray, idxC0: Int, mask: Int, isEdge: Boolean) {
        var idxC = idxC0
        val end = arr.size - 1
        var r = 4
        var fill = end
        for (i in end downTo 0) {
            if (idxC >= Cnk[i][r]) {
                idxC -= Cnk[i][r--]
                arr[i] = setVal(arr[i].toInt(), r or mask, isEdge)
            } else {
                if ((fill and 0xc) == mask) {
                    fill -= 4
                }
                arr[i] = setVal(arr[i].toInt(), fill--, isEdge)
            }
        }
    }
}
