package fi.jukkakot.rubikkisolveri.log

import java.time.Clock
import java.util.concurrent.Executor

/**
 * Writes each event to the system log ([sink]) at once and to the [file] on the [io] executor, so
 * callers never wait on disk. [writeNow] skips the executor for the crash path.
 */
class Logger(
    val file: LogFile,
    private val sink: (Level, String) -> Unit,
    private val io: Executor,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun log(level: Level, evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) {
        val line = LogLine.format(clock.instant(), level, evt, fields.toMap(), msg)
        sink(level, line)
        io.execute { runCatching { file.append(line) } }
    }

    fun writeNow(level: Level, evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) {
        val line = LogLine.format(clock.instant(), level, evt, fields.toMap(), msg)
        sink(level, line)
        runCatching { file.append(line) }
    }

    fun info(evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) = log(Level.INFO, evt, msg, *fields)
    fun warn(evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) = log(Level.WARN, evt, msg, *fields)
    fun error(evt: Evt, error: Throwable, msg: String? = null, vararg fields: Pair<String, Any?>) =
        log(Level.ERROR, evt, msg ?: error.message, *fields, "stack" to LogLine.stackOf(error))

    /** Runs pending file writes on [io] and waits for them; used before reading the file. */
    fun flush() {
        val done = java.util.concurrent.CountDownLatch(1)
        io.execute { done.countDown() }
        done.await(2, java.util.concurrent.TimeUnit.SECONDS)
    }
}
