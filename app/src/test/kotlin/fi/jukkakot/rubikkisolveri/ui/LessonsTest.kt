package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Sequences
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.CaseId
import fi.jukkakot.rubikkisolveri.cube.beginner.Checks
import fi.jukkakot.rubikkisolveri.cube.beginner.Practice
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StageCases
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.ui.cube3d.CubeAnimator
import fi.jukkakot.rubikkisolveri.ui.cube3d.rememberCubeAnimator
import fi.jukkakot.rubikkisolveri.ui.lessons.Algorithm
import fi.jukkakot.rubikkisolveri.ui.common.FitColumn
import fi.jukkakot.rubikkisolveri.ui.lessons.AlgorithmPage
import fi.jukkakot.rubikkisolveri.ui.lessons.CasesPage
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonCatalog
import fi.jukkakot.rubikkisolveri.ui.lessons.LessonPage
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
    fun middleLayerPages() {
        val pages = LessonCatalog.lessons[3].pages
        assertEquals(LessonPage.Goal(Stage.MIDDLE_LAYER), pages.first())
        assertEquals(LessonPage.Cases(Stage.MIDDLE_LAYER), pages[1])
        assertEquals(
            listOf(BeginnerSolver.MIDDLE_RIGHT, BeginnerSolver.MIDDLE_LEFT),
            pages.filterIsInstance<LessonPage.AlgorithmDemo>().map { it.algorithm.moves },
        )
        assertEquals(LessonPage.Practice(Stage.MIDDLE_LAYER), pages.last())
        assertEquals(4, LessonCatalog.lessons[0].pages.size, "basics: four picture pages")
    }

    @Test
    fun openLessons() {
        compose.setContent { RubikkiTheme(dynamicColor = false) { LessonsScreen(onOpen = {}, onBack = {}) } }
        compose.onNodeWithText("Kuution perusteet").assertIsDisplayed()
        compose.onNodeWithText("1. Valkoinen risti").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tavoite: Valkoinen risti").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("7. Keltaiset kulmat oikein päin"))
        compose.onNodeWithText("7. Keltaiset kulmat oikein päin").assertIsDisplayed()
    }

    @Test
    fun nextMovesToTheSecondPage() {
        compose.setContent { RubikkiTheme(dynamicColor = false) { LessonScreen(3, onBack = {}, onPractice = {}, onFreeCube = {}) } }
        compose.onNodeWithContentDescription("Sivu 1/5").assertIsDisplayed()
        compose.onNodeWithText("Seuraava").performClick()
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Sivu 2/5").assertIsDisplayed()
        compose.onNodeWithText("Mikä tilanne sinulla on?").assertIsDisplayed()
    }

    @Test
    fun tapACase() {
        compose.setContent { RubikkiTheme(dynamicColor = false) { FitColumn { CasesPage(Stage.WHITE_CORNERS) } } }
        compose.onNodeWithText("Valkoinen oikealle").performClick()
        compose.onNodeWithText("Toista sarja").assertIsDisplayed()
        compose.onNodeWithText("Kaikki tilanteet").performClick()
        compose.onNodeWithText("Valkoinen alas").assertIsDisplayed()
    }

    @Test
    fun playACase() {
        lateinit var animator: CubeAnimator
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                FitColumn { CasesPage(Stage.WHITE_CORNERS, animatorFor = { start -> rememberCubeAnimator(start).also { animator = it } }) }
            }
        }
        compose.onNodeWithText("Valkoinen oikealle").performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText("Toista sarja").performClick()
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle { assertTrue(StageCases.reached(CaseId.WHITE_RIGHT, animator.cube), "the corner ends in place") }
    }

    @Test
    fun playTheTrigger() {
        lateinit var animator: CubeAnimator
        compose.mainClock.autoAdvance = false
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                FitColumn {
                    AlgorithmPage(Algorithm(Res.string.alg_trigger, BeginnerSolver.TRIGGER), animatorFor = { start ->
                        rememberCubeAnimator(start).also { animator = it }
                    })
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        val start = Cube.solved().apply(Sequences.inverse(BeginnerSolver.TRIGGER))
        compose.onNodeWithText("Toista sarja").performClick()
        compose.mainClock.advanceTimeBy(450)
        compose.onNodeWithText("Käännä alakerrosta vasemmalle.").assertIsDisplayed() // D' in the guide's words while it plays
        compose.mainClock.advanceTimeBy(1050)
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
        // The goal comes first, the moves after "Continue".
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Jatka").fetchSemanticsNodes().isNotEmpty() }
        assertTrue(compose.onAllNodesWithText("Tein sen").fetchSemanticsNodes().isEmpty(), "goal card covers the guide")
        compose.onNodeWithContentDescription("Tavoite: Keltainen risti").assertExists()
        compose.onNodeWithText("Jatka").performScrollTo().performClick()
        assertTrue(compose.onAllNodesWithText("Opettele vaiheittain").fetchSemanticsNodes().isEmpty(), "no method choice in practice")
        compose.onNodeWithText("Vaihe 4/7: Keltainen risti").performScrollTo().assertIsDisplayed()
        // The learn method words a move the same way as the fast method.
        val first = exercise.steps.flatMap { it.moves }.first()
        val words = fi.jukkakot.rubikkisolveri.ui.common.MoveWords.describe(
            first, { id, args -> fi.jukkakot.rubikkisolveri.Strings.get("fi", id.key, *args) }, exercise.position.apply(first),
        )
        compose.onNodeWithText(words).assertExists()
        repeat(exercise.steps.sumOf { it.moves.size }) { compose.onNodeWithText("Tein sen").performScrollTo().performClick() }
        compose.onNodeWithText("Vaihe valmis!").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Uusi harjoitus").performScrollTo().assertIsDisplayed()
    }
}
