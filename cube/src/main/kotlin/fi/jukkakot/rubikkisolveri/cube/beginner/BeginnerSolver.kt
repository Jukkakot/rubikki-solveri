package fi.jukkakot.rubikkisolveri.cube.beginner

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Notation
import fi.jukkakot.rubikkisolveri.cube.Stickers

/** What a step does, as data; the app turns it into a sentence. */
sealed interface StepNote {
    /** Put the white–[color] edge next to the [color] centre, white on the white face. */
    data class CrossEdge(val color: CubeColor) : StepNote

    /** Put the white–[a]–[b] corner into its place with the trigger. */
    data class WhiteCorner(val a: CubeColor, val b: CubeColor) : StepNote

    /** Turn the whole cube over: yellow on top. */
    data object TurnOver : StepNote

    /** Put the [a]–[b] edge into the middle layer, to the right or left. */
    data class MiddleEdge(val a: CubeColor, val b: CubeColor) : StepNote

    /** Make the yellow cross; [yellowEdges] is how many yellow edges were up before. */
    data class YellowCross(val yellowEdges: Int) : StepNote

    /** Put the yellow edges where they belong (side colours matching the centres). */
    data object PlaceEdges : StepNote

    /** Put the yellow corners where they belong (twist ignored). */
    data object PlaceCorners : StepNote

    /** Turn one yellow corner yellow-up with the trigger. */
    data object YellowCorner : StepNote

    /** A last turn of the top: the cube is solved. */
    data object FinalTurn : StepNote
}

data class Step(val stage: Stage, val note: StepNote, val moves: List<Move>)

data class BeginnerSolution(val steps: List<Step>) {
    val moves: List<Move> get() = steps.flatMap { it.moves }
}

/**
 * Layer-by-layer solver for people. White cross and corners with white on top, then the cube is
 * turned over and the rest is done with yellow on top, using the classic beginner algorithms.
 */
object BeginnerSolver {
    val TRIGGER: List<Move> = Notation.parse("R' D' R D")
    val MIDDLE_RIGHT: List<Move> = Notation.parse("U R U' R' U' F' U F")
    val MIDDLE_LEFT: List<Move> = Notation.parse("U' L' U L U F U' F'")
    val YELLOW_CROSS: List<Move> = Notation.parse("F R U R' U' F'")
    val SWAP_EDGES: List<Move> = Notation.parse("R U R' U R U2 R' U")
    val PLACE_CORNERS: List<Move> = Notation.parse("U R U' L' U R' U' L")

    private val Y = listOf(emptyList(), Notation.parse("y"), Notation.parse("y2"), Notation.parse("y'"))
    private val U = listOf(emptyList(), Notation.parse("U"), Notation.parse("U2"), Notation.parse("U'"))
    private val D = listOf(emptyList(), Notation.parse("D"), Notation.parse("D2"), Notation.parse("D'"))

