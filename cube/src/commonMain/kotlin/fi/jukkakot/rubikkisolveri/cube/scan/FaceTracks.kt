package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import kotlin.math.ln
import kotlin.math.min

/**
 * Which face each followed face ([Track]) is, by the rules of a real cube (`scan-rules` design 3–5).
 * Each track's face and turn ([FaceOption]) is a hypothesis with a cost: its stickers against what
 * the settled tracks say (the same face, and the pieces they make with the neighbouring faces), and
 * how its centre looks, with a small weight. Tracks seen in one picture are bound by [PairRules].
 * The open tracks are assigned together each frame (a branch and bound); a track settles when every
 * other way costs [ASSIGN_MARGIN] more, first its face, then also its turn, and re-opens when another
 * way becomes cheaper. The assigned tracks' newest readings are the evidence for [BestCube].
 */
class FaceTracks(private val scheme: ColorScheme = ColorScheme.STANDARD) {
    private val tracker = Tracker()
    private val rules = PairRules()
    private var refs: Map<CubeColor, Tone> = defaultRefs()
    private var known: Map<CubeColor, Rgb> = emptyMap()

    private class State {
        var votes: List<DoubleArray> = List(9) { DoubleArray(6) }
        var totals = DoubleArray(9)
        var centre: Rgb? = null

        /** The settled face (null: open), the settled face and turn (null: open; [FaceOption.NONE]: no face). */
        var face: Face? = null
        var option: Int? = null

        /** This frame's assignment. */
        var assigned: Int = FaceOption.NONE

        /** Since when the face is open while the track counts. */
        var openSince: Long? = null

        /** The next best face while the face is open. */
        var otherFace: Face? = null
    }

    private val states = HashMap<Track, State>()
    private fun Track.state(): State = states.getOrPut(this) { State() }

    /** The tracks of the last picture's faces (null: left out). */
    var picture: List<Pair<Track, TrackReading>?> = emptyList()
        private set

    var evidence: StickerEvidence = StickerEvidence.EMPTY
        private set
    var best: BestCube? = null
        private set

    /** The track's settled face, if any. */
    fun faceOf(track: Track): Face? = track.state().face

    /** The track's face and turn: settled, else this frame's assignment (null: none, or the face open). */
    fun optionOf(track: Track): Int? = track.state().let { s -> if (s.face == null) null else s.option ?: s.assigned.takeIf { it != FaceOption.NONE } }

    /** Whether the track's face and turn are both settled. */
    fun settled(track: Track): Boolean = track.state().option.let { it != null && it != FaceOption.NONE }

    /** Tracks with enough readings to count. */
    private fun counting(): List<Track> = tracker.tracks.filter { it.size >= MIN_READINGS }

    /** Every counting track is settled (face and turn, or no face). */
    val allSettled: Boolean get() = counting().all { it.state().option != null }

    /** Faces that an open track could be: its assignment and the next best face. */
    val unsureFaces: Set<Face>
        get() = counting().filter { it.state().face == null }.flatMap { t -> listOfNotNull(FaceOption.NONE.let { n -> t.state().assigned.takeIf { it != n }?.let(FaceOption::face) }, t.state().otherFace) }.toSet()

    /** Faces with a track whose face is settled. */
    val seenFaces: Set<Face> get() = tracker.tracks.mapNotNull { it.state().face }.toSet()

    /** Faces with a track whose face and turn are settled. */
    val settledFaces: Set<Face> get() = tracker.tracks.filter { settled(it) }.map { FaceOption.face(it.state().option!!) }.toSet()

    /** Some counting track's face has been open for [HINT_MILLIS] at [now]. */
    fun undecided(now: Long): Boolean = counting().any { t -> t.state().let { it.face == null && it.openSince?.let { s -> now - s >= HINT_MILLIS } == true } }

