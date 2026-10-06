package fi.jukkakot.rubikkisolveri.cube.solve.min2phase

/** Solving one cube to another: the cube whose solution takes [from] to [to] (facelet strings). */
internal object Relative {
    private fun cubie(facelets: String): CubieCube {
        val f = ByteArray(54) { "URFDLB".indexOf(facelets[it]).toByte() }
        return CubieCube().also { Util.toCubieCube(f, it) }
    }

    /** X = to⁻¹ · from: a solution of X, made on [from], ends in [to]. */
    fun between(from: String, to: String): String {
        val s = cubie(from)
        val t = cubie(to)
        t.invCubieCube()
        val x = CubieCube()
        CubieCube.CornMult(t, s, x)
        CubieCube.EdgeMult(t, s, x)
        return Util.toFaceCube(x)
    }
}
