package fi.jukkakot.rubikkisolveri.ui.log

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import fi.jukkakot.rubikkisolveri.R
import java.io.File

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
