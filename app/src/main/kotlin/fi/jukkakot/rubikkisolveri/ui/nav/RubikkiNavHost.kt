package fi.jukkakot.rubikkisolveri.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.ui.scan.ScanScreen
import fi.jukkakot.rubikkisolveri.ui.free.FreeCubeScreen
import fi.jukkakot.rubikkisolveri.ui.manual.ManualInputScreen
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.settings.AppLanguage
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import fi.jukkakot.rubikkisolveri.ui.home.HomeEntry
import fi.jukkakot.rubikkisolveri.ui.home.HomeScreen
import fi.jukkakot.rubikkisolveri.ui.log.LogScreen
import fi.jukkakot.rubikkisolveri.ui.settings.SettingsScreen

/** What the screens need from the app; tests pass fakes. */
class AppActions(
    val themeMode: ThemeMode,
    val onThemeMode: (ThemeMode) -> Unit,
    val language: AppLanguage,
    val onLanguage: (AppLanguage) -> Unit,
    val readLog: () -> List<String>,
    val clearLog: () -> Unit,
    val shareLog: () -> Unit,
    val crashedLastTime: Boolean,
    val onCrashNoticeShown: () -> Unit,
    val version: String,
)

@Composable
fun RubikkiNavHost(navController: NavHostController, actions: AppActions) {
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                entries = listOf(
                    HomeEntry(R.string.home_scan, onOpen = { navController.navigate(ScanRoute) }),
                    HomeEntry(R.string.home_manual, onOpen = { navController.navigate(ManualInputRoute()) }),
                    HomeEntry(R.string.home_free_cube, onOpen = { navController.navigate(FreeCubeRoute()) }),
                ),
                onOpenSettings = { navController.navigate(SettingsRoute) },
                crashedLastTime = actions.crashedLastTime,
                onShowLog = { navController.navigate(LogRoute) },
                onCrashNoticeShown = actions.onCrashNoticeShown,
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                language = actions.language,
                themeMode = actions.themeMode,
                version = actions.version,
                onLanguage = actions.onLanguage,
                onThemeMode = actions.onThemeMode,
                onOpenLog = { navController.navigate(LogRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<ManualInputRoute> { entry ->
            val route = entry.toRoute<ManualInputRoute>()
            ManualInputScreen(
                onBack = { navController.popBackStack() },
                onValid = { cube -> navController.navigate(SolveRoute(cube.toColorString())) },
                initial = route.cube?.let(CubeEditor::decode) ?: CubeEditor.empty(),
                initialMarked = route.marked?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty(),
                title = if (route.fromScan) R.string.check_title else R.string.manual_title,
                note = if (route.fromScan) R.string.check_note else null,
            )
        }
        composable<ScanRoute> {
            ScanScreen(
                onBack = { navController.popBackStack() },
                onManual = {
                    navController.navigate(ManualInputRoute()) { popUpTo(ScanRoute) { inclusive = true } }
                },
                onResult = { outcome ->
                    val next: Any = if (outcome.isConfident) {
                        SolveRoute(outcome.editor.toCube()!!.toColorString())
                    } else {
                        ManualInputRoute(outcome.editor.encode(), outcome.marked.joinToString(","), fromScan = true)
                    }
                    navController.navigate(next) { popUpTo(ScanRoute) { inclusive = true } }
                },
            )
        }
        composable<FreeCubeRoute> { entry ->
            val route = entry.toRoute<FreeCubeRoute>()
            FreeCubeScreen(
                start = route.cube?.let(Cube::fromColorString) ?: Cube.solved(),
                onBack = { navController.popBackStack() },
                onSolve = { cube -> navController.navigate(SolveRoute(cube.toColorString())) },
            )
        }
        composable<SolveRoute> { entry ->
            SolveScreen(
                cube = Cube.fromColorString(entry.toRoute<SolveRoute>().cube),
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<LogRoute> {
            var lines by remember { mutableStateOf(actions.readLog()) }
            LogScreen(
                lines = lines,
                onShare = actions.shareLog,
                onClear = {
                    actions.clearLog()
                    lines = emptyList()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