    /** The picture's tracks with their readings, newest last, the readings assigned per face (for samples). */
    fun assignedReadings(): Map<Face, List<Pair<TrackReading, Int>>> {
        val out = HashMap<Face, MutableList<Pair<TrackReading, Int>>>()
        for (t in tracker.tracks) {
            val o = assignment(t) ?: continue
            val list = out.getOrPut(FaceOption.face(o)) { ArrayList() }
            for (r in t.readings) list += r to FaceOption.turn(o)
        }
        return out.mapValues { (_, l) -> l.sortedBy { it.first.seq }.takeLast(MAX_FACE_READINGS) }
    }

    /** One line per counting track (for tuning): id, readings, settled face/option or assignment. */
    fun describe(): String = counting().sortedBy { it.id }.joinToString("; ") { t ->
        val s = t.state()
        val o = s.option ?: s.assigned
        val what = if (o == FaceOption.NONE) "none" else "${FaceOption.face(o)}${FaceOption.turn(o)}"
        "#${t.id}x${t.size} " + when {
            s.option != null -> "=$what"
            s.face != null -> "${s.face}?$what"
            else -> "?$what/${s.otherFace}"
        }
    }

    /** Per settled face, the turn of its most read settled track (for the log). */
    fun rotations(): Map<Face, Int> = tracker.tracks.filter { settled(it) }.groupBy { FaceOption.face(it.state().option!!) }
        .mapValues { (_, l) -> FaceOption.turn(l.maxBy { it.size }.state().option!!) }

    private fun assignment(t: Track): Int? {
        if (t.size < MIN_READINGS) return null
        val s = t.state()
        val o = s.option ?: s.assigned
        return o.takeIf { it != FaceOption.NONE }
    }

    fun onFrame(faces: List<FaceReading>, now: Long) {
        picture = tracker.onFrame(faces, now) { rgb -> nameSticker(rgb) }
        rules.observe(picture.filterNotNull())
        updateRefs()
        for ((track, reading) in picture.filterNotNull()) {
            shareReading(reading)
            track.updateLeading()
        }
        for (t in tracker.tracks) if (now - t.lastAt <= Tracker.GAP_MILLIS) votesOf(t)
        recheck()
        assignOpen(now)
        evidence = evidenceOf()
        best = BestCube.solve(evidence, scheme)
    }

    // --- colours ---------------------------------------------------------------------------------

    private fun defaultRefs(): Map<CubeColor, Tone> = CubeColor.entries.associateWith { Tone(ColorClassifier.DEFAULT_PALETTE.getValue(it)) }

    private fun nameSticker(rgb: Rgb): CubeColor = Tone(rgb).let { t -> refs.minBy { it.value.distance(t) }.key }

    /**
     * The cube's own centres as references: the centres of the tracks whose face is settled, the
     * default palette for colours not seen yet. When they change, every reading is named again.
     */
    private fun updateRefs() {
        val byColor = HashMap<CubeColor, MutableList<Rgb>>()
        for (t in tracker.tracks) {
            val face = t.state().face ?: continue
            for (r in t.readings) r.face.colors[CENTRE]?.let { byColor.getOrPut(scheme[face]) { ArrayList() } += it }
        }
        val newKnown = byColor.mapValues { (_, l) -> Rgb(l.sumOf { it.r } / l.size, l.sumOf { it.g } / l.size, l.sumOf { it.b } / l.size) }
        val changed = newKnown.keys != known.keys || newKnown.any { (c, rgb) -> known[c]?.let { o -> kotlin.math.abs(o.r - rgb.r) + kotlin.math.abs(o.g - rgb.g) + kotlin.math.abs(o.b - rgb.b) > REF_STEP } != false }
        if (!changed) return
        known = newKnown
        refs = CubeColor.entries.associateWith { c -> known[c]?.let(::Tone) ?: Tone(ColorClassifier.DEFAULT_PALETTE.getValue(c)) }
        for (t in tracker.tracks) {
            for (r in t.readings) {
                r.names = r.face.colors.map { it?.let(::nameSticker) }
                shareReading(r)
            }
            t.updateLeading()
            votesOf(t)
        }
    }

