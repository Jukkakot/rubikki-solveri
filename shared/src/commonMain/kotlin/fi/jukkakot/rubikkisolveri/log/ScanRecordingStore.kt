package fi.jukkakot.rubikkisolveri.log

/**
 * The video scans' recordings (`scan-recording`), kept next to the log: only the newest [KEEP],
 * shared with the log and cleared with it; they leave the device only when shared.
 */
interface ScanRecordingStore {
    /**
     * Stores the recording [name] (`scan-<time>.txt`, sorting by time) with [text], replacing one of
     * the same name, and drops the oldest beyond [KEEP]. A failure is logged and swallowed: the scan
     * never stops for it.
     */
    fun write(name: String, text: String)

    /** The names kept, newest first. */
    fun names(): List<String>

    fun clear()

    companion object {
        const val KEEP = 3

        /** The file name for a recording started at [isoTime] (`2026-10-09T11:54:00.123Z` → `scan-2026-10-09T11-54-00-123.txt`). */
        fun nameOf(isoTime: String): String {
            val millis = isoTime.drop(19).removePrefix(".").takeWhile { it.isDigit() }.padEnd(3, '0').take(3)
            return "scan-" + isoTime.take(19).replace(':', '-') + "-$millis.txt"
        }
    }
}

/** Keeps nothing: previews and tests. */
object NoScanRecordings : ScanRecordingStore {
    override fun write(name: String, text: String) = Unit
    override fun names(): List<String> = emptyList()
    override fun clear() = Unit
}
