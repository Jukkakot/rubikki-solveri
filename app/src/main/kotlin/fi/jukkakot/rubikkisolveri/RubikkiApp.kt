package fi.jukkakot.rubikkisolveri

import android.app.Application
import android.os.Build
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.CrashHandler
import fi.jukkakot.rubikkisolveri.log.Evt
import java.io.File

class RubikkiApp : Application() {

    /** True when the previous run ended in a crash; read once by the first screen. */
    var crashedLastTime: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()
        val logger = AppLog.init(this)
        val marker = File(filesDir, "logs/crash.marker")
        crashedLastTime = CrashHandler.consumeCrashMarker(marker)
        Thread.setDefaultUncaughtExceptionHandler(
            CrashHandler(logger, marker, Thread.getDefaultUncaughtExceptionHandler()),
        )
        logger.info(
            Evt.APP_START, null,
            "ver" to BuildConfig.VERSION_NAME, "sdk" to Build.VERSION.SDK_INT, "device" to Build.MODEL,
            "crashedLastTime" to crashedLastTime,
        )
    }

    fun crashNoticeShown() {
        crashedLastTime = false
    }
}
