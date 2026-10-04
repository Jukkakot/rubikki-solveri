package fi.jukkakot.rubikkisolveri.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.log.Level
import fi.jukkakot.rubikkisolveri.log.LogLine
import fi.jukkakot.rubikkisolveri.ui.LocalFormats
import fi.jukkakot.rubikkisolveri.ui.currentLanguage

/** Shows the log [lines] newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(lines: List<String>, onShare: () -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.log_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(Res.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(Res.string.log_share))
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(Res.string.log_clear))
                    }
                },
            )
        },
    ) { padding ->
        if (lines.isEmpty()) {
            Text(stringResource(Res.string.log_empty), modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            SelectionContainer(Modifier.padding(padding)) {
                val language = currentLanguage()
                LazyColumn(Modifier.fillMaxSize()) {
                    items(lines.asReversed()) { line ->
                        val parsed = remember(line) { LogLine.parse(line) }
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            parsed.time?.let {
                                Text(
                                    LocalFormats.timeOrDateTime(it.toEpochMilliseconds(), language),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // INFO is the default; other levels say so, so colour is not the only signal.
                            val level = parsed.level
                            Text(
                                if (level != null && level != Level.INFO) "${level.name} ${parsed.rest}" else parsed.rest,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = levelColor(level),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** Errors red, warnings amber, debug muted, info (and unparsed lines) plain. */
@Composable
private fun levelColor(level: Level?): Color = when (level) {
    Level.ERROR -> MaterialTheme.colorScheme.error
    Level.WARN -> if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) WARN_DARK else WARN_LIGHT
    Level.DEBUG -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.onSurface
}

private val WARN_LIGHT = Color(0xFFB26A00)
private val WARN_DARK = Color(0xFFFFB74D)
