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
import androidx.compose.runtime.Composable
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

    private fun shot(name: String, dark: Boolean = false, waitForText: String? = null, content: @Composable () -> Unit) {
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
        compose.mainClock.advanceTimeBy(2000)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun freeCube() = shot("free-cube") { FreeCubeScreen(start = Cube.solved().apply("R U R' U'"), onBack = {}) }

    @Test
    fun manualInput() = shot("manual-input") {
        ManualInputScreen(onBack = {}, onValid = {}, initial = CubeEditor.of(Cube.solved().apply("R U F")).paint(0, null))
    }

    @Test
    fun manualInputDark() = shot("manual-input-dark", dark = true) { ManualInputScreen(onBack = {}, onValid = {}) }

    @Test
    fun solve() = shot("solve", waitForText = "Siirto 1/") {
        SolveScreen(Cube.solved().apply("R U R' F2 D L' B U2"), onBack = {}, onHome = {}, planner = INLINE_PLANNER)
    }

    @Test
    fun guideBack() = shot("guide-back", waitForText = "Siirto 1/") {
        SolveScreen(Cube.solved().apply("B'"), onBack = {}, onHome = {}, showNotation = true, planner = INLINE_PLANNER)
    }

    @Test
    fun guideRightDark() = shot("guide-right-dark", dark = true, waitForText = "Siirto 1/") {
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
    fun learn() = shot("learn", waitForText = "Vaihe ") {
        SolveScreen(
            Cube.solved().apply("R U F' L2 D B R2"), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
            initialMethod = fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod.LEARN,
        )
    }

    @Test
    fun lesson() = shot("lesson") {
        fi.jukkakot.rubikkisolveri.ui.lessons.LessonScreen(2, onBack = {}, onPractice = {}, onFreeCube = {})
    }

    @Test
    fun scan() = shot("scan") {
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
    }

    @Test
    fun midTurn() = shot("mid-turn") {
        Cube3D(
            colors = Cube.solved().toList().map(StickerColors::of),
            move = Notation.parse("R").single(),
            progress = 0.35f,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
