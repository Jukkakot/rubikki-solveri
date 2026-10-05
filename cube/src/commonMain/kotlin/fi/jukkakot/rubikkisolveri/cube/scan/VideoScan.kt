package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.Validity
import fi.jukkakot.rubikkisolveri.cube.Vec3

/** How the real cube is held: [front] looks at the camera, [up] is at the top of the picture. */
data class Pose(val front: Face, val up: Face) {
    val right: Face get() = faceWithNormal(up.normal cross front.normal)
}

/** A quarter tilt of the whole cube, named by the way its front face moves (UP brings the bottom into view). */
enum class Tilt {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    ;

    /** The face that comes to the front when the cube held as [pose] is tilted this way. */
    fun newFront(pose: Pose): Face = when (this) {
        UP -> pose.up.opposite
        DOWN -> pose.up
        LEFT -> pose.right
        RIGHT -> pose.right.opposite
    }
}

/**
 * What the video scan knows after a frame. [stickers] are the recognised colours (URFDLB, net
 * order; null = not yet), [contradictions] the stickers whose readings disagree, [found] the faces
 * in this frame, [pose] how the cube was last seen held, [hint] the tilt that brings most missing
 * stickers into view (only while a face is in view and its pose is known), [newStickers] how many
 * stickers were recognised with this frame. [complete]: all 54 recognised and a possible cube;
 * [finished]: complete for [VideoScan.FINISH_MILLIS].
 */
data class VideoScanState(
    val stickers: List<CubeColor?>,
    val contradictions: Set<Int>,
    val found: List<FaceReading>,
    val pose: Pose?,
    val hint: Tilt?,
    val newStickers: Int,
    val complete: Boolean,
    val finished: Boolean,
) {
    val recognised: Int get() = stickers.count { it != null }

    companion object {
        val EMPTY = VideoScanState(List(Stickers.COUNT) { null }, emptySet(), emptyList(), null, null, 0, false, false)
    }
}

/**
 * The scan from continuous video (`video-scan`): every full face found in a frame ([FaceReading])
 * votes for its stickers. Faces are told apart by their centre colour (named regardless of
 * brightness, then together once all six are seen). Each face keeps its readings in its own "frame"
 * coordinates (one reading's way round); a sticker is recognised once [MIN_VOTES] readings agree
 * with a clear margin, and a recognised sticker keeps its colour while it still leads, so a single
 * wrong frame changes nothing. How each face sits in the net comes from corner views (two faces in
 * one frame share an edge), else from a search over the faces' rotations ([RotationSearch] once all
 * stickers are known, a search over the pieces known so far before that). Pure Kotlin, one code
 * base for the phone and the browser.
 */
class VideoScan(private val scheme: ColorScheme = ColorScheme.STANDARD) {
    private class Reading(val rgb: List<Rgb?>, val area: Double, var group: Group) {
        val labs: List<Lab?> = rgb.map { it?.toLab() }
        var names: List<CubeColor?> = emptyList()

        /** All nine stickers found; only a full reading can be a group's anchor. */
        val full: Boolean = rgb.all { it != null }

        /** Quarter turns clockwise that bring this reading to its group's frame. */
        var turn = 0
        var inlier = false
        var removed = false

        /** Faces seen next to this one in the same frame: the side (in this reading) and their reading. */
        val neighbours = ArrayList<Pair<Int, Reading>>()
    }

    private class Group(var color: CubeColor) {
        val readings = ArrayList<Reading>()
        var anchor: Reading? = null

        /** Quarter turns from the anchor to the group's frame. */
        var anchorTurn = 0
        val sticky = arrayOfNulls<CubeColor>(9)
        var stickers: List<CubeColor?> = List(9) { null }
        var disputed: Set<Int> = emptySet()
        var samples: List<Rgb?> = List(9) { null }
        val inliers: List<Reading> get() = readings.filter { it.inlier }
    }

    private val groups = LinkedHashMap<CubeColor, Group>()
    private var rotations: Map<Face, Int> = emptyMap()

    /** Faces whose rotation is settled (corner views, or a search with one answer). */
    private var settled: Set<Face> = emptySet()
    private var validity: Validity? = null
    private var ambiguous: Set<Face> = emptySet()
    private var rotationKey: String? = null
    private var lastPose: Pose? = null
    private var lastRecognised = 0
    private var completeSince: Long? = null

