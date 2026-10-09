package fi.jukkakot.rubikkisolveri.store

import fi.jukkakot.rubikkisolveri.log.LogStore
import fi.jukkakot.rubikkisolveri.log.ScanPictureStore
import fi.jukkakot.rubikkisolveri.log.ScanRecordingStore
import fi.jukkakot.rubikkisolveri.settings.AppLanguage
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import fi.jukkakot.rubikkisolveri.ui.guide.HandsfreeSpeed
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val JSON = Json { ignoreUnknownKeys = true }

/** Theme, notation, handsfree speed and hidden scan marks as JSON under [StoreKeys.SETTINGS]; the language under its own key (read before the app starts). */
class StoredSettings(private val store: KeyValueStore) {
    @Serializable
    private data class Data(
        val theme: ThemeMode = ThemeMode.SYSTEM,
        val notation: Boolean = false,
        val handsfreeSpeed: HandsfreeSpeed = HandsfreeSpeed.NORMAL,
        val hideScanMarks: Boolean = false,
    )

    private val data = MutableStateFlow(
        store.get(StoreKeys.SETTINGS)?.let { runCatching { JSON.decodeFromString(Data.serializer(), it) }.getOrNull() } ?: Data(),
    )
    private val theme = MutableStateFlow(data.value.theme)
    private val notation = MutableStateFlow(data.value.notation)
    private val speed = MutableStateFlow(data.value.handsfreeSpeed)
    private val hideMarks = MutableStateFlow(data.value.hideScanMarks)

    val themeMode: StateFlow<ThemeMode> = theme
    val showNotation: StateFlow<Boolean> = notation
    val handsfreeSpeed: StateFlow<HandsfreeSpeed> = speed
    val hideScanMarks: StateFlow<Boolean> = hideMarks

    fun setThemeMode(mode: ThemeMode) = save(data.value.copy(theme = mode))
    fun setShowNotation(show: Boolean) = save(data.value.copy(notation = show))
    fun setHandsfreeSpeed(next: HandsfreeSpeed) = save(data.value.copy(handsfreeSpeed = next))
    fun setHideScanMarks(hide: Boolean) = save(data.value.copy(hideScanMarks = hide))

    val language: AppLanguage get() = AppLanguage.fromTag(store.get(StoreKeys.LANGUAGE))
    fun setLanguage(language: AppLanguage) = store.set(StoreKeys.LANGUAGE, language.tag)

    private fun save(next: Data) {
        data.value = next
        theme.value = next.theme
        notation.value = next.notation
        speed.value = next.handsfreeSpeed
        hideMarks.value = next.hideScanMarks
        store.set(StoreKeys.SETTINGS, JSON.encodeToString(Data.serializer(), next))
    }
}

/** The log as newline-separated lines under [StoreKeys.LOG]; past [maxChars] the oldest go, leaving about half. */
class StoredLog(private val store: KeyValueStore, private val maxChars: Int = 512 * 1024) : LogStore {
    override fun append(line: String) {
        var text = (store.get(StoreKeys.LOG) ?: "") + line + "\n"
        if (text.length > maxChars) {
            val cut = text.indexOf('\n', text.length - maxChars / 2)
            text = if (cut < 0) "" else text.substring(cut + 1)
        }
        store.set(StoreKeys.LOG, text)
    }

    override fun readLines(): List<String> = (store.get(StoreKeys.LOG) ?: "").split('\n').filter { it.isNotEmpty() }

    override fun clear() = store.remove(StoreKeys.LOG)
}

/** A stored scan picture: its file name and the PNG as base64. */
@Serializable
data class StoredPicture(val name: String, val pngBase64: String)

/**
 * The newest [ScanPictureStore.KEEP] scan pictures as JSON under [StoreKeys.SCAN_PICTURES].
 * [encodePng] turns a square ARGB picture into base64 PNG (the platform's image encoder); [now]
 * gives the time for names.
 */
class StoredScanPictures(
    private val store: KeyValueStore,
    private val encodePng: (argb: IntArray, size: Int) -> String,
    private val now: () -> Long,
) : ScanPictureStore {
    private var counter = 0

    override fun newName(face: String): String = "${now()}-${(counter++).toString().padStart(3, '0')}-$face.png"

    override fun write(name: String, argb: IntArray, size: Int) {
        val kept = (list() + StoredPicture(name, encodePng(argb, size))).takeLast(ScanPictureStore.KEEP)
        store.set(StoreKeys.SCAN_PICTURES, JSON.encodeToString(LIST, kept))
    }

    /** The pictures, oldest first. */
    fun list(): List<StoredPicture> =
        store.get(StoreKeys.SCAN_PICTURES)?.let { runCatching { JSON.decodeFromString(LIST, it) }.getOrNull() }.orEmpty()

    override fun clear() = store.remove(StoreKeys.SCAN_PICTURES)

    private companion object {
        val LIST = kotlinx.serialization.builtins.ListSerializer(StoredPicture.serializer())
    }
}

/** A stored scan recording: its file name and text. */
@Serializable
data class StoredRecording(val name: String, val text: String)

/**
 * The newest [ScanRecordingStore.KEEP] scan recordings as JSON under [StoreKeys.SCAN_RECORDINGS]
 * (about 1.4 MB at most). [store] should be the browser's own storage, not the memory fallback: a
 * failing write (the quota) is told to [onFail] and the recording dropped, while the rest of the app
 * keeps its storage.
 */
class StoredScanRecordings(private val store: KeyValueStore, private val onFail: (Throwable) -> Unit = {}) : ScanRecordingStore {
    override fun write(name: String, text: String) {
        try {
            val kept = (list().filter { it.name != name } + StoredRecording(name, text)).sortedBy { it.name }.takeLast(ScanRecordingStore.KEEP)
            store.set(StoreKeys.SCAN_RECORDINGS, JSON.encodeToString(RECORDINGS, kept))
        } catch (e: Throwable) {
            onFail(e)
        }
    }

    /** The recordings, oldest first. */
    fun list(): List<StoredRecording> = try {
        store.get(StoreKeys.SCAN_RECORDINGS)?.let { runCatching { JSON.decodeFromString(RECORDINGS, it) }.getOrNull() }.orEmpty()
    } catch (e: Throwable) {
        emptyList()
    }

    override fun names(): List<String> = list().map { it.name }.reversed()

    override fun clear() {
        try {
            store.remove(StoreKeys.SCAN_RECORDINGS)
        } catch (e: Throwable) {
            onFail(e)
        }
    }

    private companion object {
        val RECORDINGS = kotlinx.serialization.builtins.ListSerializer(StoredRecording.serializer())
    }
}
