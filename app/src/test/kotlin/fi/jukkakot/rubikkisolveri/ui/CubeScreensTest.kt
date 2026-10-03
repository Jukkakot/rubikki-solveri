package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeAnimator
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator
import fi.jukkakot.rubikkisolveri.ui.free.FreeCubeScreen
import fi.jukkakot.rubikkisolveri.ui.manual.ManualInputScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "fi-w411dp-h891dp")
class CubeScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private var accepted: Cube? = null

    private fun manual(initial: CubeEditor = CubeEditor.empty()) {
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                ManualInputScreen(onBack = {}, onValid = { accepted = it }, initial = initial)
            }
        }
    }

    private fun stateIs(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    @Test
    fun paintASticker() {
        manual()
        compose.onNodeWithContentDescription("Etupuoli, tarra 1").assert(stateIs("ei väriä"))
        compose.onNodeWithContentDescription("punainen").performClick()
        compose.onNodeWithContentDescription("Etupuoli, tarra 1").performClick()
        compose.onNodeWithContentDescription("Etupuoli, tarra 1").assert(stateIs("punainen"))
    }

    @Test
    fun centresAreFixedOnScreen() {
        manual()
        compose.onNodeWithContentDescription("punainen").performClick()
        compose.onNodeWithContentDescription("Etupuoli, tarra 5").performClick()
        compose.onNodeWithContentDescription("Etupuoli, tarra 5").assert(stateIs("vihreä"))
    }

    @Test
    fun nextFace() {
        manual()
        compose.onNodeWithText("Seuraava").performClick()
        compose.onNodeWithText("Pidä kuutiota näin: punainen keskiö sinua kohti, valkoinen ylhäällä.").assertIsDisplayed()
        compose.onNodeWithText("Oikea puoli (2/6)").assertIsDisplayed()
    }

    @Test
    fun unfinishedCube() {
        manual()
        compose.onNodeWithText("Tarkista").assertIsNotEnabled()
    }

    @Test
    fun invalidCube() {
        val solved = Cube.solved()
        val urf = Corner.URF.stickers
        val twisted = solved.with(urf[0], solved[urf[2]]).with(urf[1], solved[urf[0]]).with(urf[2], solved[urf[1]])
        manual(CubeEditor.of(twisted))
        compose.onNodeWithText("Tarkista").assertIsEnabled().performClick()
        compose.onNodeWithText("Yksi kulma on kiertynyt", substring = true).assertIsDisplayed()
        assertNull(accepted)
    }

    @Test
    fun validCube() {
        val cube = Cube.solved().apply("R U R' F2")
        manual(CubeEditor.of(cube))
        compose.onNodeWithText("Tarkista").performClick()
        assertEquals(cube, accepted)
    }

    @Test
    fun freeCubeTurnAndUndo() {
        compose.setContent { RubikkiTheme(dynamicColor = false) { FreeCubeScreen(start = Cube.solved(), onBack = {}) } }
        compose.onNodeWithText("Kumoa").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Oikea R").performScrollTo().performClick()
        compose.onNodeWithText("Kumoa").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithText("Kumoa").assertIsNotEnabled()
    }
}

@RunWith(RobolectricTestRunner::class)
class CubeAnimatorTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var animator: CubeAnimator

    private fun start() {
        compose.mainClock.autoAdvance = false
        compose.setContent { animator = rememberCubeAnimator(Cube.solved()) }
        compose.mainClock.advanceTimeByFrame()
    }

    @Test
    fun animatedTurn() {
        start()
        compose.runOnIdle { animator.play(Notation.parse("R").single()) }
        compose.mainClock.advanceTimeBy(150)
        compose.runOnIdle {
            assertNotNull(animator.move)
            assertTrue(animator.progress in 0.05f..0.95f, "progress ${animator.progress}")
            assertEquals(Cube.solved(), animator.cube)
        }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle {
            assertNull(animator.move)
            assertEquals(Cube.solved().apply("R"), animator.cube)
        }
    }

    @Test
    fun queuedMoves() {
        start()
        compose.runOnIdle { animator.play(Notation.parse("R U F")) }
        compose.runOnIdle { assertEquals(3, animator.pending) }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            assertEquals(Cube.solved().apply("R U F"), animator.cube)
            assertEquals(0, animator.pending)
        }
    }

    @Test
    fun snapDropsQueuedMoves() {
        start()
        compose.runOnIdle {
            animator.play(Notation.parse("R U F"))
            animator.snapTo(Cube.solved())
        }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle { assertEquals(Cube.solved(), animator.cube) }
    }
}
