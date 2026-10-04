package fi.jukkakot.rubikkisolveri.ui.common

import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Validity

fun colorName(color: CubeColor): StringResource = when (color) {
    CubeColor.WHITE -> Res.string.color_white
    CubeColor.YELLOW -> Res.string.color_yellow
    CubeColor.GREEN -> Res.string.color_green
    CubeColor.BLUE -> Res.string.color_blue
    CubeColor.RED -> Res.string.color_red
    CubeColor.ORANGE -> Res.string.color_orange
}

fun faceName(view: FaceView): StringResource = when (view) {
    FaceView.FRONT -> Res.string.face_front
    FaceView.RIGHT -> Res.string.face_right
    FaceView.BACK -> Res.string.face_back
    FaceView.LEFT -> Res.string.face_left
    FaceView.TOP -> Res.string.face_top
    FaceView.BOTTOM -> Res.string.face_bottom
}

/** "Hold the cube like this: green centre towards you, white on top." */
@Composable
fun holdHint(view: FaceView): String = stringResource(
    Res.string.manual_hold_hint,
    stringResource(colorName(view.centreColor())),
    stringResource(colorName(view.topColor())),
)

/** One plain sentence for what is wrong with a cube (or that it is fine). */
@Composable
fun validityMessage(validity: Validity): String = when (validity) {
    Validity.Valid -> stringResource(Res.string.valid_cube)
    is Validity.WrongColorCount -> stringResource(
        Res.string.invalid_counts,
        validity.counts.entries.map { (color, n) -> "${stringResource(colorName(color))} $n" }.joinToString(", "),
    )
    is Validity.BadCentres -> stringResource(Res.string.invalid_centres)
    is Validity.ImpossiblePiece -> stringResource(Res.string.invalid_piece)
    is Validity.DuplicatePiece -> stringResource(Res.string.invalid_duplicate)
    Validity.TwistedCorner -> stringResource(Res.string.invalid_twist)
    Validity.FlippedEdge -> stringResource(Res.string.invalid_flip)
    Validity.SwappedPieces -> stringResource(Res.string.invalid_swap)
}

/** The stickers a validity problem is about, for marking. */
fun markedStickers(validity: Validity?): Set<Int> = validity?.markedStickers ?: emptySet()
