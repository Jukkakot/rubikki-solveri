package fi.jukkakot.rubikkisolveri.ui.guide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.ui.common.moveDescription
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors

/**
 * The 3D cube presenting the stepper's current move: the turning layer highlighted, the direction
 * arrow when the cube is still, and the view swung round so the turning side shows.
 */
@Composable
fun GuideCube(state: StepperState, modifier: Modifier = Modifier, description: String? = null) {
    val animator = state.animator
    val presented = state.current
    val viewState = remember { CubeViewState(CubeScene.guideView(presented)) }
    LaunchedEffect(presented?.layer?.face) { viewState.animateTo(CubeScene.guideView(presented)) }
    val turning = animator.move
    Cube3D(
        colors = animator.cube.toList().map(StickerColors::of),
        move = turning,
        progress = animator.progress,
        viewState = viewState,
        highlight = turning ?: presented,
        arrow = if (turning == null && animator.pending == 0 && animator.cube == state.cubeAt(state.index)) presented else null,
        description = description,
        modifier = modifier.fillMaxWidth().aspectRatio(1.1f),
    )
}

/** The current move in words, big, and optionally its notation, small. */
@Composable
fun MoveWordsText(state: StepperState, showNotation: Boolean) {
    val move = state.current ?: return
    Column {
        Text(
            moveDescription(move),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.heightIn(min = 72.dp),
        )
        if (showNotation) {
            Text(move.toString(), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
        }
    }
}
