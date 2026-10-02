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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Validity
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.Step
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.common.noteText
import fi.jukkakot.rubikkisolveri.ui.common.stageIntro
import fi.jukkakot.rubikkisolveri.ui.common.stageName
import fi.jukkakot.rubikkisolveri.ui.common.validityMessage
import fi.jukkakot.rubikkisolveri.ui.guide.FollowPanel
import fi.jukkakot.rubikkisolveri.ui.guide.GuideCube
import fi.jukkakot.rubikkisolveri.ui.guide.MoveWordsText
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import fi.jukkakot.rubikkisolveri.ui.scan.CameraPermissionGate
import fi.jukkakot.rubikkisolveri.ui.scan.CameraPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext

enum class SolveMethod { FAST, LEARN }

/** What the solution screen steps through. */
sealed interface SolvePlan {
    /** [steps] is set for the step-by-step method. */
    data class Ready(val moves: List<Move>, val steps: List<Step>?) : SolvePlan
    data class Invalid(val validity: Validity) : SolvePlan
    data class Failed(val message: String) : SolvePlan
}

typealias Planner = suspend (Cube, SolveMethod) -> SolvePlan

/** Plans a solution on the calling thread, logging the result. */
fun plan(cube: Cube, method: SolveMethod): SolvePlan {
    val start = System.nanoTime()
    val validity = CubeCheck.validity(cube)
    if (!validity.isValid) {
        AppLog.info(Evt.SOLVE_FAILED, null, "reason" to validity.toString())
        return SolvePlan.Invalid(validity)
    }
    return when (method) {
        SolveMethod.FAST -> when (val r = TwoPhaseSolver.solve(cube)) {
            is SolveResult.Solved -> SolvePlan.Ready(r.moves, null)
            is SolveResult.Invalid -> SolvePlan.Invalid(r.reason)
            is SolveResult.Failed -> SolvePlan.Failed(r.message)
        }
        SolveMethod.LEARN -> BeginnerSolver.solve(cube).let { SolvePlan.Ready(it.moves, it.steps) }
    }.also {
        if (it is SolvePlan.Ready) {
            AppLog.info(
                Evt.SOLVE_DONE, null, "method" to method.name, "moves" to it.moves.size,
                "ms" to (System.nanoTime() - start) / 1_000_000,
            )
        }
    }
}

val BACKGROUND_PLANNER: Planner = { cube, method -> withContext(Dispatchers.Default) { plan(cube, method) } }

/** Finds a solution for [cube] in the background (shortest or step by step), then steps through it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveScreen(
    cube: Cube,
    onBack: () -> Unit,
    onHome: () -> Unit,
    showNotation: Boolean = false,
    followPanel: @Composable (StepperState) -> Unit = { DefaultFollowPanel(it) },
    planner: Planner = BACKGROUND_PLANNER,
    initialMethod: SolveMethod = SolveMethod.FAST,
    title: String? = null,
    practice: Boolean = false,
    finishedText: String? = null,
    homeLabel: String? = null,
) {
    var follow by rememberSaveable { mutableStateOf(false) }
    var method by rememberSaveable { mutableStateOf(initialMethod) }
    val result by produceState<SolvePlan?>(null, cube, method) {
        value = null
        value = planner(cube, method)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title ?: stringResource(R.string.solve_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if ((result as? SolvePlan.Ready)?.moves?.isNotEmpty() == true) {
                        IconToggleButton(checked = follow, onCheckedChange = { follow = it }) {
                            Icon(painterResource(R.drawable.ic_camera), contentDescription = stringResource(R.string.follow_camera))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!cube.isSolved && !practice) {
                MethodChoice(method, onChoose = { method = it }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            Box(Modifier.fillMaxSize()) {
                when (val r = result) {
                    null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.solve_working), modifier = Modifier.padding(16.dp))
                    }
                    is SolvePlan.Invalid -> Message(validityMessage(r.validity), stringResource(R.string.solve_fix), onBack)
                    is SolvePlan.Failed -> Message(stringResource(R.string.solve_failed), stringResource(R.string.solve_fix), onBack)
                    is SolvePlan.Ready ->
                        if (r.moves.isEmpty()) {
                            Message(stringResource(R.string.solve_already), stringResource(R.string.solve_home), onHome)
                        } else {
                            key(method) {
                                Stepper(cube, r, showNotation, onHome, follow, { follow = false }, followPanel, finishedText, homeLabel)
                            }
                        }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodChoice(method: SolveMethod, onChoose: (SolveMethod) -> Unit, modifier: Modifier) {
    val options = listOf(SolveMethod.FAST to R.string.method_fast, SolveMethod.LEARN to R.string.method_learn)
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = method == value,
                onClick = { onChoose(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(stringResource(label)) }
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
    plan: SolvePlan.Ready,
    showNotation: Boolean,
    onHome: () -> Unit,
    follow: Boolean,
    onStopFollowing: () -> Unit,
    followPanel: @Composable (StepperState) -> Unit,
    finishedText: String?,
    homeLabel: String?,
) {
    val moves = plan.moves
    val haptics = LocalHapticFeedback.current
    val state = rememberStepperState(start, moves, onDemoEnd = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) })
    val index = state.index
    val now = state.cubeAt(index)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(
                R.string.solve_hold_now,
                stringResource(colorName(now.centre(Face.F))),
                stringResource(colorName(now.centre(Face.U))),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        plan.steps?.let { steps -> StageCard(steps, index) }
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
            Text(finishedText ?: stringResource(R.string.solve_finished), style = MaterialTheme.typography.headlineSmall)
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
                Button(onClick = onHome, modifier = Modifier.weight(1.3f)) { Text(homeLabel ?: stringResource(R.string.solve_home)) }
            }
        }
    }
}

/** The beginner stage and step the current move belongs to. */
@Composable
private fun StageCard(steps: List<Step>, index: Int) {
    var at = 0
    val step = steps.firstOrNull { s -> (index < at + s.moves.size).also { at += s.moves.size } } ?: steps.last()
    val resources = LocalResources.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.stage_header, step.stage.ordinal + 1, stringResource(stageName(step.stage))),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            // The stage's idea is shown on its first step; later steps keep the screen short.
            if (steps.first { it.stage == step.stage } === step) {
                Text(stringResource(stageIntro(step.stage)), style = MaterialTheme.typography.bodySmall)
            }
            Text(
                noteText(step.note) { id, args -> resources.getString(id, *args) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
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
