package fi.jukkakot.rubikkisolveri.cube.beginner

import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Sequences
import fi.jukkakot.rubikkisolveri.cube.Stickers

/** The typical situations of each stage; the app maps each to a caption. */
enum class CaseId(val stage: Stage) {
    EDGE_DOWN(Stage.WHITE_CROSS), EDGE_MIDDLE(Stage.WHITE_CROSS), EDGE_FLIPPED(Stage.WHITE_CROSS),
    WHITE_RIGHT(Stage.WHITE_CORNERS), WHITE_FRONT(Stage.WHITE_CORNERS), WHITE_DOWN(Stage.WHITE_CORNERS),
    GOES_RIGHT(Stage.MIDDLE_LAYER), GOES_LEFT(Stage.MIDDLE_LAYER), STUCK(Stage.MIDDLE_LAYER),
    DOT(Stage.YELLOW_CROSS), L_SHAPE(Stage.YELLOW_CROSS), LINE(Stage.YELLOW_CROSS),
    NEIGHBOURS(Stage.YELLOW_EDGES), OPPOSITE(Stage.YELLOW_EDGES),
    ONE_PLACED(Stage.YELLOW_CORNERS_PLACED), NONE_PLACED(Stage.YELLOW_CORNERS_PLACED),
    YELLOW_RIGHT(Stage.YELLOW_CORNERS_TURNED), YELLOW_FRONT(Stage.YELLOW_CORNERS_TURNED),
}

/**
 * One situation: [position] (held as the stage holds the cube), the [highlight]ed stickers, and
 * the [moves] that deal with it, which use [algorithm] [times] times (null for plain turns).
 */
data class StageCase(
    val id: CaseId,
    val position: Cube,
    val highlight: Set<Int>,
    val moves: List<Move>,
    val algorithm: List<Move>?,
    val times: Int,
) {
    val stage: Stage get() = id.stage
}

/**
 * The cases are built backwards: start from the stage's goal with the unsolved rest scrambled by
 * moves that keep the goal ([noise]), then undo the case's moves. So doing the moves from the
 * pictured position always reaches the goal ([reached]).
 */
object StageCases {
    private val T = BeginnerSolver.TRIGGER
    private val MR = BeginnerSolver.MIDDLE_RIGHT
    private val ML = BeginnerSolver.MIDDLE_LEFT
    private val YC = BeginnerSolver.YELLOW_CROSS
    private val SWAP = BeginnerSolver.SWAP_EDGES
    private val PC = BeginnerSolver.PLACE_CORNERS

    private fun p(text: String) = Notation.parse(text)
    private fun times(moves: List<Move>, n: Int) = List(n) { moves }.flatten()

    // Scrambles that keep what each stage's cases aim for (see [reached]); none turns the whole cube.
    private val NOISE_CROSS = p("R B' D L2 B R' D2 L' B2 D'") // keeps the white–green edge (no U or F)
    private val NOISE_CORNERS = p("L D L' B' D2 B D L' D' L D2") // keeps the cross and the front right corner
    private val NOISE_MIDDLE_FR = p("U") + ML + p("U2") + SWAP + p("U'") + PC // keeps the first layer and the front right edge
    private val NOISE_MIDDLE_FL = p("U'") + MR + p("U2") + SWAP + p("U") + PC
    private val TWIST_TWO = times(T, 2) + p("U") + times(T, 4) + p("U'") // twists two top corners
    private val NOISE_YELLOW_CROSS = SWAP + p("U") + PC + p("U2") + PC
    private val NOISE_YELLOW_EDGES = PC + TWIST_TWO
    private val NOISE_YELLOW_CORNERS = p("U2") + TWIST_TWO + p("U2")

