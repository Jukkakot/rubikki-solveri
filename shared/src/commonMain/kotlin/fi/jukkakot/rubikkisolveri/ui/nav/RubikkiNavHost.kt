package fi.jukkakot.rubikkisolveri.ui.nav

import kotlin.time.Clock
import fi.jukkakot.rubikkisolveri.ui.scan.LastScan
import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.scan.ScanCheck
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.ui.scan.ScanScreen
import fi.jukkakot.rubikkisolveri.ui.scan.VideoScanScreen
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
import fi.jukkakot.rubikkisolveri.ui.guide.HandsfreeSpeed
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
import fi.jukkakot.rubikkisolveri.log.NoScanRecordings
import fi.jukkakot.rubikkisolveri.log.ScanRecordingStore
import fi.jukkakot.rubikkisolveri.ui.scan.ScanRecordingSetup
import fi.jukkakot.rubikkisolveri.ui.home.HomeEntry
import fi.jukkakot.rubikkisolveri.cube.SolveTarget
import fi.jukkakot.rubikkisolveri.ui.target.TargetScreen
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
    val handsfreeSpeed: HandsfreeSpeed = HandsfreeSpeed.NORMAL,
    val onHandsfreeSpeed: (HandsfreeSpeed) -> Unit = {},
    val progress: ProgressRepository = InMemoryProgressRepository(),
    /** The idle spin of the home cube; off in tests, whose clock would never go idle. */
    val homeSpin: Boolean = true,
    val scanPictures: ScanPictureStore = NoScanPictures,
    /** The video scans' recordings, kept with the log (`scan-recording`). */
    val scanRecordings: ScanRecordingStore = NoScanRecordings,
    /** Hide the scan's marks on the camera picture, for clean screen recordings. */
    val hideScanMarks: Boolean = false,
    val onHideScanMarks: (Boolean) -> Unit = {},
    val platform: Platform = Platform.ANDROID,
)

