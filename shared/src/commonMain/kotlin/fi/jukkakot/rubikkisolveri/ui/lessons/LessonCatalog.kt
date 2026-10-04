package fi.jukkakot.rubikkisolveri.ui.lessons

import org.jetbrains.compose.resources.StringResource
import fi.jukkakot.rubikkisolveri.res.*
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.beginner.CaseId
import fi.jukkakot.rubikkisolveri.cube.beginner.Stage
import fi.jukkakot.rubikkisolveri.cube.beginner.StageCases
import fi.jukkakot.rubikkisolveri.cube.beginner.StageGoals

/** An algorithm; its demo starts with the cube held as [hold] turns it from white up. */
data class Algorithm(val name: StringResource, val moves: List<Move>, val hold: List<Move> = emptyList())

/** The basics' pictures: what each one shows. */
enum class BasicsPicture { CENTRES, PIECES, HOLD }

/** One page of a lesson; every page fits the screen without scrolling. */
sealed interface LessonPage {
    data class Goal(val stage: Stage) : LessonPage
    data class Cases(val stage: Stage) : LessonPage
    data class AlgorithmDemo(val algorithm: Algorithm, val intro: StringResource? = null) : LessonPage
    data class Practice(val stage: Stage) : LessonPage
    data class Picture(val picture: BasicsPicture, val text: StringResource) : LessonPage
}

/** A lesson: [stage] is null for the basics. */
data class Lesson(
    val stage: Stage?,
    val title: StringResource,
    val summary: StringResource,
    val tip: StringResource?,
    val algorithms: List<Algorithm>,
) {
    val pages: List<LessonPage>
        get() = if (stage == null) {
            listOf(
                LessonPage.Picture(BasicsPicture.CENTRES, Res.string.lesson_basics_p1),
                LessonPage.Picture(BasicsPicture.PIECES, Res.string.lesson_basics_p2),
                LessonPage.AlgorithmDemo(algorithms.single(), Res.string.lesson_basics_p3),
                LessonPage.Picture(BasicsPicture.HOLD, Res.string.lesson_basics_p4),
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
    private fun stageLesson(stage: Stage, title: StringResource, summary: StringResource, tip: StringResource, vararg algorithms: Pair<StringResource, List<Move>>) =
        Lesson(stage, title, summary, tip, algorithms.map { (name, moves) -> Algorithm(name, moves, StageGoals.hold(stage)) })

    val lessons: List<Lesson> = listOf(
        Lesson(
            null, Res.string.lesson_basics, Res.string.lesson_basics_summary, null,
            listOf(Algorithm(Res.string.alg_basics, Notation.parse("R U R' U'"))),
        ),
        stageLesson(Stage.WHITE_CROSS, Res.string.stage_1, Res.string.lesson_1_summary, Res.string.lesson_1_tip),
        stageLesson(Stage.WHITE_CORNERS, Res.string.stage_2, Res.string.lesson_2_summary, Res.string.lesson_2_tip, Res.string.alg_trigger to BeginnerSolver.TRIGGER),
        stageLesson(
            Stage.MIDDLE_LAYER, Res.string.stage_3, Res.string.lesson_3_summary, Res.string.lesson_3_tip,
            Res.string.alg_middle_right to BeginnerSolver.MIDDLE_RIGHT, Res.string.alg_middle_left to BeginnerSolver.MIDDLE_LEFT,
        ),
        stageLesson(Stage.YELLOW_CROSS, Res.string.stage_4, Res.string.lesson_4_summary, Res.string.lesson_4_tip, Res.string.alg_yellow_cross to BeginnerSolver.YELLOW_CROSS),
        stageLesson(Stage.YELLOW_EDGES, Res.string.stage_5, Res.string.lesson_5_summary, Res.string.lesson_5_tip, Res.string.alg_swap_edges to BeginnerSolver.SWAP_EDGES),
        stageLesson(
            Stage.YELLOW_CORNERS_PLACED, Res.string.stage_6, Res.string.lesson_6_summary, Res.string.lesson_6_tip,
            Res.string.alg_place_corners to BeginnerSolver.PLACE_CORNERS,
        ),
        stageLesson(Stage.YELLOW_CORNERS_TURNED, Res.string.stage_7, Res.string.lesson_7_summary, Res.string.lesson_7_tip, Res.string.alg_trigger to BeginnerSolver.TRIGGER),
    )

    /** The name of a stage algorithm, for "name ×n" under a case. */
    fun algorithmName(moves: List<Move>): StringResource? = lessons.flatMap { it.algorithms }.firstOrNull { it.moves == moves }?.name

    fun caseCaption(id: CaseId): StringResource = when (id) {
        CaseId.EDGE_DOWN -> Res.string.case_edge_down
        CaseId.EDGE_MIDDLE -> Res.string.case_edge_middle
        CaseId.EDGE_FLIPPED -> Res.string.case_edge_flipped
        CaseId.WHITE_RIGHT -> Res.string.case_white_right
        CaseId.WHITE_FRONT -> Res.string.case_white_front
        CaseId.WHITE_DOWN -> Res.string.case_white_down
        CaseId.GOES_RIGHT -> Res.string.case_goes_right
        CaseId.GOES_LEFT -> Res.string.case_goes_left
        CaseId.STUCK -> Res.string.case_stuck
        CaseId.DOT -> Res.string.case_dot
        CaseId.L_SHAPE -> Res.string.case_l_shape
        CaseId.LINE -> Res.string.case_line
        CaseId.NEIGHBOURS -> Res.string.case_neighbours
        CaseId.OPPOSITE -> Res.string.case_opposite
        CaseId.ONE_PLACED -> Res.string.case_one_placed
        CaseId.NONE_PLACED -> Res.string.case_none_placed
        CaseId.YELLOW_RIGHT -> Res.string.case_yellow_right
        CaseId.YELLOW_FRONT -> Res.string.case_yellow_front
    }
}
