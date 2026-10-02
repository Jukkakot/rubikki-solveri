package fi.jukkakot.rubikkisolveri.log

import java.time.Instant

/**
 * One event on one line: `ts level evt key=value … msg="…"`. Values with spaces or quotes are
 * quoted, and newlines are escaped so a stack trace never spans lines.
 */
object LogLine {
    fun format(ts: Instant, level: Level, evt: Evt, fields: Map<String, Any?>, msg: String?): String =
        buildString {
            append(ts).append(' ').append(level.name).append(' ').append(evt.id)
            for ((key, value) in fields) {
                if (value != null) append(' ').append(key).append('=').append(quote(value.toString()))
            }
            if (msg != null) append(" msg=").append(quote(msg))
        }

    fun stackOf(error: Throwable): String = error.stackTraceToString().trimEnd()

    private fun quote(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n").replace("\t", " ")
        val needsQuotes = escaped.isEmpty() || escaped.any { it == ' ' || it == '"' || it == '=' }
        return if (needsQuotes) "\"" + escaped.replace("\"", "\\\"") + "\"" else escaped
    }
}