    private fun shareReading(r: TrackReading) {
        r.shares = r.tones.mapIndexed { n, tone ->
            tone?.let { t -> ColorClassifier.sharesOf(DoubleArray(6) { c -> refs.getValue(CubeColor.entries[c]).distance(t) }).also { s -> VideoScan.weight(r.face.colors[n]!!).let { w -> for (c in s.indices) s[c] *= w } } }
        }
    }

    private fun votesOf(t: Track) {
        val s = t.state()
        val votes = List(9) { DoubleArray(6) }
        var cr = 0
        var cg = 0
        var cb = 0
        var cn = 0
        for (r in t.readings) {
            for (m in 0 until 9) r.shares[r.at(m)]?.let { sh -> for (c in 0 until 6) votes[m][c] += sh[c] }
            r.face.colors[CENTRE]?.let {
                cr += it.r
                cg += it.g
                cb += it.b
                cn++
            }
        }
        s.votes = votes
        s.totals = DoubleArray(9) { votes[it].sum() }
        s.centre = if (cn == 0) null else Rgb(cr / cn, cg / cn, cb / cn)
    }

    // --- costs -----------------------------------------------------------------------------------

    /** Per net sticker, the settled tracks' votes (each track's own, net-aligned). */
    private class Tables(val votes: Array<DoubleArray>, val totals: DoubleArray)

    private fun tables(skip: Track? = null): Tables {
        val votes = Array(Stickers.COUNT) { DoubleArray(6) }
        for (t in tracker.tracks) {
            if (t === skip || t.size < MIN_READINGS) continue
            val o = t.state().option ?: continue
            if (o == FaceOption.NONE) continue
            add(votes, t, o, 1.0)
        }
        return Tables(votes, DoubleArray(Stickers.COUNT) { votes[it].sum() })
    }

    private fun add(votes: Array<DoubleArray>, t: Track, o: Int, sign: Double) {
        val face = FaceOption.face(o)
        val turn = FaceOption.turn(o)
        val s = t.state()
        for (n in 0 until 9) {
            if (n == CENTRE) continue
            val v = s.votes[RotationSearch.turnIndex(n, turn)]
            val at = votes[face.ordinal * 9 + n]
            for (c in 0 until 6) at[c] += sign * v[c]
        }
    }

    /** The smoothed share of each colour at net sticker [i], the votes scaled to at most [TABLE_CAP]. */
    private fun smoothed(tables: Tables, i: Int): DoubleArray? {
        val total = tables.totals[i]
        if (total <= 1e-9) return null
        val scale = if (total > TABLE_CAP) TABLE_CAP / total else 1.0
        val all = total * scale + 6 * SMOOTH
        return DoubleArray(6) { c -> (tables.votes[i][c] * scale + SMOOTH) / all }
    }

    /**
     * Per net sticker and colour, how much likelier the tables make that colour there than chance:
     * the same sticker's votes (6 × share) times what the pieces allow with the neighbouring faces'
     * stickers (an edge's other sticker; a corner's other two, in their order round the corner).
     */
    private fun likelihoods(tables: Tables): Array<DoubleArray> {
        val q = Array(Stickers.COUNT) { smoothed(tables, it) }
        return Array(Stickers.COUNT) { i ->
            val same = q[i]
            DoubleArray(6) { c -> (same?.let { 6 * it[c] } ?: 1.0) * pieceLikelihood(q, i, c) }
        }
    }

    private fun pieceLikelihood(q: Array<DoubleArray?>, i: Int, c: Int): Double {
        val p = PARTNERS[i]
        if (p.isEmpty()) return 1.0
        if (p.size == 1) {
            val qj = q[p[0]] ?: return 1.0
            return (0 until 6).sumOf { b -> qj[b] * adj[c][b] } / (4.0 / 6)
        }
        val qj = q[p[0]]
        val qk = q[p[1]]
        if (qj != null && qk != null) {
            var sum = 0.0
            for (b in 0 until 6) for (d in 0 until 6) if (corner[c][b][d]) sum += qj[b] * qk[d]
            return sum / (1.0 / 9)
        }
        val one = qj ?: qk ?: return 1.0
        return (0 until 6).sumOf { b -> one[b] * adj[c][b] } / (4.0 / 6)
    }

