package fi.jukkakot.rubikkisolveri.ui.lessons

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Sequences
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StageCase
import fi.jukkakot.rubikkisolveri.cube.beginner.StageCases
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
private fun LessonScaffold(
    title: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (Modifier) -> Unit,
) {
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
        bottomBar = bottomBar,
    ) { padding -> content(Modifier.padding(padding)) }
}

/** The basics and the seven stages in order, each stage with its goal picture: the path to a solved cube. */
@Composable
fun LessonsScreen(onOpen: (Int) -> Unit, onBack: () -> Unit, practiceCounts: Map<Int, Int> = emptyMap()) {
    LessonScaffold(stringResource(R.string.lessons_title), onBack) { modifier ->
        LazyColumn(modifier.fillMaxSize()) {
            itemsIndexed(LessonCatalog.lessons) { index, lesson ->
                val title = stringResource(lesson.title)
                ListItem(
                    leadingContent = {
                        val thumb = Modifier.size(56.dp)
                        if (lesson.stage != null) StageGoalCube(lesson.stage, thumb) else GoalCube(Cube.solved().toList(), thumb)
                    },
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

/** A lesson as a row of pages: swipe or use the buttons; the last page's button practises. */
@Composable
fun LessonScreen(index: Int, onBack: () -> Unit, onPractice: (Stage) -> Unit, onFreeCube: () -> Unit, initialPage: Int = 0) {
    val lesson = LessonCatalog.lessons[index]
    val pages = remember(lesson) { lesson.pages }
    val pager = rememberPagerState(initialPage) { pages.size }
    val scope = rememberCoroutineScope()
    val title = stringResource(lesson.title).let { if (lesson.stage == null) it else "${lesson.stage.ordinal + 1}. $it" }
    LessonScaffold(
        title, onBack,
        bottomBar = {
            val page = pager.currentPage
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PageDots(page, pages.size, Modifier.align(Alignment.CenterHorizontally))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { scope.launch { pager.animateScrollToPage(page - 1) } },
                        enabled = page > 0,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.lesson_previous)) }
                    val last = page == pages.lastIndex
                    Button(
                        onClick = {
                            when {
                                !last -> scope.launch { pager.animateScrollToPage(page + 1) }
                                lesson.stage != null -> onPractice(lesson.stage)
                                else -> onFreeCube()
                            }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Text(
                            stringResource(
                                when {
                                    !last -> R.string.lesson_next
                                    lesson.stage != null -> R.string.lesson_practice
                                    else -> R.string.lesson_basics_practice
                                },
                            ),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    ) { modifier ->
        HorizontalPager(pager, modifier.fillMaxSize()) { i ->
            Column(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (val page = pages[i]) {
                    is LessonPage.Goal -> GoalPage(lesson, page.stage)
                    is LessonPage.Cases -> CasesPage(page.stage)
                    is LessonPage.AlgorithmDemo -> AlgorithmPage(page.algorithm, page.intro)
                    is LessonPage.Practice -> PracticePage(page.stage)
                    is LessonPage.Picture -> PicturePage(page)
                }
            }
        }
    }
}

@Composable
private fun PageDots(page: Int, count: Int, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.lesson_page, page + 1, count)
    Row(modifier.clearAndSetSemantics { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val color = if (i == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            Box(Modifier.size(if (i == page) 10.dp else 8.dp).clip(CircleShape).background(color).align(Alignment.CenterVertically))
        }
    }
}

/** The page's picture: takes the room the texts leave, as a square. */
@Composable
private fun ColumnScope.PictureBox(content: @Composable () -> Unit) {
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun PageTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
}

@Composable
private fun ColumnScope.GoalPage(lesson: Lesson, stage: Stage) {
    PageTitle(stringResource(R.string.lesson_page_goal))
    PictureBox { StageGoalPicture(stage, Modifier.fillMaxSize()) }
    Text(stringResource(lesson.summary), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
    lesson.tip?.let { tip ->
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(stringResource(R.string.lesson_tip) + ": ") }
                append(stringResource(tip))
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ColumnScope.PracticePage(stage: Stage) {
    PageTitle(stringResource(R.string.lesson_page_practice))
    PictureBox { StageGoalCube(stage, Modifier.fillMaxSize(0.7f)) }
    Text(stringResource(R.string.lesson_practice_text), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
}

@Composable
private fun ColumnScope.PicturePage(page: LessonPage.Picture) {
    val solved = Cube.solved()
    val centres = Face.entries.map { Stickers.centre(it) }.toSet()
    val (colors, marked) = when (page.picture) {
        BasicsPicture.CENTRES -> solved.toList().mapIndexed { i, c -> c.takeIf { i in centres } } to centres
        BasicsPicture.PIECES -> solved.toList() to (Edge.UF.stickers + Corner.URF.stickers).toSet()
        BasicsPicture.HOLD -> solved.toList() to setOf(Stickers.centre(Face.U), Stickers.centre(Face.F))
    }
    PictureBox { GoalCube(colors, Modifier.fillMaxSize(), marked = marked, large = true) }
    Text(stringResource(page.text), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
}

/** "Which situation do you have?": the stage's cases as a 2×2 grid; tapping one opens it large. */
@Composable
fun ColumnScope.CasesPage(stage: Stage, animatorFor: @Composable (Cube) -> CubeAnimator = { rememberCubeAnimator(it) }) {
    val cases = remember(stage) { StageCases.of(stage) }
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    BackHandler(enabled = selected != null) { selected = null }
    val open = selected?.let { cases[it] }
    if (open != null) {
        key(open.id) { CaseDetail(open, onClose = { selected = null }, animatorFor) }
        return
    }
    PageTitle(stringResource(R.string.lesson_cases_title))
    for (row in cases.indices.chunked(2)) {
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (i in row) CaseCard(cases[i], Modifier.weight(1f).fillMaxHeight()) { selected = i }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun caseAction(case: StageCase, notation: Boolean): String {
    val algorithm = case.algorithm ?: return Notation.format(case.moves)
    val name = if (notation) Notation.format(algorithm) else LessonCatalog.algorithmName(algorithm)?.let { stringResource(it) }
    val text = name ?: Notation.format(algorithm)
    return if (case.times == 1) text else stringResource(R.string.lesson_times, text, case.times)
}

@Composable
private fun CaseCard(case: StageCase, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick)) {
        Column(Modifier.padding(8.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                GoalCube(case.position.toList(), Modifier.fillMaxSize(), marked = case.highlight)
            }
            Text(stringResource(LessonCatalog.caseCaption(case.id)), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, maxLines = 2)
            Text(caseAction(case, notation = false), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, maxLines = 1)
        }
    }
}

@Composable
private fun ColumnScope.CaseDetail(case: StageCase, onClose: () -> Unit, animatorFor: @Composable (Cube) -> CubeAnimator) {
    val animator = animatorFor(case.position)
    PageTitle(stringResource(LessonCatalog.caseCaption(case.id)))
    PictureBox {
        val atStart = animator.pending == 0 && animator.cube == case.position
        Cube3D(
            colors = animator.cube.toList().map(StickerColors::of),
            move = animator.move,
            progress = animator.progress,
            highlight = animator.move,
            marked = if (atStart) case.highlight else emptySet(),
            modifier = Modifier.fillMaxSize().aspectRatio(1f),
        )
    }
    Text(caseAction(case, notation = true), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.lesson_cases_all)) }
        Button(
            onClick = {
                animator.snapTo(case.position)
                animator.play(case.moves)
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.lesson_play)) }
    }
}

/**
 * An algorithm: a large 3D demo that plays it, the notation with the current move highlighted and
 * said in words, and before/after pictures with the pieces it moves outlined.
 */
@Composable
fun ColumnScope.AlgorithmPage(
    algorithm: Algorithm,
    intro: Int? = null,
    animatorFor: @Composable (Cube) -> CubeAnimator = { rememberCubeAnimator(it) },
) {
    val end = remember(algorithm) { Cube.solved().apply(algorithm.hold) }
    val start = remember(algorithm) { end.apply(Sequences.inverse(algorithm.moves)) }
    val moved = remember(algorithm) { Sequences.movedStickers(algorithm.moves) }
    val animator = animatorFor(start)
    val scope = rememberCoroutineScope()
    var playing by remember { mutableStateOf(false) }
    val current = if (playing && animator.pending > 0) algorithm.moves.size - animator.pending else null

    if (intro != null) Text(stringResource(intro), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    PageTitle(stringResource(algorithm.name))
    PictureBox {
        Cube3D(
            colors = animator.cube.toList().map(StickerColors::of),
            move = animator.move,
            progress = animator.progress,
            highlight = animator.move,
            modifier = Modifier.fillMaxSize().aspectRatio(1f),
        )
    }
    val highlight = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold,
    )
    Text(
        buildAnnotatedString {
            algorithm.moves.forEachIndexed { i, move ->
                if (i > 0) append(" ")
                if (i == current) withStyle(highlight) { append(move.toString()) } else append(move.toString())
            }
        },
        style = MaterialTheme.typography.titleLarge,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
    )
    Text(
        current?.let { moveDescription(algorithm.moves[it]) } ?: "",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
        minLines = 2,
    )
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Thumbnail(start, moved, stringResource(R.string.lesson_before))
        Button(
            onClick = {
                animator.snapTo(start)
                animator.play(algorithm.moves)
                playing = true
                scope.launch {
                    snapshotFlow { animator.pending }.first { it == 0 }
                    playing = false
                    delay(DEMO_END_PAUSE_MS)
                    if (animator.pending == 0) animator.snapTo(start)
                }
            },
            modifier = Modifier.heightIn(min = 48.dp),
        ) { Text(stringResource(R.string.lesson_play)) }
        Thumbnail(end, moved, stringResource(R.string.lesson_after))
    }
}

@Composable
private fun Thumbnail(cube: Cube, marked: Set<Int>, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GoalCube(cube.toList(), Modifier.size(80.dp), marked = marked, description = label)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private const val DEMO_END_PAUSE_MS = 1200L
