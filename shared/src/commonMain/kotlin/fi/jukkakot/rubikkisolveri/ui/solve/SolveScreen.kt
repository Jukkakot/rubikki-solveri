@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package fi.jukkakot.rubikkisolveri.ui.solve

import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeScene
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
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
import fi.jukkakot.rubikkisolveri.cube.SolveTarget
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
import fi.jukkakot.rubikkisolveri.ui.guide.HandsfreeSpeed
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.KeepScreenOn
import fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState
import fi.jukkakot.rubikkisolveri.ui.scan.CameraPermissionGate
import fi.jukkakot.rubikkisolveri.ui.target.TargetPicture
import fi.jukkakot.rubikkisolveri.ui.target.targetName
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

/** Plans the way from [cube] to a target other than solved. */
typealias TargetPlanner = suspend (Cube, SolveTarget) -> SolvePlan

/**
 * Plans the way to [target] on the calling thread: the shortest moves to a whole target cube, or
 * the learn method cut after the target stage.
 */
fun planTarget(cube: Cube, target: SolveTarget): SolvePlan {
    val validity = CubeCheck.validity(cube)
    if (!validity.isValid) return SolvePlan.Invalid(validity)
    val whole = target.cubeFor(cube)
    if (whole == null) {
        val stage = (target as SolveTarget.StageDone).stage
        val steps = BeginnerSolver.solve(cube).steps.filter { it.stage <= stage }
        return SolvePlan.Ready(steps.flatMap { it.moves }, steps)
    }
    return when (val r = TwoPhaseSolver.solve(cube, whole)) {
        is SolveResult.Solved -> SolvePlan.Ready(r.moves, null)
        is SolveResult.Invalid -> SolvePlan.Invalid(r.reason)
        is SolveResult.Failed -> SolvePlan.Failed(r.message)
    }.also {
        AppLog.info(Evt.SOLVE_DONE, null, "target" to target.encode().take(40), "moves" to ((it as? SolvePlan.Ready)?.moves?.size ?: -1))
    }
}

val BACKGROUND_TARGET_PLANNER: TargetPlanner = { cube, target ->
    withFrameNanos { }
    withContext(Dispatchers.Default) { planTarget(cube, target) }
}

