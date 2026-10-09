package fi.jukkakot.rubikkisolveri.store

/** Text values by key: the browser's localStorage, or a map in tests. */
interface KeyValueStore {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}

class MapKeyValueStore(val map: MutableMap<String, String> = mutableMapOf()) : KeyValueStore {
    override fun get(key: String) = map[key]
    override fun set(key: String, value: String) {
        map[key] = value
    }
    override fun remove(key: String) {
        map.remove(key)
    }
}

/**
 * [inner] until it fails once (blocked storage, private mode, quota), then memory only: the app
 * keeps working for the session. [onFail] is told once.
 */
class FallbackKeyValueStore(private val inner: KeyValueStore, private val onFail: (Throwable) -> Unit) : KeyValueStore {
    private var memory: MapKeyValueStore? = null

    private inline fun <T> guarded(fallback: (MapKeyValueStore) -> T, block: () -> T): T {
        memory?.let { return fallback(it) }
        return try {
            block()
        } catch (e: Throwable) {
            onFail(e)
            fallback(MapKeyValueStore().also { memory = it })
        }
    }

    override fun get(key: String): String? = guarded({ it.get(key) }) { inner.get(key) }
    override fun set(key: String, value: String) = guarded({ it.set(key, value) }) { inner.set(key, value) }
    override fun remove(key: String) = guarded({ it.remove(key) }) { inner.remove(key) }
}

/** The keys the browser version uses (versioned, so a format change can start fresh). */
object StoreKeys {
    const val SETTINGS = "rubikki.settings.v1"
    const val PROGRESS = "rubikki.progress.v1"
    const val LOG = "rubikki.log.v1"
    const val SCAN_PICTURES = "rubikki.scanpics.v1"
    const val SCAN_RECORDINGS = "rubikki.scanrecordings.v1"
    const val LANGUAGE = "rubikki.language"
    const val CRASHED = "rubikki.crashed"
}
