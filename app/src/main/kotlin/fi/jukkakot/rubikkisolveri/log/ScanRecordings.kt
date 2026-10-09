package fi.jukkakot.rubikkisolveri.log

import android.content.Context
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * The phone's [ScanRecordingStore]: text files in [dir] (under `logs/`, so the log's FileProvider
 * shares them), written off the main thread by [background]; a failing write is logged and dropped.
 */
class ScanRecordings(val dir: File, private val background: Executor = WRITER) : ScanRecordingStore {

    override fun write(name: String, text: String) {
        background.execute {
            try {
                synchronized(this) {
                    dir.mkdirs()
                    File(dir, name).writeText(text)
                    files().dropLast(ScanRecordingStore.KEEP).forEach { it.delete() }
                }
            } catch (e: Exception) {
                AppLog.logger.warn(Evt.SCAN_VIDEO, "recording $name not kept: ${e.message}")
            }
        }
    }

    /** The recordings, oldest first. */
    @Synchronized
    fun files(): List<File> = dir.listFiles { f -> f.name.endsWith(".txt") }.orEmpty().sortedBy { it.name }

    override fun names(): List<String> = files().map { it.name }.reversed()

    @Synchronized
    override fun clear() {
        files().forEach { it.delete() }
    }

    companion object {
        private val WRITER: Executor = Executors.newSingleThreadExecutor()

        fun of(context: Context) = ScanRecordings(File(context.filesDir, "logs/recordings"))
    }
}
