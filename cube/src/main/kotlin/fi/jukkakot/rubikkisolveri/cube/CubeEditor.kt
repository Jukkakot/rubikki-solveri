package fi.jukkakot.rubikkisolveri.cube

/**
 * A cube being entered by hand or by the scanner: each sticker may still be unknown (null). The
 * centres are fixed to the holding position's colours.
 */
data class CubeEditor(val colors: List<CubeColor?>, val scheme: ColorScheme = ColorScheme.STANDARD) {
    init {
        require(colors.size == Stickers.COUNT)
    }

    operator fun get(index: Int): CubeColor? = colors[index]

    fun isCentre(index: Int): Boolean = index % 9 == 4

    /** Paints one sticker; centres keep their colour. */
    fun paint(index: Int, color: CubeColor?): CubeEditor =
        if (isCentre(index)) this else copy(colors = colors.toMutableList().also { it[index] = color })

    /** Replaces the nine stickers of [face] (net order); the centre stays fixed. */
    fun withFace(face: Face, nine: List<CubeColor?>): CubeEditor {
        require(nine.size == 9)
        val next = colors.toMutableList()
        for (n in 0 until 9) if (n != 4) next[face.ordinal * 9 + n] = nine[n]
        return copy(colors = next)
    }

    fun counts(): Map<CubeColor, Int> = CubeColor.entries.associateWith { color -> colors.count { it == color } }

    val isComplete: Boolean get() = colors.all { it != null }

    fun isFaceComplete(face: Face): Boolean = (0 until 9).all { colors[face.ordinal * 9 + it] != null }

    fun toCube(): Cube? = if (isComplete) Cube.of(colors.map { it!! }) else null

    fun clear(): CubeEditor = empty(scheme)

    /** One letter per sticker (W Y G B R O, '.' for unknown), for saving across screen rotation. */
    fun encode(): String = colors.joinToString("") { it?.letter?.toString() ?: "." }

    companion object {
        fun empty(scheme: ColorScheme = ColorScheme.STANDARD): CubeEditor =
            CubeEditor(List(Stickers.COUNT) { i -> if (i % 9 == 4) scheme[Face.entries[i / 9]] else null }, scheme)

        fun of(cube: Cube): CubeEditor = CubeEditor(cube.toList())

        fun decode(text: String): CubeEditor? {
            if (text.length != Stickers.COUNT) return null
            return runCatching { CubeEditor(text.map { if (it == '.') null else CubeColor.fromLetter(it) }) }.getOrNull()
        }
    }
}
