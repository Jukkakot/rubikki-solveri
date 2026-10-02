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
import fi.jukkakot.rubikkisolveri.ui.guide.FollowPanel
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.scan.CameraPermissionGate
import fi.jukkakot.rubikkisolveri.ui.scan.CameraPreview
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
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
fun SolveScreen(
    cube: Cube,
    onBack: () -> Unit,
    onHome: () -> Unit,
    showNotation: Boolean = false,
    followPanel: @Composable (StepperState) -> Unit = { DefaultFollowPanel(it) },
    solver: suspend (Cube) -> SolveResult = { withContext(Dispatchers.Default) { TwoPhaseSolver.solve(it) } },
) {
    var follow by rememberSaveable { mutableStateOf(false) }
    val result by produceState<SolveResult?>(null, cube) {
        value = solver(cube).also { r ->
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
                actions = {
                    if ((result as? SolveResult.Solved)?.moves?.isNotEmpty() == true) {
                        IconToggleButton(checked = follow, onCheckedChange = { follow = it }) {
                            Icon(painterResource(R.drawable.ic_camera), contentDescription = stringResource(R.string.follow_camera))
                        }
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
                        Stepper(cube, r.moves, showNotation, onHome, follow, { follow = false }, followPanel)
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
private fun Stepper(
    start: Cube,
    moves: List<Move>,
    showNotation: Boolean,
    onHome: () -> Unit,
    follow: Boolean,
    onStopFollowing: () -> Unit,
    followPanel: @Composable (StepperState) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val state = rememberStepperState(start, moves, onDemoEnd = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) })
    val index = state.index

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.solve_hold), style = MaterialTheme.typography.bodyMedium)
        if (follow && !state.isFinished) {
            CameraPermissionGate(alternative = stringResource(R.string.follow_show_3d) to onStopFollowing) {
                followPanel(state)
            }
        } else {
            GuideCube(state)
        }
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

/** Camera mode with the real camera. */
@Composable
private fun DefaultFollowPanel(state: StepperState) {
    val frames = remember {
        MutableSharedFlow<List<Rgb>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    FollowPanel(state, frames) { modifier ->
        CameraPreview(torch = false, onSamples = { frames.tryEmit(it) }, onError = {}, modifier = modifier)
    }
}
