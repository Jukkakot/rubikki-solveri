package fi.jukkakot.rubikkisolveri.ui.progress

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import fi.jukkakot.rubikkisolveri.progress.Average
import fi.jukkakot.rubikkisolveri.progress.Penalty
import fi.jukkakot.rubikkisolveri.progress.ProgressRepository
import fi.jukkakot.rubikkisolveri.progress.SolveStats
import fi.jukkakot.rubikkisolveri.progress.TimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val RANDOM_SCRAMBLE: suspend () -> List<Move> = { withContext(Dispatchers.Default) { TwoPhaseSolver.randomStateScramble() } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    progress: ProgressRepository,
    onBack: () -> Unit,
    onHistory: () -> Unit,
    onGuidedScramble: (List<Move>) -> Unit,
    scrambles: suspend () -> List<Move> = RANDOM_SCRAMBLE,
    now: () -> Long = SystemClock::elapsedRealtime,
) {
    val timer = remember { TimerState(now) }
    var phase by remember { mutableStateOf(timer.phase) }
    var shown by remember { mutableLongStateOf(0L) }
    var scramble by remember { mutableStateOf<List<Move>?>(null) }
    var scrambleNo by remember { mutableIntStateOf(0) }
    val solves by progress.timedSolves.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(scrambleNo) {
        scramble = null
        scramble = scrambles()
    }
    // Keep the display and the hold-to-ready state moving every frame while it matters.
    LaunchedEffect(phase) {
        while (phase == TimerState.Phase.HOLDING || phase == TimerState.Phase.RUNNING) {
            withFrameMillis { }
            timer.tick()
            phase = timer.phase
            shown = timer.display()
        }
        shown = timer.display()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.timer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onHistory) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.history_title))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.scramble_title), style = MaterialTheme.typography.labelLarge)
                    Text(
                        scramble?.let(Notation::format) ?: "…",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { scrambleNo++ }) { Text(stringResource(R.string.timer_new_scramble)) }
                        TextButton(onClick = { scramble?.let(onGuidedScramble) }, enabled = scramble != null) {
                            Text(stringResource(R.string.timer_guided_scramble))
                        }
                    }
                }
            }
            val background = when (phase) {
                TimerState.Phase.HOLDING -> Color(0xFFE57373)
                TimerState.Phase.READY -> Color(0xFF66BB6A)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            val areaName = stringResource(R.string.timer_area)
            Box(
                Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.large).background(background)
                    .semantics { contentDescription = areaName }
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown()
                            val stopped = timer.press()
                            phase = timer.phase
                            shown = timer.display()
                            if (stopped != null) {
                                val text = scramble?.let(Notation::format).orEmpty()
                                scope.launch { progress.addTimed(stopped, text) }
                                scrambleNo++
                            }
                            waitForUpOrCancellation()
                            timer.release()
                            phase = timer.phase
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        SolveStats.format(shown),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        stringResource(
                            when (phase) {
                                TimerState.Phase.HOLDING -> R.string.timer_hint_holding
                                TimerState.Phase.READY -> R.string.timer_hint_ready
                                TimerState.Phase.RUNNING -> R.string.timer_hint_running
                                else -> R.string.timer_hint_idle
                            },
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
            }
            val last = solves.firstOrNull()
            if (last != null && phase != TimerState.Phase.RUNNING) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = last.penalty == Penalty.PLUS_TWO,
                        onClick = { scope.launch { progress.setPenalty(last, if (last.penalty == Penalty.PLUS_TWO) Penalty.NONE else Penalty.PLUS_TWO) } },
                        label = { Text(stringResource(R.string.penalty_plus2)) },
                    )
                    FilterChip(
                        selected = last.penalty == Penalty.DNF,
                        onClick = { scope.launch { progress.setPenalty(last, if (last.penalty == Penalty.DNF) Penalty.NONE else Penalty.DNF) } },
                        label = { Text(stringResource(R.string.penalty_dnf)) },
                    )
                    OutlinedButton(onClick = { scope.launch { progress.deleteTimed(last.id) } }) { Text(stringResource(R.string.delete)) }
                }
            }
            StatsRow(solves.map { it.result })
        }
    }
}

@Composable
private fun StatsRow(results: List<fi.jukkakot.rubikkisolveri.progress.TimedResult>) {
    val none = stringResource(R.string.stats_none)
    fun avg(a: Average) = when (a) {
        is Average.Time -> SolveStats.format(a.millis)
        Average.Dnf -> "DNF"
        Average.NotEnough -> none
    }
    val items = listOf(
        R.string.stats_best to (SolveStats.best(results)?.let(SolveStats::format) ?: none),
        R.string.stats_ao5 to avg(SolveStats.averageOf(5, results)),
        R.string.stats_ao12 to avg(SolveStats.averageOf(12, results)),
        R.string.stats_mean to (SolveStats.mean(results)?.let(SolveStats::format) ?: none),
        R.string.stats_count to results.size.toString(),
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for ((label, value) in items) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
