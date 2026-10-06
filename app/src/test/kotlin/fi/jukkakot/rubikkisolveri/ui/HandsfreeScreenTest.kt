package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.guide.GUIDE_CUBE_TAG
import fi.jukkakot.rubikkisolveri.ui.guide.HandsfreeSpeed
import fi.jukkakot.rubikkisolveri.ui.solve.HANDSFREE_STOP_TAG
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class HandsfreeScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var saved: HandsfreeSpeed? = null

    private fun solve() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    Cube.solved().apply("R U L"), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
                    onHandsfreeSpeed = { saved = it },
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Siirto 1/3").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun tapOnTheCubeConfirms() {
        solve()
        compose.onNodeWithTag(GUIDE_CUBE_TAG).performClick()
        compose.onNodeWithText("Siirto 2/3").assertIsDisplayed()
    }

    @Test
    fun handsfreeAdvancesAndATouchStopsIt() {
        solve()
        compose.onNodeWithContentDescription("Handsfree").performScrollTo().performClick()
        compose.onNodeWithText("Nopea").performClick()
        compose.onNodeWithText("Valmis").performClick()
        assertEquals(HandsfreeSpeed.FAST, saved, "the chosen speed is remembered")
        compose.mainClock.advanceTimeBy(1_000 + HandsfreeSpeed.FAST.quarterMs + 500)
        compose.onNodeWithText("Siirto 2/3").assertIsDisplayed()
        // A touch anywhere stops it on the same move.
        compose.onNodeWithTag(HANDSFREE_STOP_TAG).performTouchInput { down(center); up() }
        compose.mainClock.advanceTimeBy(20_000)
        compose.onNodeWithText("Siirto 2/3").assertIsDisplayed()
        compose.onNodeWithTag(HANDSFREE_STOP_TAG).assertDoesNotExist()
    }
}
