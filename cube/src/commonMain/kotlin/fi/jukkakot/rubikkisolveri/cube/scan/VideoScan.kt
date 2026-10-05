package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.ColorScheme
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeCheck
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.CubeEditor
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
 * A face found in a frame as the scan took it: per sticker (reading order) the colour [names] it was
 * read as in this frame (null where none was found, or for a face the scan could not use) and
 * whether that sticker is already [recognised].
 */
data class FoundFace(val reading: FaceReading, val names: List<CubeColor?>, val recognised: List<Boolean>)

/**
 * What the video scan knows after a frame. [stickers] are the known colours (URFDLB, net order;
 * null = not yet): the best possible cube's colour where its margin is clear ([BestCube]), so a
 * sticker can be known without being seen. [leading] is the colour most readings name for each
 * sticker not yet known (null for the known ones and those without readings), [contradictions] the
 * unknown stickers whose readings disagree, [found] the faces in this frame, [pose] how the cube was
 * last seen held, [orientation] how it is turned in this frame (null when no face with a settled
 * rotation is in view), [hint] the tilt that brings most missing stickers into view (only while a
 * face is in view and its pose is known), [newStickers] how many stickers became known with this
 * frame. [complete]: the best cube is clear; [finished]: complete for [VideoScan.FINISH_MILLIS].
 * [clearness] is the best cube's smallest supported margin, [brightness] the median sticker
 * brightness of the faces in this frame (null without one), [dim] whether that is too dark, [stall]
 * why the scan cannot get on, if it cannot. [projection] is where every sticker lies in this frame
 * (null without [orientation]). [confirmed] are the sides whose nine stickers the best cube makes
 * clear (not their own votes alone): the sides that get a tick.
 */
data class VideoScanState(
    val stickers: List<CubeColor?>,
    val contradictions: Set<Int>,
    val found: List<FoundFace>,
    val pose: Pose?,
    val hint: Tilt?,
    val newStickers: Int,
    val complete: Boolean,
    val finished: Boolean,
    val leading: List<CubeColor?> = List(Stickers.COUNT) { null },
    val orientation: Orientation? = null,
    val clearness: Double = 0.0,
    val brightness: Int? = null,
    val dim: Boolean = false,
    val stall: Stall? = null,
    val projection: CubeProjection? = null,
    val confirmed: Set<Face> = emptySet(),
) {
    val recognised: Int get() = stickers.count { it != null }

    companion object {
        val EMPTY = VideoScanState(List(Stickers.COUNT) { null }, emptySet(), emptyList(), null, null, 0, false, false)
    }
}

/** Why the video scan cannot get on: too dark, no cube in view, or nothing new known for a while. */
enum class Stall { DARK, NO_CUBE, STUCK }

/**
 * The scan from continuous video (`video-scan`): every full face found in a frame ([FaceReading])
 * votes for its stickers. Faces are told apart by their centre colour (named regardless of
 * brightness, then together once all six are seen). Each face keeps its readings in its own "frame"
 * coordinates (one reading's way round) and counts the votes per sticker. How each face sits in the
 * net comes from corner views (two faces in one frame share an edge), else from the turns that make
 * the best possible cube cheapest. The votes, turned into the net, are the evidence for [BestCube]:
 * the possible cube that fits them best, known sticker by sticker where it clearly beats every other
 * possible cube ([CLEAR_MARGIN]), so a single wrong frame changes nothing and a misread sticker that
 * fits no real piece is corrected by the rest. Pure Kotlin, one code base for the phone and the browser.
 */
class VideoScan(private val scheme: ColorScheme = ColorScheme.STANDARD) {
    private class Reading(val face: FaceReading, var group: Group) {
        val rgb: List<Rgb?> get() = face.colors
        val area: Double get() = face.area
        val labs: List<Lab?> = face.colors.map { it?.toLab() }
        var names: List<CubeColor?> = emptyList()

        /** Per sticker, how well it fits each colour (by ordinal; [ColorClassifier.shares]), times its [weight]. */
        var shares: List<DoubleArray?> = emptyList()

        /** All nine stickers found; only a full reading can be a group's anchor. */
        val full: Boolean = face.isFull

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

        /** The colour with the most votes per place (recognised or not). */
        var leading: List<CubeColor?> = List(9) { null }

        /** Votes per place and colour (by ordinal): the inliers' shares summed. */
        var counts: List<DoubleArray> = List(9) { DoubleArray(6) }
        var disputed: Set<Int> = emptySet()
        var samples: List<Rgb?> = List(9) { null }
        val inliers: List<Reading> get() = readings.filter { it.inlier }
    }

