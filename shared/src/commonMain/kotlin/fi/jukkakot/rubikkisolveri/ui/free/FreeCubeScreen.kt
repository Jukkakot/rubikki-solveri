package fi.jukkakot.rubikkisolveri.ui.free

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.ui.common.BackButton
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconToggle
import fi.jukkakot.rubikkisolveri.ui.common.faceName
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.painterResource
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Scramble
import fi.jukkakot.rubikkisolveri.ui.common.FitColumn
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator

/** The layer buttons, in two rows: up, down, right / left, front, back. */
private val turnButtons = listOf(
    Layer.U to FaceView.TOP, Layer.D to FaceView.BOTTOM, Layer.R to FaceView.RIGHT,
    Layer.L to FaceView.LEFT, Layer.F to FaceView.FRONT, Layer.B to FaceView.BACK,
)

/**
 * The free cube: turn any layer either way (a small cube picture per layer, its arrow following the
 * ↻/↺ toggle), scramble, undo, back to [start], and "solve" for the cube as it is. Dragging turns the
 * view; the hint saying so goes after the first drag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeCubeScreen(start: Cube, onBack: () -> Unit, onSolve: (Cube) -> Unit = {}) {
    val animator = rememberCubeAnimator(start)
    val viewState = remember { CubeViewState() }
    val history = remember { mutableStateListOf<Move>() }
    var counterClockwise by rememberSaveable { mutableStateOf(false) }
    var dragged by rememberSaveable { mutableStateOf(false) }
    // Any change of the view is a drag: only the user turns this cube's view.
    LaunchedEffect(viewState) {
        if (!dragged) snapshotFlow { viewState.rotation }.drop(1).first()
        dragged = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.free_title)) },
                navigationIcon = { Box(Modifier.padding(horizontal = 8.dp)) { BackButton(onBack) } },
            )
        },
    ) { padding ->
        FitColumn(
            Modifier.fillMaxSize().padding(padding),
        ) {
            Cube3D(
                colors = animator.cube.toList().map(StickerColors::of),
                move = animator.move,
                progress = animator.progress,
                viewState = viewState,
                description = stringResource(Res.string.free_cube_description),
                modifier = Modifier.fitSlot().aspectRatio(1f, matchHeightConstraintsFirst = true),
            )
            if (!dragged) Text(stringResource(Res.string.free_hint), style = MaterialTheme.typography.bodySmall)
            // Direction, scramble, undo and back to the start as icons.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                RoundIconToggle(checked = counterClockwise, onCheckedChange = { counterClockwise = it }) {
                    Icon(
                        painterResource(Res.drawable.ic_replay),
                        contentDescription = stringResource(Res.string.free_counter_clockwise),
                        // ↻ clockwise, mirrored ↺ counter-clockwise.
                        modifier = Modifier.scale(scaleX = if (counterClockwise) -1f else 1f, scaleY = 1f),
                    )
                }
                RoundIconButton(onClick = {
                    val scramble = Scramble.random(20)
                    history.addAll(scramble)
                    animator.play(scramble, speed = 2f)
                }) { Icon(painterResource(Res.drawable.ic_shuffle), contentDescription = stringResource(Res.string.free_scramble)) }
                RoundIconButton(
                    onClick = { history.removeLastOrNull()?.let { animator.play(it.inverse) } },
                    enabled = history.isNotEmpty(),
                ) { Icon(painterResource(Res.drawable.ic_undo), contentDescription = stringResource(Res.string.free_undo)) }
                RoundIconButton(onClick = {
                    history.clear()
                    animator.snapTo(start)
                }) { Icon(painterResource(Res.drawable.ic_to_start), contentDescription = stringResource(Res.string.free_reset)) }
            }
            for (row in turnButtons.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((layer, face) in row) {
                        val move = Move(layer, if (counterClockwise) 3 else 1)
                        val name = stringResource(faceName(face))
                        FilledTonalButton(
                            onClick = {
                                history.add(move)
                                animator.play(move)
                            },
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { contentDescription = name },
                        ) {
                            LayerPicture(move)
                        }
                    }
                }
            }
            Button(
                onClick = { onSolve(animator.target) },
                enabled = !animator.target.isSolved,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.free_solve)) }
        }
    }
}

/** A solved cube the size of an icon with [move]'s layer lit and its arrow; drawn once per direction. */
@Composable
private fun LayerPicture(move: Move) {
    val colors = remember { Cube.solved().toList().map(StickerColors::of) }
    Cube3D(
        colors = colors,
        highlight = move,
        arrow = move,
        draggable = false,
        compact = true,
        modifier = Modifier.size(48.dp),
    )
}