    /** The content cost of [t] as [o] against [like] ([likelihoods]); [FaceOption.NONE]: [NONE_COST]. */
    private fun contentCost(t: Track, o: Int, like: Array<DoubleArray>): Double {
        if (o == FaceOption.NONE) return NONE_COST
        val s = t.state()
        val face = FaceOption.face(o)
        val turn = FaceOption.turn(o)
        var cost = 0.0
        for (n in 0 until 9) {
            if (n == CENTRE) continue
            val m = RotationSearch.turnIndex(n, turn)
            val total = s.totals[m]
            if (total <= 1e-9) continue
            val l = like[face.ordinal * 9 + n]
            var e = 0.0
            for (c in 0 until 6) e += s.votes[m][c] / total * l[c]
            cost += min(total, FULL_WEIGHT) / FULL_WEIGHT * -ln(e)
        }
        return cost + lookCost(t, face)
    }

    /** How far [t]'s centre is from [face]'s colour beyond the nearest colour (the cube's own centres where known), weighted. */
    private fun lookCost(t: Track, face: Face): Double {
        val centre = t.state().centre ?: return 0.0
        val d = ColorClassifier.centreDistances(centre, known)
        val least = d.values.min()
        return LOOK_WEIGHT * min(d.getValue(scheme[face]) - least, LOOK_CAP)
    }

    /** Every option's cost for [t] alone against [like], hard rules against the settled tracks applied. */
    private fun unary(t: Track, like: Array<DoubleArray>, settled: List<Track>, domain: IntArray): DoubleArray {
        val bound = settled.filter { it !== t }.mapNotNull { s -> rules.allowed(t, s)?.let { it to s.state().option!! } }
        return DoubleArray(domain.size) { k ->
            val o = domain[k]
            if (bound.any { (allowed, so) -> !allowed[o * FaceOption.COUNT + so] }) INF else contentCost(t, o, like)
        }
    }

    /**
     * The cost of [a] as [oa] and [b] as [ob] together beyond their own: the same face (their stickers
     * agreeing, or not), neighbouring faces (the stickers they share pieces with fitting real pieces),
     * or a rule against it ([INF]).
     */
    private fun pairCost(a: Track, oa: Int, b: Track, ob: Int, allowed: BooleanArray?): Double {
        if (allowed != null && !allowed[oa * FaceOption.COUNT + ob]) return INF
        if (oa == FaceOption.NONE || ob == FaceOption.NONE) return 0.0
        val fa = FaceOption.face(oa)
        val fb = FaceOption.face(ob)
        val sa = a.state()
        val sb = b.state()
        if (fa == fb) {
            var cost = 0.0
            for (n in 0 until 9) {
                if (n == CENTRE) continue
                val ma = RotationSearch.turnIndex(n, FaceOption.turn(oa))
                val mb = RotationSearch.turnIndex(n, FaceOption.turn(ob))
                val ta = sa.totals[ma]
                val tb = sb.totals[mb]
                if (ta <= 1e-9 || tb <= 1e-9) continue
                cost += 0.5 * (agreement(sa.votes[ma], ta, sb.votes[mb], tb) + agreement(sb.votes[mb], tb, sa.votes[ma], ta))
            }
            return cost
        }
        if (fa.opposite == fb) return 0.0
        var cost = 0.0
        for ((na, nb) in SHARED.getValue(fa to fb)) {
            val ma = RotationSearch.turnIndex(na, FaceOption.turn(oa))
            val mb = RotationSearch.turnIndex(nb, FaceOption.turn(ob))
            val ta = sa.totals[ma]
            val tb = sb.totals[mb]
            if (ta <= 1e-9 || tb <= 1e-9) continue
            val scale = if (tb > TABLE_CAP) TABLE_CAP / tb else 1.0
            val all = tb * scale + 6 * SMOOTH
            var e = 0.0
            for (c in 0 until 6) for (d in 0 until 6) if (adj[c][d] > 0) e += sa.votes[ma][c] / ta * (sb.votes[mb][d] * scale + SMOOTH) / all
            cost += min(min(ta, tb), FULL_WEIGHT) / FULL_WEIGHT * -ln(e / (4.0 / 6))
        }
        return cost
    }

