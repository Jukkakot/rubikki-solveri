package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import kotlin.math.ln
import kotlin.math.min

/**
 * What the readings say about each sticker (net order, URFDLB): [votes] per sticker, one sum per
 * [CubeColor] (by ordinal); a reading gives each colour a share by how well it fits (`video-scan-light`),
 * so votes are fractional. The cost of a colour is −ln of its smoothed share; a vote for red lends
 * up to [LEND] votes to orange and the other way round (they are the usual confusion), so a red or
 * orange sticker needs more agreeing readings to be sure. A sticker without readings costs the same
 * for every colour.
 */
class StickerEvidence(val votes: List<DoubleArray>) {
    init {
        require(votes.size == Stickers.COUNT && votes.all { it.size == COLORS })
    }

    /** Readings of [sticker]. */
    fun total(sticker: Int): Double = votes[sticker].sum()

    fun cost(sticker: Int, color: CubeColor): Double {
        val v = votes[sticker]
        val lent = DoubleArray(COLORS) { c -> partner(c)?.let { min(v[it], LEND) } ?: 0.0 }
        val all = v.sum() + lent.sum() + COLORS * SMOOTH
        return -ln((v[color.ordinal] + lent[color.ordinal] + SMOOTH) / all)
    }

    /**
     * The colour [sticker]'s votes alone make sure: at least [VideoScan.MIN_VOTES] votes (twice that
     * for red and orange) and lead the next colour [VideoScan.MARGIN] times over.
     */
    fun sure(sticker: Int): CubeColor? {
        val v = votes[sticker]
        val lead = v.indices.maxBy { v[it] }
        val second = v.indices.filter { it != lead }.maxOf { v[it] }
        val need = if (partner(lead) != null) 2.0 * VideoScan.MIN_VOTES else VideoScan.MIN_VOTES.toDouble()
        return CubeColor.entries[lead].takeIf { v[lead] >= need && v[lead] >= VideoScan.MARGIN * second }
    }

    companion object {
        private const val COLORS = 6

        /** Added to every colour's count before taking shares. */
        const val SMOOTH = 1.0

        /** Most votes a red reading lends to orange (and the other way round). */
        const val LEND = 2.0

        val EMPTY = StickerEvidence(List(Stickers.COUNT) { DoubleArray(COLORS) })

        private fun partner(c: Int): Int? = when (c) {
            CubeColor.RED.ordinal -> CubeColor.ORANGE.ordinal
            CubeColor.ORANGE.ordinal -> CubeColor.RED.ordinal
            else -> null
        }
    }
}

/**
 * The possible cube that fits [StickerEvidence] best ([cube], its [cost]) and, per piece place, how
 * much more the cheapest possible cube with another piece or turn in that place costs ([cornerMargins]
 * by [Corner], [edgeMargins] by [Edge]). The centres come from the colour scheme.
 */
class BestCube(val cube: Cube, val cost: Double, val cornerMargins: DoubleArray, val edgeMargins: DoubleArray) {

    /** How sure [sticker]'s colour is: its place's margin (centres: infinite). */
    fun margin(sticker: Int): Double {
        corner[sticker]?.let { return cornerMargins[it] }
        edge[sticker]?.let { return edgeMargins[it] }
        return Double.POSITIVE_INFINITY
    }

    /** The smallest margin over all places. */
    val minMargin: Double get() = min(cornerMargins.min(), edgeMargins.min())

    /**
     * [sticker]'s margin as it counts for being known: a place not read in full counts only when
     * its stickers that were read are sure from their votes alone (and agree with the cube); else 0.
     */
    fun supportedMargin(sticker: Int, evidence: StickerEvidence): Double {
        val place = placeOf(sticker)
        if (place.any { evidence.total(it) == 0.0 } && place.any { evidence.total(it) > 0.0 && evidence.sure(it) != cube[it] }) return 0.0
        return margin(sticker)
    }

    /** Per sticker: its colour in [cube] is known (supported margin at least [threshold]). */
    fun known(evidence: StickerEvidence, threshold: Double): List<Boolean> =
        List(Stickers.COUNT) { supportedMargin(it, evidence) >= threshold }

    /** The smallest supported margin: the cube is clear when this reaches the threshold. */
    fun clearness(evidence: StickerEvidence): Double = (0 until Stickers.COUNT).minOf { supportedMargin(it, evidence) }

