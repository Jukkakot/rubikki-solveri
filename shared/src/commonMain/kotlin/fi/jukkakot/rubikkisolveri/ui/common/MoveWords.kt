package fi.jukkakot.rubikkisolveri.ui.common

import androidx.compose.runtime.Composable
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Describes a move in words: "Turn the right side clockwise (as seen from the right)." */
object MoveWords {
    /**
     * The sentence for [move] and the words it is filled with, or null for a move without words.
     * [after] is the cube after the move; whole-cube turns are described by its centres.
     */
    fun parts(move: Move, after: Cube? = null): Pair<StringResource, List<StringResource>>? {
        if (move.isRotation && after != null) {
            return Res.string.move_whole_cube to listOf(colorName(after.centre(Face.F)), colorName(after.centre(Face.U)))
        }
        val (side, from) = when (move.layer) {
            Layer.U -> Res.string.side_u to Res.string.from_u
            Layer.D -> Res.string.side_d to Res.string.from_d
            Layer.R -> Res.string.side_r to Res.string.from_r
            Layer.L -> Res.string.side_l to Res.string.from_l
            Layer.F -> Res.string.side_f to Res.string.from_f
            Layer.B -> Res.string.side_b to Res.string.from_b
            else -> return null
        }
        return when (move.quarterTurns) {
            1 -> Res.string.move_cw to listOf(side, from)
            3 -> Res.string.move_ccw to listOf(side, from)
            else -> Res.string.move_half to listOf(side)
        }
    }

    /**
     * [move] as the user sees it in the holding view (white on top, green in front, the view never
     * turning): top and bottom layers left or right, right and left sides up or down, the back by
     * which way its top row moves, the front clockwise or counter-clockwise. Null for a move without
     * words (whole-cube turns and slices do not occur in the fast method).
     */
    fun steadyParts(move: Move): Pair<StringResource, List<StringResource>>? {
        if (move.isRotation) return null
        val turns = move.quarterTurns
        if (turns == 2) {
            val side = when (move.layer) {
                Layer.U -> Res.string.layer_u
                Layer.D -> Res.string.layer_d
                Layer.R -> Res.string.side_r
                Layer.L -> Res.string.side_l
                Layer.F -> Res.string.side_f
                Layer.B -> Res.string.side_b
                else -> return null
            }
            return Res.string.move_half to listOf(side)
        }
        val cw = turns == 1
        val sentence = when (move.layer) {
            Layer.U -> if (cw) Res.string.move_top_left else Res.string.move_top_right
            Layer.D -> if (cw) Res.string.move_bottom_right else Res.string.move_bottom_left
            Layer.R -> if (cw) Res.string.move_right_up else Res.string.move_right_down
            Layer.L -> if (cw) Res.string.move_left_down else Res.string.move_left_up
            Layer.F -> if (cw) Res.string.move_front_cw else Res.string.move_front_ccw
            Layer.B -> if (cw) Res.string.move_back_left else Res.string.move_back_right
            else -> return null
        }
        return sentence to emptyList()
    }

    /**
     * [parts] (or [steadyParts] when [steady]) as a sentence; [text] resolves a string with
     * arguments (inline, so a composable can pass `stringResource`).
     */
    inline fun describe(move: Move, text: (StringResource, Array<out Any>) -> String, after: Cube? = null, steady: Boolean = false): String {
        val (sentence, words) = (if (steady) steadyParts(move) else parts(move, after)) ?: return move.toString()
        return text(sentence, words.map { text(it, emptyArray()) }.toTypedArray())
    }
}

@Composable
fun moveDescription(move: Move, after: Cube? = null, steady: Boolean = false): String =
    MoveWords.describe(move, { id, args -> stringResource(id, *args) }, after, steady)
