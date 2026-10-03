package fi.jukkakot.rubikkisolveri.ui.common

import androidx.annotation.StringRes
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StepNote

/** The colour's form after which "centre" follows ("vihreän keskiön" / "green centre"). */
@StringRes
fun colorGenitive(color: CubeColor): Int = when (color) {
    CubeColor.WHITE -> R.string.color_gen_white
    CubeColor.YELLOW -> R.string.color_gen_yellow
    CubeColor.GREEN -> R.string.color_gen_green
    CubeColor.BLUE -> R.string.color_gen_blue
    CubeColor.RED -> R.string.color_gen_red
    CubeColor.ORANGE -> R.string.color_gen_orange
}

@StringRes
fun stageName(stage: Stage): Int = when (stage) {
    Stage.WHITE_CROSS -> R.string.stage_1
    Stage.WHITE_CORNERS -> R.string.stage_2
    Stage.MIDDLE_LAYER -> R.string.stage_3
    Stage.YELLOW_CROSS -> R.string.stage_4
    Stage.YELLOW_EDGES -> R.string.stage_5
    Stage.YELLOW_CORNERS_PLACED -> R.string.stage_6
    Stage.YELLOW_CORNERS_TURNED -> R.string.stage_7
}

/** The step's explanation as a sentence; [text] resolves a string resource with arguments. */
fun noteText(note: StepNote, text: (Int, Array<out Any>) -> String): String {
    fun name(c: CubeColor) = text(colorName(c), emptyArray())
    fun gen(c: CubeColor) = text(colorGenitive(c), emptyArray())
    return when (note) {
        is StepNote.CrossEdge -> text(R.string.note_cross_edge, arrayOf(name(note.color), gen(note.color)))
        is StepNote.WhiteCorner -> text(R.string.note_white_corner, arrayOf(name(note.a), name(note.b), gen(note.a), gen(note.b)))
        StepNote.TurnOver -> text(R.string.note_turn_over, emptyArray())
        is StepNote.MiddleEdge -> text(R.string.note_middle_edge, arrayOf(name(note.a), name(note.b), gen(note.a), gen(note.b)))
        is StepNote.YellowCross -> text(R.string.note_yellow_cross, emptyArray())
        StepNote.PlaceEdges -> text(R.string.note_place_edges, emptyArray())
        StepNote.PlaceCorners -> text(R.string.note_place_corners, emptyArray())
        StepNote.YellowCorner -> text(R.string.note_yellow_corner, emptyArray())
        StepNote.FinalTurn -> text(R.string.note_final_turn, emptyArray())
    }
}