    companion object {
        private val corner: Map<Int, Int> = Corner.entries.flatMap { c -> c.stickers.map { it to c.ordinal } }.toMap()
        private val edge: Map<Int, Int> = Edge.entries.flatMap { e -> e.stickers.map { it to e.ordinal } }.toMap()

        /** The stickers of the corner or edge place [sticker] belongs to (just itself for a centre). */
        fun placeOf(sticker: Int): List<Int> =
            corner[sticker]?.let { Corner.entries[it].stickers } ?: edge[sticker]?.let { Edge.entries[it].stickers } ?: listOf(sticker)

        /**
         * Corners: an 8 × 8 assignment whose cost per place and piece is the cheapest of its three
         * twists; edges: 12 × 12 with two flips. The pieces' orders are walked cheapest first until the
         * best with twist and flip sums fixed is found for each permutation parity; corners and edges of
         * the same parity then make the cube. Null when no possible cube is found (cannot happen with
         * finite costs).
         */
        fun solve(evidence: StickerEvidence, scheme: ColorScheme = ColorScheme.STANDARD): BestCube? {
            val (corners, edges) = pieceCosts(evidence, scheme)
            val bestCorners = PieceSearch.bestByParity(corners, 3)
            val bestEdges = PieceSearch.bestByParity(edges, 2)
            val parity = (0 until 2).minBy { p -> (bestCorners[p]?.cost ?: INF) + (bestEdges[p]?.cost ?: INF) }
            val c = bestCorners[parity] ?: return null
            val e = bestEdges[parity] ?: return null
            val cost = c.cost + e.cost
            if (cost >= INF) return null

            // Margins only matter up to [MARGIN_CAP]: each search stops once nothing cheaper can follow.
            val limit = cost + MARGIN_CAP
            val edgeBest = DoubleArray(2) { bestEdges[it]?.cost ?: INF }
            val cornerBest = DoubleArray(2) { bestCorners[it]?.cost ?: INF }
            val cornerMargins = DoubleArray(8) { slot -> alternative(corners, slot, c.perm[slot], c.ori[slot], 3, edgeBest, limit) - cost }
            val edgeMargins = DoubleArray(12) { slot -> alternative(edges, slot, e.perm[slot], e.ori[slot], 2, cornerBest, limit) - cost }

            val colors = arrayOfNulls<CubeColor>(Stickers.COUNT)
            for (face in Face.entries) colors[Stickers.centre(face)] = scheme[face]
            for (slot in 0 until 8) {
                val faces = Corner.entries[c.perm[slot]].faces
                Corner.entries[slot].stickers.forEachIndexed { j, s -> colors[s] = scheme[faces[(j - c.ori[slot]).mod(3)]] }
            }
            for (slot in 0 until 12) {
                val faces = Edge.entries[e.perm[slot]].faces
                Edge.entries[slot].stickers.forEachIndexed { j, s -> colors[s] = scheme[faces[(j + e.ori[slot]) % 2]] }
            }
            return BestCube(Cube.of(colors.map { it!! }), cost, cornerMargins, edgeMargins)
        }

        /** Only the cost of the best possible cube (no margins): for comparing ways of reading the evidence. */
        fun cost(evidence: StickerEvidence, scheme: ColorScheme = ColorScheme.STANDARD): Double {
            val (corners, edges) = pieceCosts(evidence, scheme)
            val c = PieceSearch.bestByParity(corners, 3)
            val e = PieceSearch.bestByParity(edges, 2)
            return (0 until 2).minOf { p -> (c[p]?.cost ?: INF) + (e[p]?.cost ?: INF) }
        }

        /** Cost per place, piece and turn: corners (8 × 8 × 3) and edges (12 × 12 × 2). */
        private fun pieceCosts(evidence: StickerEvidence, scheme: ColorScheme): Pair<Array<Array<DoubleArray>>, Array<Array<DoubleArray>>> {
            val costOf = Array(Stickers.COUNT) { s -> DoubleArray(6) { c -> evidence.cost(s, CubeColor.entries[c]) } }
            val corners = Array(8) { slot ->
                val stickers = Corner.entries[slot].stickers
                Array(8) { piece ->
                    val faces = Corner.entries[piece].faces
                    DoubleArray(3) { t -> (0 until 3).sumOf { j -> costOf[stickers[j]][scheme[faces[(j - t).mod(3)]].ordinal] } }
                }
            }
            val edges = Array(12) { slot ->
                val stickers = Edge.entries[slot].stickers
                Array(12) { piece ->
                    val faces = Edge.entries[piece].faces
                    DoubleArray(2) { f -> (0 until 2).sumOf { j -> costOf[stickers[j]][scheme[faces[(j + f) % 2]].ordinal] } }
                }
            }
            return corners to edges
        }

        /**
         * The cheapest whole cube with [slot] holding anything but [piece] turned [ori], the other
         * kind of pieces costing [other] per parity; at most [limit].
         */
        private fun alternative(cost: Array<Array<DoubleArray>>, slot: Int, piece: Int, ori: Int, oris: Int, other: DoubleArray, limit: Double): Double {
            val otherPiece = PieceSearch.cheapest(cost.copyWith { s, p, _ -> s == slot && p == piece }, oris, other, limit)
            return PieceSearch.cheapest(cost.copyWith { s, p, o -> s == slot && (p != piece || o == ori) }, oris, other, otherPiece)
        }

        private fun Array<Array<DoubleArray>>.copyWith(forbid: (Int, Int, Int) -> Boolean): Array<Array<DoubleArray>> =
            Array(size) { s -> Array(this[s].size) { p -> DoubleArray(this[s][p].size) { o -> if (forbid(s, p, o)) INF else this[s][p][o] } } }

        internal const val INF = 1e7

        /** Margins are worked out up to this; a larger one is reported as this. */
        const val MARGIN_CAP = 15.0
    }
}

