package fi.jukkakot.rubikkisolveri.cube

/** A move text could not be read: [token] at 1-based [position] in the sequence. */
class NotationException(val token: String, val position: Int) :
    IllegalArgumentException("Unknown move '$token' at position $position")

/** Standard cube notation: "R U2 R' Uw x' M2". Accepts r/Rw, ' or ’ for prime, 2' as 2. */
object Notation {
    private val byName: Map<String, Layer> = buildMap {
        for (layer in Layer.entries) put(layer.notation, layer)
        for (layer in listOf(Layer.UW, Layer.DW, Layer.RW, Layer.LW, Layer.FW, Layer.BW)) {
            put(layer.face!!.name + "w", layer)
        }
    }

    fun parse(text: String): List<Move> {
        val tokens = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return tokens.mapIndexed { i, token -> parseMove(token) ?: throw NotationException(token, i + 1) }
    }

    fun parseMove(token: String): Move? {
        val clean = token.replace('’', '\'')
        val match = Regex("^([A-Za-z]w?)(2?)('?)$").matchEntire(clean) ?: return null
        val (name, two, prime) = match.destructured
        val layer = byName[name] ?: return null
        val turns = when {
            two.isNotEmpty() -> 2
            prime.isNotEmpty() -> 3
            else -> 1
        }
        return Move(layer, turns)
    }

    fun format(moves: List<Move>): String = moves.joinToString(" ")
}

object Sequences {
    /** The sequence that undoes [moves]. */
    fun inverse(moves: List<Move>): List<Move> = moves.asReversed().map { it.inverse }

    /** Merges consecutive turns of the same layer and drops turns that cancel out. */
    fun simplify(moves: List<Move>): List<Move> {
        val out = ArrayList<Move>()
        for (move in moves) {
            val last = out.lastOrNull()
            if (last != null && last.layer == move.layer) {
                out.removeAt(out.lastIndex)
                val turns = (last.quarterTurns + move.quarterTurns) % 4
                if (turns != 0) out.add(Move(move.layer, turns))
            } else {
                out.add(move)
            }
        }
        return out
    }
}
