package fi.jukkakot.rubikkisolveri

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import fi.jukkakot.rubikkisolveri.log.AppLog
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.settings.LanguageSetting
import fi.jukkakot.rubikkisolveri.settings.SettingsRepository
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import fi.jukkakot.rubikkisolveri.ui.log.shareLogIntent
import fi.jukkakot.rubikkisolveri.ui.nav.AppActions
import fi.jukkakot.rubikkisolveri.ui.nav.RubikkiNavHost
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import kotlinx.coroutines.launch

// AppCompatActivity (not ComponentActivity) so that the per-app language applies on Android 12 too.
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        LanguageSetting.ensureDefault()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RubikkiApp
        val settings = SettingsRepository(this)
        val logger = AppLog.logger
        setContent {
            val themeMode by settings.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val showNotation by settings.showNotation.collectAsStateWithLifecycle(false)
            val scope = rememberCoroutineScope()
            val navController = rememberNavController()
            DisposableEffect(navController) {
                val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                    logger.info(Evt.NAV_SCREEN, null, "screen" to destination.route?.substringAfterLast('.'))
                }
                navController.addOnDestinationChangedListener(listener)
                onDispose { navController.removeOnDestinationChangedListener(listener) }
            }
            RubikkiTheme(mode = themeMode) {
                RubikkiNavHost(
                    navController,
                    AppActions(
                        themeMode = themeMode,
                        onThemeMode = { mode ->
                            logger.info(Evt.SETTINGS_CHANGED, null, "theme" to mode.name)
                            scope.launch { settings.setThemeMode(mode) }
                        },
                        language = LanguageSetting.current(),
                        onLanguage = { language ->
                            logger.info(Evt.SETTINGS_CHANGED, null, "language" to language.tag)
                            LanguageSetting.apply(language)
                        },
                        readLog = {
                            logger.flush()
                            logger.file.readLines()
                        },
                        clearLog = {
                            logger.flush()
                            logger.file.clear()
                            logger.info(Evt.LOG_CLEARED)
                        },
                        shareLog = {
                            logger.info(Evt.LOG_SHARED)
                            logger.flush()
                            startActivity(shareLogIntent(this, logger.file.file))
                        },
                        crashedLastTime = app.crashedLastTime,
                        onCrashNoticeShown = app::crashNoticeShown,
                        version = BuildConfig.VERSION_NAME,
                        showNotation = showNotation,
                        onShowNotation = { show ->
                            logger.info(Evt.SETTINGS_CHANGED, null, "notation" to show)
                            scope.launch { settings.setShowNotation(show) }
                        },
                    ),
                )
            }
        }
    }
}