/**
 * Finds a solution for [cube] in the background (shortest or step by step), then steps through it.
 * With [startScreen] a start screen comes first (moves, target, method, hold, "Aloita" and
 * "Handsfree"); back in the guide returns to it. The guide's menu offers camera follow, the colour
 * check ([onCheckColors]) and the start screen.
 */
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
    target: SolveTarget = SolveTarget.Solved,
    onChangeTarget: (() -> Unit)? = null,
    targetPlanner: TargetPlanner = BACKGROUND_TARGET_PLANNER,
    handsfreeSpeed: HandsfreeSpeed = HandsfreeSpeed.NORMAL,
    onHandsfreeSpeed: (HandsfreeSpeed) -> Unit = {},
    startScreen: Boolean = false,
    onCheckColors: (() -> Unit)? = null,
) {
    var started by rememberSaveable { mutableStateOf(!startScreen) }
    var follow by rememberSaveable { mutableStateOf(false) }
    // Handsfree is started each time: not saved, and leaving the guide ends it.
    var handsfree by remember { mutableStateOf(false) }
    var speed by remember(handsfreeSpeed) { mutableStateOf(handsfreeSpeed) }
    var method by rememberSaveable { mutableStateOf(initialMethod) }
    val toSolved = target == SolveTarget.Solved
    val result by produceState<SolvePlan?>(null, cube, method, target) {
        value = null
        value = if (toSolved) planner(cube, method) else targetPlanner(cube, target)
    }
    // A target other than solved: patterns and painted cubes take the shortest way, stages the learn method.
    val shownMethod = when {
        toSolved -> method
        target is SolveTarget.StageDone -> SolveMethod.LEARN
        else -> SolveMethod.FAST
    }
    val ready = (result as? SolvePlan.Ready)?.takeIf { it.moves.isNotEmpty() }
    // Handsfree: only the shortest solution (also to a target).
    val canHandsfree = ready != null && ready.steps == null && !practice
    val toStart = {
        handsfree = false
        follow = false
        started = false
    }
    val back = if (started && startScreen) toStart else onBack
    BackHandler(enabled = started && startScreen) { toStart() }
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { (title ?: stringResource(Res.string.solve_title).takeIf { !started })?.let { Text(it) } },
                    navigationIcon = { Box(Modifier.padding(horizontal = 8.dp)) { BackButton(back) } },
                    actions = {
                        if (started && ready != null) {
                            GuideMenu(
                                follow = follow,
                                onFollow = { follow = it; handsfree = false },
                                onCheckColors = onCheckColors,
                                onToStart = toStart.takeIf { startScreen },
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (val r = result) {
                    null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.solve_working), modifier = Modifier.padding(16.dp))
                    }
                    is SolvePlan.Invalid -> Message(validityMessage(r.validity), stringResource(Res.string.solve_fix), onBack)
                    is SolvePlan.Failed -> Message(stringResource(Res.string.solve_failed), stringResource(Res.string.solve_fix), onBack)
                    is SolvePlan.Ready ->
                        if (r.moves.isEmpty()) {
                            val already = stringResource(if (toSolved) Res.string.solve_already else Res.string.target_already)
                            if (onChangeTarget != null) {
                                Message(already, stringResource(Res.string.target_pick_pattern), onChangeTarget, stringResource(Res.string.solve_home) to onHome)
                            } else {
                                Message(already, stringResource(Res.string.solve_home), onHome)
                            }
                        } else if (!started) {
                            StartPane(
                                cube = cube,
                                moves = r.moves.size,
                                target = target,
                                onChangeTarget = onChangeTarget,
                                method = method.takeIf { !cube.isSolved && !practice && toSolved },
                                onMethod = { method = it },
                                speed = speed.takeIf { canHandsfree },
                                onSpeed = { speed = it; onHandsfreeSpeed(it) },
                                onStart = { started = true },
                                onHandsfree = { started = true; handsfree = true },
                            )
                        } else {
                            val finished = finishedText ?: if (toSolved) null else stringResource(Res.string.target_reached)
                            key(shownMethod, target) {
                                Stepper(
                                    cube, r, showNotation, onHome, follow, { follow = false }, followPanel, finished, homeLabel,
                                    // Tap to confirm: only the shortest solution (also to a target).
                                    shortest = r.steps == null && !practice,
                                    handsfree = if (handsfree) speed else null,
                                    canHandsfree = canHandsfree,
                                    onHandsfree = { handsfree = true },
                                    onHandsfreeEnd = { handsfree = false },
                                ) { moves, millis ->
                                    onFinished(shownMethod, moves, millis)
                                }
                            }
                        }
                }
            }
        }
        if (handsfree && started) {
            KeepScreenOn()
            // Any touch anywhere stops handsfree and does nothing else.
            Box(
                Modifier.fillMaxSize().testTag(HANDSFREE_STOP_TAG).pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).consume()
                        handsfree = false
                    }
                },
            )
        }
    }
}

/** Test tag of the layer that stops handsfree on any touch. */
const val HANDSFREE_STOP_TAG = "handsfree_stop"

/** The guide's ⋮ menu: camera follow (or back to the 3D cube), the colour check, the start screen. */
@Composable
private fun GuideMenu(follow: Boolean, onFollow: (Boolean) -> Unit, onCheckColors: (() -> Unit)?, onToStart: (() -> Unit)?) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.padding(horizontal = 8.dp)) {
        RoundIconButton(onClick = { open = true }) {
            Icon(painterResource(Res.drawable.ic_more), contentDescription = stringResource(Res.string.solve_menu))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(if (follow) Res.string.follow_show_3d else Res.string.follow_camera)) },
                leadingIcon = { Icon(painterResource(if (follow) Res.drawable.ic_cube else Res.drawable.ic_camera), contentDescription = null) },
                onClick = { open = false; onFollow(!follow) },
            )
            if (onCheckColors != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.check_title)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_palette), contentDescription = null) },
                    onClick = { open = false; onCheckColors() },
                )
            }
            if (onToStart != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.solve_to_start)) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_undo), contentDescription = null) },
                    onClick = { open = false; onToStart() },
                )
            }
        }
    }
}

