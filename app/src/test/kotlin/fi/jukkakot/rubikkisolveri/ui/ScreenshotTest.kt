package fi.jukkakot.rubikkisolveri.ui

import android.graphics.Bitmap
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
            compose.waitUntil(10_000) { compose.onAllNodesWithText(waitForText, substring = true).fetchSemanticsNodes().isNotEmpty() }
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
        SolveScreen(Cube.solved().apply("R U R' F2 D L' B U2"), onBack = {}, onHome = {})
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