    fun solve(start: Cube): BeginnerSolution {
        require(CubeCheck.validity(start).isValid) { "Only valid cubes can be solved" }
        val steps = ArrayList<Step>()
        var cube = start
        fun add(stage: Stage, note: StepNote, moves: List<Move>) {
            if (moves.isEmpty()) return
            steps += Step(stage, note, moves)
            cube = cube.apply(moves)
        }

        // The method starts with white on top.
        if (Checks.whiteFace(cube) != Face.U) add(Stage.WHITE_CROSS, StepNote.TurnOver, wholeCubeTo(cube, Face.U, CubeColor.WHITE))

        // 1. White cross.
        for (color in sideColors(cube)) {
            if (Checks.crossDone(cube)) break
            val moves = CrossSearch.place(cube, color)
            add(Stage.WHITE_CROSS, StepNote.CrossEdge(color), moves)
        }
        check(Checks.crossDone(cube))

        // 2. White corners: one more corner each step.
        while (Checks.solvedWhiteCorners(cube) < 4) {
            val before = Checks.solvedWhiteCorners(cube)
            val macros = buildList {
                for (y in Y) for (d in D) for (n in 1..5) add(y + d + repeat(TRIGGER, n))
            }
            val moves = MacroSearch.find(cube, macros, maxDepth = 2) { c ->
                Checks.crossDone(c) && Checks.solvedWhiteCorners(c) > before && keepsSolvedCorners(cube, c)
            } ?: error("white corner not found")
            val newCorner = (solvedCornerColors(cube.apply(moves)) - solvedCornerColors(cube)).first().filter { it != CubeColor.WHITE }
            add(Stage.WHITE_CORNERS, StepNote.WhiteCorner(newCorner[0], newCorner[1]), moves)
        }

        // 3. Turn over (yellow on top, the front stays in front), then the middle layer.
        add(Stage.MIDDLE_LAYER, StepNote.TurnOver, Notation.parse("z2"))
        while (Checks.solvedMiddleEdges(cube) < 4) {
            val before = Checks.solvedMiddleEdges(cube)
            val macros = buildList {
                for (y in Y) for (u in U) {
                    add(y + u + MIDDLE_RIGHT)
                    add(y + u + MIDDLE_LEFT)
                }
            }
            val moves = MacroSearch.find(cube, macros, maxDepth = 2) { c ->
                Checks.firstLayerDone(c) && Checks.solvedMiddleEdges(c) > before
            } ?: error("middle edge not found")
            val after = cube.apply(moves)
            val edge = Checks.middleEdges(after).first { Checks.edgeSolved(after, it) && !wasSolvedEdge(cube, after, it) }
            add(Stage.MIDDLE_LAYER, StepNote.MiddleEdge(after[edge.stickers[0]], after[edge.stickers[1]]), moves)
        }

        // 4. Yellow cross.
        if (Checks.yellowEdgesUp(cube) < 4) {
            val macros = U.map { it + YELLOW_CROSS }
            val moves = MacroSearch.find(cube, macros, maxDepth = 3) { c ->
                Checks.twoLayersDone(c) && Checks.yellowEdgesUp(c) == 4
            } ?: error("yellow cross not found")
            add(Stage.YELLOW_CROSS, StepNote.YellowCross(Checks.yellowEdgesUp(cube)), moves)
        }

        // 5. Yellow edges in place (their side colours match the centres).
        if (Checks.yellowEdgesSolved(cube) < 4) {
            val macros = buildList {
                for (y in Y) for (u in U) add(y + u + SWAP_EDGES)
                addAll(U.drop(1))
            }
            val moves = MacroSearch.find(cube, macros, maxDepth = 3) { c -> Checks.stageDone(c, Stage.YELLOW_EDGES) }
                ?: error("yellow edges not found")
            add(Stage.YELLOW_EDGES, StepNote.PlaceEdges, moves)
        }

        // 6. Yellow corners into place (twist does not matter yet).
        if (Checks.yellowCornersPlaced(cube) < 4) {
            val macros = Y.map { it + PLACE_CORNERS }
            val moves = MacroSearch.find(cube, macros, maxDepth = 3) { c -> Checks.stageDone(c, Stage.YELLOW_CORNERS_PLACED) }
                ?: error("corner placement not found")
            add(Stage.YELLOW_CORNERS_PLACED, StepNote.PlaceCorners, moves)
        }

        // 7. Turn the yellow corners up: bring each to the front right with U, repeat the trigger.
        // The bottom looks broken in between and comes back when all four are up.
        while (Checks.yellowCornersUp(cube) < 4) {
            val before = Checks.yellowCornersUp(cube)
            val macros = buildList { for (u in U) for (n in listOf(2, 4)) add(u + repeat(TRIGGER, n)) }
            val moves = MacroSearch.find(cube, macros, maxDepth = 1) { c -> Checks.yellowCornersUp(c) > before }
                ?: error("yellow corner not found")
            add(Stage.YELLOW_CORNERS_TURNED, StepNote.YellowCorner, moves)
        }
        if (!cube.isSolved) {
            val fix = U.firstOrNull { cube.apply(it).isSolved } ?: error("not solved after the last stage")
            add(Stage.YELLOW_CORNERS_TURNED, StepNote.FinalTurn, fix)
        }
        check(cube.isSolved)
        return BeginnerSolution(steps)
    }

    private fun repeat(moves: List<Move>, n: Int): List<Move> = List(n) { moves }.flatten()

    /** The four side colours in the order green, red, blue, orange (as the cube is held). */
    private fun sideColors(cube: Cube): List<CubeColor> {
        val order = listOf(CubeColor.GREEN, CubeColor.RED, CubeColor.BLUE, CubeColor.ORANGE)
        val w = Checks.whiteFace(cube)
        return order.filter { Checks.faceOf(cube, it) != w && Checks.faceOf(cube, it) != w.opposite }
    }

    /** Whole-cube turns that bring [color]'s centre to [face]. */
    private fun wholeCubeTo(cube: Cube, face: Face, color: CubeColor): List<Move> {
        val options = listOf("", "x", "x2", "x'", "z", "z'").map { Notation.parse(it) }
        return options.first { cube.apply(it).centre(face) == color }
    }

