package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import kotlin.math.PI
import kotlin.math.atan2
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

    /** The hue of each counting track's centre that looks red or orange, by track id, for this frame ([warmOrderCost]). */
    private var warmHues: Map<Int, Double> = emptyMap()

    /** Both red and orange centres are known: the two are named against the cube's own colours, not the palette. */
    val warmCalibrated: Boolean get() = CubeColor.RED in known && CubeColor.ORANGE in known

    /** Each counting track's face (`?` while open) and centre as read, for the scan log: the camera's own colours. */
    val centreLog: String get() = counting().mapNotNull { t -> t.state().centre?.let { c -> "${t.state().face?.name ?: "?"}${c.toHex()}" } }.joinToString(" ")

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

        /** The turn was settled by the best cube (worked out again as the readings change), not by a picture. */
        var byCube = false

        /** The decisions after the last picture's work, the ones before them, and the newest reading when they changed ([hold]; a turn settled by the cube or by a picture is the same way back). */
        var kept: Decision? = null
        var left: Decision? = null
        var leftSeq = -1L

        fun decision() = Decision(face, option, assigned, otherFace, byCube)

        fun restore(d: Decision) {
            face = d.face
            option = d.option
            assigned = d.assigned
            otherFace = d.otherFace
            byCube = d.byCube
            if (face != null || option != null) openSince = null
        }
    }

    private data class Decision(val face: Face?, val option: Int?, val assigned: Int, val otherFace: Face?, val byCube: Boolean)

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
    private fun counting(): List<Track> = tracker.tracks.filter { counts(it) }

    /** Whether [t] counts: [MIN_READINGS] readings. */
    private fun counts(t: Track): Boolean = t.size >= MIN_READINGS

    /**
     * A short track (fewer than [KEEP_READINGS]) that ended without settling: it is still assigned and
     * may settle later, but neither votes nor holds the finish (a stray lattice for a moment).
     */
    private fun stale(t: Track): Boolean = t.state().option == null && lastNow - t.lastAt > Tracker.GAP_MILLIS && t.size < KEEP_READINGS

    /** The counting tracks that hold the finish. */
    private fun holding(): List<Track> = counting().filter { !stale(it) }

    private var lastNow = 0L

    /** Every counting track is settled (face and turn, or no face). */
    val allSettled: Boolean get() = holding().all { it.state().option != null }

    /**
     * Every counting track is settled, or is short (fewer than [KEEP_READINGS]) with its face settled
     * and reads like [cube] where it is assigned (at most one sticker otherwise): it cannot change the
     * cube, only which turn it was seen in.
     */
    fun settledFor(cube: Cube): Boolean = holding().all { t ->
        val s = t.state()
        s.option != null || (t.size < KEEP_READINGS && s.face != null && s.assigned != FaceOption.NONE && fits(t, s.assigned, cube))
    }

    /**
     * Once [cube] is clear: every counting track is settled, short (fewer than [KEEP_READINGS]), or reads
     * like [cube] (at most one sticker otherwise) in some turn of a face it could be (its face, else its
     * assignment or the next best face). A face newly in view, its face or turn not told yet, then does not
     * hold the finish back (the third phone test, 2026-10-08 09:11: each new track revoked it for a frame,
     * so its half second never came), nor does a stray lattice for a moment; one read against the cube for
     * longer does. Whether the cube stays clear is the evidence's to say.
     */
    fun quietFor(cube: Cube): Boolean = holding().all { t ->
        val s = t.state()
        val faces = s.face?.let { listOf(it) } ?: listOfNotNull(s.assigned.takeIf { it != FaceOption.NONE }?.let(FaceOption::face), s.otherFace)
        s.option != null || t.size < KEEP_READINGS || faces.any { f -> (0 until 4).any { k -> fits(t, FaceOption.of(f, k), cube) } }
    }

    private fun fits(t: Track, o: Int, cube: Cube): Boolean {
        val face = FaceOption.face(o)
        return (0 until 9).count { n ->
            val c = t.leading[RotationSearch.turnIndex(n, FaceOption.turn(o))]
            n != CENTRE && c != null && c != cube[face.ordinal * 9 + n]
        } <= 1
    }

    /** Faces that an open track could be: its assignment and the next best face. */
    val unsureFaces: Set<Face>
        get() = holding().filter { it.state().face == null }.flatMap { t -> listOfNotNull(FaceOption.NONE.let { n -> t.state().assigned.takeIf { it != n }?.let(FaceOption::face) }, t.state().otherFace) }.toSet()

    /** Faces with a track whose face is settled. */
    val seenFaces: Set<Face> get() = tracker.tracks.mapNotNull { it.state().face }.toSet()

    /** Faces with a track whose face and turn are settled. */
    val settledFaces: Set<Face> get() = tracker.tracks.filter { settled(it) }.map { FaceOption.face(it.state().option!!) }.toSet()

    /** Some counting track's face has been open for [HINT_MILLIS] at [now]. */
    fun undecided(now: Long): Boolean = holding().any { t -> t.state().let { it.face == null && it.openSince?.let { s -> now - s >= HINT_MILLIS } == true } }

    /**
     * The newest readings of the tracks assigned to each face, each with its track's turn and whether
     * that turn is still open (the face settled, the turn not).
     */
    fun assignedReadings(): Map<Face, List<Triple<TrackReading, Int, Boolean>>> {
        val out = HashMap<Face, MutableList<Triple<TrackReading, Int, Boolean>>>()
        for (t in tracker.tracks) {
            if (t !in voting) continue
            val o = assignment(t) ?: continue
            val list = out.getOrPut(FaceOption.face(o)) { ArrayList() }
            val open = t.state().face != null && (t.state().option == null || t.state().byCube)
            for (r in t.readings) list += Triple(r, FaceOption.turn(o), open)
        }
        return out.mapValues { (_, l) -> l.sortedBy { it.first.seq }.takeLast(MAX_FACE_READINGS) }
    }

    /** A counting track as the solver holds it (for tuning and tests); [newest] is its newest reading's number. */
    data class TrackInfo(
        val id: Int, val size: Int, val leading: List<CubeColor?>, val face: Face?, val option: Int?, val assigned: Int, val live: Boolean,
        val otherFace: Face? = null, val newest: Long = 0,
    )

    fun snapshot(): List<TrackInfo> = counting().sortedBy { it.id }.map { t ->
        val s = t.state()
        TrackInfo(t.id, t.size, t.leading, s.face, s.option, s.assigned, lastNow - t.lastAt <= Tracker.GAP_MILLIS, s.otherFace, t.readings.last().seq)
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
        if (!counts(t)) return null
        val s = t.state()
        val o = (s.option ?: s.assigned).takeIf { it != FaceOption.NONE } ?: return null
        if (!stale(t)) return o
        // A short track that ended unsettled counts only as the one view of a face it is known to be.
        val face = s.face ?: return null
        return o.takeIf {
            counting().none { u -> u !== t && !stale(u) && (u.state().option ?: u.state().assigned).let { it != FaceOption.NONE && FaceOption.face(it) == face } }
        }
    }

    /** The time of the last picture that worked the state out ([onFrame]); null before the first. */
    private var workedAt: Long? = null

    /**
     * Whether some track crossed a time limit since [workedAt]: it stops being live ([Tracker.GAP_MILLIS]: not
     * voted again, stale, retired), in view ([LIVE_MILLIS]: its weight in [updateVoting]), or a no-face track
     * retires ([RETIRE_MILLIS]). Every limit read against `now` inside [onFrame] must be listed here.
     */
    private fun crossedLimit(now: Long): Boolean {
        val from = workedAt ?: return true
        return tracker.tracks.any { t -> LIMITS.any { x -> from - t.lastAt <= x && now - t.lastAt > x } }
    }

    /** The last picture's work left the state as it found it: working it out again from the same readings gives the same. */
    private var steady = false

    /** What a picture's work reads and changes besides the readings: each track's decisions, the voting tracks, the references. */
    private fun decisions(): List<Any?> = tracker.tracks.map { t ->
        val s = t.state()
        listOf(t, s.face, s.option, s.assigned, s.otherFace, s.openSince, s.byCube)
    } + listOf(voting, known)

    fun onFrame(faces: List<FaceReading>, now: Long) {
        lastNow = now
        picture = tracker.onFrame(faces, now) { rgb -> nameSticker(rgb) }
        // Nothing read, no time limit crossed and the last picture's work changed nothing: the same work would give
        // the same state, so it is left as it is (`scan-speed-up-3`). A state that still moves is worked out again.
        if (steady && picture.all { it == null } && !crossedLimit(now)) return
        workedAt = now
        val before = decisions()
        work(now)
        steady = decisions() == before
    }

    private fun work(now: Long) {
        lastCosts = costs
        costs = HashMap()
        rules.observe(picture.filterNotNull())
        updateRefs()
        for ((track, reading) in picture.filterNotNull()) {
            shareReading(reading)
            track.updateLeading()
        }
        for (t in tracker.tracks) if (now - t.lastAt <= Tracker.GAP_MILLIS) votesOf(t)
        warmHues = warmHuesOf()
        recheck()
        assignOpen(now)
        updateVoting(now)
        settleTurns()
        if (hold()) updateVoting(now)
        // Faces settled in this frame give their centres as references at once: name the stickers again before they are shown.
        updateRefs()
        evidence = evidenceOf()
        val key = EvidenceKey(evidence)
        if (key != bestKey) {
            best = BestCube.solve(evidence, scheme)
            bestKey = key
        }
        retire(now)
    }

    /**
     * A track does not go back to the decisions it just left without a new reading of its own (`scan-track-settle`):
     * the steps above judge a track in different ways (alone, together, by the best cube), and what one decides another
     * could undo picture after picture. Returns whether some track was held.
     */
    private fun hold(): Boolean {
        var held = false
        for (t in tracker.tracks) {
            val s = t.state()
            val now = s.decision()
            val kept = s.kept
            if (kept == null || now == kept) {
                s.kept = now
                continue
            }
            val newest = t.readings.last().seq
            if (s.left?.copy(byCube = now.byCube) == now && newest == s.leftSeq) {
                s.restore(kept)
                held = true
                continue
            }
            s.left = kept
            s.leftSeq = newest
            s.kept = now
        }
        return held
    }

    // --- retiring --------------------------------------------------------------------------------

    /** Votes of retired voting tracks per net sticker (net-aligned), counted in the tables as before. */
    private val archived = Array(Stickers.COUNT) { DoubleArray(6) }

    /** Retired settled tracks' centre colours per face colour (summed RGB and count), for the references. */
    private val archivedCentres = HashMap<CubeColor, IntArray>()

    /** Tracks the per-frame work goes over (for tests and the log). */
    val workingTracks: Int get() = tracker.tracks.size

    /**
     * Ended tracks leave the per-frame work (`scan-rules-finish`): one that never counted, one taken for
     * no face [RETIRE_MILLIS] ago, and a settled one whose readings are all older than the newest
     * [MAX_FACE_READINGS] of its face's evidence (it gives no evidence any more). A settled one keeps its
     * votes in the tables and its centre in the references; it is no longer rechecked, assigned or aligned.
     * Unsettled ones stay: one may still settle, and retiring them lost the striped U2 cube in dim light
     * (`202403`).
     */
    private fun retire(now: Long) {
        val newest = assignedReadings().mapValues { (_, l) -> if (l.size < MAX_FACE_READINGS) Long.MIN_VALUE else l.first().first.seq }
        val out = tracker.tracks.filter { t ->
            if (now - t.lastAt <= Tracker.GAP_MILLIS) return@filter false
            val s = t.state()
            val o = s.option
            when {
                !counts(t) -> true
                o == null -> false
                o == FaceOption.NONE -> now - t.lastAt > RETIRE_MILLIS
                else -> newest[FaceOption.face(o)]?.let { from -> t.readings.last().seq < from } == true
            }
        }
        if (out.isEmpty()) return
        for (t in out) {
            val o = t.state().option
            if (o != null && o != FaceOption.NONE) {
                if (t in voting) add(archived, t, o, 1.0)
                val sum = archivedCentres.getOrPut(scheme[FaceOption.face(o)]) { IntArray(4) }
                for (r in t.readings) r.face.colors[CENTRE]?.let {
                    sum[0] += it.r
                    sum[1] += it.g
                    sum[2] += it.b
                    sum[3]++
                }
            }
            states.remove(t)
        }
        val gone = out.toHashSet()
        tracker.tracks.removeAll { it in gone }
        val goneIds = gone.map { it.id.toLong() }.toSet()
        pairTables.keys.removeAll { it / 1_000_000 in goneIds || it % 1_000_000 in goneIds }
        voting = voting - gone
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
        val newKnown = (byColor.keys + archivedCentres.keys).associateWith { c ->
            val l = byColor[c].orEmpty()
            val a = archivedCentres[c] ?: IntArray(4)
            val n = l.size + a[3]
            Rgb((l.sumOf { it.r } + a[0]) / n, (l.sumOf { it.g } + a[1]) / n, (l.sumOf { it.b } + a[2]) / n)
        }
        val changed = newKnown.keys != known.keys || newKnown.any { (c, rgb) -> known[c]?.let { o -> kotlin.math.abs(o.r - rgb.r) + kotlin.math.abs(o.g - rgb.g) + kotlin.math.abs(o.b - rgb.b) > REF_STEP } != false }
        if (!changed) return
        known = newKnown
        refs = balancedRefs(known)
        generation++
        for (t in tracker.tracks) {
            for (r in t.readings) {
                r.names = r.face.colors.map { it?.let(::nameSticker) }
                shareReading(r)
            }
            t.updateLeading()
            votesOf(t)
        }
    }

    /**
     * The references: the cube's own centres where known, the default palette for the others. With
     * one of red and orange known, the other lies from it as far as in the default palette: the two
     * stay apart in any light (an orange centre looking red, 2026-10-07 15:27; a red one looking
     * orange in the evening). (Moving every unknown colour by the known centres' average cast was
     * tried and dropped: a washed-out yellow centre pulled the others astray.)
     */
    private fun balancedRefs(known: Map<CubeColor, Rgb>): Map<CubeColor, Tone> {
        val defaults = CubeColor.entries.associateWith { Tone(ColorClassifier.DEFAULT_PALETTE.getValue(it)) }
        val own = known.mapValues { Tone(it.value) }
        if (own.isEmpty()) return defaults
        val warm = listOf(CubeColor.RED, CubeColor.ORANGE)
        fun shift(from: Collection<CubeColor>, get: (Tone) -> Lab): Lab = Lab(
            from.sumOf { get(own.getValue(it)).l - get(defaults.getValue(it)).l } / from.size,
            from.sumOf { get(own.getValue(it)).a - get(defaults.getValue(it)).a } / from.size,
            from.sumOf { get(own.getValue(it)).b - get(defaults.getValue(it)).b } / from.size,
        )
        fun moved(x: Lab, by: Lab) = Lab(x.l + by.l, x.a + by.a, x.b + by.b)
        fun cast(from: Collection<CubeColor>, c: CubeColor): Tone = defaults.getValue(c).let { d -> Tone(moved(d.plain, shift(from) { it.plain }), moved(d.bare, shift(from) { it.bare })) }
        val oneWarm = warm.filter { it in own }.singleOrNull()
        return CubeColor.entries.associateWith { c ->
            own[c] ?: if (c in warm && oneWarm != null) cast(listOf(oneWarm), c) else defaults.getValue(c)
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
        val votes = Array(Stickers.COUNT) { archived[it].copyOf() }
        for (t in tracker.tracks) {
            if (t === skip || t.size < MIN_READINGS || t !in voting) continue
            val o = t.state().option ?: continue
            if (o == FaceOption.NONE) continue
            add(votes, t, o, 1.0)
        }
        return Tables(votes, DoubleArray(Stickers.COUNT) { votes[it].sum() })
    }

    /** Whether [t]'s votes are in [tables]: it counts, votes, and its face and turn are settled. */
    private fun inTables(t: Track): Boolean = t.size >= MIN_READINGS && t in voting && t.state().option.let { it != null && it != FaceOption.NONE }

    /** [tables] with [t]'s votes as [o] taken out. */
    private fun without(tables: Tables, t: Track, o: Int): Tables {
        val votes = Array(Stickers.COUNT) { tables.votes[it].copyOf() }
        add(votes, t, o, -1.0)
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
            val same = q[i]?.let { warmBlur(it) }
            DoubleArray(6) { c -> (same?.let { 6 * it[c] } ?: 1.0) * pieceLikelihood(q, i, c) }
        }
    }

    /**
     * [q] with red and orange each lent [WARM_LEND] of the other (they are the usual confusion, as in
     * [StickerEvidence]): a red sticker read orange agrees in part with a red one.
     */
    private fun warmBlur(q: DoubleArray): DoubleArray {
        val r = CubeColor.RED.ordinal
        val o = CubeColor.ORANGE.ordinal
        return q.copyOf().also {
            it[r] = (q[r] + WARM_LEND * q[o]) / (1 + WARM_LEND)
            it[o] = (q[o] + WARM_LEND * q[r]) / (1 + WARM_LEND)
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

    /** The content cost of [t] as [o] against [like] ([likelihoods]), [look] its [lookCost] per face; [FaceOption.NONE]: [NONE_COST]. */
    private fun contentCost(t: Track, o: Int, like: Array<DoubleArray>, look: DoubleArray): Double {
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
        return cost + look[face.ordinal]
    }

    /** How far [t]'s centre is from [face]'s colour beyond the nearest colour (the cube's own centres where known), weighted. */
    private fun lookCost(t: Track, face: Face): Double {
        val centre = t.state().centre ?: return 0.0
        val d = ColorClassifier.centreDistances(centre, known)
        val least = d.values.min()
        return LOOK_WEIGHT * min(d.getValue(scheme[face]) - least, LOOK_CAP) + warmOrderCost(t, face)
    }

    /**
     * Red and orange told apart by each other: a warm centre clearly more orange (by hue) than another
     * track's is not the red face, one clearly redder is not the orange face. The palette and a lone
     * centre cannot do it in every light (the camera's orange looked red in the phone test of 2026-10-08,
     * and once taken for the red face it was its own red reference).
     */
    private fun warmOrderCost(t: Track, face: Face): Double {
        val color = scheme[face]
        if (color !in WARM) return 0.0
        val h = warmHues[t.id] ?: return 0.0
        val others = warmHues.filterKeys { it != t.id }.values
        val wrong = if (color == CubeColor.RED) others.any { it < h - WARM_HUE_STEP } else others.any { it > h + WARM_HUE_STEP }
        return if (wrong) WARM_ORDER_COST else 0.0
    }

    /** [warmHues] of the counting tracks now. */
    private fun warmHuesOf(): Map<Int, Double> = counting().mapNotNull { t ->
        t.state().centre?.takeIf { c -> ColorClassifier.rankedCentre(c).first() in WARM }?.let { c ->
            val lab = ColorClassifier.scaled(c).toLab()
            t.id to atan2(lab.b, lab.a) * 180 / PI
        }
    }.toMap()

    /** Every option's cost for [t] alone against [like], hard rules against the settled tracks applied. */
    private fun unary(t: Track, like: Array<DoubleArray>, settled: List<Track>, domain: IntArray): DoubleArray {
        // A settled one-colour face's turn is any that reads it the same: the rules allow it in each.
        val bound = settled.filter { it !== t }.mapNotNull { s ->
            val so = s.state().option!!
            rules.allowed(t, s)?.let { allowed -> allowed to (0 until FaceOption.FACES).filter { it == so || sameReading(s, so, it) } }
        }
        val look = DoubleArray(Face.entries.size) { lookCost(t, Face.entries[it]) }
        return DoubleArray(domain.size) { k ->
            val o = domain[k]
            when {
                bound.any { (allowed, ways) -> ways.none { allowed[o * FaceOption.COUNT + it] } } -> INF
                else -> contentCost(t, o, like, look)
            }
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
            // Read otherwise on more than [MAX_DISAGREE] stickers: rather two faces (the earlier scanner's anchor).
            return cost + CLASH_COST * (disagreeing(a, oa, b, ob) - MAX_DISAGREE).coerceAtLeast(0)
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
        val q = warmBlur(DoubleArray(6) { c -> (b[c] * scale + SMOOTH) / all })
        var e = 0.0
        for (c in 0 until 6) e += a[c] / ta * 6 * q[c]
        return min(ta, FULL_WEIGHT) / FULL_WEIGHT * -ln(e)
    }

    /** The references changed this many times: every reading was named again ([updateRefs]). */
    private var generation = 0

    /**
     * What a track's votes, totals and leading colours are worked out from: its readings (only ever added to, the
     * oldest dropped, so the newest's number tells them) and the references' [generation].
     */
    private fun version(t: Track): Long = t.readings.last().seq * 1_000_003 + generation

    private class PairEntry(val a: Long, val b: Long, val rules: Int, val table: Array<DoubleArray>)

    /** [pairTable]s by the ids of the two tracks in their order, kept while neither track nor their rules change (`scan-speed-up-3`). */
    private val pairTables = HashMap<Long, PairEntry>()

    /** [pairCost] of [a] and [b] for every option of each, indexed by [a]'s then [b]'s option. */
    private fun pairTable(a: Track, b: Track): Array<DoubleArray> {
        val key = a.id.toLong() * 1_000_000 + b.id
        val va = version(a)
        val vb = version(b)
        val vr = rules.version(a, b)
        pairTables[key]?.let { e -> if (e.a == va && e.b == vb && e.rules == vr) return e.table }
        val allowed = rules.allowed(a, b)
        val table = Array(FaceOption.COUNT) { oa -> DoubleArray(FaceOption.COUNT) { ob -> pairCost(a, oa, b, ob, allowed) } }
        pairTables[key] = PairEntry(va, vb, vr, table)
        return table
    }

    // --- settling --------------------------------------------------------------------------------

    /** For tuning: track [id]'s cost per face (cheapest turn) and for no face, against the other settled tracks. */
    fun costsOf(id: Int): String {
        val t = tracker.tracks.firstOrNull { it.id == id } ?: return "no track $id"
        val tables = tables(skip = t)
        val others = tracker.tracks.filter { it !== t && counts(it) && it.state().option.let { o -> o != null && o != FaceOption.NONE } }
        val cost = unary(t, likelihoods(tables), others, IntArray(FaceOption.COUNT) { it })
        fun one(x: Double) = (kotlin.math.round(x * 10) / 10).toString()
        return Face.entries.joinToString(" ") { f -> "$f=" + one((0 until 4).minOf { cost[FaceOption.of(f, it)] }) } + " none=" + one(cost[FaceOption.NONE]) + " voting=${t in voting}"
    }

    /** Settled tracks whose way is no longer the cheapest (against the others) re-open. */
    private fun recheck() {
        val settled = tracker.tracks.filter { counts(it) && (it.state().face != null || it.state().option == FaceOption.NONE) }
        if (settled.isEmpty()) return
        val all = IntArray(FaceOption.COUNT) { it }
        // The tables of all settled tracks once, each track's own votes taken out for it (`scan-speed-up-3`).
        var full = tables()
        for (t in settled) {
            val s = t.state()
            val own = s.option?.takeIf { it != FaceOption.NONE }
            val voted = own?.takeIf { inTables(t) }
            val like = likelihoods(voted?.let { without(full, t, it) } ?: full)
            val others = settled.filter { it !== t && it.state().option != null && it.state().option != FaceOption.NONE }
            val cost = unary(t, like, others, all)
            if (s.option == FaceOption.NONE) {
                // Taken for no face: re-opens when some face fits it better.
                if (!keepsNone(cost)) s.option = null
                continue
            }
            val face = s.face!!
            if (!keepsFace(cost, face)) {
                s.face = null
                s.option = null
            } else if (own != null && !s.byCube && !keepsOption(cost, own)) {
                s.option = null
            }
            // Re-opened: out of the tables for the tracks after it.
            if (voted != null && !inTables(t)) full = without(full, t, voted)
        }
        // Two tracks taken for one face that read it otherwise in every turn, one of them with its turn
        // still open (taken by its look alone, e.g. a washed-out yellow centre looking white): both
        // re-open and are assigned together again.
        val faceOnly = counting().filter { it.state().face != null }
        for (a in faceOnly) for (b in faceOnly) {
            if (a.id >= b.id || a.state().face == null || a.state().face != b.state().face || (a.state().option != null && b.state().option != null)) continue
            val f = a.state().face ?: continue
            if (clash(a, b, f)) {
                for (t in listOf(a, b)) t.state().let {
                    it.face = null
                    it.option = null
                }
            }
        }
    }

    // [recheck]'s tests, also kept by [assignOpen] before it settles anything: what one settles, the other does not
    // re-open with the same readings (`scan-track-settle`).

    /** Taken for no face, given its [cost] per option alone against the settled tracks: no face fits it better. */
    private fun keepsNone(cost: DoubleArray): Boolean = (0 until FaceOption.FACES).none { cost[it] < cost[FaceOption.NONE] - REOPEN_SLACK }

    /** Taken for [face]: no other face, nor no face, fits it better. */
    private fun keepsFace(cost: DoubleArray, face: Face): Boolean {
        val ownFace = (0 until 4).minOf { cost[FaceOption.of(face, it)] }
        return cost.indices.none { (it == FaceOption.NONE || FaceOption.face(it) != face) && cost[it] < ownFace - REOPEN_SLACK }
    }

    /** Settled as [own]: no other way fits it better. */
    private fun keepsOption(cost: DoubleArray, own: Int): Boolean = cost.indices.none { it != own && cost[it] < cost[own] - REOPEN_SLACK }

    /** [a] and [b], both taken for [f], read it otherwise in every turn of [b] ([a] as settled or assigned). */
    private fun clash(a: Track, b: Track, f: Face): Boolean {
        val oa = a.state().option ?: a.state().assigned.takeIf { it != FaceOption.NONE } ?: FaceOption.of(f, 0)
        return (0 until 4).all { k -> disagreeing(a, oa, b, FaceOption.of(f, k)) > MAX_DISAGREE }
    }

    /**
     * The open tracks (at most [MAX_OPEN], the most read first) assigned together: the cheapest
     * assignment by branch and bound, the settled tracks fixed. Then each settles its face, or its
     * face and turn, where every assignment that changes it costs [ASSIGN_MARGIN] more.
     */
    private fun assignOpen(now: Long) {
        val settled = tracker.tracks.filter { counts(it) && it.state().option.let { o -> o != null && o != FaceOption.NONE } }
        val open = tracker.tracks.filter { counts(it) && it.state().option == null }.sortedByDescending { it.size }.take(MAX_OPEN)
        for (t in tracker.tracks) if (t.size >= MIN_READINGS && t.state().option == null && t !in open) t.state().assigned = FaceOption.NONE
        if (open.isEmpty()) return
        val tables = tables()
        val like = likelihoods(tables)
        val domains = open.map { IntArray(FaceOption.COUNT) { it } }
        // Ties (a face seen alone, any turn) go to the last frame's choice, then to the track's own frame, so what is shown does not turn.
        val unary = open.mapIndexed { i, t ->
            val last = t.state().assigned
            unary(t, like, settled, domains[i]).also { c ->
                for (k in c.indices) {
                    val o = domains[i][k]
                    if (o != FaceOption.NONE && o != last) c[k] += STICKY
                    if (o != FaceOption.NONE && FaceOption.turn(o) != 0) c[k] += STICKY / 2
                    // A settled face is left only for a clearly better assignment of all (a face taken by its look alone).
                    t.state().face?.let { f -> if (o == FaceOption.NONE || FaceOption.face(o) != f) c[k] += FACE_KEEP }
                }
            }
        }
        val pairs = Array(open.size) { i -> Array(open.size) { j -> if (j <= i) null else pairTable(open[i], open[j]) } }
        val search = Search(unary, pairs)
        val (cost, pick) = search.best() ?: return
        for ((i, t) in open.withIndex()) {
            val s = t.state()
            val o = domains[i][pick[i]]
            s.assigned = o
            if (s.face != null && (o == FaceOption.NONE || FaceOption.face(o) != s.face)) s.face = null
            // Turns that read the track the same (a one-colour face) are no other way.
            val optionMargin = search.best(cap = cost + ASSIGN_MARGIN, ban = i to { k -> k == pick[i] || sameReading(t, o, domains[i][k]) })?.first?.minus(cost) ?: Double.POSITIVE_INFINITY
            if (optionMargin >= ASSIGN_MARGIN && turnBacked(t, o)) {
                s.option = o
                s.byCube = false
                s.face = if (o == FaceOption.NONE) null else FaceOption.face(o)
                s.otherFace = null
            } else if (optionMargin >= ASSIGN_MARGIN) {
                // The face is clear, the turn only by colours that may sit next to each other: the best cube decides it.
                s.face = FaceOption.face(o)
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

    /**
     * Turns of faces whose face is settled but whose turn no picture has fixed (seen alone): all the
     * face's open tracks turned together the way that makes the best possible cube cheapest (two
     * rounds over the faces, as the earlier scanner did), settled where each other turn that reads
     * the face differently makes the best cube at least [TURN_MARGIN] costlier. Faces with a track
     * whose turn is settled are left to the pictures (their open tracks align by their stickers).
     */
    private fun settleTurns() {
        fun byCube(t: Track) = t.state().let { it.option == null || it.byCube }
        val openOn = Face.entries.associateWith { f -> counting().filter { it.state().face == f && byCube(it) && (it.state().option ?: it.state().assigned) != FaceOption.NONE } }
            .filter { (f, list) -> list.isNotEmpty() && counting().none { t -> !byCube(t) && t.state().option?.let { it != FaceOption.NONE && FaceOption.face(it) == f } == true } }
        if (openOn.isEmpty()) return
        for (list in openOn.values) for (t in list) t.state().let { it.assigned = it.option ?: it.assigned }
        val extra = IntArray(6)
        var turned = TurnedEvidence()
        fun cost(): Double = bestCost(turned.of(extra))
        var base = cost()
        repeat(2) {
            for (face in openOn.keys) {
                val now = extra[face.ordinal]
                var pick = now
                for (k in 1 until 4) {
                    extra[face.ordinal] = (now + k) % 4
                    val c = cost()
                    if (c < base - 1e-9) {
                        base = c
                        pick = extra[face.ordinal]
                    }
                }
                extra[face.ordinal] = pick
            }
        }
        for ((face, list) in openOn) {
            val k = extra[face.ordinal]
            for (t in list) t.state().assigned = FaceOption.of(face, FaceOption.turn(t.state().assigned) + k)
        }
        extra.fill(0)
        turned = TurnedEvidence()
        for ((face, list) in openOn) {
            val leading = turned.leading(face, 0)
            val settled = (1 until 4).filter { k -> turned.leading(face, k) != leading }.all { k ->
                extra[face.ordinal] = k
                val c = cost()
                extra[face.ordinal] = 0
                c - base >= TURN_MARGIN
            }
            for (t in list) t.state().let {
                it.option = if (settled) it.assigned else null
                it.byCube = settled
            }
        }
    }

    /**
     * The turns the best cube settled stay clear also when two of those faces turn together: every
     * such pair of other turns that reads them differently makes the best cube at least [TURN_MARGIN]
     * costlier (the striped cube with its front and back both half round is another possible cube,
     * 2026-10-07 20:24), and also when three or more of them all turn half round (the striped cube with
     * every side half round is its mirror, `202058`, 2026-10-08). Checked for the finish only.
     */
    fun turnsClear(): Boolean {
        val faces = Face.entries.filter { f -> counting().any { t -> t.state().byCube && t.state().option?.let { it != FaceOption.NONE && FaceOption.face(it) == f } == true } }
        if (faces.size < 2) return true
        val turned = TurnedEvidence()
        val base = bestCost(turned.of(IntArray(6)))
        val reads = faces.associateWith { f -> List(4) { k -> turned.leading(f, k) } }
        val ways = ArrayList<List<Int>>()
        for (i in faces.indices) for (j in i + 1 until faces.size) for (a in 0 until 4) for (b in 0 until 4) {
            val fa = faces[i]
            val fb = faces[j]
            if (a == 0 && b == 0) continue
            if (reads.getValue(fa)[a] == reads.getValue(fa)[0] && reads.getValue(fb)[b] == reads.getValue(fb)[0]) continue
            ways += List(6) { when (it) { fa.ordinal -> a; fb.ordinal -> b; else -> 0 } }
        }
        for (mask in 1 until (1 shl faces.size)) {
            val set = faces.filterIndexed { i, _ -> mask and (1 shl i) != 0 }
            if (set.size < 3 || set.all { f -> reads.getValue(f)[2] == reads.getValue(f)[0] }) continue
            ways += List(6) { n -> if (set.any { it.ordinal == n }) 2 else 0 }
        }
        // The way that was too close last frame first: while the turns stay unclear it usually still is (one
        // cube cost instead of dozens a frame, `scan-rules-finish`).
        lastClose?.let { w -> if (ways.remove(w)) ways.add(0, w) }
        val close = ways.firstOrNull { w -> bestCost(turned.of(w.toIntArray())) - base < TURN_MARGIN }
        lastClose = close
        return close == null
    }

    /** Evidence by its votes, for the best-cube memos: equal votes, equal best cube. */
    private class EvidenceKey(evidence: StickerEvidence) {
        private val votes = DoubleArray(Stickers.COUNT * 6).also { a -> evidence.votes.forEachIndexed { i, v -> v.copyInto(a, i * 6) } }
        private val hash = votes.contentHashCode()

        override fun hashCode(): Int = hash
        override fun equals(other: Any?): Boolean = other is EvidenceKey && other.hash == hash && other.votes.contentEquals(votes)
    }

    /** The evidence [best] was worked out from: the same evidence again keeps it (`scan-speed-up-3`). */
    private var bestKey: EvidenceKey? = null

    /** [BestCube.cost] by evidence in this picture's work and the last one's: the turn trials repeat many (`scan-speed-up-3`). */
    private var costs = HashMap<EvidenceKey, Double>()
    private var lastCosts = HashMap<EvidenceKey, Double>()

    private fun bestCost(e: StickerEvidence): Double {
        val key = EvidenceKey(e)
        costs[key]?.let { return it }
        return (lastCosts[key] ?: BestCube.cost(e, scheme)).also { costs[key] = it }
    }

    /** The other turns [turnsClear] last found too close to the best (per face, the extra quarter turns). */
    private var lastClose: List<Int>? = null

    /**
     * The evidence with each face's open-turn tracks turned some quarter turns more, as [evidenceOf] with
     * that extra, from each face's votes worked out once per turn (`scan-speed-up`): a trial turn is a
     * look-up, not a pass over every reading. A face's votes add up in [evidenceOf]'s order, so the numbers
     * are the same. Made anew whenever the assignments change.
     */
    private inner class TurnedEvidence {
        private val readings = assignedReadings()
        private val byTurn = Array(6) { arrayOfNulls<List<DoubleArray>>(4) }

        private fun votes(face: Int, k: Int): List<DoubleArray> {
            byTurn[face][k]?.let { return it }
            val list = readings[Face.entries[face]].orEmpty()
            // A face without open-turn readings reads the same in every turn.
            if (k != 0 && list.none { it.third }) return votes(face, 0)
            val votes = List(9) { DoubleArray(6) }
            for ((r, turn, open) in list) {
                val t = if (open) turn + k else turn
                for (n in 0 until 9) {
                    if (n == CENTRE) continue
                    r.shares[r.at(RotationSearch.turnIndex(n, t))]?.let { sh -> for (c in 0 until 6) votes[n][c] += sh[c] }
                }
            }
            byTurn[face][k] = votes
            return votes
        }

        /** The evidence, each face's open-turn tracks turned [extra] (per face) more. */
        fun of(extra: IntArray): StickerEvidence = StickerEvidence(List(Stickers.COUNT) { s -> votes(s / 9, extra[s / 9])[s % 9] })

        /** The colours [face]'s evidence leads with, its open-turn tracks turned [k] more. */
        fun leading(face: Face, k: Int): List<Int> = votes(face.ordinal, k).map { v -> if (v.sum() <= 0.0) -1 else v.indices.maxBy { v[it] } }
    }

    /** The tracks whose readings are evidence: per face, those agreeing with its most supported track. */
    private var voting: Set<Track> = emptySet()

    /**
     * Per face, the tracks assigned to it that agree with the track most others agree with (at most
     * [MAX_DISAGREE] stickers read otherwise), counted by readings, a track in view counting double
     * (recent clear views over old ones): only they are evidence. A face read wrong at first, or a
     * stray lattice taken for a face, then does not spoil it (as the earlier scanner's anchor).
     */
    private fun updateVoting(now: Long) {
        val out = HashSet<Track>()
        for ((_, list) in counting().filter { assignment(it) != null }.groupBy { FaceOption.face(assignment(it)!!) }) {
            fun weight(t: Track) = t.size * if (now - t.lastAt <= LIVE_MILLIS) 2 else 1
            fun agree(a: Track, b: Track): Boolean {
                val oa = assignment(a)!!
                val ob = assignment(b)!!
                var wrong = 0
                for (n in 0 until 9) {
                    if (n == CENTRE) continue
                    val x = a.leading[RotationSearch.turnIndex(n, FaceOption.turn(oa))] ?: continue
                    val y = b.leading[RotationSearch.turnIndex(n, FaceOption.turn(ob))] ?: continue
                    if (x != y && !warm(x, y)) wrong++
                }
                return wrong <= MAX_DISAGREE
            }
            val anchor = list.maxWith(compareBy<Track> { a -> list.filter { agree(a, it) }.sumOf { weight(it) } }.thenBy { it.lastAt })
            out += list.filter { agree(anchor, it) }
        }
        voting = out
    }

    /**
     * Whether [t]'s turn as [o] rests on more than colours that may neighbour each other: a picture
     * binding it to another track by a side or corner, a track of the same face whose turn is settled
     * (their stickers align them), or a one-colour face (any turn reads the same).
     */
    private fun turnBacked(t: Track, o: Int): Boolean {
        if (o == FaceOption.NONE) return true
        val face = FaceOption.face(o)
        if ((0 until 4).all { sameReading(t, o, FaceOption.of(face, it)) }) return true
        return counting().any { it !== t && (rules.fixesTurns(t, it) || (settled(it) && !it.state().byCube && FaceOption.face(it.state().option!!) == face)) }
    }

    /** On how many stickers [a] as [oa] and [b] as [ob] (the same face) lead with different colours, red for orange not counted. */
    private fun disagreeing(a: Track, oa: Int, b: Track, ob: Int): Int = (0 until 9).count { n ->
        val x = a.leading[RotationSearch.turnIndex(n, FaceOption.turn(oa))]
        val y = b.leading[RotationSearch.turnIndex(n, FaceOption.turn(ob))]
        n != CENTRE && x != null && y != null && x != y && !warm(x, y)
    }

    /** Red and orange: told apart in part only. */
    private fun warm(x: CubeColor, y: CubeColor): Boolean = (x == CubeColor.RED && y == CubeColor.ORANGE) || (x == CubeColor.ORANGE && y == CubeColor.RED)

    /** Whether [t] as [a] and as [b] reads the same: the same face, the leading colours alike in both turns. */
    private fun sameReading(t: Track, a: Int, b: Int): Boolean {
        if (a == FaceOption.NONE || b == FaceOption.NONE || FaceOption.face(a) != FaceOption.face(b)) return false
        return (0 until 9).all { n ->
            n == CENTRE || t.leading[RotationSearch.turnIndex(n, FaceOption.turn(a))].let { it != null && it == t.leading[RotationSearch.turnIndex(n, FaceOption.turn(b))] }
        }
    }

    /**
     * The newest readings of the tracks assigned to each face as evidence per net sticker; [extra]
     * turns the open-turn tracks of each face further.
     */
    private fun evidenceOf(extra: IntArray? = null): StickerEvidence {
        val votes = List(Stickers.COUNT) { DoubleArray(6) }
        for ((face, list) in assignedReadings()) for ((r, turn, open) in list) {
            val k = if (open && extra != null) turn + extra[face.ordinal] else turn
            for (n in 0 until 9) {
                if (n == CENTRE) continue
                r.shares[r.at(RotationSearch.turnIndex(n, k))]?.let { sh -> for (c in 0 until 6) votes[face.ordinal * 9 + n][c] += sh[c] }
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
            // At depth d, per later track and option, its pairs with the tracks before d as picked (summed in
            // their order, as the bound always was): kept per depth instead of summed again at every node.
            val acc = Array(n + 1) { Array(n) { DoubleArray(FaceOption.COUNT) } }

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
                for (u in d + 1 until n) rest += order[u].minOfOrNull { k -> unary[u][k] + acc[d][u][k] } ?: INF
                for (k in order[d]) {
                    var c = cost + unary[d][k]
                    for (w in 0 until d) c += pairs[w][d]!![pick[w]][k]
                    if (c >= INF || c + rest >= bestCost) continue
                    pick[d] = k
                    for (u in d + 1 until n) {
                        val row = pairs[d][u]!![k]
                        val from = acc[d][u]
                        val to = acc[d + 1][u]
                        for (j in row.indices) to[j] = from[j] + row[j]
                    }
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

        /** A track that ended unsettled with fewer readings than this no longer counts. */
        const val KEEP_READINGS = 10

        /** Open tracks assigned together at most. */
        const val MAX_OPEN = 8

        /** Search nodes per branch and bound at most. */
        const val MAX_NODES = 20_000

        /** A face's turn settles by the best cube when every other turn makes it this much costlier ([VideoScan.CLEAR_MARGIN], as the earlier scanner). */
        const val TURN_MARGIN = 2.0

        /** What leaving a settled face costs a track in the joint assignment. */
        const val FACE_KEEP = 2.0

        /** A tie-breaker: a way other than last frame's costs this much more (half of it for a turn other than the track's own). */
        const val STICKY = 0.02

        /** Red and orange, which the palette alone cannot always tell apart. */
        val WARM: Set<CubeColor> = setOf(CubeColor.RED, CubeColor.ORANGE)

        /**
         * Hue (degrees) by which one warm centre must be more orange than another to rule it out as red
         * ([warmOrderCost]), and the cost: high enough to be a rule (a mild cost lost to misread stickers on
         * the phone, 2026-10-08 07:43).
         */
        const val WARM_HUE_STEP = 6.0
        const val WARM_ORDER_COST = 1000.0

        /** Cost per sticker beyond [MAX_DISAGREE] that two tracks taken for one face read otherwise. */
        const val CLASH_COST = 4.0

        /** How much of red's agreement orange gets, and the other way round. */
        const val WARM_LEND = 0.5

        /** Two tracks of one face agree when at most this many stickers read otherwise (red for orange not counted). */
        const val MAX_DISAGREE = 2

        /** A track seen this recently is in view (its readings count double for its face's anchor). */
        const val LIVE_MILLIS = 1_000L

        /** A track settles when every other way costs this much more. */
        const val ASSIGN_MARGIN = 2.5

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

        /** A track taken for no face leaves the per-frame work this long after it ended. */
        const val RETIRE_MILLIS = 2_000L

        /** The time limits after a track's last reading that change the state without a reading ([crossedLimit]). */
        private val LIMITS = longArrayOf(Tracker.GAP_MILLIS, LIVE_MILLIS, RETIRE_MILLIS)

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
