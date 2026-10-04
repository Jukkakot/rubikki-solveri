package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.Face.B
import fi.jukkakot.rubikkisolveri.cube.Face.D
import fi.jukkakot.rubikkisolveri.cube.Face.F
import fi.jukkakot.rubikkisolveri.cube.Face.L
import fi.jukkakot.rubikkisolveri.cube.Face.R
import fi.jukkakot.rubikkisolveri.cube.Face.U

/** The eight corner positions/pieces, named by their faces (first is the U or D face). */
enum class Corner(vararg faces: Face) {
    URF(U, R, F), UFL(U, F, L), ULB(U, L, B), UBR(U, B, R),
    DFR(D, F, R), DLF(D, L, F), DBL(D, B, L), DRB(D, R, B),
    ;

    val faces: List<Face> = faces.toList()

    /** Sticker indices of this position, in the same order as [faces] (clockwise around the corner). */
    val stickers: List<Int> = faces.map { face -> stickerOf(face, faces.toList()) }
}

/** The twelve edge positions/pieces, named by their faces. */
enum class Edge(vararg faces: Face) {
    UR(U, R), UF(U, F), UL(U, L), UB(U, B),
    DR(D, R), DF(D, F), DL(D, L), DB(D, B),
    FR(F, R), FL(F, L), BL(B, L), BR(B, R),
    ;

    val faces: List<Face> = faces.toList()
    val stickers: List<Int> = faces.map { face -> stickerOf(face, faces.toList()) }
}

/** The sticker on [face] of the little cube that touches all [faces]. */
private fun stickerOf(face: Face, faces: List<Face>): Int {
    val position = Vec3(
        faces.sumOf { it.normal.x },
        faces.sumOf { it.normal.y },
        faces.sumOf { it.normal.z },
    )
    return Stickers.indexAt(position, face.normal)
}

/**
 * The cube as pieces: which corner/edge sits at each position and how it is turned.
 * Orientation follows the two-phase convention: a corner's twist is how many steps clockwise its
 * U/D sticker is from the position's U/D side; an edge is flipped (1) when its first face's sticker
 * is not on the position's first face.
 */
data class Pieces(
    val cornerPermutation: List<Corner>,
    val cornerTwist: List<Int>,
    val edgePermutation: List<Edge>,
    val edgeFlip: List<Int>,
) {
    val isSolved: Boolean
        get() = cornerPermutation == Corner.entries && edgePermutation == Edge.entries &&
            cornerTwist.all { it == 0 } && edgeFlip.all { it == 0 }
}
