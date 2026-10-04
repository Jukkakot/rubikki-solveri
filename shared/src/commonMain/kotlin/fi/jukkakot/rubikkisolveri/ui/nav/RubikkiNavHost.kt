package fi.jukkakot.rubikkisolveri.ui.nav

import kotlin.time.Clock
import fi.jukkakot.rubikkisolveri.ui.scan.LastScan
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.scan.ScanCheck
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
import fi.jukkakot.rubikkisolveri.ui.theme.DarkIf
import fi.jukkakot.rubikkisolveri.ui.theme.ForcedDark
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
import org.jetbrains.compose.resources.stringResource
import kotlin.random.Random
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.progress.InMemoryProgressRepository
import fi.jukkakot.rubikkisolveri.progress.ProgressRepository
import fi.jukkakot.rubikkisolveri.ui.progress.HistoryScreen
import fi.jukkakot.rubikkisolveri.ui.progress.TimerScreen
import kotlinx.coroutines.launch
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.settings.AppLanguage
import fi.jukkakot.rubikkisolveri.settings.ThemeMode
import fi.jukkakot.rubikkisolveri.Platform
import fi.jukkakot.rubikkisolveri.log.NoScanPictures
import fi.jukkakot.rubikkisolveri.log.ScanPictureStore
import fi.jukkakot.rubikkisolveri.ui.home.HomeEntry
import fi.jukkakot.rubikkisolveri.ui.home.HomeSummary
import fi.jukkakot.rubikkisolveri.ui.home.HomeScreen
import fi.jukkakot.rubikkisolveri.ui.log.LogScreen
import fi.jukkakot.rubikkisolveri.ui.settings.SettingsScreen
import fi.jukkakot.rubikkisolveri.ui.settings.AboutScreen

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
    /** The idle spin of the home cube; off in tests, whose clock would never go idle. */
    val homeSpin: Boolean = true,
    val scanPictures: ScanPictureStore = NoScanPictures,
    val platform: Platform = Platform.ANDROID,
)

@Composable
fun RubikkiNavHost(navController: NavHostController, actions: AppActions) {
    val scope = rememberCoroutineScope()
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            val timed by actions.progress.timedSolves.collectAsStateWithLifecycle(emptyList())
            HomeScreen(
                primary = HomeEntry(Res.string.home_scan, Res.drawable.ic_camera) { navController.navigate(ScanRoute()) },
                entries = listOf(
                    HomeEntry(Res.string.home_manual, Res.drawable.ic_palette) { navController.navigate(ManualInputRoute()) },
                    HomeEntry(Res.string.home_learn, Res.drawable.ic_school) { navController.navigate(LessonsRoute) },
                    HomeEntry(Res.string.home_timer, Res.drawable.ic_timer) { navController.navigate(TimerRoute) },
                    HomeEntry(Res.string.home_free_cube, Res.drawable.ic_cube) { navController.navigate(FreeCubeRoute()) },
                ),
                onOpenSettings = { navController.navigate(SettingsRoute) },
                version = actions.version,
                summary = HomeSummary.of(timed.map { it.result }),
                spin = actions.homeSpin,
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
                onOpenAbout = { navController.navigate(AboutRoute) },
                onOpenLog = { navController.navigate(LogRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<ManualInputRoute> { entry ->
            val route = entry.toRoute<ManualInputRoute>()
            val initial = route.cube?.let(CubeEditor::decode) ?: CubeEditor.empty()
            val marked = route.marked?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
            // The colour check after a scan is part of the scan, so it stays dark like it.
            DarkIf(route.fromScan) { ManualInputScreen(
                onBack = { navController.popBackStack() },
                onValid = { cube -> navController.navigate(SolveRoute(cube.toColorString())) },
                initial = initial,
                initialMarked = marked,
                title = if (route.fromScan) Res.string.check_title else Res.string.manual_title,
                note = when {
                    route.confident -> Res.string.check_note_ok
                    route.fromScan -> Res.string.check_note
                    else -> null
                },
                autoContinue = route.confident,
                pictures = if (route.fromScan) LastScan.pictures else emptyMap(),
                onScanAgain = if (route.fromScan) {
                    { navController.navigate(ScanRoute()) { popUpTo<ManualInputRoute> { inclusive = true } } }
                } else {
                    null
                },
                check = if (route.fromScan) ScanCheck.start(initial, marked, LastScan.readings) else null,
                onScanFace = { view -> navController.navigate(ScanRoute(view.name)) },
                rescanned = LastScan.rescanned,
                onRescanUsed = { LastScan.rescanned = null },
                onReadings = { LastScan.readings = it },
                onRescanTurned = { face, turns -> LastScan.turnPicture(face, turns) },
            ) }
        }
        composable<ScanRoute> { entry ->
            val only = entry.toRoute<ScanRoute>().face?.let(FaceView::valueOf)
            ForcedDark { ScanScreen(
                onBack = { navController.popBackStack() },
                onManual = {
                    if (only != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(ManualInputRoute()) { popUpTo<ScanRoute> { inclusive = true } }
                    }
                },
                only = only,
                onFace = { view, samples ->
                    LastScan.rescanned = view to samples
                    navController.popBackStack()
                },
                onResult = { outcome ->
                    // Always the check next to the pictures; a sure scan goes on to the solution by itself.
                    val next = ManualInputRoute(
                        outcome.editor.encode(),
                        outcome.marked.joinToString(","),
                        fromScan = true,
                        confident = outcome.isConfident,
                    )
                    navController.navigate(next) { popUpTo<ScanRoute> { inclusive = true } }
                },
                pictures = actions.scanPictures,
            ) }
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
                title = stringResource(Res.string.scramble_title),
                practice = true,
                finishedText = stringResource(Res.string.scramble_done),
                homeLabel = stringResource(Res.string.scramble_back),
            )
        }
        composable<LessonRoute> { entry ->
            LessonScreen(
                index = entry.toRoute<LessonRoute>().index,
                onBack = { navController.popBackStack() },
                onPractice = { stage -> navController.navigate(PracticeRoute(stage.ordinal, Clock.System.now().toEpochMilliseconds())) },
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
                title = stringResource(Res.string.practice_title, stringResource(stageName(stage))),
                practice = true,
                finishedText = stringResource(Res.string.practice_done),
                homeLabel = stringResource(Res.string.practice_new),
                onFinished = { _, _, millis -> scope.launch { actions.progress.addPractice(route.stage, millis) } },
            )
        }
        composable<AboutRoute> {
            AboutScreen(actions.version, onBack = { navController.popBackStack() }, platform = actions.platform)
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
