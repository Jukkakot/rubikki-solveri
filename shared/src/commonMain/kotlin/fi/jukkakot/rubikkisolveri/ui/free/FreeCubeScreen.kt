package fi.jukkakot.rubikkisolveri.ui.free

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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

private val turnButtons = listOf(
    Layer.U to Res.string.turn_top, Layer.D to Res.string.turn_bottom, Layer.R to Res.string.turn_right,
    Layer.L to Res.string.turn_left, Layer.F to Res.string.turn_front, Layer.B to Res.string.turn_back,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeCubeScreen(start: Cube, onBack: () -> Unit, onSolve: (Cube) -> Unit = {}) {
    val animator = rememberCubeAnimator(start)
    val viewState = remember { CubeViewState() }
    val history = remember { mutableStateListOf<Move>() }
    var counterClockwise by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.free_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
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
            Text(stringResource(Res.string.free_hint), style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.free_counter_clockwise), modifier = Modifier.weight(1f))
                Switch(checked = counterClockwise, onCheckedChange = { counterClockwise = it })
            }
            for (pair in turnButtons.chunked(3)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((layer, label) in pair) {
                        val move = Move(layer, if (counterClockwise) 3 else 1)
                        FilledTonalButton(
                            onClick = {
                                history.add(move)
                                animator.play(move)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("${stringResource(label)} $move")
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val scramble = Scramble.random(20)
                        history.addAll(scramble)
                        animator.play(scramble, speed = 2f)
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.free_scramble)) }
                OutlinedButton(
                    onClick = { history.removeLastOrNull()?.let { animator.play(it.inverse) } },
                    enabled = history.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.free_undo)) }
                OutlinedButton(
                    onClick = {
                        history.clear()
                        animator.snapTo(Cube.solved())
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.free_reset)) }
            }
            Button(
                onClick = { onSolve(animator.target) },
                enabled = !animator.target.isSolved,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.free_solve)) }
        }
    }
}
