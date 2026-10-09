package fi.jukkakot.rubikkisolveri.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fi.jukkakot.rubikkisolveri.ui.guide.HandsfreeSpeed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** User settings kept on the phone. The language is stored by the system (per-app language). */
class SettingsRepository(private val store: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.settingsStore)

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    /** Show the standard move notation (R, U', F2) under the words; off by default. */
    val showNotation: Flow<Boolean> = store.data.map { it[NOTATION] ?: false }

    suspend fun setShowNotation(show: Boolean) {
        store.edit { it[NOTATION] = show }
    }

    /** The handsfree guide's speed, remembered between solves. */
    val handsfreeSpeed: Flow<HandsfreeSpeed> = store.data.map { HandsfreeSpeed.fromName(it[HANDSFREE_SPEED]) }

    suspend fun setHandsfreeSpeed(speed: HandsfreeSpeed) {
        store.edit { it[HANDSFREE_SPEED] = speed.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val NOTATION = booleanPreferencesKey("show_notation")
        val HANDSFREE_SPEED = stringPreferencesKey("handsfree_speed")
    }
}

/** Reads and changes the per-app language through AppCompat (also stored on Android 12). */
object LanguageSetting {
    fun current(): AppLanguage = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales()[0]?.language)

    /** Sets Finnish on the very first start, so a phone in English still starts in Finnish. */
    fun ensureDefault() {
        if (AppCompatDelegate.getApplicationLocales().isEmpty) apply(AppLanguage.DEFAULT)
    }

    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
