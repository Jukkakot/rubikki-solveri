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

    /** A line split for display; [time] and [level] are null when the line does not start with them. */
    data class Parsed(val time: Instant?, val level: Level?, val rest: String)

    /** Splits off the time and level of a [format]ted line; any other line comes back whole. */
    fun parse(line: String): Parsed {
        val parts = line.split(' ', limit = 3)
        if (parts.size < 2) return Parsed(null, null, line)
        val time = runCatching { Instant.parse(parts[0]) }.getOrNull()
        val level = Level.entries.firstOrNull { it.name == parts[1] }
        if (time == null || level == null) return Parsed(null, null, line)
        return Parsed(time, level, parts.getOrElse(2) { "" })
    }

    private fun quote(value: String): String {
        val escaped = value.replace("\\", "\\\\").replace("\r", "").replace("\n", "\\n").replace("\t", " ")
        val needsQuotes = escaped.isEmpty() || escaped.any { it == ' ' || it == '"' || it == '=' }
        return if (needsQuotes) "\"" + escaped.replace("\"", "\\\"") + "\"" else escaped
    }
}
