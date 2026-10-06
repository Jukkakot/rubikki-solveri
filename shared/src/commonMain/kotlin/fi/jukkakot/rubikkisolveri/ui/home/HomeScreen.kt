package fi.jukkakot.rubikkisolveri.ui.home

import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.input.pointer.pointerInput
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.common.RoundIconButton
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeViewState
import fi.jukkakot.rubikkisolveri.ui.cube3d.Quat
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.cube3d.V3
import kotlin.math.PI

/**
 * A feature entry on the home screen: [label] (one word, shown under the icon) and [icon] are
 * resources; [description] is the full name for screen readers (defaults to [label]).
 */
data class HomeEntry(
    val label: StringResource,
    val icon: DrawableResource,
    val description: StringResource = label,
    val onOpen: () -> Unit,
)

/** One full turn of the idle spin, and how long it waits after the user lets go. */
private const val SPIN_MILLIS = 20_000f
private const val SPIN_PAUSE_NANOS = 2_000_000_000L

/** Test tag of the home cube, which opens the scan when tapped. */
const val HOME_CUBE_TAG = "home_cube"

/**
 * The home screen: the app name over the slowly turning cube, which is the scan action (a tap on it
 * or on the round camera button on its lower edge opens [primary]; a drag only turns it), the other
 * features as a row of icons with one-word labels, and the version.
 */
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
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                        Hero(primary, spin)
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
                        Features(entries)
                        Footer(version)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
                    Hero(primary, spin)
                    Features(entries)
                    Footer(version)
                }
            }
        }
    }
}

/**
 * The app name over the slowly turning cube; dragging turns it and pauses the spin, a tap opens
 * [primary]. The round camera button sits on the cube's lower edge with the short label under it.
 */
@Composable
private fun ColumnScope.Hero(primary: HomeEntry, spin: Boolean) {
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
    Box(
        Modifier
            .weight(1f, fill = false)
            // A square that fits the space left; aspectRatio would overflow onto the texts when space runs out (browser).
            .layout { measurable, constraints ->
                val side = minOf(constraints.maxWidth, constraints.maxHeight)
                val placeable = measurable.measure(Constraints.fixed(side, side))
                layout(side, side) { placeable.place(0, 0) }
            }
            .align(Alignment.CenterHorizontally),
    ) {
        Cube3D(
            colors = remember { Cube.solved().toList().map(StickerColors::of) },
            viewState = viewState,
            description = stringResource(Res.string.home_cube_description),
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = SCAN_BUTTON / 2)
                .testTag(HOME_CUBE_TAG)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            touchedAt = now
                        }
                    }
                }
                // A tap opens the scan; a drag turns the cube and cancels the tap.
                .pointerInput(primary) { detectTapGestures { primary.onOpen() } },
        )
        Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
            FilledIconButton(onClick = primary.onOpen, modifier = Modifier.size(SCAN_BUTTON), shape = CircleShape) {
                Icon(painterResource(primary.icon), contentDescription = stringResource(primary.description), modifier = Modifier.size(30.dp))
            }
        }
    }
    Text(
        stringResource(primary.label),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The other features: equal icons in one row, a one-word label under each. */
@Composable
private fun Features(entries: List<HomeEntry>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        for (entry in entries) {
            val description = stringResource(entry.description)
            Column(
                Modifier.weight(1f).clip(MaterialTheme.shapes.large).clickable(onClickLabel = description, onClick = entry.onOpen).padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(painterResource(entry.icon), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Text(
                    stringResource(entry.label),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun Footer(version: String) {
    if (version.isEmpty()) return
    Text(
        stringResource(Res.string.version_label, version),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

private val SCAN_BUTTON = 64.dp
