package fi.jukkakot.rubikkisolveri.cube

/** A known cube pattern, made from the solved cube by [moves] (notation). */
enum class CubePattern(val moves: String) {
    CHECKERBOARD("R2 L2 U2 D2 F2 B2"),
    SIX_SPOTS("U D' R L' F B' U D'"),
    CUBE_IN_CUBE("F L F U' R U F2 L2 U' L' B D' B' L2 U"),
    CUBE_IN_CUBE_IN_CUBE("U' L' U' F' R2 B' R F U B2 U B' L U' F U R F'"),
    SUPERFLIP("U R2 F B R B2 R U2 L B2 R U' D' R2 F R' L B2 U2 F2"),
    ANACONDA("L U B' U' R L' B R' F B' D R D' F'"),
    PYTHON("F2 R' B' U R' L F' L F' B D' R B L2"),
    TETRIS("L R F B U' D' L' R'"),
    TWISTER("F R' U L F' L' F U' R U L' U' L F'"),
    CROSS("U F B' L2 U2 L2 F' B U2 L2 U"),
    VERTICAL_STRIPES("F U F R L2 B D' R D2 L D' B R2 L F U F"),
    ;

    /** The pattern on a cube whose centres are those of [like] (held the same way). */
    fun cube(like: Cube = Cube.solved()): Cube = solvedLike(like).apply(moves)

    companion object {
        /** The solved cube with [like]'s centres. */
        fun solvedLike(like: Cube): Cube = Cube.of(Stickers.all.map { like.centre(it.face) })
    }
}