    /** The corners solved before stay solved (they may move with whole-cube turns). */
    private fun keepsSolvedCorners(before: Cube, after: Cube): Boolean =
        Checks.solvedWhiteCorners(after) >= Checks.solvedWhiteCorners(before)

    private fun wasSolvedEdge(before: Cube, after: Cube, edge: Edge): Boolean {
        val colors = edge.stickers.map { after[it] }.toSet()
        return Checks.middleEdges(before).any { e -> Checks.edgeSolved(before, e) && e.stickers.map { before[it] }.toSet() == colors }
    }

    /** Colour sets of the solved white corners (independent of how the cube is held). */
    private fun solvedCornerColors(cube: Cube): Set<Set<CubeColor>> =
        Checks.cornersAround(Checks.whiteFace(cube)).filter { Checks.cornerSolved(cube, it) }
            .map { c -> c.stickers.map { cube[it] }.toSet() }.toSet()
}

/** Breadth-first search over sequences of macros: fewest macros, then fewest moves. */
internal object MacroSearch {
    fun find(start: Cube, macros: List<List<Move>>, maxDepth: Int, goal: (Cube) -> Boolean): List<Move>? {
        if (goal(start)) return emptyList()
        var frontier = listOf(start to emptyList<Move>())
        repeat(maxDepth) {
            val next = ArrayList<Pair<Cube, List<Move>>>()
            var best: List<Move>? = null
            for ((cube, path) in frontier) {
                for (macro in macros) {
                    val moves = path + macro
                    val result = cube.apply(macro)
                    if (goal(result)) {
                        if (best == null || moves.size < best!!.size) best = moves
                    } else {
                        next += result to moves
                    }
                }
            }
            if (best != null) return best
            frontier = next
        }
        return null
    }
}

/**
 * Places one white cross edge by iterative deepening over face turns, tracking only the stickers
 * of that edge and the cross edges already placed; a per-sticker distance table bounds the search.
 */
internal object CrossSearch {
    private val FACE_MOVES: List<Move> = listOf(Layer.U, Layer.D, Layer.R, Layer.L, Layer.F, Layer.B)
        .flatMap { l -> (1..3).map { Move(l, it) } }

    /** Fewest face turns taking a sticker from index a to index b. */
    private val distance: Array<IntArray> = Array(Stickers.COUNT) { from ->
        val d = IntArray(Stickers.COUNT) { -1 }
        d[from] = 0
        val queue = ArrayDeque(listOf(from))
        while (queue.isNotEmpty()) {
            val s = queue.removeFirst()
            for (m in FACE_MOVES) {
                val t = m.permutation[s]
                if (d[t] < 0) {
                    d[t] = d[s] + 1
                    queue.add(t)
                }
            }
        }
        d
    }

    fun place(cube: Cube, color: CubeColor): List<Move> {
        val white = Checks.whiteFace(cube)
        val placed = Checks.edgesAround(white).filter { Checks.edgeSolved(cube, it) }
        val targetHome = Checks.edgesAround(white).first { e -> e.faces.any { f -> f != white && cube.centre(f) == color } }
        // Current sticker indices of the white sticker of each tracked edge, and their homes.
        val current = ArrayList<Int>()
        val home = ArrayList<Int>()
        for (e in placed + targetHome) {
            val want = e.faces.map { cube.centre(it) }.toSet()
            val at = Edge.entries.first { p -> p.stickers.map { cube[it] }.toSet() == want }
            current += at.stickers.first { cube[it] == CubeColor.WHITE }
            home += e.stickers[e.faces.indexOf(white)]
        }
        // The other sticker of each edge follows the white one, so tracking the white sticker is enough.
        val state = current.toIntArray()
        val goal = home.toIntArray()
        for (limit in 0..10) {
            val path = ArrayList<Move>()
            if (search(state, goal, limit, path, null)) return path
        }
        error("cross edge not found")
    }

    private fun search(state: IntArray, goal: IntArray, left: Int, path: MutableList<Move>, last: Move?): Boolean {
        var h = 0
        for (i in state.indices) h = maxOf(h, distance[state[i]][goal[i]])
        if (h == 0) return true
        if (h > left) return false
        for (m in FACE_MOVES) {
            if (last != null) {
                if (m.layer == last.layer) continue
                // Opposite faces commute: only one order.
                if (m.layer.axisIndex == last.layer.axisIndex && m.layer.ordinal < last.layer.ordinal) continue
            }
            val next = IntArray(state.size) { m.permutation[state[it]] }
            path.add(m)
            if (search(next, goal, left - 1, path, m)) return true
            path.removeAt(path.lastIndex)
        }
        return false
    }
}