    /** −ln of how likely [a]'s colours are given [b]'s (smoothed), against chance, weighted by [a]'s readings. */
    private fun agreement(a: DoubleArray, ta: Double, b: DoubleArray, tb: Double): Double {
        val scale = if (tb > TABLE_CAP) TABLE_CAP / tb else 1.0
        val all = tb * scale + 6 * SMOOTH
        var e = 0.0
        for (c in 0 until 6) e += a[c] / ta * 6 * (b[c] * scale + SMOOTH) / all
        return min(ta, FULL_WEIGHT) / FULL_WEIGHT * -ln(e)
    }

    // --- settling --------------------------------------------------------------------------------

    /** Settled tracks whose way is no longer the cheapest (against the others) re-open. */
    private fun recheck() {
        val settled = tracker.tracks.filter { it.size >= MIN_READINGS && it.state().face != null }
        if (settled.isEmpty()) return
        val all = IntArray(FaceOption.COUNT) { it }
        for (t in settled) {
            val s = t.state()
            val own = s.option?.takeIf { it != FaceOption.NONE }
            val like = likelihoods(tables(skip = t))
            val others = settled.filter { it !== t && it.state().option != null && it.state().option != FaceOption.NONE }
            val cost = unary(t, like, others, all)
            val face = s.face!!
            val ownFace = (0 until 4).minOf { cost[FaceOption.of(face, it)] }
            val otherFace = all.filter { it == FaceOption.NONE || FaceOption.face(it) != face }.minOf { cost[it] }
            if (otherFace < ownFace - REOPEN_SLACK) {
                s.face = null
                s.option = null
            } else if (own != null && all.filter { it != own }.minOf { cost[it] } < cost[own] - REOPEN_SLACK) {
                s.option = null
            }
        }
    }

    /**
     * The open tracks (at most [MAX_OPEN], the most read first) assigned together: the cheapest
     * assignment by branch and bound, the settled tracks fixed. Then each settles its face, or its
     * face and turn, where every assignment that changes it costs [ASSIGN_MARGIN] more.
     */
    private fun assignOpen(now: Long) {
        val settled = tracker.tracks.filter { it.size >= MIN_READINGS && it.state().option.let { o -> o != null && o != FaceOption.NONE } }
        val open = tracker.tracks.filter { it.size >= MIN_READINGS && it.state().option == null }.sortedByDescending { it.size }.take(MAX_OPEN)
        for (t in tracker.tracks) if (t.size >= MIN_READINGS && t.state().option == null && t !in open) t.state().assigned = FaceOption.NONE
        if (open.isEmpty()) return
        val like = likelihoods(tables())
        val domains = open.map { t -> t.state().face?.let { f -> IntArray(4) { FaceOption.of(f, it) } } ?: IntArray(FaceOption.COUNT) { it } }
        val unary = open.mapIndexed { i, t -> unary(t, like, settled, domains[i]) }
        val pairs = Array(open.size) { i ->
            Array(open.size) { j ->
                if (j <= i) null else {
                    val allowed = rules.allowed(open[i], open[j])
                    Array(domains[i].size) { a -> DoubleArray(domains[j].size) { b -> pairCost(open[i], domains[i][a], open[j], domains[j][b], allowed) } }
                }
            }
        }
        val search = Search(unary, pairs)
        val (cost, pick) = search.best() ?: return
        for ((i, t) in open.withIndex()) {
            val s = t.state()
            val o = domains[i][pick[i]]
            s.assigned = o
            val optionMargin = search.best(cap = cost + ASSIGN_MARGIN, ban = i to { k -> k == pick[i] })?.first?.minus(cost) ?: Double.POSITIVE_INFINITY
            if (optionMargin >= ASSIGN_MARGIN) {
                s.option = o
                s.face = if (o == FaceOption.NONE) null else FaceOption.face(o)
                s.otherFace = null
            } else if (s.face == null && o != FaceOption.NONE) {
                val face = FaceOption.face(o)
                val alt = search.best(cap = cost + ASSIGN_MARGIN, ban = i to { k -> domains[i][k] != FaceOption.NONE && FaceOption.face(domains[i][k]) == face })
                if (alt == null || alt.first - cost >= ASSIGN_MARGIN) {
                    s.face = face
                    s.otherFace = null
                } else {
                    s.otherFace = domains[i][alt.second[i]].takeIf { it != FaceOption.NONE }?.let(FaceOption::face)
                }
            }
            s.openSince = if (s.face == null && s.option == null) s.openSince ?: now else null
        }
    }

