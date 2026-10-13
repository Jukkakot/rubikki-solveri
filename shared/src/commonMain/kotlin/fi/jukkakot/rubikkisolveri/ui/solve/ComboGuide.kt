package fi.jukkakot.rubikkisolveri.ui.solve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.Step
import fi.jukkakot.rubikkisolveri.cube.beginner.StepPart
import fi.jukkakot.rubikkisolveri.cube.beginner.StepPieces
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.common.BigButton
import fi.jukkakot.rubikkisolveri.ui.common.MoveWords
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.common.colorName
import fi.jukkakot.rubikkisolveri.ui.common.noteText
import fi.jukkakot.rubikkisolveri.ui.common.stageName
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator
import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import fi.jukkakot.rubikkisolveri.ui.guide.GuideCube
import fi.jukkakot.rubikkisolveri.ui.guide.MoveWordsText
import fi.jukkakot.rubikkisolveri.ui.guide.StepperState
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonCatalog
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Test tag of a combination part on the combinations card. */
const val COMBO_PART_TAG = "combo_part"

/**
 * The combinations way: the beginner solve one step at a time, each step a card telling which piece
 * goes where (outlined on the cube) and its parts in order: how to hold the cube, the setup turns,
 * the combination with how many times, free turns. "Näytä" plays a part from where it begins (a
 * combination turns layers only: hold changes are their own part), the arrows step move by move and
 * "Tein sen" goes to the next step.
 */
@Composable
internal fun ComboGuide(
    start: Cube,
    steps: List<Step>,
    showNotation: Boolean,
    onHome: () -> Unit,
    onLesson: ((Stage, List<Move>) -> Unit)?,
    finishedText: String?,
    homeLabel: String?,
    onFinished: (moves: Int, durationMillis: Long) -> Unit,
) {
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val startedAt = remember { elapsedMillis() }
    var reported by rememberSaveable { mutableStateOf(false) }
    val finished = stepIndex >= steps.size
    LaunchedEffect(finished) {
        if (finished && !reported) {
            reported = true
            onFinished(steps.sumOf { it.moves.size }, elapsedMillis() - startedAt)
        }
    }
    if (finished) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(finishedText ?: stringResource(Res.string.solve_finished), style = MaterialTheme.typography.headlineSmall)
            BigButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text(homeLabel ?: stringResource(Res.string.solve_home)) }
        }
        return
    }
    val before = remember(stepIndex) { start.apply(steps.take(stepIndex).flatMap { it.moves }) }
    key(stepIndex) {
        ComboStep(before, steps[stepIndex], stepIndex, steps.size, showNotation, onLesson) { stepIndex++ }
    }
}

@Composable
private fun ComboStep(
    before: Cube,
    step: Step,
    number: Int,
    count: Int,
    showNotation: Boolean,
    onLesson: ((Stage, List<Move>) -> Unit)?,
    onDone: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animator = rememberCubeAnimator(before)
    val state = remember { StepperState(before, step.moves, 0, animator, scope) }
    val marks = StepPieces.marks(animator.cube, step.note)
    val current = step.parts.indices.lastOrNull { step.partStart(it) <= state.index } ?: 0
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(Res.string.combo_step, number + 1, count) + " · " +
                stringResource(Res.string.stage_header, step.stage.ordinal + 1, stringResource(stageName(step.stage))),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(noteText(step.note) { id, args -> stringResource(id, *args) }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (StepPieces.colors(step.note) != null) {
            Text(stringResource(Res.string.combo_marked), style = MaterialTheme.typography.bodySmall)
        }
        GuideCube(state, mirror = true, marked = marks, modifier = Modifier.fillMaxWidth().height(240.dp))
        // The move now presented, in words, with move-by-move stepping.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RoundIconButton(onClick = state::back, enabled = state.index > 0, size = 44.dp) {
                Icon(painterResource(Res.drawable.ic_previous), contentDescription = stringResource(Res.string.solve_previous))
            }
            Box(Modifier.weight(1f)) { MoveWordsText(state, showNotation) }
            RoundIconButton(onClick = state::done, enabled = !state.isFinished, size = 44.dp) {
                Icon(painterResource(Res.drawable.ic_next), contentDescription = stringResource(Res.string.combo_next_move))
            }
        }
        step.parts.forEachIndexed { i, part ->
            PartCard(
                part = part,
                active = i == current && !state.isFinished,
                after = state.cubeAt(step.partStart(i + 1)),
                onShow = { state.play(step.partStart(i), step.partStart(i + 1)) },
                onLesson = onLesson?.let { open -> { open(step.stage, (part as StepPart.Combo).algorithm.moves) } }
                    ?.takeIf { part is StepPart.Combo },
            )
        }
        BigButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.combo_done)) }
    }
}

/** One part of a step: its title, what to do, "Näytä" and, for a combination, its lesson. */
@Composable
private fun PartCard(part: StepPart, active: Boolean, after: Cube, onShow: () -> Unit, onLesson: (() -> Unit)?) {
    val container = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth().then(if (part is StepPart.Combo) Modifier.testTag(COMBO_PART_TAG) else Modifier),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val title = when (part) {
                is StepPart.Hold -> stringResource(Res.string.combo_hold_title)
                is StepPart.Setup -> stringResource(Res.string.combo_setup_title)
                is StepPart.Combo -> LessonCatalog.algorithmName(part.algorithm.moves)?.let { stringResource(it) } ?: ""
                is StepPart.Free -> stringResource(Res.string.combo_free_title)
            }
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            when (part) {
                is StepPart.Hold -> Text(
                    stringResource(
                        Res.string.combo_hold,
                        stringResource(colorName(after.centre(Face.F))),
                        stringResource(colorName(after.centre(Face.U))),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
                is StepPart.Combo -> Text(
                    stringResource(Res.string.combo_times, Notation.format(part.algorithm.moves), part.times),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                )
                is StepPart.Setup, is StepPart.Free -> {
                    Text(Notation.format(part.moves), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
                    for (move in part.moves) {
                        Text(MoveWords.describe(move, { id, args -> stringResource(id, *args) }), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = onShow, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(painterResource(Res.drawable.ic_play), contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(Res.string.solve_show), modifier = Modifier.padding(start = 6.dp))
                }
                if (onLesson != null) {
                    TextButton(onClick = onLesson, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.combo_read_more)) }
                }
            }
        }
    }
}
