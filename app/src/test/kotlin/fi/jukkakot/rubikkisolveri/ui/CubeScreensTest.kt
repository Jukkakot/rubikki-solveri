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
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.performTouchInput
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
        compose.onNodeWithContentDescription("Edellinen").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Seuraava").performClick()
        compose.onNodeWithText("Pidä kuutiota näin: punainen keskiö sinua kohti, valkoinen ylhäällä.").assertIsDisplayed()
        compose.onNodeWithText("Oikea puoli (2/6)").assertIsDisplayed()
    }

    @Test
    fun unfinishedCube() {
        manual()
        compose.onNodeWithContentDescription("Tarkista").assertIsNotEnabled()
    }

    @Test
    fun invalidCube() {
        val solved = Cube.solved()
        val urf = Corner.URF.stickers
        val twisted = solved.with(urf[0], solved[urf[2]]).with(urf[1], solved[urf[0]]).with(urf[2], solved[urf[1]])
        manual(CubeEditor.of(twisted))
        compose.onNodeWithContentDescription("Tarkista").assertIsEnabled().performClick()
        compose.onNodeWithText("Yksi kulma on kiertynyt", substring = true).assertIsDisplayed()
        assertNull(accepted)
    }

    @Test
    fun validCube() {
        val cube = Cube.solved().apply("R U R' F2")
        manual(CubeEditor.of(cube))
        compose.onNodeWithContentDescription("Tarkista").performClick()
        assertEquals(cube, accepted)
    }

    private var freeSolved: Cube? = null

    private fun freeCube() {
        compose.setContent { RubikkiTheme(dynamicColor = false) { FreeCubeScreen(start = Cube.solved(), onBack = {}, onSolve = { freeSolved = it }) } }
    }

    @Test
    fun freeCubeTurnsTheRightLayer() {
        freeCube()
        compose.onNodeWithContentDescription("Oikea puoli").performScrollTo().performClick()
        compose.onNodeWithText("Ratkaise").performScrollTo().performClick()
        assertEquals(Cube.solved().apply("R"), freeSolved)
    }

    @Test
    fun freeCubeToggleTurnsTheOtherWay() {
        freeCube()
        compose.onNodeWithContentDescription("Vastapäivään").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Oikea puoli").performScrollTo().performClick()
        compose.onNodeWithText("Ratkaise").performScrollTo().performClick()
        assertEquals(Cube.solved().apply("R'"), freeSolved)
    }

    @Test
    fun freeCubeTurnAndUndo() {
        freeCube()
        compose.onNodeWithContentDescription("Kumoa").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("Oikea puoli").performScrollTo().performClick()
        compose.onNodeWithText("Ratkaise").assertIsEnabled()
        compose.onNodeWithContentDescription("Kumoa").performScrollTo().assertIsEnabled().performClick()
        compose.onNodeWithContentDescription("Kumoa").assertIsNotEnabled()
        compose.onNodeWithText("Ratkaise").assertIsNotEnabled()
    }

    @Test
    fun freeCubeDragHintGoesAfterTheFirstDrag() {
        freeCube()
        compose.onNodeWithText("Vedä kuutiota", substring = true).assertExists()
        compose.onNodeWithContentDescription("Kolmiulotteinen kuutio").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        compose.onNodeWithText("Vedä kuutiota", substring = true).assertDoesNotExist()
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
    fun halfTurnPlaysAsTwoQuarterSteps() {
        start()
        var halfways = 0
        compose.runOnIdle {
            animator.onHalfway = { halfways++ }
            animator.play(Notation.parse("R2 U"))
        }
        compose.mainClock.advanceTimeBy(150)
        compose.runOnIdle {
            assertEquals(Notation.parse("R").single(), animator.move)
            assertEquals(Cube.solved(), animator.cube)
        }
        compose.mainClock.advanceTimeBy(250)
        compose.runOnIdle {
            assertNull(animator.move, "pause between the steps")
            assertEquals(Cube.solved().apply("R"), animator.cube)
            assertEquals(1, halfways)
            assertEquals(2, animator.pending)
        }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            assertEquals(Cube.solved().apply("R2 U"), animator.cube)
            assertEquals(0, animator.pending)
            assertEquals(1, halfways)
        }
    }

    @Test
    fun snapMidHalfTurn() {
        start()
        compose.runOnIdle { animator.play(Notation.parse("R2 U")) }
        compose.mainClock.advanceTimeBy(400)
        val snapped = Cube.solved().apply("F")
        compose.runOnIdle { animator.snapTo(snapped) }
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            assertEquals(snapped, animator.cube)
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
