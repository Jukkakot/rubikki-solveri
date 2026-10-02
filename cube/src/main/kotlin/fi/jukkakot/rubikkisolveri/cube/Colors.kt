package fi.jukkakot.rubikkisolveri.cube

/** The six sticker colours; [letter] is used in compact text forms (e.g. tests, logs). */
enum class CubeColor(val letter: Char) {
    WHITE('W'),
    YELLOW('Y'),
    GREEN('G'),
    BLUE('B'),
    RED('R'),
    ORANGE('O'),
    ;

    companion object {
        fun fromLetter(letter: Char): CubeColor =
            entries.firstOrNull { it.letter == letter.uppercaseChar() }
                ?: throw IllegalArgumentException("Unknown colour letter '$letter'")
    }
}

/**
 * The six faces in the common URFDLB order. [normal] points out of the face; [right] and [down]
 * span the face as it is drawn in the standard net (U seen from above with B at the top, D seen
 * from below with F at the top, the side faces seen from outside with U at the top).
 */
enum class Face(val normal: Vec3, val right: Vec3, val down: Vec3) {
    U(Vec3(0, 1, 0), Vec3(1, 0, 0), Vec3(0, 0, 1)),
    R(Vec3(1, 0, 0), Vec3(0, 0, -1), Vec3(0, -1, 0)),
    F(Vec3(0, 0, 1), Vec3(1, 0, 0), Vec3(0, -1, 0)),
    D(Vec3(0, -1, 0), Vec3(1, 0, 0), Vec3(0, 0, -1)),
    L(Vec3(-1, 0, 0), Vec3(0, 0, 1), Vec3(0, -1, 0)),
    B(Vec3(0, 0, -1), Vec3(-1, 0, 0), Vec3(0, -1, 0)),
    ;

    val opposite: Face get() = entries.first { it.normal == normal * -1 }
}

/** Which colour belongs on which face of the solved cube in the app's holding position. */
class ColorScheme(private val colors: Map<Face, CubeColor>) {
    init {
        require(colors.keys == Face.entries.toSet() && colors.values.toSet().size == 6) {
            "A scheme needs six different colours, one per face"
        }
    }

    operator fun get(face: Face): CubeColor = colors.getValue(face)

    fun faceOf(color: CubeColor): Face = colors.entries.first { it.value == color }.key

    companion object {
        /** Standard (Western) colours, held with white on top and green facing you. */
        val STANDARD = ColorScheme(
            mapOf(
                Face.U to CubeColor.WHITE,
                Face.R to CubeColor.RED,
                Face.F to CubeColor.GREEN,
                Face.D to CubeColor.YELLOW,
                Face.L to CubeColor.ORANGE,
                Face.B to CubeColor.BLUE,
            ),
        )
    }
}
