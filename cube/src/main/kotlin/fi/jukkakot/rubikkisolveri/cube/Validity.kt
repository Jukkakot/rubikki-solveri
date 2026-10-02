package fi.jukkakot.rubikkisolveri.cube

/** Whether a colouring is a solvable cube, and if not, the first reason found. */
sealed interface Validity {
    data object Valid : Validity

    /** Colours not used exactly nine times, with their counts. */
    data class WrongColorCount(val counts: Map<CubeColor, Int>) : Validity

    /** The centres are not six different colours, or are not a real arrangement of the scheme. */
    data class BadCentres(val stickers: List<Int>) : Validity

    /** These stickers form a corner or edge that does not exist on a real cube. */
    data class ImpossiblePiece(val stickers: List<Int>) : Validity

    /** The same piece appears at two places (both places' stickers). */
    data class DuplicatePiece(val stickers: List<Int>) : Validity

    /** One corner is twisted in place (the colours are right but turned). */
    data object TwistedCorner : Validity

    /** One edge is flipped in place. */
    data object FlippedEdge : Validity

    /** Two pieces are swapped: the cube was taken apart and reassembled wrongly. */
    data object SwappedPieces : Validity

    val isValid: Boolean get() = this == Valid
}

object CubeCheck {

    fun validity(cube: Cube, scheme: ColorScheme = ColorScheme.STANDARD): Validity {
        val counts = CubeColor.entries.associateWith { color -> (0 until Stickers.COUNT).count { cube[it] == color } }
        val wrong = counts.filterValues { it != 9 }
        if (wrong.isNotEmpty()) return Validity.WrongColorCount(wrong)

        val centreStickers = Face.entries.map { Stickers.centre(it) }
        val centres = Face.entries.map { cube.centre(it) }
        if (centres !in centreArrangements(scheme)) return Validity.BadCentres(centreStickers)

        val faceOf: Map<CubeColor, Face> = Face.entries.associateBy { cube.centre(it) }
        val faces = (0 until Stickers.COUNT).map { faceOf.getValue(cube[it]) }

        val cornerPieces = ArrayList<Corner>()
        val cornerTwist = ArrayList<Int>()
        for (position in Corner.entries) {
            val seen = position.stickers.map { faces[it] }
            val (piece, twist) = readCorner(seen) ?: return Validity.ImpossiblePiece(position.stickers)
            cornerPieces += piece
            cornerTwist += twist
        }
        val edgePieces = ArrayList<Edge>()
        val edgeFlip = ArrayList<Int>()
        for (position in Edge.entries) {
            val seen = position.stickers.map { faces[it] }
            val (piece, flip) = readEdge(seen) ?: return Validity.ImpossiblePiece(position.stickers)
            edgePieces += piece
            edgeFlip += flip
        }

        duplicate(cornerPieces) { Corner.entries[it].stickers }?.let { return Validity.DuplicatePiece(it) }
        duplicate(edgePieces) { Edge.entries[it].stickers }?.let { return Validity.DuplicatePiece(it) }

        if (cornerTwist.sum() % 3 != 0) return Validity.TwistedCorner
        if (edgeFlip.sum() % 2 != 0) return Validity.FlippedEdge
        if (parity(cornerPieces.map { it.ordinal }) != parity(edgePieces.map { it.ordinal })) return Validity.SwappedPieces
        return Validity.Valid
    }

    /** The piece view of a cube with real pieces; null when the cube has no real piece view. */
    fun pieces(cube: Cube): Pieces? {
        val faceOf: Map<CubeColor, Face> = Face.entries.associateBy { cube.centre(it) }
        if (faceOf.size != 6) return null
        val faces = (0 until Stickers.COUNT).map { faceOf[cube[it]] ?: return null }
        val corners = Corner.entries.map { readCorner(it.stickers.map { s -> faces[s] }) ?: return null }
        val edges = Edge.entries.map { readEdge(it.stickers.map { s -> faces[s] }) ?: return null }
        return Pieces(corners.map { it.first }, corners.map { it.second }, edges.map { it.first }, edges.map { it.second })
    }

    private fun readCorner(seen: List<Face>): Pair<Corner, Int>? {
        val twist = seen.indexOfFirst { it == Face.U || it == Face.D }
        if (twist < 0) return null
        val first = seen[twist]
        val second = seen[(twist + 1) % 3]
        val third = seen[(twist + 2) % 3]
        val piece = Corner.entries.firstOrNull { it.faces == listOf(first, second, third) } ?: return null
        return piece to twist
    }

    private fun readEdge(seen: List<Face>): Pair<Edge, Int>? {
        for (piece in Edge.entries) {
            if (piece.faces == seen) return piece to 0
            if (piece.faces == seen.reversed()) return piece to 1
        }
        return null
    }

    /** Stickers of the two places holding the same piece, or null when every piece is unique. */
    private fun <P : Enum<P>> duplicate(pieces: List<P>, stickersAt: (Int) -> List<Int>): List<Int>? {
        val firstAt = HashMap<P, Int>()
        for ((place, piece) in pieces.withIndex()) {
            val earlier = firstAt.put(piece, place)
            if (earlier != null) return stickersAt(earlier) + stickersAt(place)
        }
        return null
    }

    /** 0 for an even permutation, 1 for odd. */
    fun parity(permutation: List<Int>): Int {
        var swaps = 0
        val p = permutation.toMutableList()
        for (i in p.indices) {
            while (p[i] != i) {
                val j = p[i]
                p[i] = p[j]
                p[j] = j
                swaps++
            }
        }
        return swaps % 2
    }

    /** The centre colours (URFDLB) of the solved cube held in each of its 24 orientations. */
    fun centreArrangements(scheme: ColorScheme): Set<List<CubeColor>> {
        val seen = HashSet<List<CubeColor>>()
        val queue = ArrayDeque(listOf(Cube.solved(scheme)))
        val turns = listOf(Move(Layer.X, 1), Move(Layer.Y, 1))
        while (queue.isNotEmpty()) {
            val cube = queue.removeFirst()
            val centres = Face.entries.map { cube.centre(it) }
            if (seen.add(centres)) turns.forEach { queue.add(cube.apply(it)) }
        }
        return seen
    }
}
