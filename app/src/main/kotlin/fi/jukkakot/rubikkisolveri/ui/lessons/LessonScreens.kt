package fi.jukkakot.rubikkisolveri.ui.lessons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Sequences
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.ui.common.moveDescription
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeAnimator
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LessonScaffold(title: String, onBack: () -> Unit, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding -> content(Modifier.padding(padding)) }
}

/** The basics and the seven stages, in order. */
@Composable
fun LessonsScreen(onOpen: (Int) -> Unit, onBack: () -> Unit, practiceCounts: Map<Int, Int> = emptyMap()) {
    LessonScaffold(stringResource(R.string.lessons_title), onBack) { modifier ->
        LazyColumn(modifier.fillMaxSize()) {
            itemsIndexed(LessonCatalog.lessons) { index, lesson ->
                val title = stringResource(lesson.title)
                ListItem(
                    headlineContent = { Text(if (lesson.stage == null) title else "${lesson.stage.ordinal + 1}. $title") },
                    supportingContent = {
                        val count = lesson.stage?.let { practiceCounts[it.ordinal] } ?: 0
                        val summary = stringResource(lesson.summary)
                        Text(if (count > 0) "$summary · ${stringResource(R.string.lesson_practised, count)}" else summary)
                    },
                    modifier = Modifier.clickable { onOpen(index) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun LessonScreen(index: Int, onBack: () -> Unit, onPractice: (Stage) -> Unit, onFreeCube: () -> Unit) {
    val lesson = LessonCatalog.lessons[index]
    LessonScaffold(stringResource(lesson.title), onBack) { modifier ->
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for ((heading, text) in lesson.paragraphs) {
                if (heading != null) Text(stringResource(heading), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
            }
            if (lesson.algorithms.isNotEmpty()) {
                Text(stringResource(R.string.lesson_algorithms), style = MaterialTheme.typography.titleMedium)
                for (algorithm in lesson.algorithms) AlgorithmCard(algorithm)
            }
            if (lesson.stage != null) {
                Button(onClick = { onPractice(lesson.stage) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lesson_practice))
                }
            } else {
                OutlinedButton(onClick = onFreeCube, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.lesson_basics_practice))
                }
            }
        }
    }
}

/** An algorithm: name, notation, each move in words, and a 3D demo that plays it. */
@Composable
fun AlgorithmCard(algorithm: Algorithm, animatorFor: @Composable (Cube) -> CubeAnimator = { rememberCubeAnimator(it) }) {
    val start = remember(algorithm) { Cube.solved().apply(Sequences.inverse(algorithm.moves)) }
    val animator = animatorFor(start)
    val scope = rememberCoroutineScope()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(algorithm.name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(Notation.format(algorithm.moves), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace)
            Cube3D(
                colors = animator.cube.toList().map(StickerColors::of),
                move = animator.move,
                progress = animator.progress,
                highlight = animator.move,
                modifier = Modifier.fillMaxWidth(0.7f).aspectRatio(1f),
            )
            Button(
                onClick = {
                    animator.snapTo(start)
                    animator.play(algorithm.moves)
                    scope.launch {
                        snapshotFlow { animator.pending }.first { it == 0 }
                        delay(DEMO_END_PAUSE_MS)
                        if (animator.pending == 0) animator.snapTo(start)
                    }
                },
            ) { Text(stringResource(R.string.lesson_play)) }
            algorithm.moves.forEachIndexed { i, move ->
                Text("${i + 1}. ${moveDescription(move)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private const val DEMO_END_PAUSE_MS = 1200L
