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
import fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod
import fi.jukkakot.rubikkisolveri.ui.solve.SolvePlan
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonScreen
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonsScreen
import fi.jukkakot.rubikkisolveri.ui.common.stageName
import fi.jukkakot.rubikkisolveri.cube.beginner.Practice
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import androidx.compose.ui.res.stringResource
import kotlin.random.Random
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.progress.InMemoryProgressRepository
import fi.jukkakot.rubikkisolveri.progress.ProgressRepository
import fi.jukkakot.rubikkisolveri.ui.progress.HistoryScreen
import fi.jukkakot.rubikkisolveri.ui.progress.TimerScreen
import kotlinx.coroutines.launch
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
    val showNotation: Boolean = false,
    val onShowNotation: (Boolean) -> Unit = {},
    val progress: ProgressRepository = InMemoryProgressRepository(),
)

@Composable
fun RubikkiNavHost(navController: NavHostController, actions: AppActions) {
    val scope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                entries = listOf(
                    HomeEntry(R.string.home_scan, onOpen = { navController.navigate(ScanRoute) }),
                    HomeEntry(R.string.home_manual, onOpen = { navController.navigate(ManualInputRoute()) }),
                    HomeEntry(R.string.home_learn, onOpen = { navController.navigate(LessonsRoute) }),
                    HomeEntry(R.string.home_timer, onOpen = { navController.navigate(TimerRoute) }),
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
                showNotation = actions.showNotation,
                onShowNotation = actions.onShowNotation,
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
                showNotation = actions.showNotation,
                onFinished = { method, moves, millis -> scope.launch { actions.progress.addGuided(method.name, moves, millis) } },
            )
        }
        composable<LessonsRoute> {
            val counts by actions.progress.practiceCounts.collectAsStateWithLifecycle(emptyMap())
            LessonsScreen(onOpen = { navController.navigate(LessonRoute(it)) }, onBack = { navController.popBackStack() }, practiceCounts = counts)
        }
        composable<TimerRoute> {
            TimerScreen(
                progress = actions.progress,
                onBack = { navController.popBackStack() },
                onHistory = { navController.navigate(HistoryRoute) },
                onGuidedScramble = { moves -> navController.navigate(ScrambleGuideRoute(Notation.format(moves))) },
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(actions.progress, onBack = { navController.popBackStack() })
        }
        composable<ScrambleGuideRoute> { entry ->
            val moves = remember(entry) { Notation.parse(entry.toRoute<ScrambleGuideRoute>().moves) }
            SolveScreen(
                cube = Cube.solved(),
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack() },
                showNotation = actions.showNotation,
                planner = { _, _ -> SolvePlan.Ready(moves, null) },
                title = stringResource(R.string.scramble_title),
                practice = true,
                finishedText = stringResource(R.string.scramble_done),
                homeLabel = stringResource(R.string.scramble_back),
            )
        }
        composable<LessonRoute> { entry ->
            LessonScreen(
                index = entry.toRoute<LessonRoute>().index,
                onBack = { navController.popBackStack() },
                onPractice = { stage -> navController.navigate(PracticeRoute(stage.ordinal, System.currentTimeMillis())) },
                onFreeCube = { navController.navigate(FreeCubeRoute()) },
            )
        }
        composable<PracticeRoute> { entry ->
            val route = entry.toRoute<PracticeRoute>()
            val stage = Stage.entries[route.stage]
            val exercise = remember(route) { Practice.exercise(stage, Random(route.seed)) }
            SolveScreen(
                cube = exercise.position,
                onBack = { navController.popBackStack() },
                onHome = {
                    navController.navigate(PracticeRoute(route.stage, route.seed + 1)) {
                        popUpTo<PracticeRoute> { inclusive = true }
                    }
                },
                showNotation = actions.showNotation,
                planner = { _, _ -> SolvePlan.Ready(exercise.steps.flatMap { it.moves }, exercise.steps) },
                initialMethod = SolveMethod.LEARN,
                title = stringResource(R.string.practice_title, stringResource(stageName(stage))),
                practice = true,
                finishedText = stringResource(R.string.practice_done),
                homeLabel = stringResource(R.string.practice_new),
                onFinished = { _, _, millis -> scope.launch { actions.progress.addPractice(route.stage, millis) } },
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
