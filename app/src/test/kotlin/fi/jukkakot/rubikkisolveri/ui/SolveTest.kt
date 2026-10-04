package fi.jukkakot.rubikkisolveri.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import fi.jukkakot.rubikkisolveri.Strings
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.ui.common.MoveWords
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MoveWordsTest {
    private fun words(language: String, move: String): String {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        val context = base.createConfigurationContext(config)
        return MoveWords.describe(Notation.parseMove(move)!!, { id, args -> Strings.get(language, context.resources.getResourceEntryName(id), *args) })
    }

    @Test
    fun describeAMove() {
        assertEquals("Turn the top counter-clockwise (as seen from above).", words("en", "U'"))
        assertEquals("Käännä yläpuolta vastapäivään (katsottuna ylhäältä).", words("fi", "U'"))
        assertEquals("Käännä oikeaa puolta myötäpäivään (katsottuna oikealta).", words("fi", "R"))
        assertEquals("Turn the back half a turn.", words("en", "B2"))
    }

    @Test
    fun everyFaceMoveHasItsOwnWords() {
        val faces = listOf(Layer.U, Layer.D, Layer.R, Layer.L, Layer.F, Layer.B)
        for (language in listOf("fi", "en")) {
            val texts = faces.flatMap { layer -> (1..3).map { words(language, Move(layer, it).toString()) } }
            assertEquals(18, texts.toSet().size, language)
            assertTrue(texts.none { it.contains('%') })
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class SolveTest {
    @get:Rule
    val compose = createComposeRule()

    private var home = false

    private fun solve(cube: Cube) {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) { SolveScreen(cube, onBack = {}, onHome = { home = true }, planner = INLINE_PLANNER) }
        }
    }

    private fun waitFor(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun nextAndBack() {
        solve(Cube.solved().apply("R U"))
        waitFor("Siirto 1/2")
        compose.onNodeWithText("Tein sen").performScrollTo().performClick()
        compose.onNodeWithText("Siirto 2/2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Edellinen").performScrollTo().performClick()
        compose.onNodeWithText("Siirto 1/2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Käännä yläpuolta vastapäivään (katsottuna ylhäältä).").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun finished() {
        solve(Cube.solved().apply("R U"))
        waitFor("Siirto 1/2")
        compose.onNodeWithText("Näytä").performScrollTo().performClick()
        compose.onNodeWithText("Tein sen").performScrollTo().performClick()
        compose.onNodeWithText("Tein sen").performScrollTo().performClick()
        compose.onNodeWithText("Valmis! Kuutio on ratkaistu.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Alkuun").performScrollTo().performClick()
        assertTrue(home)
    }

    @Test
    fun solvedCube() {
        solve(Cube.solved())
        waitFor("Kuutio on jo ratkaistu!")
    }
}
