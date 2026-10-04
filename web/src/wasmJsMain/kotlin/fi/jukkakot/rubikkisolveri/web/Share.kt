package fi.jukkakot.rubikkisolveri.web

import kotlin.time.Clock

/**
 * The log and the scan pictures to the share sheet where the browser can share files (Chrome on
 * Android); otherwise the log is downloaded as a text file.
 */
fun shareLog(services: WebServices) {
    val stamp = Clock.System.now().toString().take(19).replace(':', '-')
    val text = services.logStore.readLines().joinToString("\n", postfix = "\n")
    val pictures = services.scanPictures.list()
    shareOrDownload(
        "rubikki-log-$stamp.txt",
        text,
        pictures.joinToString("\n") { it.name },
        pictures.joinToString("\n") { it.pngBase64 },
    )
}
