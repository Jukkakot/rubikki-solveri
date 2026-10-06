package fi.jukkakot.rubikkisolveri.ui.target

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubePattern
import fi.jukkakot.rubikkisolveri.cube.SolveTarget
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.common.BackButton
import fi.jukkakot.rubikkisolveri.ui.common.stageName
import fi.jukkakot.rubikkisolveri.ui.lessons.GoalCube
import fi.jukkakot.rubikkisolveri.ui.lessons.StageGoalCube
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random

/** The name of [pattern] in the app's language. */
fun patternName(pattern: CubePattern): StringResource = when (pattern) {
    CubePattern.CHECKERBOARD -> Res.string.pattern_checkerboard
    CubePattern.SIX_SPOTS -> Res.string.pattern_six_spots
    CubePattern.CUBE_IN_CUBE -> Res.string.pattern_cube_in_cube
    CubePattern.CUBE_IN_CUBE_IN_CUBE -> Res.string.pattern_cube_in_cube_in_cube
    CubePattern.SUPERFLIP -> Res.string.pattern_superflip
    CubePattern.ANACONDA -> Res.string.pattern_anaconda
    CubePattern.PYTHON -> Res.string.pattern_python
    CubePattern.TETRIS -> Res.string.pattern_tetris
    CubePattern.TWISTER -> Res.string.pattern_twister
    CubePattern.CROSS -> Res.string.pattern_cross
    CubePattern.VERTICAL_STRIPES -> Res.string.pattern_stripes
}

/** The short name of [target]. */
@Composable
fun targetName(target: SolveTarget): String = when (target) {
    SolveTarget.Solved -> stringResource(Res.string.target_solved)
    is SolveTarget.Pattern -> stringResource(patternName(target.pattern))
    is SolveTarget.StageDone -> stringResource(Res.string.target_stage_done, stringResource(stageName(target.stage)))
    is SolveTarget.Painted -> stringResource(Res.string.target_painted)
}

/** A still picture of [target] on a cube like [start] (a stage shows its goal picture). */
@Composable
fun TargetPicture(target: SolveTarget, start: Cube, modifier: Modifier = Modifier, large: Boolean = false) {
    when (target) {
        is SolveTarget.StageDone -> StageGoalCube(target.stage, modifier, large = large)
        else -> GoalCube(target.cubeFor(start)!!.toList(), modifier, large = large, description = targetName(target))
    }
}

/**
 * Choosing where the guide leads the cube [start] (`solve-to-target`): a surprise, the pattern
 * gallery (a tap shows the pattern large first), the learn method's stages, or painting one's own.
 * [current] is not offered by the surprise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetScreen(
    start: Cube,
    current: SolveTarget,
    onChoose: (SolveTarget) -> Unit,
    onPaint: () -> Unit,
    onBack: () -> Unit,
) {
    var preview by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.target_title)) },
                navigationIcon = { Row(Modifier.padding(horizontal = 8.dp)) { BackButton(onBack) } },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(3) }) {
                Button(
                    onClick = {
                        val others = CubePattern.entries.filter { SolveTarget.Pattern(it) != current }
                        preview = SolveTarget.Pattern(others[Random.nextInt(others.size)]).encode()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { Text(stringResource(Res.string.target_surprise), style = MaterialTheme.typography.titleMedium) }
            }
            item(span = { GridItemSpan(3) }) { Section(stringResource(Res.string.target_patterns)) }
            items(CubePattern.entries) { pattern ->
                val target = SolveTarget.Pattern(pattern)
                Column(
                    Modifier.clickable { preview = target.encode() }.padding(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TargetPicture(target, start, Modifier.fillMaxWidth().aspectRatio(1f))
                    Text(stringResource(patternName(pattern)), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            }
            item(span = { GridItemSpan(3) }) { Section(stringResource(Res.string.target_stages)) }
            items(Stage.entries, span = { GridItemSpan(3) }) { stage ->
                ListItem(
                    leadingContent = { StageGoalCube(stage, Modifier.size(48.dp)) },
                    headlineContent = { Text("${stage.ordinal + 1}. ${stringResource(stageName(stage))}") },
                    modifier = Modifier.clickable { onChoose(SolveTarget.StageDone(stage)) },
                )
            }
            item(span = { GridItemSpan(3) }) {
                FilledTonalButton(onClick = onPaint, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text(stringResource(Res.string.target_paint))
                }
            }
        }
    }
    preview?.let(SolveTarget::decode)?.let { target ->
        AlertDialog(
            onDismissRequest = { preview = null },
            confirmButton = {
                Button(onClick = {
                    preview = null
                    onChoose(target)
                }) { Text(stringResource(Res.string.target_choose)) }
            },
            dismissButton = { OutlinedButton(onClick = { preview = null }) { Text(stringResource(Res.string.target_close)) } },
            title = { Text(targetName(target)) },
            text = { TargetPicture(target, start, Modifier.fillMaxWidth().aspectRatio(1f), large = true) },
        )
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}
