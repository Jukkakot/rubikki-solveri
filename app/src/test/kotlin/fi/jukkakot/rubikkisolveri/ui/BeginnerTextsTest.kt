package fi.jukkakot.rubikkisolveri.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import fi.jukkakot.rubikkisolveri.Strings
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.beginner.StepNote
import fi.jukkakot.rubikkisolveri.ui.common.MoveWords
import fi.jukkakot.rubikkisolveri.ui.common.noteText
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class BeginnerTextsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun context(language: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
        return base.createConfigurationContext(config)
    }

    private fun note(language: String, note: StepNote): String {
        val c = context(language)
        return noteText(note) { id, args -> Strings.get(language, id.key, *args) }
    }

    @Test
    fun crossStep() {
        assertEquals("Vie valkoinen–punainen särmä punaisen keskiön viereen, valkoinen ylöspäin.", note("fi", StepNote.CrossEdge(CubeColor.RED)))
        assertEquals("Put the white–red edge next to the red centre, white on top.", note("en", StepNote.CrossEdge(CubeColor.RED)))
        assertEquals(
            "Vie valkoinen–vihreä–punainen kulma vihreän ja punaisen keskiön väliin, valkoinen ylöspäin.",
            note("fi", StepNote.WhiteCorner(CubeColor.GREEN, CubeColor.RED)),
        )
    }

    @Test
    fun wholeCubeTurn() {
        val y = Notation.parseMove("y")!!
        val after = Cube.solved().apply("y")
        val en = context("en")
        assertEquals(
            "Turn the whole cube: red centre towards you, white on top.",
            MoveWords.describe(y, { id, args -> Strings.en(id.key, *args) }, after),
        )
    }

    @Test
    fun chooseLearning() {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(Cube.solved().apply("R U F' L2 D B"), onBack = {}, onHome = {}, planner = INLINE_PLANNER, startScreen = true)
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Opettele").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Opettele").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Aloita").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Aloita").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Vaihe 1/7: Valkoinen risti").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Vaihe 1/7: Valkoinen risti").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Jatka").performScrollTo().performClick()
        compose.onNodeWithText("Vie valkoinen–", substring = true).performScrollTo().assertIsDisplayed()
    }
}
