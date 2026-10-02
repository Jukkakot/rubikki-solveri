package fi.jukkakot.rubikkisolveri.cube

/**
 * How to hold the cube so that [face] looks at the user, in the order the manual input and the
 * scanner go through the faces. Starting from the holding position (white on top, green in front),
 * [hold] is the whole-cube rotation that brings the face to the front, with the side that is at
 * the top of the face in the standard net on top. The front face then shows the stickers of [face]
 * in net order: F1 = top left as seen.
 */
enum class FaceView(val face: Face, hold: String) {
    FRONT(Face.F, ""),
    RIGHT(Face.R, "y"),
    BACK(Face.B, "y2"),
    LEFT(Face.L, "y'"),
    TOP(Face.U, "x'"),
    BOTTOM(Face.D, "x"),
    ;

    val hold: List<Move> = Notation.parse(hold)

    /** The face that is on top while this face looks at the user. */
    val topFace: Face get() = when (this) {
        TOP -> Face.B
        BOTTOM -> Face.F
        else -> Face.U
    }

    fun centreColor(scheme: ColorScheme = ColorScheme.STANDARD): CubeColor = scheme[face]
    fun topColor(scheme: ColorScheme = ColorScheme.STANDARD): CubeColor = scheme[topFace]

    val next: FaceView? get() = entries.getOrNull(ordinal + 1)
    val previous: FaceView? get() = entries.getOrNull(ordinal - 1)

    companion object {
        fun of(face: Face): FaceView = entries.first { it.face == face }
    }
}