/**
 * Assigning n pieces to n places, each in one of `oris` turns, with the turns summing to 0 mod
 * `oris`: the cheapest such assignment per permutation parity. Permutations come cheapest first
 * (by each place's cheapest turn; Murty's partitioning over [Hungarian]); each is given its best
 * turns with the sum fixed, until no cheaper one can follow.
 */
internal object PieceSearch {
    class Solution(val perm: IntArray, val ori: IntArray, val cost: Double) {
        val parity: Int get() = CubeCheck.parity(perm.toList())
    }

    /** Most permutations walked per search (only reached when many tie, i.e. little is known). */
    const val MAX_PERMUTATIONS = 40

    private class Node(val bound: Double, val perm: IntArray, val fixed: List<Pair<Int, Int>>, val banned: List<Pair<Int, Int>>)

    fun bestByParity(cost: Array<Array<DoubleArray>>, oris: Int): Array<Solution?> {
        val best = arrayOfNulls<Solution>(2)
        walk(cost, oris, goOn = { bound -> best[0] == null || best[1] == null || bound < maxOf(best[0]!!.cost, best[1]!!.cost) }) { solution ->
            if (solution.cost < (best[solution.parity]?.cost ?: BestCube.INF)) best[solution.parity] = solution
        }
        return Array(2) { p -> best[p]?.takeIf { it.cost < BestCube.INF } }
    }

    /** The cheapest solution plus [extra] of its parity, at most [limit]. */
    fun cheapest(cost: Array<Array<DoubleArray>>, oris: Int, extra: DoubleArray, limit: Double): Double {
        var best = limit
        val least = extra.min()
        walk(cost, oris, goOn = { bound -> bound + least < best }) { solution ->
            best = minOf(best, solution.cost + extra[solution.parity])
        }
        return best
    }

    /**
     * Walks the permutations cheapest first by their bound (each place's cheapest turn) while [goOn]
     * says so for the next bound, giving each with its best fixed turns to [visit]; at most
     * [MAX_PERMUTATIONS].
     */
    private fun walk(cost: Array<Array<DoubleArray>>, oris: Int, goOn: (Double) -> Boolean, visit: (Solution) -> Unit) {
        val n = cost.size
        val flat = Array(n) { s -> DoubleArray(n) { p -> cost[s][p].min() } }
        val queue = ArrayList<Node>()
        assign(flat, emptyList(), emptyList())?.let { queue += it }
        var walked = 0
        while (queue.isNotEmpty() && walked < MAX_PERMUTATIONS) {
            val node = queue.minBy { it.bound }
            queue.remove(node)
            if (node.bound >= BestCube.INF || !goOn(node.bound)) break
            walked++
            visit(withTurns(cost, node.perm, oris))
            // Murty: children ban the node's choice at one free place, keeping its choices before it.
            val free = (0 until n).filter { s -> node.fixed.none { it.first == s } }
            for ((i, s) in free.withIndex()) {
                val fixed = node.fixed + free.take(i).map { it to node.perm[it] }
                val banned = node.banned + (s to node.perm[s])
                assign(flat, fixed, banned)?.let { if (goOn(it.bound)) queue += it }
            }
        }
    }

    private fun assign(flat: Array<DoubleArray>, fixed: List<Pair<Int, Int>>, banned: List<Pair<Int, Int>>): Node? {
        val n = flat.size
        val m = Array(n) { flat[it].copyOf() }
        for ((s, p) in banned) m[s][p] = BestCube.INF
        for ((s, p) in fixed) for (q in 0 until n) {
            if (q != p) m[s][q] = BestCube.INF
            if (q != s) m[q][p] = BestCube.INF
        }
        val perm = Hungarian.solve(m)
        val bound = (0 until n).sumOf { m[it][perm[it]] }
        return if (bound >= BestCube.INF) null else Node(bound, perm, fixed, banned)
    }

    /** The cheapest turns for [perm] whose sum is 0 mod [oris] (a small DP over the sum). */
    private fun withTurns(cost: Array<Array<DoubleArray>>, perm: IntArray, oris: Int): Solution {
        val n = perm.size
        val dp = Array(n + 1) { DoubleArray(oris) { Double.POSITIVE_INFINITY } }
        val pick = Array(n) { IntArray(oris) }
        dp[0][0] = 0.0
        for (s in 0 until n) for (sum in 0 until oris) {
            if (dp[s][sum] == Double.POSITIVE_INFINITY) continue
            for (o in 0 until oris) {
                val next = (sum + o) % oris
                val c = dp[s][sum] + cost[s][perm[s]][o]
                if (c < dp[s + 1][next]) {
                    dp[s + 1][next] = c
                    pick[s][next] = o
                }
            }
        }
        val ori = IntArray(n)
        var sum = 0
        for (s in n - 1 downTo 0) {
            ori[s] = pick[s][sum]
            sum = (sum - ori[s]).mod(oris)
        }
        return Solution(perm.copyOf(), ori, dp[n][0])
    }
}
