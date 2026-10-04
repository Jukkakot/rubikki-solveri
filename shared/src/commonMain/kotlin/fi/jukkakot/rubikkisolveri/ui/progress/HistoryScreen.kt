package fi.jukkakot.rubikkisolveri.ui.progress

import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.progress.Penalty
import fi.jukkakot.rubikkisolveri.progress.ProgressRepository
import fi.jukkakot.rubikkisolveri.progress.SolveStats
import fi.jukkakot.rubikkisolveri.progress.TimedSolve
import fi.jukkakot.rubikkisolveri.ui.common.stageName
import kotlinx.coroutines.launch
import fi.jukkakot.rubikkisolveri.ui.LocalFormats
import fi.jukkakot.rubikkisolveri.ui.currentLanguage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(progress: ProgressRepository, onBack: () -> Unit) {
    val timed by progress.timedSolves.collectAsStateWithLifecycle(emptyList())
    val guided by progress.guidedSolves.collectAsStateWithLifecycle(emptyList())
    val practice by progress.practiceSessions.collectAsStateWithLifecycle(emptyList())
    val language = currentLanguage()
    val dates = remember(language) { { millis: Long -> LocalFormats.shortDateTime(millis, language) } }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item { Section(Res.string.history_timed, timed.isEmpty()) }
            items(timed, key = { "t${it.id}" }) { solve -> TimedRow(solve, dates(solve.finishedAt), progress) }
            item { Section(Res.string.history_guided, guided.isEmpty()) }
            items(guided, key = { "g${it.id}" }) { g ->
                val method = stringResource(if (g.method == "LEARN") Res.string.method_learn else Res.string.method_fast)
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.history_guided_row, method, pluralStringResource(Res.plurals.history_moves, g.moves, g.moves), SolveStats.format(g.durationMillis))) },
                    supportingContent = { Text(dates(g.finishedAt)) },
                )
            }
            item { Section(Res.string.history_practice, practice.isEmpty()) }
            items(practice, key = { "p${it.id}" }) { p ->
                val stage = Stage.entries.getOrNull(p.stage)
                ListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.history_practice_row, stage?.let { stringResource(stageName(it)) } ?: "?", SolveStats.format(p.durationMillis)))
                    },
                    supportingContent = { Text(dates(p.finishedAt)) },
                )
            }
        }
    }
}

@Composable
private fun Section(title: StringResource, empty: Boolean) {
    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp))
    if (empty) Text(stringResource(Res.string.history_empty), modifier = Modifier.padding(horizontal = 16.dp))
    HorizontalDivider(Modifier.padding(top = 8.dp))
}

@Composable
private fun TimedRow(solve: TimedSolve, date: String, progress: ProgressRepository) {
    var menu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val time = when (solve.penalty) {
        Penalty.NONE -> SolveStats.format(solve.millis)
        Penalty.PLUS_TWO -> SolveStats.format(solve.millis + 2000) + "+"
        Penalty.DNF -> "DNF (" + SolveStats.format(solve.millis) + ")"
    }
    ListItem(
        headlineContent = { Text(time, fontFamily = FontFamily.Monospace) },
        supportingContent = { Text("$date · ${solve.scramble}") },
        trailingContent = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = stringResource(Res.string.more_actions)) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    for ((label, penalty) in listOf(Res.string.penalty_none to Penalty.NONE, Res.string.penalty_plus2 to Penalty.PLUS_TWO, Res.string.penalty_dnf to Penalty.DNF)) {
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                            menu = false
                            scope.launch { progress.setPenalty(solve, penalty) }
                        })
                    }
                    DropdownMenuItem(text = { Text(stringResource(Res.string.delete)) }, onClick = {
                        menu = false
                        scope.launch { progress.deleteTimed(solve.id) }
                    })
                }
            }
        },
    )
}