/**
 * The start screen: how many moves, the target (with a way to change it), the method ([method] null:
 * no choice), the hold as a small cube picture with one line, "Aloita" and, with a [speed],
 * "Handsfree" with its pace.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartPane(
    cube: Cube,
    moves: Int,
    target: SolveTarget,
    onChangeTarget: (() -> Unit)?,
    method: SolveMethod?,
    onMethod: (SolveMethod) -> Unit,
    speed: HandsfreeSpeed?,
    onSpeed: (HandsfreeSpeed) -> Unit,
    onStart: () -> Unit,
    onHandsfree: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(Res.string.start_moves, moves),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (onChangeTarget != null) TargetRow(cube, target, onChangeTarget, Modifier)
        if (method != null) MethodChoice(method, onMethod, Modifier)
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainer).padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Cube3D(
                colors = remember(cube) { cube.toList().map(StickerColors::of) },
                viewState = remember { CubeViewState(CubeScene.DEFAULT_VIEW) },
                draggable = false,
                modifier = Modifier.size(64.dp),
            )
            Text(
                stringResource(
                    Res.string.solve_hold_now,
                    stringResource(colorName(cube.centre(Face.F))),
                    stringResource(colorName(cube.centre(Face.U))),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        BigButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(Res.drawable.ic_play), contentDescription = null)
            Text(stringResource(Res.string.start_go))
        }
        if (speed != null) {
            FilledTonalButton(onClick = onHandsfree, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(painterResource(Res.drawable.ic_handsfree), contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(Res.string.handsfree), modifier = Modifier.padding(start = 8.dp))
            }
            val labels = mapOf(
                HandsfreeSpeed.SLOW to Res.string.handsfree_slow,
                HandsfreeSpeed.NORMAL to Res.string.handsfree_normal,
                HandsfreeSpeed.FAST to Res.string.handsfree_fast,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                HandsfreeSpeed.entries.forEachIndexed { i, s ->
                    SegmentedButton(
                        selected = s == speed,
                        onClick = { onSpeed(s) },
                        shape = SegmentedButtonDefaults.itemShape(i, HandsfreeSpeed.entries.size),
                    ) { Text(stringResource(labels.getValue(s))) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodChoice(method: SolveMethod, onChoose: (SolveMethod) -> Unit, modifier: Modifier) {
    val options = listOf(SolveMethod.FAST to Res.string.start_method_fast, SolveMethod.LEARN to Res.string.start_method_learn)
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
private fun Message(text: String, action: String, onAction: () -> Unit, secondary: Pair<String, () -> Unit>? = null) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium)
        Button(onClick = onAction) { Text(action) }
        secondary?.let { (label, onClick) -> OutlinedButton(onClick = onClick) { Text(label) } }
    }
}

/** Where the guide leads: a small picture and name of [target], and a button to change it. */
@Composable
private fun TargetRow(start: Cube, target: SolveTarget, onChange: () -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TargetPicture(target, start, Modifier.size(40.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.target_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(targetName(target), style = MaterialTheme.typography.titleSmall)
        }
        OutlinedButton(onClick = onChange, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.target_change)) }
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
    shortest: Boolean = false,
    handsfree: HandsfreeSpeed? = null,
    canHandsfree: Boolean = false,
    onHandsfree: () -> Unit = {},
    onHandsfreeEnd: () -> Unit = {},
    onFinished: (moves: Int, durationMillis: Long) -> Unit,
) {
    val moves = plan.moves
    val haptics = LocalHapticFeedback.current
    val running = handsfree != null && !follow
    val state = rememberStepperState(
        start, moves,
        onDemoTick = { haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
        handsfree = handsfree?.takeIf { !follow },
        onHandsfreeWarn = { haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick) },
        onHandsfreeAdvance = { haptics.performHapticFeedback(HapticFeedbackType.Confirm) },
    )
    val confirm = {
        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        state.done()
    }
    // The tap hint stays until the first move of the solve is confirmed.
    var confirmedOnce by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.index) { if (state.index > 0) confirmedOnce = true }
    val index = state.index
    val now = state.cubeAt(index)
    val startedAt = remember { elapsedMillis() }
    // Learn mode: when a stage begins, its goal card covers the guide until the user continues.
    var seenStages by rememberSaveable { mutableIntStateOf(0) }
    val stage = plan.steps?.let { stepAt(it, index).stage }
    val showGoal = stage != null && !state.isFinished && seenStages and (1 shl stage.ordinal) == 0
    var reported by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.isFinished) {
        if (state.isFinished) onHandsfreeEnd()
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
        // The hold in words only where it matters: the first move and a move after the hold changed.
        val before = if (index > 0) state.cubeAt(index - 1) else null
        if (before == null || before.centre(Face.F) != now.centre(Face.F) || before.centre(Face.U) != now.centre(Face.U)) hold()
        plan.steps?.let { steps -> StageCard(steps, index) }
        Box(Modifier.fitSlot(), contentAlignment = Alignment.Center) {
            if (follow && !state.isFinished) {
                CameraPermissionGate(alternative = stringResource(Res.string.follow_show_3d) to onStopFollowing) {
                    followPanel(state)
                }
            } else {
                val tap = shortest && !state.isFinished
                GuideCube(
                    state,
                    mirror = true,
                    onTap = confirm.takeIf { tap },
                    hint = stringResource(Res.string.solve_tap_hint).takeIf { tap && !running && !confirmedOnce },
                )
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
            // The move in words, and ↻ to show it again.
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { MoveWordsText(state, showNotation) }
                if (!running) {
                    RoundIconButton(onClick = state::demo, size = 44.dp) {
                        Icon(painterResource(Res.drawable.ic_replay), contentDescription = stringResource(Res.string.solve_show))
                    }
                }
            }
        } else {
            Text(finishedText ?: stringResource(Res.string.solve_finished), style = MaterialTheme.typography.headlineSmall)
        }
        if (running && !state.isFinished) {
            // Handsfree: the time of this move fills; a touch anywhere stops it.
            LinearProgressIndicator(
                progress = { state.handsfreeProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
        }
        if (state.isFinished) {
            BigButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(homeLabel ?: stringResource(Res.string.solve_home)) }
            return@FitColumn
        }
        // A media player: previous, play/pause (handsfree), next (done).
        val play = canHandsfree && !follow
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundIconButton(onClick = state::back, enabled = index > 0 && !running, size = 56.dp) {
                Icon(painterResource(Res.drawable.ic_previous), contentDescription = stringResource(Res.string.solve_previous))
            }
            if (play) {
                FilledIconButton(onClick = { if (!running) onHandsfree() }, modifier = Modifier.size(64.dp), shape = CircleShape) {
                    Icon(
                        painterResource(if (running) Res.drawable.ic_pause else Res.drawable.ic_play),
                        contentDescription = stringResource(if (running) Res.string.solve_pause else Res.string.handsfree),
                        modifier = Modifier.size(28.dp),
                    )
                }
                RoundIconButton(onClick = confirm, enabled = !running, size = 56.dp) {
                    Icon(painterResource(Res.drawable.ic_next), contentDescription = stringResource(Res.string.solve_done_move))
                }
            } else {
                FilledIconButton(onClick = confirm, modifier = Modifier.size(64.dp), shape = CircleShape) {
                    Icon(painterResource(Res.drawable.ic_next), contentDescription = stringResource(Res.string.solve_done_move), modifier = Modifier.size(28.dp))
                }
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