    private val groups = LinkedHashMap<CubeColor, Group>()
    private var rotations: Map<Face, Int> = emptyMap()

    /** Faces whose rotation is settled (corner views, or every other turn makes a clearly costlier cube). */
    private var settled: Set<Face> = emptySet()
    private var rotationKey: String? = null
    private var sinceRotations = 0
    private var evidence: StickerEvidence = StickerEvidence.EMPTY
    private var best: BestCube? = null
    private var lastPose: Pose? = null
    private var lastOrientation: Orientation? = null
    private var lastRecognised = 0
    private var completeSince: Long? = null
    private var startedAt: Long? = null
    private var lastFaceAt: Long? = null
    private var dimSince: Long? = null
    private var progressAt = 0L
    private var mostKnown = 0

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
            Reading(face, group).also { group.add(it) }
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

        evidence = evidenceFor(rotations)
        val best = BestCube.solve(evidence, scheme)
        this.best = best
        val seen = Face.entries.filter { groups[scheme[it]] != null }
        val clearness = best?.clearness(evidence) ?: 0.0
        val complete = best != null && seen.all { it in settled } && clearness >= CLEAR_MARGIN
        // Known: from the best cube where it is clear, else from the votes alone.
        val voted = netOf { it.stickers }
        val clearAt = BooleanArray(Stickers.COUNT)
        val net = List(Stickers.COUNT) { i ->
            val face = Face.entries[i / 9]
            val clear = when {
                best == null -> false
                complete -> true
                i % 9 == CENTRE -> face in seen
                face in seen && face !in settled -> false
                else -> best.supportedMargin(i, evidence) >= CLEAR_MARGIN
            }
            clearAt[i] = clear
            if (clear) best!!.cube[i] else voted[i]
        }
        val confirmed = Face.entries.filter { f -> (0 until 9).all { clearAt[f.ordinal * 9 + it] } }.toSet()
        val contradictions = HashSet<Int>()
        for (face in Face.entries) {
            val group = groups[scheme[face]] ?: continue
            val k = rotations[face] ?: 0
            for (n in 0 until 9) {
                val i = face.ordinal * 9 + n
                if (RotationSearch.turnIndex(n, k) in group.disputed && net[i] == null) contradictions += i
            }
        }
        if (!complete) completeSince = null else if (completeSince == null) completeSince = nowMillis

