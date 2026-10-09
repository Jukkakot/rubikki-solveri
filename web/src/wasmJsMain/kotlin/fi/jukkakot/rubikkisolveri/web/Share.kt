package fi.jukkakot.rubikkisolveri.web

import kotlin.time.Clock

/**
 * The log, the scan pictures and the scan recordings to the share sheet where the browser can share files (Chrome on
 * Android); otherwise, or when the browser refuses, the log alone, then a download. [onOutcome] gets
 * the outcome and the browser's refusal ("" when none).
 */
fun shareLog(services: WebServices, onOutcome: (outcome: String, error: String) -> Unit) {
    val stamp = Clock.System.now().toString().take(19).replace(':', '-')
    val text = services.logStore.readLines().joinToString("\n", postfix = "\n")
    val pictures = services.scanPictures.list()
    val recordings = services.scanRecordings.list()
    shareOrDownload(
        "rubikki-log-$stamp.txt",
        text,
        pictures.joinToString("\n") { it.name },
        pictures.joinToString("\n") { it.pngBase64 },
        recordings.joinToString("\n") { it.name },
        recordings.joinToString("\u0001") { it.text },
        onOutcome,
    )
}
