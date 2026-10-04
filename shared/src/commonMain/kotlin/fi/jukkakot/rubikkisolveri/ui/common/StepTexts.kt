package fi.jukkakot.rubikkisolveri.ui.common

import org.jetbrains.compose.resources.StringResource
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StepNote

/** The colour's form after which "centre" follows ("vihreän keskiön" / "green centre"). */
fun colorGenitive(color: CubeColor): StringResource = when (color) {
    CubeColor.WHITE -> Res.string.color_gen_white
    CubeColor.YELLOW -> Res.string.color_gen_yellow
    CubeColor.GREEN -> Res.string.color_gen_green
    CubeColor.BLUE -> Res.string.color_gen_blue
    CubeColor.RED -> Res.string.color_gen_red
    CubeColor.ORANGE -> Res.string.color_gen_orange
}

fun stageName(stage: Stage): StringResource = when (stage) {
    Stage.WHITE_CROSS -> Res.string.stage_1
    Stage.WHITE_CORNERS -> Res.string.stage_2
    Stage.MIDDLE_LAYER -> Res.string.stage_3
    Stage.YELLOW_CROSS -> Res.string.stage_4
    Stage.YELLOW_EDGES -> Res.string.stage_5
    Stage.YELLOW_CORNERS_PLACED -> Res.string.stage_6
    Stage.YELLOW_CORNERS_TURNED -> Res.string.stage_7
}

/** A step's sentence and the colour words it is filled with, in order. */
fun noteParts(note: StepNote): Pair<StringResource, List<StringResource>> = when (note) {
    is StepNote.CrossEdge -> Res.string.note_cross_edge to listOf(colorName(note.color), colorGenitive(note.color))
    is StepNote.WhiteCorner ->
        Res.string.note_white_corner to listOf(colorName(note.a), colorName(note.b), colorGenitive(note.a), colorGenitive(note.b))
    StepNote.TurnOver -> Res.string.note_turn_over to emptyList()
    is StepNote.MiddleEdge ->
        Res.string.note_middle_edge to listOf(colorName(note.a), colorName(note.b), colorGenitive(note.a), colorGenitive(note.b))
    is StepNote.YellowCross -> Res.string.note_yellow_cross to emptyList()
    StepNote.PlaceEdges -> Res.string.note_place_edges to emptyList()
    StepNote.PlaceCorners -> Res.string.note_place_corners to emptyList()
    StepNote.YellowCorner -> Res.string.note_yellow_corner to emptyList()
    StepNote.FinalTurn -> Res.string.note_final_turn to emptyList()
}

/**
 * The step's explanation as a sentence; [text] resolves a string resource with arguments (inline,
 * so a composable can pass `stringResource`).
 */
inline fun noteText(note: StepNote, text: (StringResource, Array<out Any>) -> String): String {
    val (sentence, words) = noteParts(note)
    return text(sentence, words.map { text(it, emptyArray()) }.toTypedArray())
}