        val usable = fresh.filterNotNull()
        val main = mainReading(usable)
        val pose = main?.let { poseOf(it) }
        if (pose != null) lastPose = pose
        val orientation = main?.let { orientationOf(it, usable) }
        val projection = if (main != null && orientation != null) projectionOf(main, orientation) else null
        if (orientation != null) lastOrientation = orientation
        val recognised = net.count { it != null }
        val leading = netOf { it.leading }.mapIndexed { i, c -> if (net[i] == null) c else null }
        val brightness = brightness(faces)
        val dim = brightness != null && brightness < DIM_BELOW
        state = VideoScanState(
            stickers = net,
            leading = leading,
            orientation = orientation,
            projection = projection,
            confirmed = confirmed,
            contradictions = contradictions,
            found = faces.mapIndexed { i, face -> foundFace(face, fresh[i], net) },
            pose = lastPose,
            hint = pose?.let { hint(it, net, contradictions) },
            newStickers = (recognised - lastRecognised).coerceAtLeast(0),
            complete = complete,
            finished = complete && nowMillis - completeSince!! >= FINISH_MILLIS,
            clearness = clearness,
            brightness = brightness,
            dim = dim,
            stall = stall(nowMillis, faces.isNotEmpty(), brightness, dim, recognised, complete),
        )
        lastRecognised = recognised
        return state
    }

    /**
     * Why the scan cannot get on at [now]: too dark for [DARK_MILLIS] (judged only while faces are
     * found), no face found for [NO_CUBE_MILLIS], or the cube in view but no more stickers known than
     * ever before for [STUCK_MILLIS] (also when the readings fit no possible cube clearly).
     */
    private fun stall(now: Long, anyFace: Boolean, brightness: Int?, dim: Boolean, known: Int, complete: Boolean): Stall? {
        val start = startedAt ?: now.also {
            startedAt = it
            progressAt = it
        }
        if (anyFace) lastFaceAt = now
        if (brightness != null) dimSince = if (dim) dimSince ?: now else null
        if (known > mostKnown || complete) {
            mostKnown = maxOf(mostKnown, known)
            progressAt = now
        }
        return when {
            complete -> null
            dimSince?.let { now - it >= DARK_MILLIS } == true -> Stall.DARK
            now - (lastFaceAt ?: start) >= NO_CUBE_MILLIS -> Stall.NO_CUBE
            now - progressAt >= STUCK_MILLIS && lastFaceAt?.let { now - it < IN_VIEW_MILLIS } == true -> Stall.STUCK
            else -> null
        }
    }

    /** Starts the scan again from nothing (the camera keeps running). */
    fun reset() {
        groups.clear()
        rotations = emptyMap()
        settled = emptySet()
        rotationKey = null
        sinceRotations = 0
        evidence = StickerEvidence.EMPTY
        best = null
        lastPose = null
        lastOrientation = null
        lastRecognised = 0
        completeSince = null
        startedAt = null
        lastFaceAt = null
        dimSince = null
        progressAt = 0L
        mostKnown = 0
        state = VideoScanState.EMPTY
    }

    /**
     * The scan as it stands, for the colour check (and the solution when it is sure): the known
     * colours, the leading colour of the others (marked uncertain), the centres of faces not seen
     * from the colour scheme. Known stickers whose colour came from the rest of the cube (never seen,
     * or their readings not sure or not agreeing) are [ScanOutcome.inferred].
     */
    fun outcome(): ScanOutcome {
        val known = state.stickers
        val net = known.mapIndexed { i, c -> c ?: if (i % 9 == CENTRE) scheme[Face.entries[i / 9]] else state.leading[i] }
        val editor = CubeEditor(net, scheme)
        // A face whose rotation is not settled may sit turned wrong in the net: all of it is doubtful.
        val turnedUnsure = Face.entries.filter { groups[scheme[it]] != null && it !in settled && !state.complete }
        val uncertain = known.indices.filter { (known[it] == null || Face.entries[it / 9] in turnedUnsure) && it % 9 != CENTRE }.toSet()
        val inferred = known.indices.filter { known[it] != null && it % 9 != CENTRE && evidence.sure(it) != known[it] }.toSet()
        val cube = editor.toCube()
        val samples = netOf { it.samples }
        return ScanOutcome(
            editor = editor,
            uncertain = uncertain,
            validity = cube?.let { CubeCheck.validity(it, scheme) } ?: Validity.WrongColorCount(editor.counts().filterValues { it != 9 }),
            samples = if (samples.all { it != null }) samples.map { it!! } else emptyList(),
            rotations = rotations,
            inferred = inferred,
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

    /**
     * Every sticker named with the cube's own centres as references (the guided scan's live reading),
     * and its share of each colour against the same references for the votes.
     */
    private fun renameStickers() {
        val refs = ColorClassifier.references(groups.mapValues { (_, g) -> g.inliers.ifEmpty { g.readings }.map { it.labs[CENTRE]!! } })
        for (g in groups.values) for (r in g.readings) {
            r.names = r.rgb.mapIndexed { n, rgb -> if (n == CENTRE) g.color else rgb?.let { ColorClassifier.live(it, refs) } }
            r.shares = r.labs.mapIndexed { n, lab ->
                lab?.let { ColorClassifier.shares(it, refs).also { s -> weight(r.rgb[n]!!).let { w -> for (c in s.indices) s[c] *= w } } }
            }
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
        val leading = arrayOfNulls<CubeColor>(9)
        val samples = arrayOfNulls<Rgb>(9)
        val disputed = HashSet<Int>()
        val votes = List(9) { DoubleArray(6) }
        for (n in 0 until 9) {
            val v = votes[n]
            val voters = inliers.mapNotNull { r -> r.names[RotationSearch.turnIndex(n, r.turn)]?.let { r to it } }
            for ((r, _) in voters) r.shares[RotationSearch.turnIndex(n, r.turn)]!!.forEachIndexed { c, share -> v[c] += share }
            val lead = if (voters.isEmpty()) null else v.indices.maxBy { v[it] }
            leading[n] = lead?.let { CubeColor.entries[it] }
            val second = v.indices.filter { it != lead }.maxOf { v[it] }
            val sticky = g.sticky[n]
            val color = when {
                lead == null -> null
                v[lead] >= MIN_VOTES && v[lead] >= MARGIN * second -> CubeColor.entries[lead]
                sticky != null && v[sticky.ordinal] >= v[lead] -> sticky
                else -> null
            }
            if (color == null && lead != null && v[lead] >= MIN_VOTES) disputed += n
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
        g.leading = leading.toList()
        g.counts = votes
        g.samples = samples.toList()
        g.disputed = disputed
    }

    /**
     * How each face sits in the net: corner views first (a reading's side towards a neighbouring
     * face, turned to the group's frame, against the side that neighbour is on in the net). The
     * other faces take the turns that make the best possible cube cheapest (a few rounds of trying
     * each face's other turns); a face is settled when each other turn that reads it differently
     * makes the best cube at least [CLEAR_MARGIN] costlier. Worked out again when the leading
     * colours change, else every [ROTATION_EVERY] frames as the votes grow.
     */
    private fun updateRotations() {
        val views = HashMap<Face, IntArray>()
        for (g in groups.values) {
            val face = scheme.faceOf(g.color)
            for (r in g.readings) {
                if (!r.inlier) continue
                for ((side, other) in r.neighbours) {
                    if (other.removed || !other.inlier || other.group === g) continue
                    val netSide = netSide(face, scheme.faceOf(other.group.color)) ?: continue
                    views.getOrPut(face) { IntArray(4) }[(netSide - (side + r.turn)).mod(4)]++
                }
            }
        }
        val known = views.mapNotNull { (face, votes) ->
            val top = votes.indices.maxBy { votes[it] }
            val second = votes.indices.filter { it != top }.maxOf { votes[it] }
            if (votes[top] >= MIN_EVIDENCE && votes[top] > second) face to top else null
        }.toMap()

        val key = known.toString() + groups.values.joinToString("|") { g -> g.color.letter + g.leading.joinToString("") { it?.letter?.toString() ?: "." } }
        if (key == rotationKey && ++sinceRotations < ROTATION_EVERY) return
        rotationKey = key
        sinceRotations = 0

        val free = Face.entries.filter { groups[scheme[it]] != null && it !in known }
        val turns = HashMap(Face.entries.associateWith { known[it] ?: rotations[it] ?: 0 })
        fun costWith(face: Face, k: Int) = BestCube.cost(evidenceFor(HashMap(turns).also { it[face] = k }), scheme)
        var cost = BestCube.cost(evidenceFor(turns), scheme)
        repeat(2) {
            for (face in free) for (k in 0 until 4) {
                if (k == turns[face]) continue
                val c = costWith(face, k)
                if (c < cost - 1e-9) {
                    cost = c
                    turns[face] = k
                }
            }
        }
        rotations = turns
        settled = known.keys + free.filter { face ->
            val leading = groups.getValue(scheme[face]).leading
            val now = turns.getValue(face)
            (0 until 4).filter { k -> k != now && (0 until 9).any { leading[RotationSearch.turnIndex(it, k)] != leading[RotationSearch.turnIndex(it, now)] } }
                .all { k -> costWith(face, k) - cost >= CLEAR_MARGIN }
        }
    }

    /** The groups' votes as evidence per net sticker, each face's frame turned by [turns] (centres: none). */
    private fun evidenceFor(turns: Map<Face, Int>): StickerEvidence = StickerEvidence(
        Face.entries.flatMap { face ->
            val group = groups[scheme[face]]
            val k = turns[face] ?: 0
            List(9) { n -> if (group == null || n == CENTRE) DoubleArray(6) else group.counts[RotationSearch.turnIndex(n, k)] }
        },
    )

    /** The colours of each face in the net, from each group's frame turned by its rotation. */
    private fun <T> netOf(get: (Group) -> List<T?>): List<T?> = Face.entries.flatMap { face ->
        val group = groups[scheme[face]]
        val k = rotations[face] ?: 0
        List(9) { n -> group?.let { get(it)[RotationSearch.turnIndex(n, k)] } }
    }

    /** The largest face in view whose rotation is settled: the pose and the orientation come from it. */
    private fun mainReading(fresh: List<Reading>): Reading? =
        fresh.filter { !it.removed && it.inlier && scheme.faceOf(it.group.color) in settled }.maxByOrNull { it.area }

    /** Quarter turns from [r]'s reading order to its face in the net: its side s is the net's side s + this. */
    private fun netTurn(r: Reading): Int = (r.turn + (rotations[scheme.faceOf(r.group.color)] ?: 0)) % 4

    private fun poseOf(main: Reading): Pose {
        val front = scheme.faceOf(main.group.color)
        // The reading's top side (0), turned to the group's frame and then into the net.
        return Pose(front, neighbourAt(front, netTurn(main)))
    }

    /**
     * The cube's orientation from [main]'s steps (its right and bottom sides in the net give the
     * cube vectors they show), the tilt's sign from the other faces in view or the last orientation.
     */
    private fun orientationOf(main: Reading, fresh: List<Reading>): Orientation? {
        val front = scheme.faceOf(main.group.color)
        val k = netTurn(main)
        val candidates = Orientation.candidates(main.face.u, main.face.v, sideVector(front, k + 1), sideVector(front, k + 2))
        val others = fresh.filter { it !== main && it.group !== main.group }.map { scheme.faceOf(it.group.color).normal to it.face.centre }
        return Orientation.choose(candidates, front.normal, main.face.centre, others, lastOrientation)
    }

    /** Every sticker projected into this frame from [main] and the cube's [orientation]. */
    private fun projectionOf(main: Reading, orientation: Orientation): CubeProjection? {
        val front = scheme.faceOf(main.group.color)
        val k = netTurn(main)
        return CubeProjection.of(orientation, front, main.face.centre, main.face.u, main.face.v, sideVector(front, k + 1), sideVector(front, k + 2))
    }

    /** [face] as the scan took it in this frame: names in reading order, and which of its places are known in [net]. */
    private fun foundFace(face: FaceReading, r: Reading?, net: List<CubeColor?>): FoundFace {
        if (r == null || r.removed) return FoundFace(face, List(9) { null }, List(9) { false })
        val recognised = MutableList(9) { false }
        if (r.inlier) {
            val netFace = scheme.faceOf(r.group.color)
            val k = rotations[netFace] ?: 0
            for (j in 0 until 9) recognised[RotationSearch.turnIndex(RotationSearch.turnIndex(j, k), r.turn)] = net[netFace.ordinal * 9 + j] != null
        }
        return FoundFace(face, r.names.toList(), recognised)
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

        /** The best cube is clear when every place's margin reaches this ([BestCube.clearness]; chosen by simulation with soft votes, `video-scan-light` findings). */
        const val CLEAR_MARGIN = 2.0

        /** Frames between working the faces' rotations out again while the leading colours stay the same. */
        const val ROTATION_EVERY = 10

        /**
         * Median sticker brightness (brightest channel) under which the picture is too dark. The camera
         * evens out exposure, so dim warm light does not show here (evening videos: median 130 to 209,
         * failing or not); only real darkness does (`video-scan-progress` findings).
         */
        const val DIM_BELOW = 70

        /** Too dark this long: stall. */
        const val DARK_MILLIS = 3_000L

        /** No face found this long: stall. */
        const val NO_CUBE_MILLIS = 8_000L

        /** No more stickers known this long with the cube in view: stall (user, 2026-10-05). */
        const val STUCK_MILLIS = 15_000L

        /** A face found this recently counts as the cube in view. */
        const val IN_VIEW_MILLIS = 1_000L

        /** A reading with a channel at least this high is washed out (the median of its blob: half its pixels or more). */
        const val WASHED_FROM = 250

        /** How much a washed-out reading's vote counts (too much light: orange reads yellow, blue white). */
        const val WASHED_WEIGHT = 0.2

        /** How much a reading of [rgb] counts in the votes. */
        fun weight(rgb: Rgb): Double = if (maxOf(rgb.r, rgb.g, rgb.b) >= WASHED_FROM) WASHED_WEIGHT else 1.0

        /** Median brightness (brightest channel) of the stickers of [faces], or null without any. */
        fun brightness(faces: List<FaceReading>): Int? {
            val values = faces.flatMap { f -> f.colors.filterNotNull().map { maxOf(it.r, it.g, it.b) } }.sorted()
            return if (values.isEmpty()) null else values[values.size / 2]
        }


        /** The quarter turns (clockwise) bringing [a] closest to [b], and how many stickers then agree. */
        fun bestTurn(a: List<CubeColor?>, b: List<CubeColor?>): Pair<Int, Int> =
            (0 until 4).map { k -> k to agreeing(a, b, k) }
                .maxWith(compareBy<Pair<Int, Int>> { it.second }.thenBy { -it.first })

        /** How many stickers of [a], turned [k] quarter turns, agree with [b] (missing ones never agree). */
        private fun agreeing(a: List<CubeColor?>, b: List<CubeColor?>, k: Int): Int =
            (0 until 9).count { n -> b[n] != null && a[RotationSearch.turnIndex(n, k)] == b[n] }

        /** The side (0 top, 1 right, 2 bottom, 3 left) of [face] in the net that [other] lies across, or null when they do not touch. */
        fun netSide(face: Face, other: Face): Int? = (0 until 4).firstOrNull { neighbourAt(face, it) == other }

        /** One sticker step on [face] towards its side [side] in the net (0 top, 1 right, 2 bottom, 3 left). */
        fun sideVector(face: Face, side: Int): Vec3 = when (side.mod(4)) {
            0 -> face.down * -1
            1 -> face.right
            2 -> face.down
            else -> face.right * -1
        }

        /** The face across side [side] of [face] in the net. */
        fun neighbourAt(face: Face, side: Int): Face = faceWithNormal(sideVector(face, side))

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
