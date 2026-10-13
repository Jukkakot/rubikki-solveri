package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.solve.COMBO_PART_TAG
import fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ComboGuideTest {
    @get:Rule
    val compose = createComposeRule()

    private fun has(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    private fun combos() = compose.onAllNodesWithTag(COMBO_PART_TAG).fetchSemanticsNodes().size

    @Test
    fun crossStepHasNoCombinationAndCornerStepHasOne() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    Cube.solved().apply("R U F' L2 D B"), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
                    initialMethod = SolveMethod.COMBOS, startScreen = true,
                )
            }
        }
        compose.waitUntil(10_000) { has("Aloita") }
        compose.onNodeWithText("Liikesarjat").assertExists()
        compose.onNodeWithText("Aloita").performClick()
        compose.waitUntil(10_000) { has("Askel 1/") }
        // The first steps are the white cross: an edge and its turns, no combination.
        assertTrue(has("Valkoinen risti"))
        assertTrue(combos() == 0)
        // On to the first white corner: it names its combination and how many times.
        var guard = 0
        while (!has("kulma") && guard++ < 10) {
            compose.onNodeWithText("Tein sen").performScrollTo().performClick()
            compose.waitForIdle()
        }
        assertTrue(has("kulma"), "no corner step")
        assertTrue(combos() >= 1)
        assertTrue(has("Liipaisin"))
        assertTrue(has("R' D' R D ×"))
    }
}