    /** The newest readings of the tracks assigned to each face as evidence per net sticker. */
    private fun evidenceOf(): StickerEvidence {
        val votes = List(Stickers.COUNT) { DoubleArray(6) }
        for ((face, list) in assignedReadings()) for ((r, turn) in list) {
            for (n in 0 until 9) {
                if (n == CENTRE) continue
                r.shares[r.at(RotationSearch.turnIndex(n, turn))]?.let { sh -> for (c in 0 until 6) votes[face.ordinal * 9 + n][c] += sh[c] }
            }
        }
        return StickerEvidence(votes)
    }

    /**
     * Branch and bound over the open tracks' options: [unary] per track and option, [pairs] per pair
     * (i < j) and option pair. The cheapest assignment and its cost, at most [cap], with [ban] taking
     * options out of one track's domain; null when none is found under the cap.
     */
    private class Search(val unary: List<DoubleArray>, val pairs: Array<Array<Array<DoubleArray>?>>) {
        private val n = unary.size
        private val minPair = Array(n) { i -> DoubleArray(n) { j -> if (j <= i) 0.0 else pairs[i][j]!!.minOf { row -> row.min() }.coerceAtMost(0.0) } }

        fun best(cap: Double = Double.POSITIVE_INFINITY, ban: Pair<Int, (Int) -> Boolean>? = null): Pair<Double, IntArray>? {
            val pick = IntArray(n)
            var bestCost = cap
            var bestPick: IntArray? = null
            var nodes = 0
            val order = Array(n) { i -> unary[i].indices.filter { k -> unary[i][k] < INF && (ban == null || ban.first != i || !ban.second(k)) }.sortedBy { unary[i][it] } }
            // Pairs not yet both assigned can still lower the cost by at most their least value.
            val restPairs = DoubleArray(n + 1)
            for (d in n - 1 downTo 0) restPairs[d] = restPairs[d + 1] + (d + 1 until n).sumOf { minPair[d][it] }

            fun go(d: Int, cost: Double) {
                if (++nodes > MAX_NODES) return
                if (d == n) {
                    if (cost < bestCost) {
                        bestCost = cost
                        bestPick = pick.copyOf()
                    }
                    return
                }
                // Lower bound for what follows: each later track at its cheapest given the ones before
                // this one, the pairs with this one and among the later ones at their least.
                var rest = restPairs[d]
                for (u in d + 1 until n) rest += order[u].minOfOrNull { k -> unary[u][k] + (0 until d).sumOf { w -> pairs[w][u]!![pick[w]][k] } } ?: INF
                for (k in order[d]) {
                    var c = cost + unary[d][k]
                    for (w in 0 until d) c += pairs[w][d]!![pick[w]][k]
                    if (c >= INF || c + rest >= bestCost) continue
                    pick[d] = k
                    go(d + 1, c)
                }
            }
            go(0, 0.0)
            return bestPick?.let { bestCost to it }
        }
    }

