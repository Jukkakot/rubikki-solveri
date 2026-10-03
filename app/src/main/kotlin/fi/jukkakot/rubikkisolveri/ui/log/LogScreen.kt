package fi.jukkakot.rubikkisolveri.ui.log

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import fi.jukkakot.rubikkisolveri.R
import java.io.File

/** Shows the log [lines] newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(lines: List<String>, onShare: () -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.log_share))
                    }
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.log_clear))
                    }
                },
            )
        },
    ) { padding ->
        if (lines.isEmpty()) {
            Text(stringResource(R.string.log_empty), modifier = Modifier.padding(padding).padding(16.dp))
        } else {
            SelectionContainer(Modifier.padding(padding)) {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(lines.asReversed()) { line ->
                        Text(
                            line,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/**
 * The share-sheet intent for the log [file] and the scan [pictures], readable by the receiving app
 * through our FileProvider.
 */
fun shareLogIntent(context: Context, file: File, pictures: List<File> = emptyList()): Intent {
    val uris = (listOf(file) + pictures).map { FileProvider.getUriForFile(context, "${context.packageName}.files", it) }
    return shareFilesIntent(context, uris)
}

/** One file is sent as plain text; the log with pictures as several files. */
fun shareFilesIntent(context: Context, uris: List<Uri>): Intent {
    val send = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uris.single())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }.apply {
        putExtra(Intent.EXTRA_SUBJECT, "rubikki-solveri app.log")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return Intent.createChooser(send, context.getString(R.string.log_share_chooser))
}
