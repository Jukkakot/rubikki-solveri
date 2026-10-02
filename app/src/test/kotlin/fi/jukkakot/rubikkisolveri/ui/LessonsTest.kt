package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Sequences
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.Checks
import fi.jukkakot.rubikkisolveri.cube.beginner.Practice
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeAnimator
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator
import fi.jukkakot.rubikkisolveri.ui.lessons.Algorithm
import fi.jukkakot.rubikkisolveri.ui.lessons.AlgorithmCard
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonCatalog
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonScreen
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonsScreen
import fi.jukkakot.rubikkisolveri.ui.solve.SolveMethod
import fi.jukkakot.rubikkisolveri.ui.solve.SolvePlan
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class LessonsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun everyStageHasALesson() {
        assertEquals(listOf(null) + Stage.entries, LessonCatalog.lessons.map { it.stage })
    }

    @Test
    fun openLessons() {
        compose.setContent { RubikkiTheme(dynamicColor = false) { LessonsScreen(onOpen = {}, onBack = {}) } }
        compose.onNodeWithText("Kuution perusteet").assertIsDisplayed()
        compose.onNodeWithText("1. Valkoinen risti").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("7. Keltaiset kulmat oikein päin"))
        compose.onNodeWithText("7. Keltaiset kulmat oikein päin").assertIsDisplayed()
    }

    @Test
    fun middleLayerLesson() {
        compose.setContent { RubikkiTheme(dynamicColor = false) { LessonScreen(3, onBack = {}, onPractice = {}, onFreeCube = {}) } }
        compose.onNodeWithText("Miten").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("U R U' R' U' F' U F").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("U' L' U L U F U' F'").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun playTheTrigger() {
        lateinit var animator: CubeAnimator
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                AlgorithmCard(Algorithm(R.string.alg_trigger, BeginnerSolver.TRIGGER), animatorFor = { start ->
                    rememberCubeAnimator(start).also { animator = it }
                })
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val start = Cube.solved().apply(Sequences.inverse(BeginnerSolver.TRIGGER))
        compose.onNodeWithText("Toista sarja").performClick()
        compose.mainClock.advanceTimeBy(1500)
        compose.runOnIdle { assertTrue(animator.cube.isSolved, "solved at the end of the demo") }
        compose.mainClock.advanceTimeBy(2000)
        compose.runOnIdle { assertEquals(start, animator.cube, "back to the start") }
    }

    @Test
    fun practiseTheYellowCross() {
        val exercise = Practice.exercise(Stage.YELLOW_CROSS, Random(3))
        assertTrue(Checks.twoLayersDone(exercise.position) && Checks.yellowEdgesUp(exercise.position) < 4)
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    exercise.position, onBack = {}, onHome = {},
                    planner = { _, _ -> SolvePlan.Ready(exercise.steps.flatMap { it.moves }, exercise.steps) },
                    initialMethod = SolveMethod.LEARN, title = "Harjoitus", practice = true,
                    finishedText = "Vaihe valmis!", homeLabel = "Uusi harjoitus",
                )
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Tein sen").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodesWithText("Opettele vaiheittain").fetchSemanticsNodes().isEmpty(), "no method choice in practice")
        compose.onNodeWithText("Vaihe 4/7: Keltainen risti").performScrollTo().assertIsDisplayed()
        repeat(exercise.steps.sumOf { it.moves.size }) { compose.onNodeWithText("Tein sen").performScrollTo().performClick() }
        compose.onNodeWithText("Vaihe valmis!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Uusi harjoitus").performScrollTo().assertIsDisplayed()
    }
}
