package fi.jukkakot.rubikkisolveri.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubePattern
import fi.jukkakot.rubikkisolveri.cube.SolveTarget
import fi.jukkakot.rubikkisolveri.cube.beginner.Checks
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.ui.solve.SolvePlan
import fi.jukkakot.rubikkisolveri.ui.solve.planTarget
import fi.jukkakot.rubikkisolveri.ui.solve.SolveScreen
import fi.jukkakot.rubikkisolveri.ui.target.TargetScreen
import fi.jukkakot.rubikkisolveri.ui.theme.RubikkiTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class TargetTest {
    @get:Rule
    val compose = createComposeRule()

    private fun waitFor(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun choosingAPatternLooksFirstThenReturnsIt() {
        var chosen: SolveTarget? = null
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                TargetScreen(Cube.solved(), SolveTarget.Solved, onChoose = { chosen = it }, onPaint = {}, onBack = {})
            }
        }
        compose.onNodeWithText("Shakkilauta").performScrollTo().performClick()
        assertEquals(null, chosen, "a tap only shows the pattern")
        compose.onNodeWithText("Valitse").performClick()
        assertEquals(SolveTarget.Pattern(CubePattern.CHECKERBOARD), chosen)
    }

    @Test
    fun changingTheTargetRestartsTheGuide() {
        var target by mutableStateOf<SolveTarget>(SolveTarget.Pattern(CubePattern.CHECKERBOARD))
        compose.setContent {
            RubikkiTheme(dynamicColor = false) {
                SolveScreen(
                    Cube.solved(), onBack = {}, onHome = {}, planner = INLINE_PLANNER,
                    target = target, onChangeTarget = {}, targetPlanner = INLINE_TARGET_PLANNER,
                )
            }
        }
        waitFor("Siirto 1/6")
        compose.onNodeWithContentDescription("Tein sen").performScrollTo().performClick()
        waitFor("Siirto 2/6")
        target = SolveTarget.Pattern(CubePattern.SIX_SPOTS)
        waitFor("Siirto 1/")
        assertTrue(compose.onAllNodesWithText("Siirto 2/6").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun aStageTargetEndsAfterThatStage() {
        val cube = Cube.solved().apply("R U F' L2 D B R2 U' F")
        val plan = planTarget(cube, SolveTarget.StageDone(Stage.WHITE_CROSS))
        assertIs<SolvePlan.Ready>(plan)
        val after = cube.apply(plan.moves)
        assertTrue(Checks.crossDone(after))
        assertTrue(!after.isSolved)
        assertTrue(plan.steps!!.all { it.stage == Stage.WHITE_CROSS })
    }
}
