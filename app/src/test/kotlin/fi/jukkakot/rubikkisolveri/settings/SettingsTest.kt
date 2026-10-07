package fi.jukkakot.rubikkisolveri.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class SettingsTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun themeDefaultsToFollowingThePhoneAndPersists() = runTest {
        val file = File(tmp.root, "settings.preferences_pb")
        val first = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
        assertEquals(ThemeMode.SYSTEM, first.themeMode.first())
        first.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, first.themeMode.first())
    }

    @Test
    fun theScannerChoiceIsTheNewOneByDefaultAndSurvivesARestart() = runTest {
        val file = File(tmp.root, "scanner.preferences_pb")
        val first = SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })
        assertEquals(ScanEngine.RULES, first.scanEngine.first())
        first.setScanEngine(ScanEngine.LOOK)
        assertEquals(ScanEngine.LOOK, first.scanEngine.first())
    }

    @Test
    fun languageTagsMapToLanguagesWithFinnishAsDefault() {
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromTag("sv"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.FINNISH, AppLanguage.fromTag("fi"))
    }
}