    var state: VideoScanState = VideoScanState.EMPTY
        private set

    /**
     * Handles the faces found in one frame taken at [nowMillis]. A partial face (stickers missing)
     * counts only with its centre and only for a face already started by a full one.
     */
    fun onFrame(faces: List<FaceReading>, nowMillis: Long): VideoScanState {
        val fresh = faces.map { face ->
            val centre = face.colors[CENTRE] ?: return@map null
            val color = nameCentre(centre)
            val group = if (face.isFull) groups.getOrPut(color) { Group(color) } else groups[color] ?: return@map null
            Reading(face.colors, face.area, group).also { group.add(it) }
        }
        for (i in faces.indices) for (j in faces.indices) {
            val a = fresh[i] ?: continue
            val b = fresh[j] ?: continue
            if (i != j) faces[i].sideTowards(faces[j])?.let { a.neighbours += it to b }
        }
        nameJointly()
        renameStickers()
        groups.values.forEach { consensus(it) }
        updateRotations()

        val net = netOf { it.stickers }
        val contradictions = HashSet<Int>()
        for (face in Face.entries) {
            val group = groups[scheme[face]] ?: continue
            val k = rotations[face] ?: 0
            for (n in 0 until 9) if (RotationSearch.turnIndex(n, k) in group.disputed) contradictions += face.ordinal * 9 + n
        }
        val all = net.all { it != null }
        if (all) validity?.let { contradictions += it.markedStickers }
        val complete = all && contradictions.isEmpty() && validity?.isValid == true && (ambiguous - settled).isEmpty()
        if (!complete) completeSince = null else if (completeSince == null) completeSince = nowMillis

        val pose = poseOf(fresh.filterNotNull())
        if (pose != null) lastPose = pose
        val recognised = net.count { it != null }
        state = VideoScanState(
            stickers = net,
            contradictions = contradictions,
            found = faces,
            pose = lastPose,
            hint = pose?.let { hint(it, net, contradictions) },
            newStickers = (recognised - lastRecognised).coerceAtLeast(0),
            complete = complete,
            finished = complete && nowMillis - completeSince!! >= FINISH_MILLIS,
        )
        lastRecognised = recognised
        return state
    }

    /**
     * The scan as it stands, for the colour check (and the solution when it is sure): the recognised
     * colours, the centres of faces not seen from the colour scheme, missing and disputed stickers
     * marked as uncertain.
     */
    fun outcome(): ScanOutcome {
        val net = netOf { it.stickers }.mapIndexed { i, c -> c ?: if (i % 9 == CENTRE) scheme[Face.entries[i / 9]] else null }
        val editor = CubeEditor(net, scheme)
        val uncertain = HashSet(state.contradictions)
        net.indices.filterTo(uncertain) { net[it] == null }
        for (face in ambiguous - settled) (0 until 9).filter { it != CENTRE }.mapTo(uncertain) { face.ordinal * 9 + it }
        val cube = editor.toCube()
        val samples = netOf { it.samples }
        return ScanOutcome(
            editor = editor,
            uncertain = uncertain,
            validity = cube?.let { CubeCheck.validity(it, scheme) } ?: Validity.WrongColorCount(editor.counts().filterValues { it != 9 }),
            samples = if (samples.all { it != null }) samples.map { it!! } else emptyList(),
            rotations = rotations,
        )
    }

    /** The colour a reading's centre names its face by: regardless of brightness, against the six centres once all are seen. */
    private fun nameCentre(rgb: Rgb): CubeColor {
        if (groups.size < 6) return ColorClassifier.rankedCentre(rgb).first()
        return ColorClassifier.rankedCentre(rgb, groups.mapValues { (_, g) -> meanCentre(g) }).first()
    }

    private fun meanCentre(group: Group): Rgb {
        val list = group.inliers.ifEmpty { group.readings }
        return Rgb(list.sumOf { it.rgb[CENTRE]!!.r } / list.size, list.sumOf { it.rgb[CENTRE]!!.g } / list.size, list.sumOf { it.rgb[CENTRE]!!.b } / list.size)
    }

