package fi.jukkakot.rubikkisolveri.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.R

/** A main feature on the home screen; [onOpen] is null until the feature is built. */
data class HomeEntry(val label: Int, val onOpen: (() -> Unit)?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    entries: List<HomeEntry>,
    onOpenSettings: () -> Unit,
    crashedLastTime: Boolean,
    onShowLog: () -> Unit,
    onCrashNoticeShown: () -> Unit,
    version: String = "",
) {
    val snackbar = remember { SnackbarHostState() }
    val notice = stringResource(R.string.crash_notice)
    val action = stringResource(R.string.crash_show_log)
    LaunchedEffect(crashedLastTime) {
        if (crashedLastTime) {
            onCrashNoticeShown()
            if (snackbar.showSnackbar(notice, action) == SnackbarResult.ActionPerformed) onShowLog()
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.settings))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.home_tagline), style = MaterialTheme.typography.titleMedium)
            for (entry in entries) {
                FilledTonalButton(
                    onClick = { entry.onOpen?.invoke() },
                    enabled = entry.onOpen != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                ) {
                    val label = stringResource(entry.label)
                    Text(if (entry.onOpen == null) "$label · ${stringResource(R.string.coming_soon)}" else label)
                }
            }
            if (version.isNotEmpty()) {
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.version_label, version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}
