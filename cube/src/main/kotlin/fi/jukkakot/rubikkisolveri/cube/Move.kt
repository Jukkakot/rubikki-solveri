package fi.jukkakot.rubikkisolveri.cube

/**
 * What a move turns: an [axis] (a quarter turn is clockwise seen from its tip) and the [layers]
 * whose coordinate along the axis is turned (1 = the outer layer on the axis side, 0 = the middle).
 */
enum class Layer(val notation: String, val axis: Vec3, val layers: Set<Int>, val kind: Kind) {
    U("U", Vec3(0, 1, 0), setOf(1), Kind.FACE),
    D("D", Vec3(0, -1, 0), setOf(1), Kind.FACE),
    R("R", Vec3(1, 0, 0), setOf(1), Kind.FACE),
    L("L", Vec3(-1, 0, 0), setOf(1), Kind.FACE),
    F("F", Vec3(0, 0, 1), setOf(1), Kind.FACE),
    B("B", Vec3(0, 0, -1), setOf(1), Kind.FACE),
    M("M", Vec3(-1, 0, 0), setOf(0), Kind.SLICE),
    E("E", Vec3(0, -1, 0), setOf(0), Kind.SLICE),
    S("S", Vec3(0, 0, 1), setOf(0), Kind.SLICE),
    UW("u", Vec3(0, 1, 0), setOf(0, 1), Kind.WIDE),
    DW("d", Vec3(0, -1, 0), setOf(0, 1), Kind.WIDE),
    RW("r", Vec3(1, 0, 0), setOf(0, 1), Kind.WIDE),
    LW("l", Vec3(-1, 0, 0), setOf(0, 1), Kind.WIDE),
    FW("f", Vec3(0, 0, 1), setOf(0, 1), Kind.WIDE),
    BW("b", Vec3(0, 0, -1), setOf(0, 1), Kind.WIDE),
    X("x", Vec3(1, 0, 0), setOf(-1, 0, 1), Kind.ROTATION),
    Y("y", Vec3(0, 1, 0), setOf(-1, 0, 1), Kind.ROTATION),
    Z("z", Vec3(0, 0, 1), setOf(-1, 0, 1), Kind.ROTATION),
    ;

    enum class Kind { FACE, SLICE, WIDE, ROTATION }

    /** The face whose turn this is, for face and wide turns. */
    val face: Face? get() = when (kind) {
        Kind.FACE, Kind.WIDE -> Face.entries.first { it.normal == axis }
        else -> null
    }

    /** Axis without direction (x, y or z), used to keep scrambles varied. */
    val axisIndex: Int get() = when {
        axis.x != 0 -> 0
        axis.y != 0 -> 1
        else -> 2
    }

    fun turns(position: Vec3): Boolean = (position dot axis) in layers
}

/**
 * A single move: [layer] turned [quarterTurns] times clockwise (1 = clockwise, 2 = half turn,
 * 3 = counter-clockwise, written with ').
 */
data class Move(val layer: Layer, val quarterTurns: Int) {
    init {
        require(quarterTurns in 1..3) { "quarterTurns must be 1..3, was $quarterTurns" }
    }

    val inverse: Move get() = Move(layer, 4 - quarterTurns)

    /** Where each sticker goes: the sticker at index i moves to permutation[i]. */
    val permutation: IntArray get() = PERMUTATIONS.getValue(this)

    val isRotation: Boolean get() = layer.kind == Layer.Kind.ROTATION

    override fun toString(): String = layer.notation + when (quarterTurns) {
        1 -> ""
        2 -> "2"
        else -> "'"
    }

    companion object {
        val ALL: List<Move> = Layer.entries.flatMap { layer -> (1..3).map { Move(layer, it) } }

        private val PERMUTATIONS: Map<Move, IntArray> = ALL.associateWith { computePermutation(it) }

        private fun computePermutation(move: Move): IntArray = IntArray(Stickers.COUNT) { i ->
            val sticker = Stickers.all[i]
            if (!move.layer.turns(sticker.position)) {
                i
            } else {
                var position = sticker.position
                var normal = sticker.normal
                repeat(move.quarterTurns) {
                    position = position.quarterTurn(move.layer.axis)
                    normal = normal.quarterTurn(move.layer.axis)
                }
                Stickers.indexAt(position, normal)
            }
        }
    }
}
