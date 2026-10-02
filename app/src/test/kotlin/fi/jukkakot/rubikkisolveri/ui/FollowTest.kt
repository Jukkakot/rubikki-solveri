package fi.jukkakot.rubikkisolveri.ui

import android.Manifest
import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.follow.front
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.ui.guide.FollowPanel
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FollowTest {
    @get:Rule
    val compose = createComposeRule()

    private val frames = MutableSharedFlow<List<Rgb>>(extraBufferCapacity = 64)

    // Solving "R U" gives U' R'.
    private val cube = Cube.solved().apply("R U")

    private fun solve() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(cube, onBack = {}, onHome = {}, followPanel = { FollowPanel(it, frames, preview = {}) }, solver = INLINE_SOLVER)
            }
        }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Siirto 1/2").fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithContentDescription("Seuraa kameralla").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun show(state: Cube, times: Int = 4) {
        repeat(times) {
            compose.runOnIdle { frames.tryEmit(front(state).map { ColorClassifier.DEFAULT_PALETTE.getValue(it) }) }
            compose.waitForIdle()
        }
    }

    @Test
    fun switchToCameraMode() {
        solve()
        compose.onNodeWithContentDescription("Seuraa kameralla").performClick()
        // No camera permission in tests: the gate offers the way back to the 3D cube.
        compose.onNodeWithText("Kamera tarvitaan").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Näytä 3D-kuutio").performScrollTo().performClick()
        compose.onNodeWithText("Näytä").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun moveDoneAdvancesAndWrongMoveIsNamed() {
        shadowOf(ApplicationProvider.getApplicationContext<Application>()).grantPermissions(Manifest.permission.CAMERA)
        solve()
        compose.onNodeWithContentDescription("Seuraa kameralla").performClick()
        compose.onNodeWithText("Tee siirto – sovellus huomaa sen itse.").performScrollTo().assertIsDisplayed()
        show(cube)
        show(cube.apply("U'"))
        compose.onNodeWithText("Siirto 2/2").performScrollTo().assertIsDisplayed()
        // Next move is R'; turning R instead is noticed.
        show(cube.apply("U' R"))
        compose.onNodeWithText("Käännös meni toisin", substring = true).performScrollTo().assertIsDisplayed()
    }
}
