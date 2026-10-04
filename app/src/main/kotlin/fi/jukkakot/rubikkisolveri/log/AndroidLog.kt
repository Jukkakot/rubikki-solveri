package fi.jukkakot.rubikkisolveri.log

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "Rubikki"

private val executor by lazy { Executors.newSingleThreadExecutor { Thread(it, "app-log").apply { isDaemon = true } } }

/** Sets up the phone's logger: Logcat at once, the file on a background thread. Once per process. */
fun AppLog.init(context: Context): Logger {
    val file = LogFile(File(context.filesDir, "logs/app.log"))
    val logger = Logger(file, ::logcat, { executor.execute(it) }, awaitPosted = ::awaitExecutor)
    install(logger)
    return logger
}

/** The phone's log file behind a logger set up by [init]. */
val Logger.file: LogFile get() = store as LogFile

private fun awaitExecutor() {
    val done = CountDownLatch(1)
    executor.execute { done.countDown() }
    done.await(2, TimeUnit.SECONDS)
}

private fun logcat(level: Level, line: String) {
    when (level) {
        Level.DEBUG -> Log.d(TAG, line)
        Level.INFO -> Log.i(TAG, line)
        Level.WARN -> Log.w(TAG, line)
        Level.ERROR -> Log.e(TAG, line)
    }
}
