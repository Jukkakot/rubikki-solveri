package fi.jukkakot.rubikkisolveri.log

import java.io.File

/**
 * The on-phone log: lines appended to [file]. When the file passes [maxBytes] the oldest lines
 * are dropped so that about half of the cap remains.
 */
class LogFile(val file: File, private val maxBytes: Long = DEFAULT_MAX_BYTES) : LogStore {

    @Synchronized
    override fun append(line: String) {
        file.parentFile?.mkdirs()
        file.appendText(line + "\n")
        if (file.length() > maxBytes) trim()
    }

    /** All lines, oldest first. */
    @Synchronized
    override fun readLines(): List<String> = if (file.exists()) file.readLines().filter { it.isNotEmpty() } else emptyList()

    @Synchronized
    override fun clear() {
        if (file.exists()) file.writeText("")
    }

    private fun trim() {
        val lines = file.readLines()
        var size = 0L
        val kept = ArrayDeque<String>()
        for (line in lines.asReversed()) {
            size += line.toByteArray().size + 1
            if (size > maxBytes / 2) break
            kept.addFirst(line)
        }
        file.writeText(kept.joinToString(separator = "\n", postfix = if (kept.isEmpty()) "" else "\n"))
    }

    companion object {
        const val DEFAULT_MAX_BYTES = 512L * 1024
    }
}
