package fi.jukkakot.rubikkisolveri.log

import kotlin.time.Clock

/** Where log lines are kept: a file on the phone, browser storage on the web. */
interface LogStore {
    fun append(line: String)

    /** All lines, oldest first. */
    fun readLines(): List<String>
    fun clear()
}

/**
 * Writes each event to the system log ([sink]) at once and to the [store] through [post], so
 * callers never wait on disk. [writeNow] skips [post] for the crash path; [awaitPosted] waits for
 * the posted writes (the phone's executor; nothing to wait for where [post] runs inline).
 */
class Logger(
    val store: LogStore,
    private val sink: (Level, String) -> Unit,
    private val post: (() -> Unit) -> Unit,
    private val clock: Clock = Clock.System,
    private val awaitPosted: () -> Unit = {},
) {
    fun log(level: Level, evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) {
        val line = LogLine.format(clock.now(), level, evt, fields.toMap(), msg)
        sink(level, line)
        post { runCatching { store.append(line) } }
    }

    fun writeNow(level: Level, evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) {
        val line = LogLine.format(clock.now(), level, evt, fields.toMap(), msg)
        sink(level, line)
        runCatching { store.append(line) }
    }

    fun info(evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) = log(Level.INFO, evt, msg, *fields)
    fun warn(evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) = log(Level.WARN, evt, msg, *fields)
    fun error(evt: Evt, error: Throwable, msg: String? = null, vararg fields: Pair<String, Any?>) =
        log(Level.ERROR, evt, msg ?: error.message, *fields, "stack" to LogLine.stackOf(error))

    /** Waits for pending writes; used before reading the store. */
    fun flush() = awaitPosted()
}

/** The app-wide [Logger], installed once at start by the platform (tests install their own). */
object AppLog {
    lateinit var logger: Logger
        private set

    val isReady: Boolean get() = ::logger.isInitialized

    fun install(logger: Logger) {
        this.logger = logger
    }

    fun info(evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) {
        if (isReady) logger.info(evt, msg, *fields)
    }
}
