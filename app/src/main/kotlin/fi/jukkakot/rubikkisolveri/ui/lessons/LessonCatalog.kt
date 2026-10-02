package fi.jukkakot.rubikkisolveri.ui.lessons

import androidx.annotation.StringRes
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage

data class Algorithm(@StringRes val name: Int, val moves: List<Move>)

/**
 * A lesson: [stage] is null for the basics. [paragraphs] come under "what", "how" and "tip"
 * headings for a stage; for the basics they are plain paragraphs.
 */
data class Lesson(
    val stage: Stage?,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    val paragraphs: List<Pair<Int?, Int>>,
    val algorithms: List<Algorithm>,
)

object LessonCatalog {
    private fun stageLesson(stage: Stage, title: Int, summary: Int, what: Int, how: Int, tip: Int, algorithms: List<Algorithm>) =
        Lesson(stage, title, summary, listOf(R.string.lesson_what to what, R.string.lesson_how to how, R.string.lesson_tip to tip), algorithms)

    val lessons: List<Lesson> = listOf(
        Lesson(
            null, R.string.lesson_basics, R.string.lesson_basics_summary,
            listOf(
                null to R.string.lesson_basics_p1, null to R.string.lesson_basics_p2,
                null to R.string.lesson_basics_p3, null to R.string.lesson_basics_p4,
            ),
            listOf(Algorithm(R.string.alg_basics, Notation.parse("R U R' U'"))),
        ),
        stageLesson(Stage.WHITE_CROSS, R.string.stage_1, R.string.lesson_1_summary, R.string.stage_1_intro, R.string.lesson_1_how, R.string.lesson_1_tip, emptyList()),
        stageLesson(
            Stage.WHITE_CORNERS, R.string.stage_2, R.string.lesson_2_summary, R.string.stage_2_intro, R.string.lesson_2_how, R.string.lesson_2_tip,
            listOf(Algorithm(R.string.alg_trigger, BeginnerSolver.TRIGGER)),
        ),
        stageLesson(
            Stage.MIDDLE_LAYER, R.string.stage_3, R.string.lesson_3_summary, R.string.stage_3_intro, R.string.lesson_3_how, R.string.lesson_3_tip,
            listOf(Algorithm(R.string.alg_middle_right, BeginnerSolver.MIDDLE_RIGHT), Algorithm(R.string.alg_middle_left, BeginnerSolver.MIDDLE_LEFT)),
        ),
        stageLesson(
            Stage.YELLOW_CROSS, R.string.stage_4, R.string.lesson_4_summary, R.string.stage_4_intro, R.string.lesson_4_how, R.string.lesson_4_tip,
            listOf(Algorithm(R.string.alg_yellow_cross, BeginnerSolver.YELLOW_CROSS)),
        ),
        stageLesson(
            Stage.YELLOW_EDGES, R.string.stage_5, R.string.lesson_5_summary, R.string.stage_5_intro, R.string.lesson_5_how, R.string.lesson_5_tip,
            listOf(Algorithm(R.string.alg_swap_edges, BeginnerSolver.SWAP_EDGES)),
        ),
        stageLesson(
            Stage.YELLOW_CORNERS_PLACED, R.string.stage_6, R.string.lesson_6_summary, R.string.stage_6_intro, R.string.lesson_6_how, R.string.lesson_6_tip,
            listOf(Algorithm(R.string.alg_place_corners, BeginnerSolver.PLACE_CORNERS)),
        ),
        stageLesson(
            Stage.YELLOW_CORNERS_TURNED, R.string.stage_7, R.string.lesson_7_summary, R.string.stage_7_intro, R.string.lesson_7_how, R.string.lesson_7_tip,
            listOf(Algorithm(R.string.alg_trigger, BeginnerSolver.TRIGGER)),
        ),
    )
}