    /** Once all six faces are seen well, their centres are named together (each colour once, the best fit). */
    private fun nameJointly() {
        if (groups.size != 6 || groups.values.any { it.inliers.size < MIN_VOTES }) return
        val list = groups.values.toList()
        val naming = ColorClassifier.centreNamings(list.map { meanCentre(it) }, list.map { it.color }, 1).first()
        if (naming == list.map { it.color }) return
        groups.clear()
        list.forEachIndexed { i, g ->
            g.color = naming[i]
            g.sticky.fill(null)
            groups[g.color] = g
        }
    }

    /** Every sticker named with the cube's own centres as references (the guided scan's live reading). */
    private fun renameStickers() {
        val refs = ColorClassifier.references(groups.mapValues { (_, g) -> g.inliers.ifEmpty { g.readings }.map { it.labs[CENTRE]!! } })
        for (g in groups.values) for (r in g.readings) {
            r.names = r.rgb.mapIndexed { n, rgb -> if (n == CENTRE) g.color else rgb?.let { ColorClassifier.live(it, refs) } }
        }
    }

    private fun Group.add(reading: Reading) {
        readings += reading
        if (readings.size > MAX_READINGS) {
            val drop = readings.firstOrNull { !it.inlier && it !== anchor } ?: readings.first { it !== anchor }
            drop.removed = true
            readings.remove(drop)
        }
    }

    /**
     * The group's anchor is the full reading most others agree with (in some rotation); full readings
     * agreeing with it on [MIN_AGREE] stickers vote, each turned to the group's frame. A partial
     * reading votes when all but one of its stickers agree with the anchor in a single best turn.
     */
    private fun consensus(g: Group) {
        val rs = g.readings
        val support = IntArray(rs.size)
        for (i in rs.indices) for (j in i + 1 until rs.size) {
            if (bestTurn(rs[i].names, rs[j].names).second >= MIN_AGREE) {
                support[i]++
                support[j]++
            }
        }
        val current = rs.indexOf(g.anchor)
        val best = rs.indices.filter { rs[it].full }.maxWith(compareBy<Int> { support[it] }.thenBy { if (it == current) 1 else 0 }.thenBy { -it })
        val anchor = rs[best]
        if (anchor !== g.anchor) {
            // Keep the frame: turn the new anchor the way it fits the colours known so far.
            val known = g.stickers.count { it != null }
            g.anchorTurn = if (known >= MIN_AGREE) bestTurn(anchor.names, g.stickers).first else 0
            if (known < MIN_AGREE) g.sticky.fill(null)
            g.anchor = anchor
        }
        for (r in rs) {
            val (k, agree) = bestTurn(r.names, anchor.names)
            r.turn = (k + g.anchorTurn) % 4
            r.inlier = if (r.full) {
                agree >= MIN_AGREE
            } else {
                val present = r.names.count { it != null }
                val unique = (0 until 4).none { j -> j != k && agreeing(r.names, anchor.names, j) == agree }
                agree >= present - 1 && unique
            }
        }
        val inliers = g.inliers
        val stickers = arrayOfNulls<CubeColor>(9)
        val samples = arrayOfNulls<Rgb>(9)
        val disputed = HashSet<Int>()
        for (n in 0 until 9) {
            val counts = HashMap<CubeColor, Int>()
            val voters = inliers.mapNotNull { r -> r.names[RotationSearch.turnIndex(n, r.turn)]?.let { r to it } }
            for ((_, c) in voters) counts[c] = (counts[c] ?: 0) + 1
            val ranked = counts.entries.sortedByDescending { it.value }
            val lead = ranked.getOrNull(0)
            val second = ranked.getOrNull(1)?.value ?: 0
            val sticky = g.sticky[n]
            val color = when {
                lead == null -> null
                lead.value >= MIN_VOTES && lead.value >= MARGIN * second -> lead.key
                sticky != null && (counts[sticky] ?: 0) >= lead.value -> sticky
                else -> null
            }
            if (color == null && lead != null && lead.value >= MIN_VOTES) disputed += n
            g.sticky[n] = color
            stickers[n] = color
            if (color != null) {
                val agreeing = voters.filter { it.second == color }.map { it.first.rgb[RotationSearch.turnIndex(n, it.first.turn)]!! }
                samples[n] = Rgb(
                    FrameSampler.median(agreeing.map { it.r }),
                    FrameSampler.median(agreeing.map { it.g }),
                    FrameSampler.median(agreeing.map { it.b }),
                )
            }
        }
        g.stickers = stickers.toList()
        g.samples = samples.toList()
        g.disputed = disputed
    }

