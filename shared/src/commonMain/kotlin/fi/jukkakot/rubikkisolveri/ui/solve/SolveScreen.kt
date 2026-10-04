package fi.jukkakot.rubikkisolveri.ui.solve

import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Validity
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.Step
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.ui.common.BackButton
import fi.jukkakot.rubikkisolveri.ui.common.BigButton
import fi.jukkakot.rubikkisolveri.ui.common.FitColumn
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconToggle
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.common.noteText
import fi.jukkakot.rubikkisolveri.ui.common.stageName
import fi.jukkakot.rubikkisolveri.ui.common.validityMessage
import fi.jukkakot.rubikkisolveri.ui.guide.FollowPanel
import fi.jukkakot.rubikkisolveri.ui.lessons.StageGoalCube
import fi.jukkakot.rubikkisolveri.ui.lessons.StageGoalPicture
import fi.jukkakot.rubikkisolveri.ui.guide.GuideCube
import fi.jukkakot.rubikkisolveri.ui.guide.MoveWordsText
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import fi.jukkakot.rubikkisolveri.ui.scan.CameraPermissionGate
import fi.jukkakot.rubikkisolveri.ui.scan.CameraPreview
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
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
    val start = elapsedMillis()
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
                "ms" to elapsedMillis() - start,
            )
        }
    }
}

// One frame first, so "computing" is on screen before the browser (one thread) starts the search.
val BACKGROUND_PLANNER: Planner = { cube, method ->
    withFrameNanos { }
    withContext(Dispatchers.Default) { plan(cube, method) }
}

/** Finds a solution for [cube] in the background (shortest or step by step), then steps through it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveScreen(
    cube: Cube,
    onBack: () -> Unit,
    onHome: () -> Unit,
    showNotation: Boolean = false,
    followPanel: @Composable (state: StepperState) -> Unit = { DefaultFollowPanel(it) },
    planner: Planner = BACKGROUND_PLANNER,
    initialMethod: SolveMethod = SolveMethod.FAST,
    title: String? = null,
    practice: Boolean = false,
    finishedText: String? = null,
    homeLabel: String? = null,
    onFinished: (method: SolveMethod, moves: Int, durationMillis: Long) -> Unit = { _, _, _ -> },
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
                title = { Text(title ?: stringResource(Res.string.solve_title)) },
                navigationIcon = { Box(Modifier.padding(horizontal = 8.dp)) { BackButton(onBack) } },
                actions = {
                    if ((result as? SolvePlan.Ready)?.moves?.isNotEmpty() == true) {
                        RoundIconToggle(checked = follow, onCheckedChange = { follow = it }, modifier = Modifier.padding(horizontal = 8.dp)) {
                            Icon(painterResource(Res.drawable.ic_camera), contentDescription = stringResource(Res.string.follow_camera))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!cube.isSolved && !practice) {
                MethodChoice(method, onChoose = { method = it }, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp))
            }
            Box(Modifier.fillMaxSize()) {
                when (val r = result) {
                    null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.solve_working), modifier = Modifier.padding(16.dp))
                    }
                    is SolvePlan.Invalid -> Message(validityMessage(r.validity), stringResource(Res.string.solve_fix), onBack)
                    is SolvePlan.Failed -> Message(stringResource(Res.string.solve_failed), stringResource(Res.string.solve_fix), onBack)
                    is SolvePlan.Ready ->
                        if (r.moves.isEmpty()) {
                            Message(stringResource(Res.string.solve_already), stringResource(Res.string.solve_home), onHome)
                        } else {
                            key(method) {
                                Stepper(cube, r, showNotation, onHome, follow, { follow = false }, followPanel, finishedText, homeLabel) { moves, millis ->
                                    onFinished(method, moves, millis)
                                }
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
    val options = listOf(SolveMethod.FAST to Res.string.method_fast, SolveMethod.LEARN to Res.string.method_learn)
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
    followPanel: @Composable (state: StepperState) -> Unit,
    finishedText: String?,
    homeLabel: String?,
    onFinished: (moves: Int, durationMillis: Long) -> Unit,
) {
    val moves = plan.moves
    val haptics = LocalHapticFeedback.current
    val state = rememberStepperState(start, moves, onDemoTick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) })
    val index = state.index
    val now = state.cubeAt(index)
    val startedAt = remember { elapsedMillis() }
    // Learn mode: when a stage begins, its goal card covers the guide until the user continues.
    var seenStages by rememberSaveable { mutableIntStateOf(0) }
    val stage = plan.steps?.let { stepAt(it, index).stage }
    val showGoal = stage != null && !state.isFinished && seenStages and (1 shl stage.ordinal) == 0
    var reported by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isFinished) {
        if (state.isFinished && !reported) {
            reported = true
            onFinished(moves.size, elapsedMillis() - startedAt)
        }
    }

    val hold: @Composable () -> Unit = {
        Text(
            stringResource(
                Res.string.solve_hold_now,
                stringResource(colorName(now.centre(Face.F))),
                stringResource(colorName(now.centre(Face.U))),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (stage != null && showGoal) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            hold()
            GoalCard(stage) { seenStages = seenStages or (1 shl stage.ordinal) }
        }
        return
    }
    FitColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        keepSize = true,
    ) {
        hold()
        plan.steps?.let { steps -> StageCard(steps, index) }
        Box(Modifier.fitSlot(), contentAlignment = Alignment.Center) {
            if (follow && !state.isFinished) {
                CameraPermissionGate(alternative = stringResource(Res.string.follow_show_3d) to onStopFollowing) {
                    followPanel(state)
                }
            } else {
                GuideCube(state, mirror = true)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(
                progress = { index / moves.size.toFloat() },
                modifier = Modifier.weight(1f).height(8.dp),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            if (!state.isFinished) {
                Text(stringResource(Res.string.solve_step, index + 1, moves.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!state.isFinished) {
            MoveWordsText(state, showNotation)
        } else {
            Text(finishedText ?: stringResource(Res.string.solve_finished), style = MaterialTheme.typography.headlineSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(onClick = state::back, enabled = index > 0, size = 56.dp) {
                Icon(painterResource(Res.drawable.ic_undo), contentDescription = stringResource(Res.string.solve_previous))
            }
            if (!state.isFinished) {
                OutlinedButton(onClick = state::demo, modifier = Modifier.heightIn(min = 56.dp)) { Text(stringResource(Res.string.solve_show)) }
                BigButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        state.done()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.solve_done_move)) }
            } else {
                BigButton(onClick = onHome, modifier = Modifier.weight(1f)) { Text(homeLabel ?: stringResource(Res.string.solve_home)) }
            }
        }
    }
}

private fun stepAt(steps: List<Step>, index: Int): Step {
    var at = 0
    return steps.firstOrNull { s -> (index < at + s.moves.size).also { at += s.moves.size } } ?: steps.last()
}

/** "Next: stage N": the stage's goal picture, shown when the stage begins. */
@Composable
private fun GoalCard(stage: Stage, onContinue: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(Res.string.goal_next), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(Res.string.stage_header, stage.ordinal + 1, stringResource(stageName(stage))),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            StageGoalPicture(stage, Modifier.fillMaxWidth(0.8f))
            BigButton(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.goal_continue)) }
        }
    }
}

