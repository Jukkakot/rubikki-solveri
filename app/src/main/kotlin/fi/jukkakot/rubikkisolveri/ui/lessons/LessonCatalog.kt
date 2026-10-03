package fi.jukkakot.rubikkisolveri.ui.lessons

import androidx.annotation.StringRes
import fi.jukkakot.rubikkisolveri.R
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.CaseId
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StageCases
import fi.jukkakot.rubikkisolveri.cube.beginner.StageGoals

/** An algorithm; its demo starts with the cube held as [hold] turns it from white up. */
data class Algorithm(@StringRes val name: Int, val moves: List<Move>, val hold: List<Move> = emptyList())

/** The basics' pictures: what each one shows. */
enum class BasicsPicture { CENTRES, PIECES, HOLD }

/** One page of a lesson; every page fits the screen without scrolling. */
sealed interface LessonPage {
    data class Goal(val stage: Stage) : LessonPage
    data class Cases(val stage: Stage) : LessonPage
    data class AlgorithmDemo(val algorithm: Algorithm, @StringRes val intro: Int? = null) : LessonPage
    data class Practice(val stage: Stage) : LessonPage
    data class Picture(val picture: BasicsPicture, @StringRes val text: Int) : LessonPage
}

/** A lesson: [stage] is null for the basics. */
data class Lesson(
    val stage: Stage?,
    @StringRes val title: Int,
    @StringRes val summary: Int,
    @StringRes val tip: Int?,
    val algorithms: List<Algorithm>,
) {
    val pages: List<LessonPage>
        get() = if (stage == null) {
            listOf(
                LessonPage.Picture(BasicsPicture.CENTRES, R.string.lesson_basics_p1),
                LessonPage.Picture(BasicsPicture.PIECES, R.string.lesson_basics_p2),
                LessonPage.AlgorithmDemo(algorithms.single(), R.string.lesson_basics_p3),
                LessonPage.Picture(BasicsPicture.HOLD, R.string.lesson_basics_p4),
            )
        } else {
            buildList {
                add(LessonPage.Goal(stage))
                if (StageCases.of(stage).isNotEmpty()) add(LessonPage.Cases(stage))
                algorithms.forEach { add(LessonPage.AlgorithmDemo(it)) }
                add(LessonPage.Practice(stage))
            }
        }
}

object LessonCatalog {
    private fun stageLesson(stage: Stage, title: Int, summary: Int, tip: Int, vararg algorithms: Pair<Int, List<Move>>) =
        Lesson(stage, title, summary, tip, algorithms.map { (name, moves) -> Algorithm(name, moves, StageGoals.hold(stage)) })

    val lessons: List<Lesson> = listOf(
        Lesson(
            null, R.string.lesson_basics, R.string.lesson_basics_summary, null,
            listOf(Algorithm(R.string.alg_basics, Notation.parse("R U R' U'"))),
        ),
        stageLesson(Stage.WHITE_CROSS, R.string.stage_1, R.string.lesson_1_summary, R.string.lesson_1_tip),
        stageLesson(Stage.WHITE_CORNERS, R.string.stage_2, R.string.lesson_2_summary, R.string.lesson_2_tip, R.string.alg_trigger to BeginnerSolver.TRIGGER),
        stageLesson(
            Stage.MIDDLE_LAYER, R.string.stage_3, R.string.lesson_3_summary, R.string.lesson_3_tip,
            R.string.alg_middle_right to BeginnerSolver.MIDDLE_RIGHT, R.string.alg_middle_left to BeginnerSolver.MIDDLE_LEFT,
        ),
        stageLesson(Stage.YELLOW_CROSS, R.string.stage_4, R.string.lesson_4_summary, R.string.lesson_4_tip, R.string.alg_yellow_cross to BeginnerSolver.YELLOW_CROSS),
        stageLesson(Stage.YELLOW_EDGES, R.string.stage_5, R.string.lesson_5_summary, R.string.lesson_5_tip, R.string.alg_swap_edges to BeginnerSolver.SWAP_EDGES),
        stageLesson(
            Stage.YELLOW_CORNERS_PLACED, R.string.stage_6, R.string.lesson_6_summary, R.string.lesson_6_tip,
            R.string.alg_place_corners to BeginnerSolver.PLACE_CORNERS,
        ),
        stageLesson(Stage.YELLOW_CORNERS_TURNED, R.string.stage_7, R.string.lesson_7_summary, R.string.lesson_7_tip, R.string.alg_trigger to BeginnerSolver.TRIGGER),
    )

    /** The name of a stage algorithm, for "name ×n" under a case. */
    @StringRes
    fun algorithmName(moves: List<Move>): Int? = lessons.flatMap { it.algorithms }.firstOrNull { it.moves == moves }?.name

    @StringRes
    fun caseCaption(id: CaseId): Int = when (id) {
        CaseId.EDGE_DOWN -> R.string.case_edge_down
        CaseId.EDGE_MIDDLE -> R.string.case_edge_middle
        CaseId.EDGE_FLIPPED -> R.string.case_edge_flipped
        CaseId.WHITE_RIGHT -> R.string.case_white_right
        CaseId.WHITE_FRONT -> R.string.case_white_front
        CaseId.WHITE_DOWN -> R.string.case_white_down
        CaseId.GOES_RIGHT -> R.string.case_goes_right
        CaseId.GOES_LEFT -> R.string.case_goes_left
        CaseId.STUCK -> R.string.case_stuck
        CaseId.DOT -> R.string.case_dot
        CaseId.L_SHAPE -> R.string.case_l_shape
        CaseId.LINE -> R.string.case_line
        CaseId.NEIGHBOURS -> R.string.case_neighbours
        CaseId.OPPOSITE -> R.string.case_opposite
        CaseId.ONE_PLACED -> R.string.case_one_placed
        CaseId.NONE_PLACED -> R.string.case_none_placed
        CaseId.YELLOW_RIGHT -> R.string.case_yellow_right
        CaseId.YELLOW_FRONT -> R.string.case_yellow_front
    }
}