    /**
     * How each face sits in the net: corner views first (a reading's side towards a neighbouring
     * face, turned to the group's frame, against the side that neighbour is on in the net). The
     * other faces are searched: with every sticker known, [RotationSearch]; before that, the turns
     * giving the most real pieces among the pieces fully known, settled when only one turn does.
     */
    private fun updateRotations() {
        val evidence = HashMap<Face, IntArray>()
        for (g in groups.values) {
            val face = scheme.faceOf(g.color)
            for (r in g.readings) {
                if (!r.inlier) continue
                for ((side, other) in r.neighbours) {
                    if (other.removed || !other.inlier || other.group === g) continue
                    val netSide = netSide(face, scheme.faceOf(other.group.color)) ?: continue
                    evidence.getOrPut(face) { IntArray(4) }[(netSide - (side + r.turn)).mod(4)]++
                }
            }
        }
        val known = evidence.mapNotNull { (face, votes) ->
            val top = votes.indices.maxBy { votes[it] }
            val second = votes.indices.filter { it != top }.maxOf { votes[it] }
            if (votes[top] >= MIN_EVIDENCE && votes[top] > second) face to top else null
        }.toMap()

        val frames = Face.entries.associateWith { groups[scheme[it]]?.stickers }
        val key = known.toString() + frames.values.joinToString("|") { s -> s?.joinToString("") { it?.letter?.toString() ?: "." } ?: "-" }
        if (key == rotationKey) return
        rotationKey = key

        if (frames.values.all { s -> s != null && s.all { it != null } }) {
            val colors = Face.entries.flatMap { frames.getValue(it)!!.map { c -> c!! } }
            var result = RotationSearch.search(colors, scheme, renamePairs = false, fixed = known)
            if (!result.validity.isValid && known.isNotEmpty()) {
                val free = RotationSearch.search(colors, scheme, renamePairs = false)
                if (free.validity.isValid) result = free
            }
            rotations = result.rotations
            validity = result.validity
            ambiguous = result.ambiguous
            settled = known.keys + (Face.entries.toSet() - result.ambiguous)
            return
        }
        validity = null
        ambiguous = emptySet()
        val free = Face.entries.filter { frames[it] != null && it !in known }
        var bestScore = -1
        val bests = ArrayList<IntArray>()
        val turns = IntArray(free.size)
        val net = arrayOfNulls<CubeColor>(Stickers.COUNT)
        for (combo in 0 until (1 shl (2 * free.size))) {
            for (i in free.indices) turns[i] = (combo shr (2 * i)) and 3
            for (face in Face.entries) {
                val frame = frames[face]
                val k = known[face] ?: free.indexOf(face).let { if (it < 0) 0 else turns[it] }
                for (n in 0 until 9) net[face.ordinal * 9 + n] = frame?.get(RotationSearch.turnIndex(n, k))
            }
            val score = realPieces(net)
            if (score > bestScore) {
                bestScore = score
                bests.clear()
            }
            if (score == bestScore) bests += turns.copyOf()
        }
        val chosen = bests.minBy { t -> t.sumOf { minOf(it, 4 - it) } }
        rotations = Face.entries.associateWith { face -> known[face] ?: free.indexOf(face).let { if (it < 0) 0 else chosen[it] } }
        settled = known.keys + free.filterIndexed { i, _ -> bests.all { it[i] == chosen[i] } }
    }

    /** Pieces whose stickers are all known and form a real corner or edge. */
    private fun realPieces(net: Array<CubeColor?>): Int {
        var count = 0
        for (c in Corner.entries) {
            val faces = c.stickers.map { net[it]?.let(scheme::faceOf) }
            if (faces.all { it != null } && faces in realCorners) count++
        }
        for (e in Edge.entries) {
            val faces = e.stickers.map { net[it]?.let(scheme::faceOf) }
            if (faces.all { it != null } && faces in realEdges) count++
        }
        return count
    }

