package fi.jukkakot.rubikkisolveri.web

import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.log.Level
import fi.jukkakot.rubikkisolveri.log.Logger
import fi.jukkakot.rubikkisolveri.store.FallbackKeyValueStore
import fi.jukkakot.rubikkisolveri.store.KeyValueStore
import fi.jukkakot.rubikkisolveri.store.StoreKeys
import fi.jukkakot.rubikkisolveri.store.StoredLog
import fi.jukkakot.rubikkisolveri.store.StoredProgressRepository
import fi.jukkakot.rubikkisolveri.store.StoredScanPictures
import fi.jukkakot.rubikkisolveri.store.StoredSettings
import fi.jukkakot.rubikkisolveri.ui.BrowserHooks
import kotlin.time.Clock

/** localStorage through platform.mjs. */
private object BrowserStorage : KeyValueStore {
    override fun get(key: String) = storageGet(key)
    override fun set(key: String, value: String) = storageSet(key, value)
    override fun remove(key: String) = storageRemove(key)
}

/** Everything the browser app keeps and the hooks the shared screens call; built once in [main]. */
class WebServices {
    private var storageWarning: Throwable? = null
    val store: KeyValueStore = FallbackKeyValueStore(BrowserStorage) { storageWarning = it }
    val settings = StoredSettings(store)
    val progress = StoredProgressRepository(store)
    val logStore = StoredLog(store)
    val scanPictures = StoredScanPictures(store, ::encodePngBase64) { Clock.System.now().toEpochMilliseconds() }
    val logger = Logger(logStore, { level, line -> log(level.name, line) }, { it() })

    /** True once if the previous visit ended in an uncaught error; clears the mark. */
    val crashedLastTime: Boolean = store.get(StoreKeys.CRASHED) != null

    init {
        AppLog.install(logger)
        store.remove(StoreKeys.CRASHED)
        persistStorage()
        var reloadingForGraphics = false
        installCrashHooks { report ->
            logger.writeNow(Level.ERROR, Evt.APP_CRASH, report.substringBefore('\n'), "stack" to report.substringAfter('\n', ""))
            // The engine fails on its first frame after lost graphics; the reload is the recovery.
            if (!reloadingForGraphics) store.set(StoreKeys.CRASHED, Clock.System.now().toEpochMilliseconds().toString())
        }
        installGraphicsLostHook { willReload ->
            reloadingForGraphics = willReload
            logger.writeNow(Level.WARN, Evt.APP_GRAPHICS_LOST, if (willReload) "reloading when visible" else "lost again soon after a reload")
        }
        BrowserHooks.reducedMotion = ::reducedMotion
        BrowserHooks.keepScreenOn = ::keepScreenOn
        BrowserHooks.formatDateTime = { millis, language, timeOnly -> formatDateTime(millis.toDouble(), language, timeOnly) }
        installCamera()
        storageWarning?.let { logger.warn(Evt.APP_START, "storage unavailable, keeping data in memory: ${it.message}") }
    }
}
