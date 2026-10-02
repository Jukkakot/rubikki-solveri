package fi.jukkakot.rubikkisolveri.ui.solve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.ui.common.moveDescription
import fi.jukkakot.rubikkisolveri.ui.common.validityMessage
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Finds a solution for [cube] in the background, then steps through it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveScreen(cube: Cube, onBack: () -> Unit, onHome: () -> Unit) {
    val result by produceState<SolveResult?>(null, cube) {
        value = withContext(Dispatchers.Default) { TwoPhaseSolver.solve(cube) }.also { r ->
            when (r) {
                is SolveResult.Solved -> AppLog.info(Evt.SOLVE_DONE, null, "moves" to r.moves.size, "ms" to r.millis)
                is SolveResult.Invalid -> AppLog.info(Evt.SOLVE_FAILED, null, "reason" to r.reason.toString())
                is SolveResult.Failed -> AppLog.info(Evt.SOLVE_FAILED, r.message)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.solve_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val r = result) {
                null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.solve_working), modifier = Modifier.padding(16.dp))
                }
                is SolveResult.Invalid -> Message(validityMessage(r.reason), stringResource(R.string.solve_fix), onBack)
                is SolveResult.Failed -> Message(stringResource(R.string.solve_failed), stringResource(R.string.solve_fix), onBack)
                is SolveResult.Solved ->
                    if (r.moves.isEmpty()) {
                        Message(stringResource(R.string.solve_already), stringResource(R.string.solve_home), onHome)
                    } else {
                        Stepper(cube, r.moves, onHome)
                    }
            }
        }
    }
}

@Composable
private fun Message(text: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAction) { Text(action) }
    }
}

@Composable
private fun Stepper(start: Cube, moves: List<Move>, onHome: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    fun cubeAt(i: Int) = start.apply(moves.take(i))
    val animator = rememberCubeAnimator(cubeAt(index))
    val viewState = remember { CubeViewState() }
    val scope = rememberCoroutineScope()

    // A "show" demo may still be running or waiting: start from the real state of this step.
    fun settle() {
        if (animator.target != cubeAt(index) || animator.pending > 0) animator.snapTo(cubeAt(index))
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.solve_hold), style = MaterialTheme.typography.bodyMedium)
        Cube3D(
            colors = animator.cube.toList().map(StickerColors::of),
            move = animator.move,
            progress = animator.progress,
            viewState = viewState,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.1f),
        )
        LinearProgressIndicator(progress = { index / moves.size.toFloat() }, modifier = Modifier.fillMaxWidth())
        if (index < moves.size) {
            val move = moves[index]
            Text(stringResource(R.string.solve_step, index + 1, moves.size), style = MaterialTheme.typography.labelLarge)
            Text(
                moveDescription(move),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.heightIn(min = 96.dp),
            )
        } else {
            Text(stringResource(R.string.solve_finished), style = MaterialTheme.typography.headlineSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    settle()
                    index--
                    animator.play(moves[index].inverse)
                },
                enabled = index > 0,
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.solve_previous)) }
            if (index < moves.size) {
                OutlinedButton(
                    onClick = {
                        settle()
                        val shownAt = index
                        animator.play(moves[index])
                        scope.launch {
                            snapshotFlow { animator.pending }.first { it == 0 }
                            delay(SHOW_PAUSE_MS)
                            if (index == shownAt && animator.pending == 0) animator.snapTo(cubeAt(index))
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.solve_show)) }
                Button(
                    onClick = {
                        settle()
                        animator.play(moves[index])
                        index++
                    },
                    modifier = Modifier.weight(1.3f).heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.solve_done_move)) }
            } else {
                Button(onClick = onHome, modifier = Modifier.weight(1.3f)) { Text(stringResource(R.string.solve_home)) }
            }
        }
    }
}

private const val SHOW_PAUSE_MS = 700L
