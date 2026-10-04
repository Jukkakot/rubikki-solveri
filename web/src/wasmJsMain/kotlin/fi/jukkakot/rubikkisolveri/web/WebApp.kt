package fi.jukkakot.rubikkisolveri.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToNavigation
import androidx.navigation.compose.rememberNavController
import fi.jukkakot.rubikkisolveri.ui.nav.HomeRoute
import kotlinx.browser.window
import fi.jukkakot.rubikkisolveri.Platform
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.res.Res
import fi.jukkakot.rubikkisolveri.res.version_built
import fi.jukkakot.rubikkisolveri.ui.LocalFormats
import fi.jukkakot.rubikkisolveri.ui.currentLanguage
import fi.jukkakot.rubikkisolveri.ui.elapsedMillis
import fi.jukkakot.rubikkisolveri.ui.nav.AppActions
import fi.jukkakot.rubikkisolveri.ui.nav.RubikkiNavHost
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** Wider than this, the app is a centred phone-width column. */
private val PHONE_WIDTH = 480.dp
private val WIDE_FROM = 600.dp

/** The shared app with the browser's services: storage, log, language by reload, vibration. */
@OptIn(ExperimentalBrowserHistoryApi::class)
@Composable
fun WebApp(services: WebServices) {
    val settings = services.settings
    val themeMode by settings.themeMode.collectAsState()
    val showNotation by settings.showNotation.collectAsState()
    val navController = rememberNavController()
    val language = currentLanguage()
    val built = LocalFormats.shortDateTime(BuildInfo.BUILT_AT, language)
    val version = BuildInfo.VERSION_NAME + " · " + stringResource(Res.string.version_built, built)
    var crashed by remember { mutableStateOf(services.crashedLastTime) }
    val logger = services.logger

    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener(
            NavController.OnDestinationChangedListener { _, destination, _ ->
                logger.info(Evt.NAV_SCREEN, null, "screen" to destination.route?.substringAfterLast('.'))
            },
        )
    }
    LaunchedEffect(navController) {
        // Browser back and reload follow the app's screens (routes in the URL fragment). A route
        // that cannot be restored falls back to the home screen. The NavHost is placed during
        // layout (BoxWithConstraints), so its graph exists only after the first frame.
        withFrameNanos { }
        try {
            window.bindToNavigation(navController)
        } catch (e: Throwable) {
            logger.warn(Evt.NAV_SCREEN, "restoring the route failed: ${e.message}")
            navController.navigate(HomeRoute) { popUpTo(0) }
            window.bindToNavigation(navController)
        }
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        hideLoading()
        log("INFO", "RUBIKKI ready")
        // The solver's tables take a moment to build; after the home screen is up, not before it.
        delay(1_500)
        val start = elapsedMillis()
        TwoPhaseSolver.warmUp()
        logger.info(Evt.SOLVER_READY, null, "ms" to elapsedMillis() - start)
    }

    RubikkiTheme(mode = themeMode, dynamicColor = false) {
        val background = MaterialTheme.colorScheme.surfaceContainer
        LaunchedEffect(background) { setThemeColor(cssColor(background.toArgb())) }
        CompositionLocalProvider(LocalHapticFeedback provides BrowserHaptics) {
            BoxWithConstraints(Modifier.fillMaxSize().background(background), contentAlignment = Alignment.TopCenter) {
                val column = if (maxWidth > WIDE_FROM) Modifier.widthIn(max = PHONE_WIDTH).fillMaxHeight() else Modifier.fillMaxSize()
                Box(column) {
                    RubikkiNavHost(
                        navController,
                        AppActions(
                            themeMode = themeMode,
                            onThemeMode = { mode ->
                                logger.info(Evt.SETTINGS_CHANGED, null, "theme" to mode.name)
                                settings.setThemeMode(mode)
                            },
                            language = settings.language,
                            onLanguage = { lang ->
                                if (lang != settings.language) {
                                    logger.info(Evt.SETTINGS_CHANGED, null, "language" to lang.tag)
                                    settings.setLanguage(lang)
                                    reload()
                                }
                            },
                            readLog = { services.logStore.readLines() },
                            clearLog = {
                                services.logStore.clear()
                                services.scanPictures.clear()
                                logger.info(Evt.LOG_CLEARED)
                            },
                            shareLog = {
                                logger.info(Evt.LOG_SHARED)
                                shareLog(services)
                            },
                            crashedLastTime = crashed,
                            onCrashNoticeShown = { crashed = false },
                            version = version,
                            showNotation = showNotation,
                            onShowNotation = { show ->
                                logger.info(Evt.SETTINGS_CHANGED, null, "notation" to show)
                                settings.setShowNotation(show)
                            },
                            progress = services.progress,
                            scanPictures = services.scanPictures,
                            platform = Platform.WEB,
                        ),
                    )
                }
            }
        }
    }
}

private fun cssColor(argb: Int): String = "#" + (argb and 0xFFFFFF).toString(16).padStart(6, '0')