    companion object {
        private const val CENTRE = 4
        const val INF = 1e9

        /** Readings a track needs before it counts. */
        const val MIN_READINGS = 3

        /** Open tracks assigned together at most. */
        const val MAX_OPEN = 8

        /** Search nodes per branch and bound at most. */
        const val MAX_NODES = 20_000

        /** A track settles when every other way costs this much more. */
        const val ASSIGN_MARGIN = 3.0

        /** A settled track re-opens when another way is cheaper by this much. */
        const val REOPEN_SLACK = 0.0

        /** The cost of taking a track for no face (a stray lattice). */
        const val NONE_COST = 6.0

        /** Weight of the centre's look (Lab distance beyond the nearest colour), and its cap. */
        const val LOOK_WEIGHT = 0.3
        const val LOOK_CAP = 40.0

        /** A track's sticker counts fully with this many votes. */
        const val FULL_WEIGHT = 3.0

        /** Votes of a table sticker taken at most (scaled down beyond). */
        const val TABLE_CAP = 20.0

        /** Added to every colour's votes before taking shares. */
        const val SMOOTH = 1.0

        /** Newest readings per face in the evidence. */
        const val MAX_FACE_READINGS = 40

        /** A centre reference moving less than this (summed RGB) does not rename the readings. */
        const val REF_STEP = 6

        /** A face open this long asks to turn the cube. */
        const val HINT_MILLIS = 2_000L

        private val scheme0 = ColorScheme.STANDARD

        /** Colours that can share a piece: different and not opposite. */
        private val adj: Array<DoubleArray> = Array(6) { c ->
            DoubleArray(6) { b ->
                val fc = scheme0.faceOf(CubeColor.entries[c])
                val fb = scheme0.faceOf(CubeColor.entries[b])
                if (fc != fb && fc.opposite != fb) 1.0 else 0.0
            }
        }

        /** Colours (a, b, c) that are a corner piece in that order round it. */
        private val corner: Array<Array<BooleanArray>> = Array(6) { Array(6) { BooleanArray(6) } }.also { t ->
            for (piece in Corner.entries) for (s in 0 until 3) {
                val x = List(3) { scheme0[piece.faces[(it + s) % 3]].ordinal }
                t[x[0]][x[1]][x[2]] = true
            }
        }

        /** Per net sticker, the other stickers of its piece: an edge's one, a corner's two in order round it. */
        private val PARTNERS: Array<IntArray> = Array(Stickers.COUNT) { i ->
            Edge.entries.firstOrNull { i in it.stickers }?.let { e -> intArrayOf(e.stickers.first { it != i }) }
                ?: Corner.entries.firstOrNull { i in it.stickers }?.let { c ->
                    val k = c.stickers.indexOf(i)
                    intArrayOf(c.stickers[(k + 1) % 3], c.stickers[(k + 2) % 3])
                } ?: IntArray(0)
        }

        /** For two neighbouring faces, the net index pairs (on the first, on the second) of the stickers that share pieces. */
        private val SHARED: Map<Pair<Face, Face>, List<Pair<Int, Int>>> = buildMap {
            for (a in Face.entries) for (b in Face.entries) {
                if (a == b || a.opposite == b) continue
                val list = ArrayList<Pair<Int, Int>>()
                for (e in Edge.entries) if (a in e.faces && b in e.faces) list += e.stickers[e.faces.indexOf(a)] % 9 to e.stickers[e.faces.indexOf(b)] % 9
                for (c in Corner.entries) if (a in c.faces && b in c.faces) list += c.stickers[c.faces.indexOf(a)] % 9 to c.stickers[c.faces.indexOf(b)] % 9
                put(a to b, list)
            }
        }
    }
}
