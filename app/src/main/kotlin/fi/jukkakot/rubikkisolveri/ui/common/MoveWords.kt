package fi.jukkakot.rubikkisolveri.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move

/** Describes a move in words: "Turn the right side clockwise (as seen from the right)." */
object MoveWords {
    /** [after] is the cube after the move; whole-cube turns are described by its centres. */
    fun describe(move: Move, text: (Int, Array<out Any>) -> String, after: Cube? = null): String {
        if (move.isRotation && after != null) {
            return text(
                R.string.move_whole_cube,
                arrayOf(text(colorName(after.centre(Face.F)), emptyArray()), text(colorName(after.centre(Face.U)), emptyArray())),
            )
        }
        val (side, from) = when (move.layer) {
            Layer.U -> R.string.side_u to R.string.from_u
            Layer.D -> R.string.side_d to R.string.from_d
            Layer.R -> R.string.side_r to R.string.from_r
            Layer.L -> R.string.side_l to R.string.from_l
            Layer.F -> R.string.side_f to R.string.from_f
            Layer.B -> R.string.side_b to R.string.from_b
            else -> return move.toString()
        }
        val sideText = text(side, emptyArray())
        return when (move.quarterTurns) {
            1 -> text(R.string.move_cw, arrayOf(sideText, text(from, emptyArray())))
            3 -> text(R.string.move_ccw, arrayOf(sideText, text(from, emptyArray())))
            else -> text(R.string.move_half, arrayOf(sideText))
        }
    }
}

@Composable
fun moveDescription(move: Move, after: Cube? = null): String {
    val resources = LocalResources.current
    return MoveWords.describe(move, { id, args -> resources.getString(id, *args) }, after)
}
