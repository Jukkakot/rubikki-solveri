package fi.jukkakot.rubikkisolveri.store

import fi.jukkakot.rubikkisolveri.log.LogStore
import fi.jukkakot.rubikkisolveri.log.ScanPictureStore
import fi.jukkakot.rubikkisolveri.settings.AppLanguage
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val JSON = Json { ignoreUnknownKeys = true }

/** Theme and notation as JSON under [StoreKeys.SETTINGS]; the language under its own key (read before the app starts). */
class StoredSettings(private val store: KeyValueStore) {
    @Serializable
    private data class Data(val theme: ThemeMode = ThemeMode.SYSTEM, val notation: Boolean = false)

    private val data = MutableStateFlow(
        store.get(StoreKeys.SETTINGS)?.let { runCatching { JSON.decodeFromString(Data.serializer(), it) }.getOrNull() } ?: Data(),
    )
    private val theme = MutableStateFlow(data.value.theme)
    private val notation = MutableStateFlow(data.value.notation)

    val themeMode: StateFlow<ThemeMode> = theme
    val showNotation: StateFlow<Boolean> = notation

    fun setThemeMode(mode: ThemeMode) = save(data.value.copy(theme = mode))
    fun setShowNotation(show: Boolean) = save(data.value.copy(notation = show))

    val language: AppLanguage get() = AppLanguage.fromTag(store.get(StoreKeys.LANGUAGE))
    fun setLanguage(language: AppLanguage) = store.set(StoreKeys.LANGUAGE, language.tag)

    private fun save(next: Data) {
        data.value = next
        theme.value = next.theme
        notation.value = next.notation
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
