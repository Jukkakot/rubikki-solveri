package fi.jukkakot.rubikkisolveri.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Validity

@StringRes
fun colorName(color: CubeColor): Int = when (color) {
    CubeColor.WHITE -> R.string.color_white
    CubeColor.YELLOW -> R.string.color_yellow
    CubeColor.GREEN -> R.string.color_green
    CubeColor.BLUE -> R.string.color_blue
    CubeColor.RED -> R.string.color_red
    CubeColor.ORANGE -> R.string.color_orange
}

@StringRes
fun faceName(view: FaceView): Int = when (view) {
    FaceView.FRONT -> R.string.face_front
    FaceView.RIGHT -> R.string.face_right
    FaceView.BACK -> R.string.face_back
    FaceView.LEFT -> R.string.face_left
    FaceView.TOP -> R.string.face_top
    FaceView.BOTTOM -> R.string.face_bottom
}

/** "Hold the cube like this: green centre towards you, white on top." */
@Composable
fun holdHint(view: FaceView): String = stringResource(
    R.string.manual_hold_hint,
    stringResource(colorName(view.centreColor())),
    stringResource(colorName(view.topColor())),
)

/** One plain sentence for what is wrong with a cube (or that it is fine). */
@Composable
fun validityMessage(validity: Validity): String = when (validity) {
    Validity.Valid -> stringResource(R.string.valid_cube)
    is Validity.WrongColorCount -> {
        val resources = LocalResources.current
        stringResource(
            R.string.invalid_counts,
            validity.counts.entries.joinToString(", ") { (color, n) -> "${resources.getString(colorName(color))} $n" },
        )
    }
    is Validity.BadCentres -> stringResource(R.string.invalid_centres)
    is Validity.ImpossiblePiece -> stringResource(R.string.invalid_piece)
    is Validity.DuplicatePiece -> stringResource(R.string.invalid_duplicate)
    Validity.TwistedCorner -> stringResource(R.string.invalid_twist)
    Validity.FlippedEdge -> stringResource(R.string.invalid_flip)
    Validity.SwappedPieces -> stringResource(R.string.invalid_swap)
}

/** The stickers a validity problem is about, for marking. */
fun markedStickers(validity: Validity?): Set<Int> = validity?.markedStickers ?: emptySet()
