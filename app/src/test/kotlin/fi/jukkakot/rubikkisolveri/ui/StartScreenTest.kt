package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class StartScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private var backs = 0
    private var checks = 0

    private fun open(cube: Cube = Cube.solved().apply("R U"), method: SolveMethod = SolveMethod.FAST) {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    cube, onBack = { backs++ }, onHome = {}, planner = INLINE_PLANNER, initialMethod = method,
                    startScreen = true, onCheckColors = { checks++ },
                )
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Aloita").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun showsTheMovesTheHoldAndStarts() {
        open()
        compose.onNodeWithText("2 siirtoa").assertIsDisplayed()
        compose.onNodeWithText("Pidä kuutiota", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Nopein").assertIsDisplayed()
        compose.onNodeWithText("Aloita").performClick()
        compose.onNodeWithText("Siirto 1/2").assertIsDisplayed()
    }

    @Test
    fun backFromTheGuideReturnsToTheStart() {
        open()
        compose.onNodeWithText("Aloita").performClick()
        compose.onNodeWithContentDescription("Takaisin").performClick()
        compose.onNodeWithText("Aloita").assertIsDisplayed()
        assertEquals(0, backs)
        compose.onNodeWithContentDescription("Takaisin").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun menuOffersTheColourCheckAndTheStart() {
        open()
        compose.onNodeWithText("Aloita").performClick()
        compose.onNodeWithContentDescription("Valikko").performClick()
        compose.onNodeWithText("Tarkista värit").performClick()
        assertEquals(1, checks)
        compose.onNodeWithContentDescription("Valikko").performClick()
        compose.onNodeWithText("Aloitusruutuun").performClick()
        compose.onNodeWithText("Aloita").assertIsDisplayed()
    }

    @Test
    fun holdInWordsOnlyOnTheFirstMove() {
        open()
        compose.onNodeWithText("Aloita").performClick()
        compose.onNodeWithText("Pidä kuutiota", substring = true).assertExists()
        compose.onNodeWithContentDescription("Tein sen").performClick()
        compose.waitForIdle()
        assertTrue(compose.onAllNodesWithText("Pidä kuutiota", substring = true).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun changingTheMethodUpdatesTheCount() {
        open(Cube.solved().apply("R U F' L2 D B"))
        val fast = compose.onAllNodesWithText("siirtoa", substring = true).fetchSemanticsNodes().single()
        compose.onNodeWithText("Opettele").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Aloita").fetchSemanticsNodes().isNotEmpty() }
        val learn = compose.onAllNodesWithText("siirtoa", substring = true).fetchSemanticsNodes().single()
        assertTrue(fast.config.toString() != learn.config.toString(), "the learn method has its own count")
    }
}
