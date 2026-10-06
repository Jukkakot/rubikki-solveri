package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.guide.GUIDE_CUBE_TAG
import fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The solution screen fits a short browser window (about 560 dp) without scrolling. */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "fi-w411dp-h560dp-xxhdpi")
class FitScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun solve(cube: Cube, method: SolveMethod = SolveMethod.FAST, waitFor: String = "Siirto 1/") {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) { SolveScreen(cube, onBack = {}, onHome = {}, planner = INLINE_PLANNER, initialMethod = method) }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText(waitFor, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun cubeWidth() = compose.onNodeWithTag(GUIDE_CUBE_TAG).getUnclippedBoundsInRoot().let { it.right - it.left }

    @Test
    fun shortScreenShowsMoveAndDoneWithoutScrolling() {
        solve(Cube.solved().apply("R U"))
        compose.onNodeWithContentDescription("Tein sen").assertIsDisplayed()
        compose.onNodeWithText("Käännä", substring = true).assertIsDisplayed()
        val before = cubeWidth()
        assertTrue(before < 379.dp, "cube $before should be narrower than the screen")
        compose.onNodeWithContentDescription("Tein sen").performClick()
        compose.waitForIdle()
        assertEquals(before, cubeWidth())
    }

    @Test
    @Config(qualifiers = "fi-w411dp-h891dp-xxhdpi")
    fun tallPhoneKeepsTheFullWidthCube() {
        solve(Cube.solved().apply("R U"))
        val width = cubeWidth()
        assertTrue(width >= 378.dp, "cube $width should be full width")
    }
}