    val all: List<StageCase> by lazy {
        listOf(
            case(CaseId.EDGE_DOWN, NOISE_CROSS, p("F2"), null, 0),
            case(CaseId.EDGE_MIDDLE, NOISE_CROSS, p("F"), null, 0),
            case(CaseId.EDGE_FLIPPED, NOISE_CROSS, p("F U' R U"), null, 0),
            case(CaseId.WHITE_RIGHT, NOISE_CORNERS, times(T, 1), T, 1),
            case(CaseId.WHITE_FRONT, NOISE_CORNERS, times(T, 5), T, 5),
            case(CaseId.WHITE_DOWN, NOISE_CORNERS, times(T, 3), T, 3),
            case(CaseId.GOES_RIGHT, NOISE_MIDDLE_FR, MR, MR, 1),
            case(CaseId.GOES_LEFT, NOISE_MIDDLE_FL, ML, ML, 1),
            case(CaseId.STUCK, NOISE_MIDDLE_FR, MR + p("U2") + MR, MR, 2),
            case(CaseId.DOT, NOISE_YELLOW_CROSS, YC + p("U2") + YC + YC, YC, 3),
            case(CaseId.L_SHAPE, NOISE_YELLOW_CROSS, YC + YC, YC, 2),
            case(CaseId.LINE, NOISE_YELLOW_CROSS, YC, YC, 1),
            case(CaseId.NEIGHBOURS, NOISE_YELLOW_EDGES, SWAP, SWAP, 1),
            case(CaseId.OPPOSITE, NOISE_YELLOW_EDGES, p("U'") + SWAP + p("U2") + SWAP, SWAP, 2),
            case(CaseId.ONE_PLACED, NOISE_YELLOW_CORNERS, PC, PC, 1),
            case(CaseId.NONE_PLACED, NOISE_YELLOW_CORNERS, PC + p("y") + PC + p("y'"), PC, 2),
            case(CaseId.YELLOW_RIGHT, emptyList(), times(T, 2) + p("U") + times(T, 4) + p("U'"), T, 2),
            case(CaseId.YELLOW_FRONT, emptyList(), times(T, 4) + p("U") + times(T, 2) + p("U'"), T, 4),
        )
    }

    fun of(stage: Stage): List<StageCase> = all.filter { it.stage == stage }

    /** Whether [cube] shows what [id]'s moves promise: the stage's piece in place, or the stage done. */
    fun reached(id: CaseId, cube: Cube): Boolean = when (id.stage) {
        Stage.WHITE_CROSS -> Checks.edgeSolved(cube, Edge.UF)
        Stage.WHITE_CORNERS -> Checks.crossDone(cube) && Checks.cornerSolved(cube, Corner.URF)
        Stage.MIDDLE_LAYER -> Checks.firstLayerDone(cube) &&
            Checks.edgeSolved(cube, if (id == CaseId.GOES_LEFT) Edge.FL else Edge.FR)
        else -> Checks.stageDone(cube, id.stage)
    }

    private fun case(id: CaseId, noise: List<Move>, moves: List<Move>, algorithm: List<Move>?, times: Int): StageCase {
        val goal = Cube.solved().apply(StageGoals.hold(id.stage)).apply(noise)
        val position = goal.apply(Sequences.inverse(moves))
        return StageCase(id, position, highlight(id, position), moves, algorithm, times)
    }

    /** The piece the case is about, wherever it is in [cube]. */
    private fun highlight(id: CaseId, cube: Cube): Set<Int> {
        val solved = Cube.solved().apply(StageGoals.hold(id.stage))
        fun edge(home: Edge): List<Int> {
            val colors = home.stickers.map { solved[it] }.toSet()
            return Edge.entries.first { e -> e.stickers.map { cube[it] }.toSet() == colors }.stickers
        }
        fun corner(home: Corner): List<Int> {
            val colors = home.stickers.map { solved[it] }.toSet()
            return Corner.entries.first { c -> c.stickers.map { cube[it] }.toSet() == colors }.stickers
        }
        val yellow = Checks.yellowFace(cube)
        return when (id.stage) {
            Stage.WHITE_CROSS -> edge(Edge.UF)
            Stage.WHITE_CORNERS -> corner(Corner.URF)
            Stage.MIDDLE_LAYER -> edge(if (id == CaseId.GOES_LEFT) Edge.FL else Edge.FR)
            Stage.YELLOW_CROSS -> Checks.edgesAround(yellow).map { it.stickers[it.faces.indexOf(yellow)] }
                .filter { cube[it] == CubeColor.YELLOW } + Stickers.centre(yellow)
            Stage.YELLOW_EDGES -> Checks.edgesAround(yellow).filter { Checks.edgeSolved(cube, it) }.flatMap { it.stickers }
            // The corner already in place, or all four when none is.
            Stage.YELLOW_CORNERS_PLACED -> Checks.cornersAround(yellow).filter { Checks.cornerPlaced(cube, it) }
                .ifEmpty { Checks.cornersAround(yellow) }.flatMap { it.stickers }
            Stage.YELLOW_CORNERS_TURNED -> corner(Corner.URF)
        }.toSet()
    }
}
