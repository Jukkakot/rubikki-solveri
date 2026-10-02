package fi.jukkakot.rubikkisolveri.ui.solve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.ui.guide.GuideCube
import fi.jukkakot.rubikkisolveri.ui.guide.MoveWordsText
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import fi.jukkakot.rubikkisolveri.ui.common.validityMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Finds a solution for [cube] in the background, then steps through it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveScreen(cube: Cube, onBack: () -> Unit, onHome: () -> Unit, showNotation: Boolean = false) {
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
                        Stepper(cube, r.moves, showNotation, onHome)
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
private fun Stepper(start: Cube, moves: List<Move>, showNotation: Boolean, onHome: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val state = rememberStepperState(start, moves, onDemoEnd = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) })
    val index = state.index

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.solve_hold), style = MaterialTheme.typography.bodyMedium)
        GuideCube(state)
        LinearProgressIndicator(progress = { index / moves.size.toFloat() }, modifier = Modifier.fillMaxWidth())
        if (!state.isFinished) {
            Text(stringResource(R.string.solve_step, index + 1, moves.size), style = MaterialTheme.typography.labelLarge)
            MoveWordsText(state, showNotation)
        } else {
            Text(stringResource(R.string.solve_finished), style = MaterialTheme.typography.headlineSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = state::back, enabled = index > 0, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.solve_previous))
            }
            if (!state.isFinished) {
                OutlinedButton(onClick = state::demo, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.solve_show)) }
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        state.done()
                    },
                    modifier = Modifier.weight(1.3f).heightIn(min = 56.dp),
                ) { Text(stringResource(R.string.solve_done_move)) }
            } else {
                Button(onClick = onHome, modifier = Modifier.weight(1.3f)) { Text(stringResource(R.string.solve_home)) }
            }
        }
    }
}
