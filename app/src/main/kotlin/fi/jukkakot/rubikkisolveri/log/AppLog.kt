package fi.jukkakot.rubikkisolveri.log

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.Executors

/** The app-wide [Logger], set up once in the Application. */
object AppLog {
    private const val TAG = "Rubikki"

    lateinit var logger: Logger
        private set

    val isReady: Boolean get() = ::logger.isInitialized

    /** Called once per process from the Application (tests create one per test). */
    fun init(context: Context): Logger {
        val file = LogFile(File(context.filesDir, "logs/app.log"))
        logger = Logger(file, ::logcat, executor)
        return logger
    }

    private val executor by lazy { Executors.newSingleThreadExecutor { Thread(it, "app-log").apply { isDaemon = true } } }

    /** For tests: replace the logger. */
    fun install(logger: Logger) {
        this.logger = logger
    }

    fun info(evt: Evt, msg: String? = null, vararg fields: Pair<String, Any?>) {
        if (isReady) logger.info(evt, msg, *fields)
    }

    private fun logcat(level: Level, line: String) {
        when (level) {
            Level.DEBUG -> Log.d(TAG, line)
            Level.INFO -> Log.i(TAG, line)
            Level.WARN -> Log.w(TAG, line)
            Level.ERROR -> Log.e(TAG, line)
        }
    }
}