@Composable
fun RubikkiNavHost(navController: NavHostController, actions: AppActions) {
    val scope = rememberCoroutineScope()
    // A chosen target opens its solution: in place of the solution screen the picker came from,
    // or (from home) on top of the picker.
    fun chooseTarget(start: String, target: SolveTarget, fromSolve: Boolean) {
        // A scanned cube stays a scanned cube with a new target (its check is still the scan's).
        val solve = navController.currentBackStack.value.lastOrNull { it.destination.hasRoute<SolveRoute>() }
        val scanned = fromSolve && solve?.toRoute<SolveRoute>()?.fromScan == true
        navController.navigate(SolveRoute(start, target.encode(), fromScan = scanned)) {
            if (fromSolve) popUpTo<SolveRoute> { inclusive = true } else popUpTo<TargetRoute> { inclusive = false }
        }
    }
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                primary = HomeEntry(Res.string.home_scan_short, Res.drawable.ic_video, Res.string.home_scan) { navController.navigate(VideoScanRoute()) },
                entries = listOf(
                    HomeEntry(Res.string.home_manual_short, Res.drawable.ic_palette, Res.string.home_manual) { navController.navigate(ManualInputRoute()) },
                    HomeEntry(Res.string.home_learn, Res.drawable.ic_school) { navController.navigate(LessonsRoute) },
                    HomeEntry(Res.string.home_timer, Res.drawable.ic_timer) { navController.navigate(TimerRoute) },
                    HomeEntry(Res.string.home_free_short, Res.drawable.ic_cube, Res.string.home_free_cube) { navController.navigate(FreeCubeRoute()) },
                    HomeEntry(Res.string.home_patterns, Res.drawable.ic_pattern) { navController.navigate(TargetRoute(Cube.solved().toColorString())) },
                ),
                onOpenSettings = { navController.navigate(SettingsRoute) },
                version = actions.version,
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
                hideScanMarks = actions.hideScanMarks,
                onHideScanMarks = actions.onHideScanMarks,
                onOpenAbout = { navController.navigate(AboutRoute) },
                onOpenLog = { navController.navigate(LogRoute) },
                onBack = { navController.popBackStack() },
            )
        }
        composable<ManualInputRoute> { entry ->
            val route = entry.toRoute<ManualInputRoute>()
            if (route.targetStart != null) {
                ManualInputScreen(
                    onBack = { navController.popBackStack() },
                    onValid = { cube ->
                        if (route.targetFromSolve) {
                            chooseTarget(route.targetStart, SolveTarget.Painted(cube), fromSolve = true)
                        } else {
                            // From home the picker asks where the cube starts, so the target goes back there.
                            navController.previousBackStackEntry?.savedStateHandle?.set(PAINTED_KEY, SolveTarget.Painted(cube).encode())
                            navController.popBackStack()
                        }
                    },
                    initial = route.cube?.let(CubeEditor::decode) ?: CubeEditor.empty(),
                    title = Res.string.target_paint_title,
                    onScanFace = null,
                )
                return@composable
            }
            val initial = route.cube?.let(CubeEditor::decode) ?: CubeEditor.empty()
            val marked = route.marked?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
            // The colour check after a scan is part of the scan, so it stays dark like it.
            DarkIf(route.fromScan) { ManualInputScreen(
                onBack = { navController.popBackStack() },
                // A checked scan replaces the check (and a solution it was opened from), so going
                // back from the solution starts a new scan.
                onValid = { cube ->
                    if (route.fromScan || route.replaceSolve) {
                        navController.popBackStack()
                        if (navController.currentBackStackEntry?.destination?.hasRoute<SolveRoute>() == true) navController.popBackStack()
                    }
                    navController.navigate(SolveRoute(cube.toColorString(), route.target, fromScan = route.fromScan))
                },
                initial = initial,
                initialMarked = marked,
                title = if (route.fromScan) Res.string.check_title else Res.string.manual_title,
                note = when {
                    route.confident -> Res.string.check_note_ok
                    route.fromScan -> Res.string.check_note
                    else -> null
                },
                confident = route.confident,
                pictures = if (route.fromScan) LastScan.pictures else emptyMap(),
                onScanAgain = if (route.fromScan) {
                    { scanAgain(navController, route.target) }
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
            val route = entry.toRoute<ScanRoute>()
            val only = route.face?.let(FaceView::valueOf)
            ForcedDark { ScanScreen(
                onBack = { navController.popBackStack() },
                onManual = {
                    if (only != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(ManualInputRoute(target = route.target)) { popUpTo<ScanRoute> { inclusive = true } }
                    }
                },
                only = only,
                // The whole scan can switch to the video scan, which takes its place.
                onSwitch = if (only == null) {
                    { navController.navigate(VideoScanRoute(route.target)) { popUpTo<ScanRoute> { inclusive = true } } }
                } else {
                    null
                },
                onFace = { view, samples ->
                    LastScan.rescanned = view to samples
                    navController.popBackStack()
                },
                onResult = { outcome ->
                    LastScan.check = afterScanCheck(outcome, route.target)
                    navController.navigate(afterScan(outcome, route.target))
                },
                pictures = actions.scanPictures,
            ) }
        }
        composable<VideoScanRoute> { entry ->
            val route = entry.toRoute<VideoScanRoute>()
            ForcedDark { VideoScanScreen(
                onBack = { navController.popBackStack() },
                onManual = { navController.navigate(ManualInputRoute(target = route.target)) { popUpTo<VideoScanRoute> { inclusive = true } } },
                onSwitch = { navController.navigate(ScanRoute(target = route.target)) { popUpTo<VideoScanRoute> { inclusive = true } } },
                onResult = { outcome ->
                    // No face pictures from a video; the readings let the check re-read a rescanned face.
                    LastScan.pictures = emptyMap()
                    LastScan.readings = outcome.samples.ifEmpty { null }
                    LastScan.check = afterScanCheck(outcome, route.target)
                    navController.navigate(afterScan(outcome, route.target))
                },
                recording = ScanRecordingSetup(actions.scanRecordings, actions.platform.name.lowercase(), actions.version),
                hideMarks = actions.hideScanMarks,
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
            val route = entry.toRoute<SolveRoute>()
            SolveScreen(
                cube = Cube.fromColorString(route.cube),
                target = SolveTarget.decode(route.target),
                onChangeTarget = { navController.navigate(TargetRoute(route.cube, route.target, fromSolve = true)) },
                onBack = { navController.popBackStack() },
                onHome = { navController.popBackStack(HomeRoute, inclusive = false) },
                showNotation = actions.showNotation,
                handsfreeSpeed = actions.handsfreeSpeed,
                onHandsfreeSpeed = actions.onHandsfreeSpeed,
                onFinished = { method, moves, millis -> scope.launch { actions.progress.addGuided(method.name, moves, millis) } },
                startScreen = true,
                onCheckColors = {
                    // The check keeps the target now on the screen (it may have been changed here).
                    val check = LastScan.check?.takeIf { route.fromScan }?.copy(target = route.target)
                        ?: ManualInputRoute(CubeEditor.of(Cube.fromColorString(route.cube)).encode(), replaceSolve = true, target = route.target)
                    navController.navigate(check)
                },
            )
        }
        composable<TargetRoute> { entry ->
            val route = entry.toRoute<TargetRoute>()
            val start = Cube.fromColorString(route.start)
            val current = SolveTarget.decode(route.current)
            val painted by entry.savedStateHandle.getStateFlow<String?>(PAINTED_KEY, null).collectAsStateWithLifecycle()
            TargetScreen(
                start = start,
                current = current,
                onChoose = { chooseTarget(route.start, it, route.fromSolve) },
                onPaint = {
                    val from = current.cubeFor(start) ?: SolveTarget.Solved.cubeFor(start)!!
                    navController.navigate(ManualInputRoute(CubeEditor.of(from).encode(), targetStart = route.start, targetFromSolve = route.fromSolve))
                },
                onBack = { navController.popBackStack() },
                askStart = !route.fromSolve,
                onScan = { navController.navigate(VideoScanRoute(it.encode())) },
                painted = painted?.let(SolveTarget::decode),
                onPaintedSeen = { entry.savedStateHandle[PAINTED_KEY] = null },
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

/** A target painted from home's picker, handed back to the picker in its saved state. */
private const val PAINTED_KEY = "painted"

/**
 * Where a finished scan goes, on top of the scan (so going back starts a new scan): a sure scan
 * (valid, nothing uncertain or marked) to its solution, any other to the check next to the pictures.
 */
fun afterScan(outcome: ScanOutcome, target: String? = null): Any {
    val sure = outcome.editor.toCube()?.takeIf { outcome.isConfident && outcome.marked.isEmpty() }
    return sure?.let { SolveRoute(it.toColorString(), target, fromScan = true) } ?: afterScanCheck(outcome, target)
}

/**
 * The check of a finished scan: its colours, the uncertain stickers and those known only from the
 * rest of the cube marked. Also opened from the solution's menu.
 */
fun afterScanCheck(outcome: ScanOutcome, target: String? = null): ManualInputRoute = ManualInputRoute(
    outcome.editor.encode(),
    (outcome.marked + outcome.inferred).sorted().joinToString(","),
    fromScan = true,
    confident = false,
    target = target,
)

/** "Scan again" from the check: back to the scan under it, or a new video scan in place of the check. */
private fun scanAgain(navController: NavHostController, target: String?) {
    navController.popBackStack()
    val under = navController.currentBackStackEntry?.destination
    if (under?.hasRoute<VideoScanRoute>() == true || under?.hasRoute<ScanRoute>() == true) return
    navController.navigate(VideoScanRoute(target))
}