/** The beginner stage (with its goal picture, tap to enlarge) and the step the current move belongs to. */
@Composable
private fun StageCard(steps: List<Step>, index: Int) {
    val step = stepAt(steps, index)
    var goalOpen by remember { mutableStateOf(false) }
    val name = stringResource(stageName(step.stage))
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(Res.string.stage_header, step.stage.ordinal + 1, name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                StageGoalCube(step.stage, Modifier.size(48.dp).clickable { goalOpen = true })
            }
            Text(
                noteText(step.note) { id, args -> stringResource(id, *args) },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
    if (goalOpen) {
        AlertDialog(
            onDismissRequest = { goalOpen = false },
            confirmButton = { TextButton(onClick = { goalOpen = false }) { Text(stringResource(Res.string.goal_close)) } },
            title = { Text(stringResource(Res.string.goal_description, name)) },
            text = { StageGoalPicture(step.stage, Modifier.fillMaxWidth()) },
        )
    }
}

/** Camera mode with the real camera. */
@Composable
@OptIn(ExperimentalAtomicApi::class)
private fun DefaultFollowPanel(state: StepperState) {
    val frames = remember {
        MutableSharedFlow<Pair<List<Rgb>, Boolean>>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }
    // Whether the latest grid picture shows a cube face; the picture comes just before its readings.
    val face = remember { AtomicBoolean(false) }
    FollowPanel(state, frames) { modifier ->
        CameraPreview(
            torch = false,
            onSamples = { frames.tryEmit(it to face.load()) },
            onPicture = { face.store(FrameSampler.looksLikeCube(it)) },
            onError = {},
            modifier = modifier,
        )
    }
}
