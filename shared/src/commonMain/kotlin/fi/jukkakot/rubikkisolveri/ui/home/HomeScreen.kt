package fi.jukkakot.rubikkisolveri.ui.home

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.input.pointer.pointerInput
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.progress.SolveStats
import fi.jukkakot.rubikkisolveri.progress.TimedResult
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.Quat
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3
import kotlin.math.PI

/** A feature entry on the home screen: [label] and [icon] are resources. */
data class HomeEntry(val label: StringResource, val icon: DrawableResource, val onOpen: () -> Unit)

/** The user's timed solves in short: [best] is null when every solve is a DNF. */
data class HomeSummary(val best: Long?, val count: Int) {
    companion object {
        /** Null when there are no timed solves, so the line is left out. */
        fun of(results: List<TimedResult>): HomeSummary? =
            if (results.isEmpty()) null else HomeSummary(SolveStats.best(results), results.size)
    }
}

/** One full turn of the idle spin, and how long it waits after the user lets go. */
private const val SPIN_MILLIS = 20_000f
private const val SPIN_PAUSE_NANOS = 2_000_000_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    primary: HomeEntry,
    entries: List<HomeEntry>,
    onOpenSettings: () -> Unit,
    crashedLastTime: Boolean,
    onShowLog: () -> Unit,
    onCrashNoticeShown: () -> Unit,
    version: String = "",
    summary: HomeSummary? = null,
    spin: Boolean = true,
) {
    val snackbar = remember { SnackbarHostState() }
    val notice = stringResource(Res.string.crash_notice)
    val action = stringResource(Res.string.crash_show_log)
    LaunchedEffect(crashedLastTime) {
        if (crashedLastTime) {
            onCrashNoticeShown()
            if (snackbar.showSnackbar(notice, action) == SnackbarResult.ActionPerformed) onShowLog()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    RoundIconButton(onClick = onOpenSettings, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(Res.string.settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 8.dp)) {
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        Hero(spin)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                        Actions(primary, entries)
                        Footer(summary, version)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                    Hero(spin)
                    Actions(primary, entries)
                    Footer(summary, version)
                }
            }
        }
    }
}

/** The app name over the slowly turning cube; dragging turns it and pauses the spin. */
@Composable
private fun ColumnScope.Hero(spin: Boolean) {
    val viewState = remember { CubeViewState() }
    var touchedAt by remember { mutableLongStateOf(Long.MIN_VALUE / 2) }
    var now by remember { mutableLongStateOf(0L) }
    LaunchedEffect(spin) {
        if (!spin) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val t = withFrameNanos { it }
            now = t
            if (t - touchedAt > SPIN_PAUSE_NANOS) {
                val angle = ((t - last) / 1_000_000f) / SPIN_MILLIS * (2 * PI).toFloat()
                viewState.rotation = (Quat.axisAngle(V3(0f, 1f, 0f), angle) * viewState.rotation).normalized()
            }
            last = t
        }
    }
    Text(
        stringResource(Res.string.app_name),
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Cube3D(
        colors = remember { Cube.solved().toList().map(StickerColors::of) },
        viewState = viewState,
        description = stringResource(Res.string.home_cube_description),
        modifier = Modifier
            .weight(1f, fill = false)
            // A square that fits the space left; aspectRatio would overflow onto the texts when space runs out (browser).
            .layout { measurable, constraints ->
                val side = minOf(constraints.maxWidth, constraints.maxHeight)
                val placeable = measurable.measure(Constraints.fixed(side, side))
                layout(side, side) { placeable.place(0, 0) }
            }
            .align(Alignment.CenterHorizontally)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        touchedAt = now
                    }
                }
            },
    )
    Text(
        stringResource(Res.string.home_tagline),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The scan as the one filled button, the other features as tonal tiles two to a row (a lone last tile spans the row). */
@Composable
private fun Actions(primary: HomeEntry, entries: List<HomeEntry>) {
    Button(onClick = primary.onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
        Icon(painterResource(primary.icon), contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(primary.label), style = MaterialTheme.typography.titleMedium)
    }
    for (pair in entries.chunked(2)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            for (entry in pair) {
                FilledTonalButton(
                    onClick = entry.onOpen,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.weight(1f).heightIn(min = 80.dp),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(entry.icon), contentDescription = null)
                        Text(stringResource(entry.label), textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun Footer(summary: HomeSummary?, version: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (summary != null && summary.count > 0) {
            val count = pluralStringResource(Res.plurals.home_solve_count, summary.count, summary.count)
            Text(
                if (summary.best != null) stringResource(Res.string.home_summary, SolveStats.format(summary.best), count) else count,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (version.isNotEmpty()) {
            Text(
                stringResource(Res.string.version_label, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
