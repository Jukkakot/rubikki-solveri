package fi.jukkakot.rubikkisolveri.cube

/** An immutable cube: the colour of each of the 54 stickers in URFDLB order (see [Stickers]). */
class Cube private constructor(private val colors: Array<CubeColor>) {

    operator fun get(index: Int): CubeColor = colors[index]

    fun colorAt(face: Face, n: Int): CubeColor = colors[Stickers.index(face, n)]

    fun centre(face: Face): CubeColor = colors[Stickers.centre(face)]

    fun toList(): List<CubeColor> = colors.toList()

    fun apply(move: Move): Cube {
        val permutation = move.permutation
        val next = arrayOfNulls<CubeColor>(Stickers.COUNT)
        for (i in 0 until Stickers.COUNT) next[permutation[i]] = colors[i]
        @Suppress("UNCHECKED_CAST")
        return Cube(next as Array<CubeColor>)
    }

    fun apply(moves: List<Move>): Cube = moves.fold(this) { cube, move -> cube.apply(move) }

    fun apply(notation: String): Cube = apply(Notation.parse(notation))

    /** A copy with the sticker at [index] painted [color]. */
    fun with(index: Int, color: CubeColor): Cube = Cube(colors.copyOf().also { it[index] = color })

    /** Solved means each face is one colour, whichever way the cube is held. */
    val isSolved: Boolean
        get() = Face.entries.all { face -> (1..9).all { colorAt(face, it) == centre(face) } }

    /** The 54 colour letters in sticker order, e.g. "WWWWWWWWWRRR…". */
    fun toColorString(): String = colors.joinToString("") { it.letter.toString() }

    /**
     * The 54 stickers as face letters (URFDLB), naming each colour by the face whose centre has
     * it. This is the input format of the two-phase solver. Requires six different centres.
     */
    fun toFaceletString(): String {
        val faceOf = Face.entries.associateBy { centre(it) }
        require(faceOf.size == 6) { "Centres are not six different colours" }
        return colors.joinToString("") { faceOf.getValue(it).name }
    }

    override fun equals(other: Any?): Boolean = other is Cube && colors.contentEquals(other.colors)

    override fun hashCode(): Int = colors.contentHashCode()

    override fun toString(): String = toColorString()

    companion object {
        fun solved(scheme: ColorScheme = ColorScheme.STANDARD): Cube =
            Cube(Array(Stickers.COUNT) { scheme[Stickers.all[it].face] })

        fun of(colors: List<CubeColor>): Cube {
            require(colors.size == Stickers.COUNT) { "A cube has ${Stickers.COUNT} stickers, got ${colors.size}" }
            return Cube(colors.toTypedArray())
        }

        /** From 54 colour letters (W Y G B R O) in sticker order; whitespace is ignored. */
        fun fromColorString(text: String): Cube = of(text.filterNot { it.isWhitespace() }.map(CubeColor::fromLetter))
    }
}
