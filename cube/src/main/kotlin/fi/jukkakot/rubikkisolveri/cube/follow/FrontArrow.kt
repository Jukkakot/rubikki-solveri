package fi.jukkakot.rubikkisolveri.cube.follow

import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move

/**
 * How a move looks on the front face seen by the camera, in grid coordinates (0..1, x right,
 * y down): a straight arrow from ([x0], [y0]) to ([x1], [y1]), or a round arrow about the centre.
 * [double] marks a half turn.
 */
data class FrontArrow(
    val kind: Kind,
    val x0: Float = 0f,
    val y0: Float = 0f,
    val x1: Float = 0f,
    val y1: Float = 0f,
    val double: Boolean = false,
) {
    enum class Kind { STRAIGHT, CLOCKWISE, COUNTER_CLOCKWISE }

    companion object {
        private const val NEAR = 0.1f
        private const val FAR = 0.9f
        private val ROW = floatArrayOf(1f / 6, 0.5f, 5f / 6)

        /** The arrow for [move] on the front face, or null when the front does not show it. */
        fun of(move: Move): FrontArrow? {
            val double = move.quarterTurns == 2
            // +1: the move's clockwise direction; a half turn is drawn like the clockwise turn.
            val forward = move.quarterTurns != 3
            fun row(r: Int, leftwards: Boolean) =
                if (leftwards) FrontArrow(Kind.STRAIGHT, FAR, ROW[r], NEAR, ROW[r], double)
                else FrontArrow(Kind.STRAIGHT, NEAR, ROW[r], FAR, ROW[r], double)
            fun column(c: Int, upwards: Boolean) =
                if (upwards) FrontArrow(Kind.STRAIGHT, ROW[c], FAR, ROW[c], NEAR, double)
                else FrontArrow(Kind.STRAIGHT, ROW[c], NEAR, ROW[c], FAR, double)
            return when (move.layer) {
                Layer.U, Layer.UW -> row(0, leftwards = forward)
                Layer.D, Layer.DW -> row(2, leftwards = !forward)
                Layer.E -> row(1, leftwards = !forward)
                Layer.R, Layer.RW -> column(2, upwards = forward)
                Layer.L, Layer.LW -> column(0, upwards = !forward)
                Layer.M -> column(1, upwards = !forward)
                Layer.F, Layer.FW -> FrontArrow(if (forward) Kind.CLOCKWISE else Kind.COUNTER_CLOCKWISE, double = double)
                else -> null
            }
        }
    }
}
