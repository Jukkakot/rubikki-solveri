package fi.jukkakot.rubikkisolveri.cube

import kotlin.random.Random

object Scramble {
    private val faceLayers = listOf(Layer.U, Layer.D, Layer.R, Layer.L, Layer.F, Layer.B)

    /**
     * A random face-turn sequence of [length] moves: never the same face twice in a row and never
     * three moves in a row on one axis (so nothing cancels or collapses).
     */
    fun random(length: Int = 25, random: Random = Random.Default): List<Move> {
        val moves = ArrayList<Move>(length)
        while (moves.size < length) {
            val layer = faceLayers[random.nextInt(faceLayers.size)]
            val last = moves.lastOrNull()?.layer
            val beforeLast = moves.getOrNull(moves.size - 2)?.layer
            if (layer == last) continue
            if (last != null && beforeLast != null && layer.axisIndex == last.axisIndex && last.axisIndex == beforeLast.axisIndex) continue
            moves.add(Move(layer, 1 + random.nextInt(3)))
        }
        return moves
    }
}
