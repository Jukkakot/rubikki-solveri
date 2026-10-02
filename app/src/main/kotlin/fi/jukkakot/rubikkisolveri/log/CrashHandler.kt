package fi.jukkakot.rubikkisolveri.log

import java.io.File

/**
 * Writes an uncaught error to the log (synchronously, the process is about to die), leaves a
 * [marker] so the next start can tell the user, then hands over to the [previous] handler.
 */
class CrashHandler(
    private val logger: Logger,
    private val marker: File,
    private val previous: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, error: Throwable) {
        runCatching {
            logger.writeNow(
                Level.ERROR, Evt.APP_CRASH, error.toString(),
                "thread" to thread.name, "stack" to LogLine.stackOf(error),
            )
            marker.parentFile?.mkdirs()
            marker.writeText(System.currentTimeMillis().toString())
        }
        previous?.uncaughtException(thread, error)
    }

    companion object {
        /** True once if the app crashed since the last call; clears the marker. */
        fun consumeCrashMarker(marker: File): Boolean = marker.exists() && marker.delete()
    }
}
