package fi.jukkakot.rubikkisolveri.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import androidx.compose.material3.Surface
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.ui.free.FreeCubeScreen
import fi.jukkakot.rubikkisolveri.ui.manual.ManualInputScreen
import fi.jukkakot.rubikkisolveri.ui.cube3d.Cube3D
import fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import fi.jukkakot.rubikkisolveri.ui.theme.ForcedDark
import androidx.compose.runtime.Composable
import fi.jukkakot.rubikkisolveri.R
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders key screens to PNG files in app/build/screenshots so they can be looked at without a
 * phone. Not a pixel comparison: it only fails when rendering crashes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "fi-w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    /**
     * Renders [content] in the light and the dark theme to `<name>-light.png` and `<name>-dark.png`
     * (one composition; the theme switches in between, so the screen keeps its state).
     */
    private fun shot(name: String, waitForText: String? = null, content: @Composable () -> Unit) {
        var dark by mutableStateOf(false)
        compose.setContent {
            RubikkiTheme(systemDark = dark, dynamicColor = false) { Surface(Modifier.fillMaxSize()) { content() } }
        }
        if (waitForText != null) {
            // Background work (the solver) runs on real threads: poll in real time.
            var tries = 0
            while (tries++ < 100 && compose.onAllNodesWithText(waitForText, substring = true).fetchSemanticsNodes().isEmpty()) {
                Thread.sleep(100)
                compose.mainClock.advanceTimeBy(100)
            }
        }
        val dir = File("build/screenshots").apply { mkdirs() }
        for (theme in listOf("light", "dark")) {
            dark = theme == "dark"
            compose.mainClock.advanceTimeBy(2000)
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            File(dir, "$name-$theme.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    @Test
    fun freeCube() = shot("free-cube") { FreeCubeScreen(start = Cube.solved().apply("R U R' U'"), onBack = {}) }

    @Test
    fun manualInput() = shot("manual-input") {
        ManualInputScreen(onBack = {}, onValid = {}, initial = CubeEditor.of(Cube.solved().apply("R U F")).paint(0, null))
    }

    /** A picture like the scan's: nine stickers in [cube] colours (letters) with dark gaps. */
    private fun stickerPicture(cube: String): IntArray = IntArray(120 * 120) { i ->
        val x = i % 120
        val y = i / 120
        if (x % 40 < 4 || x % 40 >= 36 || y % 40 < 4 || y % 40 >= 36) {
            0xff151515.toInt()
        } else {
            val c = fi.jukkakot.rubikkisolveri.ui.cube3d.StickerColors.of(fi.jukkakot.rubikkisolveri.cube.CubeColor.fromLetter(cube[(y / 40) * 3 + x / 40]))
            (0xff shl 24) or ((c.red * 255).toInt() shl 16) or ((c.green * 255).toInt() shl 8) or (c.blue * 255).toInt()
        }
    }

    /** The phone scan of 2026-10-03 08:17 (overexposed, invalid). */
    private val phoneScan = CubeEditor.decode("YWYGWWGYOWYGRRBRRBYBBWGGROOBOWBYYRRRWROYOOBGGGBOGBWYOW")!!

    /** Camera readings that match [phoneScan]'s colours. */
    private val phoneReadings = phoneScan.colors.map { fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier.DEFAULT_PALETTE.getValue(it!!) }

    @Composable
    private fun CheckOf(marked: Set<Int>) = ManualInputScreen(
        onBack = {}, onValid = {},
        initial = phoneScan,
        initialMarked = marked,
        title = R.string.check_title,
        note = R.string.check_note,
        pictures = mapOf(fi.jukkakot.rubikkisolveri.cube.Face.U to stickerPicture(cube = "YWYGWWGYO")),
        onScanAgain = {},
        check = fi.jukkakot.rubikkisolveri.cube.scan.ScanCheck.start(phoneScan, marked, phoneReadings),
        onScanFace = {},
    )

    /** The check after an unsure scan: two faces left to check. */
    @Test
    fun scanCheck() = shot("scan-check") { ForcedDark { CheckOf(setOf(8, 9, 20)) } }

    /** Every face found right but the cube cannot be: the faces to look at again. */
    @Test
    fun scanCheckVerdict() = shot("scan-check-verdict", waitForText = "Tällaista kuutiota") { ForcedDark { CheckOf(emptySet()) } }

    /** Rescanning one face from the check. */
    @Test
    fun scanOneFace() = shot("scan-one-face") { ForcedDark {
        val top = Cube.solved().apply("R U F'").let { cube ->
            (1..9).map { fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier.DEFAULT_PALETTE.getValue(cube.colorAt(fi.jukkakot.rubikkisolveri.cube.Face.U, it)) }
        }
        fi.jukkakot.rubikkisolveri.ui.scan.ScanContent(
            kotlinx.coroutines.flow.MutableStateFlow(top), torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = {},
            only = fi.jukkakot.rubikkisolveri.cube.FaceView.TOP,
            preview = { androidx.compose.foundation.layout.Box(it.then(Modifier.background(androidx.compose.ui.graphics.Color(0xFF3A3530)))) },
        )
    } }

    @Test
    fun solve() = shot("solve", waitForText = "Siirto 1/") {
        SolveScreen(Cube.solved().apply("R U R' F2 D L' B U2"), onBack = {}, onHome = {}, planner = INLINE_PLANNER)
    }

    @Test
    fun guideBack() = shot("guide-back", waitForText = "Siirto 1/") {
        SolveScreen(Cube.solved().apply("B'"), onBack = {}, onHome = {}, showNotation = true, planner = INLINE_PLANNER)
    }

    @Test
    fun guideRight() = shot("guide-right", waitForText = "Siirto 1/") {
        SolveScreen(Cube.solved().apply("R2 U' R"), onBack = {}, onHome = {}, planner = INLINE_PLANNER)
    }

    @Test
    fun follow() = shot("follow") {
        val start = Cube.solved().apply("R'")
        val state = fi.jukkakot.rubikkisolveri.ui.guide.rememberStepperState(start, Notation.parse("R"))
        val frames = kotlinx.coroutines.flow.MutableStateFlow(
            fi.jukkakot.rubikkisolveri.cube.follow.front(start).map { fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier.DEFAULT_PALETTE.getValue(it) },
        )
        androidx.compose.foundation.layout.Column(Modifier.background(androidx.compose.ui.graphics.Color.White)) {
            fi.jukkakot.rubikkisolveri.ui.guide.FollowPanel(state, frames) {
                androidx.compose.foundation.layout.Box(it.then(Modifier.background(androidx.compose.ui.graphics.Color(0xFF3A3530))))
            }
        }
    }

    @Test
    fun learn() = shot("solve-learn", waitForText = "Vaihe ") {
        SolveScreen(
            Cube.solved().apply("R U F' L2 D B R2"), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
            initialMethod = fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod.LEARN,
        )
    }

    private fun lessonPage(name: String, lesson: Int, page: Int) = shot(name) {
        fi.jukkakot.rubikkisolveri.ui.lessons.LessonScreen(lesson, onBack = {}, onPractice = {}, onFreeCube = {}, initialPage = page)
    }

    @Test
    fun middleLayerGoal() = shot("goal-middle-layer") {
        fi.jukkakot.rubikkisolveri.ui.lessons.StageGoalPicture(fi.jukkakot.rubikkisolveri.cube.beginner.Stage.MIDDLE_LAYER)
    }

    @Test
    fun lessonGoal() = lessonPage("lesson-goal", 3, 0)

    @Test
    fun lessonCases() = lessonPage("lesson-cases", 3, 1)

    @Test
    fun lessonAlgorithm() = lessonPage("lesson-algorithm", 3, 2)

    @Test
    fun lessonPractice() = lessonPage("lesson-practice", 3, 4)

    @Test
    fun lessonYellowCrossCases() = lessonPage("lesson-cases-yellow-cross", 4, 1)

    @Test
    fun lessonCornersGoal() = lessonPage("lesson-goal-corners-placed", 6, 0)

    @Test
    fun basicsMove() = lessonPage("basics-move", 0, 2)

    @Test
    fun basicsCentres() = lessonPage("basics-centres", 0, 0)

    @Test
    fun timer() = shot("timer") {
        val repo = androidx.compose.runtime.remember {
            fi.jukkakot.rubikkisolveri.progress.InMemoryProgressRepository().also { r ->
                kotlinx.coroutines.runBlocking { for (ms in listOf(41_230L, 38_900L, 45_010L, 36_420L, 39_870L)) r.addTimed(ms, "R U") }
            }
        }
        fi.jukkakot.rubikkisolveri.ui.progress.TimerScreen(
            repo, onBack = {}, onHistory = {}, onGuidedScramble = {},
            scrambles = { Notation.parse("D2 F2 U' R2 D B2 U2 L2 F2 R2 U' F' L U' B R' D2 B' U F'") },
        )
    }

    @Test
    fun scan() = shot("scan") { ForcedDark {
        val frames = kotlinx.coroutines.flow.MutableStateFlow(
            listOf(
                fi.jukkakot.rubikkisolveri.cube.CubeColor.RED, fi.jukkakot.rubikkisolveri.cube.CubeColor.GREEN,
                fi.jukkakot.rubikkisolveri.cube.CubeColor.WHITE, fi.jukkakot.rubikkisolveri.cube.CubeColor.BLUE,
                fi.jukkakot.rubikkisolveri.cube.CubeColor.GREEN, fi.jukkakot.rubikkisolveri.cube.CubeColor.YELLOW,
                fi.jukkakot.rubikkisolveri.cube.CubeColor.ORANGE, fi.jukkakot.rubikkisolveri.cube.CubeColor.GREEN,
                fi.jukkakot.rubikkisolveri.cube.CubeColor.RED,
            ).map { fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier.DEFAULT_PALETTE.getValue(it) },
        )
        fi.jukkakot.rubikkisolveri.ui.scan.ScanContent(
            frames, torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = {},
            preview = { androidx.compose.foundation.layout.Box(it.then(Modifier.background(androidx.compose.ui.graphics.Color(0xFF3A3530)))) },
        )
    } }

    @Test
    fun scanReview() = shot("scan-review", waitForText = "Tunnistettu") { ForcedDark {
        val front = Cube.solved().apply("R U F'").let { cube ->
            (1..9).map { fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier.DEFAULT_PALETTE.getValue(cube.colorAt(fi.jukkakot.rubikkisolveri.cube.Face.F, it)) }
        }
        fi.jukkakot.rubikkisolveri.ui.scan.ScanContent(
            kotlinx.coroutines.flow.flowOf(front, front, front), torch = false, onTorch = {}, onBack = {}, onManual = {}, onResult = {},
            holdMillis = 0,
            preview = { androidx.compose.foundation.layout.Box(it.then(Modifier.background(androidx.compose.ui.graphics.Color(0xFF3A3530)))) },
        )
    } }

    @Test
    fun midTurn() = shot("mid-turn") {
        Cube3D(
            colors = Cube.solved().toList().map(StickerColors::of),
            move = Notation.parse("R").single(),
            progress = 0.35f,
            modifier = Modifier.fillMaxSize(),
        )
    }

    @Composable
    private fun Home() {
        fi.jukkakot.rubikkisolveri.ui.home.HomeScreen(
            primary = fi.jukkakot.rubikkisolveri.ui.home.HomeEntry(R.string.home_scan, R.drawable.ic_camera) {},
            entries = listOf(
                fi.jukkakot.rubikkisolveri.ui.home.HomeEntry(R.string.home_manual, R.drawable.ic_palette) {},
                fi.jukkakot.rubikkisolveri.ui.home.HomeEntry(R.string.home_learn, R.drawable.ic_school) {},
                fi.jukkakot.rubikkisolveri.ui.home.HomeEntry(R.string.home_timer, R.drawable.ic_timer) {},
                fi.jukkakot.rubikkisolveri.ui.home.HomeEntry(R.string.home_free_cube, R.drawable.ic_cube) {},
            ),
            onOpenSettings = {}, crashedLastTime = false, onShowLog = {}, onCrashNoticeShown = {},
            version = "1.0.51-0365b23 · 3.10.2026 11.30",
            summary = fi.jukkakot.rubikkisolveri.ui.home.HomeSummary(best = 42_310, count = 12),
            spin = false,
        )
    }

    @Test
    fun home() = shot("home") { Home() }

    @Test
    @Config(qualifiers = "fi-w891dp-h411dp-land-xxhdpi")
    fun homeLandscape() = shot("home-landscape") { Home() }

    @Test
    fun settings() = shot("settings") {
        fi.jukkakot.rubikkisolveri.ui.settings.SettingsScreen(
            language = fi.jukkakot.rubikkisolveri.settings.AppLanguage.entries.first(),
            themeMode = fi.jukkakot.rubikkisolveri.settings.ThemeMode.SYSTEM,
            version = "1.0.51-0365b23", onLanguage = {}, onThemeMode = {}, onOpenLog = {}, onBack = {},
        )
    }

    @Test
    fun about() = shot("about") { fi.jukkakot.rubikkisolveri.ui.settings.AboutScreen("1.0.51-0365b23", onBack = {}) }

    @Test
    fun aboutLicences() = shot("about-licences") { fi.jukkakot.rubikkisolveri.ui.settings.AboutScreen("1.0.51-0365b23", onBack = {}, licencesOpen = true) }

    @Test
    fun log() = shot("log") {
        fi.jukkakot.rubikkisolveri.ui.log.LogScreen(
            lines = listOf(
                "2026-10-03T08:09:50Z INFO scan.capture face=D picture=20261003-110950-204-D.png",
                "2026-10-03T08:09:51Z INFO scan.done valid=true validity=Valid uncertain=0",
                "2026-10-03T08:09:51Z INFO solve.done method=FAST moves=17 ms=100",
                "2026-10-03T08:09:52Z WARN scan.stall where=ui ms=180",
                "2026-10-03T08:09:53Z ERROR scan.error msg=\"camera closed\"",
            ),
            onShare = {}, onClear = {}, onBack = {},
        )
    }

    @Test
    fun lessons() = shot("lessons") { fi.jukkakot.rubikkisolveri.ui.lessons.LessonsScreen(onOpen = {}, onBack = {}, practiceCounts = mapOf(0 to 3)) }

    @Test
    fun history() = shot("history") {
        val repo = androidx.compose.runtime.remember {
            fi.jukkakot.rubikkisolveri.progress.InMemoryProgressRepository().also { r ->
                kotlinx.coroutines.runBlocking {
                    r.addTimed(41_230L, "R U R' U'", 1_759_480_000_000)
                    r.addGuided("FAST", 17, 95_000, 1_759_480_100_000)
                    r.addPractice(1, 64_000, 1_759_480_200_000)
                }
            }
        }
        fi.jukkakot.rubikkisolveri.ui.progress.HistoryScreen(repo, onBack = {})
    }

    @Test
    fun scanPermission() = shot("scan-permission") { ForcedDark {
        // Robolectric grants no camera permission: the screen shows why it is needed.
        fi.jukkakot.rubikkisolveri.ui.scan.ScanScreen(onBack = {}, onManual = {}, onResult = {})
    } }
}