    /** The colours of each face in the net, from each group's frame turned by its rotation. */
    private fun <T> netOf(get: (Group) -> List<T?>): List<T?> = Face.entries.flatMap { face ->
        val group = groups[scheme[face]]
        val k = rotations[face] ?: 0
        List(9) { n -> group?.let { get(it)[RotationSearch.turnIndex(n, k)] } }
    }

    /** The pose from the largest face in view whose rotation is settled. */
    private fun poseOf(fresh: List<Reading>): Pose? {
        val main = fresh.filter { !it.removed && it.inlier && scheme.faceOf(it.group.color) in settled }.maxByOrNull { it.area } ?: return null
        val front = scheme.faceOf(main.group.color)
        // The reading's top side (0), turned to the group's frame and then into the net.
        val up = neighbourAt(front, (main.turn + (rotations[front] ?: 0)) % 4)
        return Pose(front, up)
    }

    companion object {
        private const val CENTRE = 4

        /** Readings that must agree before a sticker counts (`video-scan-spike`: wrong readings lasted at most 2 frames, 5 for one sticker). */
        const val MIN_VOTES = 3

        /** The leading colour needs this many times the votes of the next. */
        const val MARGIN = 2

        /** A reading belongs with a face's others when this many of its nine stickers agree (wrong lattices: ≤ 6). */
        const val MIN_AGREE = 7

        /** Corner views needed before a face's rotation is taken from them. */
        const val MIN_EVIDENCE = 2

        /** Most readings kept per face. */
        const val MAX_READINGS = 40

        /** How long the whole cube must stay recognised before the scan finishes. */
        const val FINISH_MILLIS = 500L

        private val realCorners: Set<List<Face>> = Corner.entries.flatMap { c -> (0 until 3).map { s -> List(3) { c.faces[(s + it) % 3] } } }.toSet()
        private val realEdges: Set<List<Face>> = Edge.entries.flatMap { listOf(it.faces, it.faces.reversed()) }.toSet()

        /** The quarter turns (clockwise) bringing [a] closest to [b], and how many stickers then agree. */
        fun bestTurn(a: List<CubeColor?>, b: List<CubeColor?>): Pair<Int, Int> =
            (0 until 4).map { k -> k to agreeing(a, b, k) }
                .maxWith(compareBy<Pair<Int, Int>> { it.second }.thenBy { -it.first })

        /** How many stickers of [a], turned [k] quarter turns, agree with [b] (missing ones never agree). */
        private fun agreeing(a: List<CubeColor?>, b: List<CubeColor?>, k: Int): Int =
            (0 until 9).count { n -> b[n] != null && a[RotationSearch.turnIndex(n, k)] == b[n] }

        /** The side (0 top, 1 right, 2 bottom, 3 left) of [face] in the net that [other] lies across, or null when they do not touch. */
        fun netSide(face: Face, other: Face): Int? = (0 until 4).firstOrNull { neighbourAt(face, it) == other }

        /** The face across side [side] of [face] in the net. */
        fun neighbourAt(face: Face, side: Int): Face = faceWithNormal(
            when (side.mod(4)) {
                0 -> face.down * -1
                1 -> face.right
                2 -> face.down
                else -> face.right * -1
            },
        )

        /**
         * The tilt that brings the most missing stickers into view from [pose]: the new front face's
         * missing stickers, a quarter for each missing sticker on the faces around it (seen at an
         * angle). Null when nothing is missing or staying put shows as many.
         */
        fun hint(pose: Pose, stickers: List<CubeColor?>, contradictions: Set<Int>): Tilt? {
            fun missing(face: Face) = (0 until 9).count { n -> (face.ordinal * 9 + n).let { stickers[it] == null || it in contradictions } }
            fun score(front: Face) = missing(front) + 0.25 * (0 until 4).sumOf { missing(neighbourAt(front, it)) }
            if (Face.entries.none { missing(it) > 0 }) return null
            val stay = score(pose.front)
            val best = Tilt.entries.maxBy { score(it.newFront(pose)) }
            return best.takeIf { score(it.newFront(pose)) > stay }
        }
    }
}

internal fun faceWithNormal(n: Vec3): Face = Face.entries.first { it.normal == n }
