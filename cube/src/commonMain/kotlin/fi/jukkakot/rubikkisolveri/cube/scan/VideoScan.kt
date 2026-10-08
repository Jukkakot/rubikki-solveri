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

/**
 * A face found in a frame as the scan took it: per sticker (reading order) the colour [names] it was
 * read as in this frame (null where none was found, or for a face the scan could not use) and
 * whether that sticker is already [recognised].
 */
data class FoundFace(
    val reading: FaceReading,
    val names: List<CubeColor?>,
    val recognised: List<Boolean>,
    /** The known colour of each recognised sticker in reading order (null where not recognised). */
    val known: List<CubeColor?> = List(9) { null },
)

/**
 * What the video scan knows after a frame. [stickers] are the known colours (URFDLB, net order;
 * null = not yet): the best possible cube's colour where its margin is clear ([BestCube]), so a
 * sticker can be known without being seen. [leading] is the colour most readings name for each
 * sticker not yet known (null for the known ones and those without readings), [contradictions] the
 * unknown stickers whose readings disagree, [found] the faces in this frame, [pose] how the cube was
 * last seen held, [orientation] how it is turned in this frame (null when no face with a settled
 * rotation is in view),
 * [newStickers] how many stickers became known with this
 * frame. [complete]: the best cube is clear; [finished]: complete for [VideoScan.FINISH_MILLIS].
 * [clearness] is the best cube's smallest supported margin, [brightness] the median sticker
 * brightness of the faces in this frame (null without one), [dim] whether that is too dark, [stall]
 * why the scan cannot get on, if it cannot. [projection] is where every sticker lies: built in a
 * frame with [orientation], held over frames without one (moved onto the largest face found, if any)
 * for at most [VideoScan.HOLD_MILLIS]; [projectionAge] is how long ago it was built or moved. [confirmed] are the sides whose nine stickers the best cube makes
 * clear (not their own votes alone): the sides that get a tick.
 */
data class VideoScanState(
    val stickers: List<CubeColor?>,
    val contradictions: Set<Int>,
    val found: List<FoundFace>,
    val pose: Pose?,
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
    val projectionAge: Long = 0,
    /** Two faces could still be told apart either way for a while: the status line asks to turn the cube (`scan-rules`). */
    val undecided: Boolean = false,
) {
    val recognised: Int get() = stickers.count { it != null }

    companion object {
        val EMPTY = VideoScanState(List(Stickers.COUNT) { null }, emptySet(), emptyList(), null, 0, false, false)
    }
}

/** Why the video scan cannot get on: too dark, no cube in view, or nothing new known for a while. */
enum class Stall { DARK, NO_CUBE, STUCK }

/**
 * Which way the video scan tells the faces apart (`scan-rules`): [RULES] by the rules of a real cube
 * ([FaceTracks]), [LOOK] by how the centres look (the earlier scanner). The log name is [logName].
 */
enum class ScanEngine(val logName: String) { RULES("rules"), LOOK("look") }

/**
 * The scan from continuous video (`video-scan`), with either of two ways to tell the faces apart
 * ([engine]; `scan-rules`). [ScanEngine.RULES], the default: faces followed from picture to picture
 * and known by the rules of a real cube ([FaceTracks], [rulesFrame]). [ScanEngine.LOOK], the earlier
 * scanner, described here: every full face found in a frame ([FaceReading])
 * votes for its stickers. Faces are told apart by how they look on this cube in this light (their
 * stickers and centre colour), kept in piles named together, each colour once; a pile that could as
 * well be another colour waits (`scan-centre-naming`). Each face keeps its readings in its own "frame"
 * coordinates (one reading's way round) and counts the votes per sticker. How each face sits in the
 * net comes from corner views (two faces in one frame share an edge), else from the turns that make
 * the best possible cube cheapest. The votes, turned into the net, are the evidence for [BestCube]:
 * the possible cube that fits them best, known sticker by sticker where it clearly beats every other
 * possible cube ([CLEAR_MARGIN]), so a single wrong frame changes nothing and a misread sticker that
 * fits no real piece is corrected by the rest. Pure Kotlin, one code base for the phone and the browser.
 */
class VideoScan(
    private val scheme: ColorScheme = ColorScheme.STANDARD,
    val engine: ScanEngine = ScanEngine.RULES,
) {
    private class Reading(val face: FaceReading, var group: Group, val seq: Long) {
        val rgb: List<Rgb?> get() = face.colors
        val area: Double get() = face.area
        /** Each sticker's colour for naming it, with and without its brightness. */
        val labs: List<Tone?> = face.colors.map { it?.let(::Tone) }

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

    /**
     * A pile: the readings of one face of the cube, told apart from the other piles by its own centre
     * colour. [color] is its name from the joint naming ([named] once it has one); a [doubtful] pile
     * could as well be another colour and counts for nothing yet.
     */
    private class Group(var color: CubeColor) {
        var named = false
        var doubtful = false

        /** A doubtful pile's next-best colour. */
        var otherColor: CubeColor? = null

        /** Doubtful only because it was seen too few times to be named with the others: hides no colour. */
        var waiting = false

        /** Piles seen in one picture with this one: never the same face. */
        val apart = HashSet<Group>()

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

    private val piles = ArrayList<Group>()

    /** The piles that count, by name: every pile but the doubtful ones. */
    private var groups: Map<CubeColor, Group> = emptyMap()
    private var seq = 0L
    private var rotations: Map<Face, Int> = emptyMap()

    /** Faces whose rotation is settled (corner views, or every other turn makes a clearly costlier cube). */
    private var settled: Set<Face> = emptySet()
    private var rotationKey: String? = null
    private var sinceRotations = 0
    private var evidence: StickerEvidence = StickerEvidence.EMPTY
    private var best: BestCube? = null
    private var lastPose: Pose? = null
    private var lastOrientation: Orientation? = null
    private var held: CubeProjection? = null
    private var heldBuiltAt = 0L
    private var heldMovedAt = 0L
    private var lastRecognised = 0
    private var completeSince: Long? = null
    private var startedAt: Long? = null
    private var lastFaceAt: Long? = null
    private var dimSince: Long? = null
    private var progressAt = 0L
    private var mostKnown = 0

    var state: VideoScanState = VideoScanState.EMPTY
        private set

    /** The rules scanner's tracks ([ScanEngine.RULES]). */
    private var tracks = FaceTracks(scheme)

    /** The rules scanner's face centres as read ([FaceTracks.centreLog]), for the scan log; empty for the look scanner. */
    val centreLog: String get() = if (engine == ScanEngine.RULES) tracks.centreLog else ""

    /**
     * Handles the faces found in one frame taken at [nowMillis]. A partial face (stickers missing)
     * counts only with its centre and only for a face already started by a full one.
     */
    fun onFrame(faces: List<FaceReading>, nowMillis: Long): VideoScanState {
        if (engine == ScanEngine.RULES) return rulesFrame(faces, nowMillis)
        val pileOf = pileFaces(faces)
        val fresh = faces.mapIndexed { i, face ->
            val group = pileOf[i] ?: return@mapIndexed null
            Reading(face, group, seq++).also { group.add(it) }
        }
        for (i in faces.indices) for (j in faces.indices) {
            val a = fresh[i] ?: continue
            val b = fresh[j] ?: continue
            if (i != j) faces[i].sideTowards(faces[j])?.let { a.neighbours += it to b }
            if (i != j && faces[i].isFull && faces[j].isFull) a.group.apart += b.group
        }
        mergeClosePiles()
        nameJointly()
        renameStickers()
        piles.forEach { consensus(it) }
        updateRotations()

        evidence = evidenceFor(rotations)
        val best = BestCube.solve(evidence, scheme)
        this.best = best
        val seen = Face.entries.filter { groups[scheme[it]] != null }
        val clearness = best?.clearness(evidence) ?: 0.0
        val complete = best != null && seen.all { it in settled } && clearness >= CLEAR_MARGIN && piles.none { it.doubtful && it.inliers.size >= MIN_VOTES }
        // Known: from the best cube where it is clear, else from the votes alone.
        val voted = netOf { it.stickers }
        // While a pile seen several times could be either of two colours, no sticker of those colours is known.
        val unsure = piles.filter { it.doubtful && !it.waiting && it.inliers.size >= MIN_VOTES }.flatMap { listOfNotNull(it.color, it.otherColor) }.toSet()
        val clearAt = BooleanArray(Stickers.COUNT)
        val net = List(Stickers.COUNT) { i ->
            val face = Face.entries[i / 9]
            val clear = when {
                best == null -> false
                complete -> true
                i % 9 == CENTRE -> face in seen
                face in seen && face !in settled -> false
                face !in seen && unsure.isNotEmpty() -> false
                else -> best.supportedMargin(i, evidence) >= CLEAR_MARGIN
            }
            clearAt[i] = clear
            if (clear) best!!.cube[i] else voted[i]
        }.map { c -> c?.takeIf { it !in unsure } }
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
        val usable = fresh.filterNotNull()
        val main = mainReading(usable)
        val others = main?.let { m -> usable.filter { it !== m && it.group !== m.group && !it.group.doubtful }.map { scheme.faceOf(it.group.color).normal to it.face.centre } }.orEmpty()
        val anchor = usable.filter { !it.removed && trusted(it.group) }.maxByOrNull { it.area }?.let { it.face to scheme.faceOf(it.group.color) }
        val leading = netOf { it.leading }.mapIndexed { i, c -> if (net[i] == null) c else null }
        return finish(
            faces, nowMillis, net, leading, confirmed, contradictions, faces.mapIndexed { i, face -> foundFace(face, fresh[i], net) }, complete, clearness,
            main?.let { Held(it.face, scheme.faceOf(it.group.color), netTurn(it)) }, others, anchor,
        )
    }

    /** The face a frame's pose and projection come from: its reading, which face it is and its turn into the net ([netTurn]). */
    private class Held(val face: FaceReading, val front: Face, val turn: Int)

    /**
     * The state after a frame, either scanner: the pose, orientation and projection from [main] (held
     * over frames without one, moved onto [anchor]), the finish timing, the light and the stall.
     */
    private fun finish(
        faces: List<FaceReading>,
        nowMillis: Long,
        net: List<CubeColor?>,
        leading: List<CubeColor?>,
        confirmed: Set<Face>,
        contradictions: Set<Int>,
        found: List<FoundFace>,
        complete: Boolean,
        clearness: Double,
        main: Held?,
        others: List<Pair<Vec3, Point>>,
        anchor: Pair<FaceReading, Face>?,
        undecided: Boolean = false,
    ): VideoScanState {
        if (!complete) completeSince = null else if (completeSince == null) completeSince = nowMillis
        if (main != null) lastPose = Pose(main.front, neighbourAt(main.front, main.turn))
        val orientation = main?.let { orientationOf(it, others) }
        val built = if (main != null && orientation != null) projectionOf(main, orientation) else null
        if (orientation != null) lastOrientation = orientation
        val projection = holdProjection(built, anchor, nowMillis)
        val recognised = net.count { it != null }
        val brightness = brightness(faces)
        val dim = brightness != null && brightness < DIM_BELOW
        state = VideoScanState(
            stickers = net,
            leading = leading,
            orientation = orientation,
            projection = projection,
            confirmed = confirmed,
            contradictions = contradictions,
            found = found,
            pose = lastPose,
            newStickers = (recognised - lastRecognised).coerceAtLeast(0),
            complete = complete,
            finished = complete && nowMillis - completeSince!! >= FINISH_MILLIS,
            clearness = clearness,
            brightness = brightness,
            dim = dim,
            stall = stall(nowMillis, faces.isNotEmpty(), brightness, dim, recognised, complete),
            projectionAge = if (projection == null) 0 else nowMillis - heldMovedAt,
            undecided = undecided && !complete,
        )
        lastRecognised = recognised
        return state
    }

    /**
     * The projection to draw at [now]: a freshly [built] one, else the last one held. In a frame with
     * faces but no settled orientation the held one is moved (not turned) so that the side of the
     * largest face found lies on that face; it is dropped [HOLD_MILLIS] after it was built.
     */
    private fun holdProjection(built: CubeProjection?, anchor: Pair<FaceReading, Face>?, now: Long): CubeProjection? {
        if (built != null) {
            held = built
            heldBuiltAt = now
            heldMovedAt = now
            return built
        }
        val last = held ?: return null
        if (now - heldBuiltAt > HOLD_MILLIS) {
            held = null
            return null
        }
        val (face, side) = anchor ?: return last
        val moved = last.movedBy(face.centre - last.points[side.ordinal * 9 + CENTRE])
        held = moved
        heldMovedAt = now
        return moved
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
        tracks = FaceTracks(scheme)
        piles.clear()
        groups = emptyMap()
        rotations = emptyMap()
        settled = emptySet()
        rotationKey = null
        sinceRotations = 0
        evidence = StickerEvidence.EMPTY
        best = null
        lastPose = null
        lastOrientation = null
        held = null
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
        if (engine == ScanEngine.RULES) return rulesOutcome()
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

    /**
     * The pile each face found goes to (null: none), by the face itself as this cube shows it in this
     * light (`scan-centre-naming`). A full face joins a pile whose anchor its stickers agree with
     * ([MIN_AGREE]; one face in two lights), else the closest pile whose centre is within
     * [JOIN_WITHIN] of its own, unless no pile that close goes by the colour the default palette clearly
     * names it ([NAME_CLEAR]: red next to an orange pile, which the distance alone cannot tell apart);
     * otherwise it starts a pile. Faces of one picture take the closest piles first, each pile once (a
     * dark blue centre beside the white face, `scan-centre-clash`). With six piles a new one only
     * replaces a stray named by what was left over; else the face is left out of this picture. A partial
     * face only joins its closest pile within [JOIN_WITHIN]: a wrong lattice is caught by the anchor.
     */
    private fun pileFaces(faces: List<FaceReading>): List<Group?> {
        val means = piles.associateWith { pileLab(it) }
        val centres = faces.map { f -> f.colors[CENTRE]?.let { ColorClassifier.scaled(it).toLab() } }
        val out = arrayOfNulls<Group>(faces.size)
        val taken = HashSet<Group>()
        fun near(i: Int) = piles.filter { it !in taken }.map { it to centres[i]!!.distance(means[it] ?: pileLab(it)) }.sortedBy { it.second }
        // The stickers as the piles' own centres name them: a face agreeing with a pile's anchor is that face.
        val refs = centreRefs()
        val names = faces.map { f -> f.colors.mapIndexed { n, c -> if (n == CENTRE) null else c?.let { nameSticker(it, refs) } } }
        fun agrees(i: Int, g: Group) = g.anchor?.let { bestTurn(names[i], it.names).second >= MIN_AGREE } == true

        // A face that would put a neighbour of this picture on another side of the pile than the pile has
        // seen it is another face (`scan-steady-progress`: on a striped cube the red face turned half
        // round reads as the orange face when its red looks orange; the white face beside it tells).
        fun clashes(i: Int, g: Group): Boolean {
            val anchor = g.anchor ?: return false
            val turn = (bestTurn(names[i], anchor.names).first + g.anchorTurn) % 4
            return faces.indices.any { j ->
                val other = out[j] ?: return@any false
                val side = if (j == i || !faces[j].isFull) null else faces[i].sideTowards(faces[j])
                val usual = side?.let { usualSide(g, other) }
                side != null && usual != null && (side + turn) % 4 != usual
            }
        }
        val left = faces.indices.filter { faces[it].isFull }.toMutableList()
        while (left.isNotEmpty()) {
            val i = left.minBy { near(it).firstOrNull()?.second ?: Double.MAX_VALUE }
            left.remove(i)
            // A second lattice on a face this picture already has (the same stickers): left out.
            if (taken.any { agrees(i, it) }) continue
            val clashing = piles.filter { clashes(i, it) }.toSet()
            val near = near(i).filter { it.first !in clashing }
            val close = near.filter { it.second <= JOIN_WITHIN }.map { it.first }
            val ranked = ColorClassifier.centreDistances(faces[i].colors[CENTRE]!!).entries.sortedBy { it.value }
            val clearName = ranked[0].key.takeIf { ranked[1].value - ranked[0].value >= NAME_CLEAR }
            val agreeing = near.map { it.first }.filter { agrees(i, it) }
            val own = agreeing.isEmpty() && (close.isEmpty() || (clearName != null && close.none { ColorClassifier.rankedCentre(meanCentre(it)).first() == clearName }))
            val stray = if (own && piles.size >= 6) piles.firstOrNull { it !in taken && it.readings.size < MIN_VOTES && !trusted(it) } else null
            val pile = when {
                !own -> agreeing.firstOrNull() ?: close.first()
                piles.size < 6 -> Group(ColorClassifier.rankedCentre(faces[i].colors[CENTRE]!!).first()).also { piles += it }
                stray != null -> {
                    drop(stray)
                    Group(stray.color).also { piles += it }
                }
                else -> null
            }
            if (pile != null) {
                out[i] = pile
                taken += pile
                // Never merged with a pile it clashed with: they are two faces.
                for (g in clashing) if (g !== pile) {
                    pile.apart += g
                    g.apart += pile
                }
            }
        }
        for ((i, face) in faces.withIndex()) if (!face.isFull && centres[i] != null) {
            out[i] = piles.filter { it.readings.isNotEmpty() }.map { it to centres[i]!!.distance(means[it] ?: pileLab(it)) }
                .filter { it.second <= JOIN_WITHIN }.minByOrNull { it.second }?.first
        }
        return out.toList()
    }

    /**
     * The side of [g]'s frame on which pile [other] is seen next to it: null until its agreeing readings
     * show it there [MIN_VOTES] times and on no other side.
     */
    private fun usualSide(g: Group, other: Group): Int? {
        val votes = IntArray(4)
        for (r in g.readings) if (r.inlier) for ((side, o) in r.neighbours) if (o.group === other && !o.removed && o.inlier) votes[(side + r.turn) % 4]++
        val top = votes.indices.maxBy { votes[it] }
        return top.takeIf { votes[it] >= MIN_VOTES && votes.indices.all { k -> k == it || votes[k] == 0 } }
    }

    private fun drop(pile: Group) {
        piles.remove(pile)
        pile.readings.forEach { it.removed = true }
        piles.forEach { it.apart.remove(pile) }
    }

    /**
     * Two piles whose centres have come within [MERGE_WITHIN], or whose anchors agree (one face seen in
     * two lights), become one, unless they were ever seen in one picture.
     */
    private fun mergeClosePiles() {
        while (true) {
            val pair = piles.flatMap { a -> piles.filter { b -> b !== a && b !in a.apart }.map { b -> a to b } }
                .firstOrNull { (a, b) -> pileLab(a).distance(pileLab(b)) <= MERGE_WITHIN || sameStickers(a, b) } ?: return
            val (keep, gone) = pair.let { (a, b) -> if (a.readings.size >= b.readings.size) a to b else b to a }
            piles.remove(gone)
            piles.forEach { it.apart.remove(gone) }
            keep.apart += gone.apart
            for (r in gone.readings) r.group = keep
            keep.readings += gone.readings
            keep.readings.sortBy { it.seq }
            while (keep.readings.size > MAX_READINGS) {
                val old = keep.readings.first { it !== keep.anchor }
                old.removed = true
                keep.readings.remove(old)
            }
        }
    }

    /** Two piles whose anchors agree on [MIN_AGREE] stickers in some turn: one face seen in two lights. */
    private fun sameStickers(a: Group, b: Group): Boolean {
        val x = a.anchor ?: return false
        val y = b.anchor ?: return false
        return bestTurn(x.names.mapIndexed { n, c -> if (n == CENTRE) null else c }, y.names).second >= MIN_AGREE
    }

    private fun meanCentre(group: Group): Rgb {
        val list = group.inliers.ifEmpty { group.readings }.filter { it.rgb[CENTRE] != null }
        return Rgb(list.sumOf { it.rgb[CENTRE]!!.r } / list.size, list.sumOf { it.rgb[CENTRE]!!.g } / list.size, list.sumOf { it.rgb[CENTRE]!!.b } / list.size)
    }

    /** A pile whose name can be used: not doubtful, or seen only once or twice under the colour its centre is closest to. */
    private fun trusted(g: Group): Boolean = !g.doubtful || (g.inliers.size < MIN_VOTES && ColorClassifier.rankedCentre(meanCentre(g)).first() == g.color)

    /**
     * The cube's own centres as references for naming stickers: every trusted pile whose centre the
     * default palette names as the pile is named (a face seen rarely still tells its colour; a yellow
     * centre washed out to near white in bright light does not stand for yellow, `scan-centre-naming`),
     * the default palette for the other colours.
     */
    private fun centreRefs(): Map<CubeColor, Tone> {
        val known = piles.filter { trusted(it) && ColorClassifier.rankedCentre(meanCentre(it)).first() == it.color }
            .associate { g -> g.color to g.inliers.ifEmpty { g.readings }.mapNotNull { it.labs[CENTRE] } }
        return CubeColor.entries.associateWith { c -> known[c]?.takeIf { it.isNotEmpty() }?.let(Tone::mean) ?: Tone(ColorClassifier.DEFAULT_PALETTE.getValue(c)) }
    }

    /** A pile's centre without its brightness, to tell piles apart. */
    private fun pileLab(group: Group): Lab = ColorClassifier.scaled(meanCentre(group)).toLab()

    /**
     * The piles named together, each colour once, the best fit overall (current names win a tie). A
     * pile is doubtful while naming it otherwise costs less than [DOUBT_MARGIN] more. A pile renamed
     * keeps its readings and its stickers; the votes are worked out again with the new references.
     */
    private fun nameJointly() {
        // A pile seen fewer than MIN_VOTES times may be a stray (a lattice across an edge): it takes a colour left over and waits.
        val seen = piles.filter { it.inliers.size >= MIN_VOTES }.ifEmpty { piles.toList() }
        val used = HashSet<CubeColor>()
        fun rename(g: Group, color: CubeColor) {
            // The stickers keep what they were read as (`scan-steady-progress`): a sticky colour holds only while its votes still lead.
            g.color = color
            g.named = true
            used += color
        }
        if (seen.isNotEmpty()) {
            val namings = ColorClassifier.centreNamingCosts(seen.map { meanCentre(it) }, seen.map { if (it.named) it.color else null })
            val (cost, naming) = namings.first()
            seen.forEachIndexed { i, g ->
                val other = namings.first { it.second[i] != naming[i] }
                g.doubtful = other.first - cost < DOUBT_MARGIN
                g.waiting = false
                g.otherColor = other.second[i]
                rename(g, naming[i])
            }
        }
        for (g in piles) if (g !in seen) {
            val left = ColorClassifier.centreDistances(meanCentre(g)).entries.filter { it.key !in used }.sortedBy { it.value }
            // Only a face its own clear colour is left for counts at once: a stray named by what was left over waits.
            val own = ColorClassifier.rankedCentre(meanCentre(g)).first()
            g.doubtful = left.isEmpty() || left[0].key != own || (left.size > 1 && left[1].value - left[0].value < DOUBT_MARGIN)
            g.waiting = true
            rename(g, left.firstOrNull()?.key ?: g.color)
        }
        groups = piles.filter { !it.doubtful }.associateBy { it.color }
    }

    /**
     * Every sticker named with the cube's own centres as references (the guided scan's live reading),
     * and its share of each colour against the same references for the votes.
     */
    private fun renameStickers() {
        val refs = centreRefs()
        for (g in piles) for (r in g.readings) {
            r.names = r.rgb.mapIndexed { n, rgb -> if (n == CENTRE) g.color else rgb?.let { nameSticker(it, refs) } }
            r.shares = r.labs.mapIndexed { n, lab ->
                lab?.let { t -> ColorClassifier.sharesOf(DoubleArray(6) { c -> refs.getValue(CubeColor.entries[c]).distance(t) }).also { s -> weight(r.rgb[n]!!).let { w -> for (c in s.indices) s[c] *= w } } }
            }
        }
    }

    private fun nameSticker(rgb: Rgb, refs: Map<CubeColor, Tone>): CubeColor = Tone(rgb).let { t -> refs.minBy { it.value.distance(t) }.key }

    private fun Group.add(reading: Reading) {
        readings += reading
        if (readings.size > MAX_READINGS) {
            // The oldest goes, agreeing or not, so wrong readings age out (`scan-centre-clash`); never the
            // reading just added (not judged yet, this frame's marks come from it) nor the anchor (the
            // group keeps a full reading; a newer one takes over once more readings agree with it).
            val drop = readings.first { it !== reading && it !== anchor }
            drop.removed = true
            readings.remove(drop)
        }
    }

    /**
     * The group's anchor is the full reading most others agree with (in some rotation); as old
     * readings age out, a newer group of agreeing readings takes over. Full readings agreeing with it on
     * [MIN_AGREE] stickers vote, each turned to the group's frame. A partial reading votes when all
     * but one of its stickers agree with the anchor in a single best turn.
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
                // With soft votes the colour may lead without any reading named it: then all readings give the sample.
                val agreeing = (voters.filter { it.second == color }.ifEmpty { voters }).map { it.first.rgb[RotationSearch.turnIndex(n, it.first.turn)]!! }
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
                    if (other.removed || !other.inlier || other.group === g || other.group.doubtful) continue
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
        fresh.filter { !it.removed && it.inlier && !it.group.doubtful && scheme.faceOf(it.group.color) in settled }.maxByOrNull { it.area }

    /** Quarter turns from [r]'s reading order to its face in the net: its side s is the net's side s + this. */
    private fun netTurn(r: Reading): Int = (r.turn + (rotations[scheme.faceOf(r.group.color)] ?: 0)) % 4

    /**
     * The cube's orientation from [main]'s steps (its right and bottom sides in the net give the
     * cube vectors they show), the tilt's sign from the [others] faces in view (normal, centre) or the
     * last orientation.
     */
    private fun orientationOf(main: Held, others: List<Pair<Vec3, Point>>): Orientation? {
        val candidates = Orientation.candidates(main.face.u, main.face.v, sideVector(main.front, main.turn + 1), sideVector(main.front, main.turn + 2))
        return Orientation.choose(candidates, main.front.normal, main.face.centre, others, lastOrientation)
    }

    /** Every sticker projected into this frame from [main] and the cube's [orientation]. */
    private fun projectionOf(main: Held, orientation: Orientation): CubeProjection? =
        CubeProjection.of(orientation, main.front, main.face.centre, main.face.u, main.face.v, sideVector(main.front, main.turn + 1), sideVector(main.front, main.turn + 2))

    /** [face] as the scan took it in this frame: names in reading order, and which of its places are known in [net]. */
    private fun foundFace(face: FaceReading, r: Reading?, net: List<CubeColor?>): FoundFace {
        if (r == null || r.removed) return FoundFace(face, List(9) { null }, List(9) { false })
        // A doubtful face: read, but which side it is stays open and nothing on it is known.
        if (r.group.doubtful) return FoundFace(face, r.names.mapIndexed { n, c -> if (n == CENTRE) null else c }, List(9) { false })
        val recognised = MutableList(9) { false }
        val known = MutableList<CubeColor?>(9) { null }
        if (r.inlier) {
            val netFace = scheme.faceOf(r.group.color)
            val k = rotations[netFace] ?: 0
            for (j in 0 until 9) {
                val at = RotationSearch.turnIndex(RotationSearch.turnIndex(j, k), r.turn)
                known[at] = net[netFace.ordinal * 9 + j]
                recognised[at] = known[at] != null
            }
        }
        return FoundFace(face, r.names.toList(), recognised, known)
    }

    /**
     * A frame through the rules scanner ([FaceTracks]): known stickers from the best possible cube
     * where it is clear and its faces are settled, else from their own agreeing votes; nothing on a
     * face an open track could be; complete when every counting track is settled and the cube is clear.
     */
    private fun rulesFrame(faces: List<FaceReading>, nowMillis: Long): VideoScanState {
        val ft = tracks
        ft.onFrame(faces, nowMillis)
        val best = ft.best
        val evidence = ft.evidence
        val seen = ft.seenFaces
        val settledFaces = ft.settledFaces
        val unsure = ft.unsureFaces
        val clearness = best?.clearness(evidence) ?: 0.0
        // Once clear, a face newly in view holds it back only by reading against the cube.
        val quiet = best != null && if (completeSince != null) ft.quietFor(best.cube) else ft.settledFor(best.cube) && unsure.isEmpty()
        val complete = best != null && seen.isNotEmpty() && quiet && clearness >= CLEAR_MARGIN && ft.turnsClear()
        val clearAt = BooleanArray(Stickers.COUNT)
        val net = List(Stickers.COUNT) { i ->
            val face = Face.entries[i / 9]
            val shown = face in seen && face !in unsure
            val clear = when {
                best == null -> false
                complete -> true
                i % 9 == CENTRE -> shown
                face in unsure -> false
                // A piece that touches a face an open track could be is not known either.
                BestCube.placeOf(i).any { Face.entries[it / 9] in unsure } -> false
                face in seen && face !in settledFaces -> false
                face !in seen && unsure.isNotEmpty() -> false
                else -> best.supportedMargin(i, evidence) >= CLEAR_MARGIN
            }
            clearAt[i] = clear
            when {
                clear && i % 9 == CENTRE -> scheme[face]
                clear -> best!!.cube[i]
                // Its own votes, unless the best cube is clearly of another mind (a red sticker named orange when the references changed).
                shown -> ownVotes(evidence, i)?.takeIf { c -> best == null || best.margin(i) < CLEAR_MARGIN || c == best.cube[i] }
                else -> null
            }?.takeIf { c ->
                // Until both the red and the orange centre are known, neither colour nor the red and orange faces are
                // known: in a light where the camera's orange looks red the palette names it red (phone test 2026-10-08),
                // and a lone orange face looking red is taken for the red one.
                val held = !complete && !ft.warmCalibrated && (c in FaceTracks.WARM || scheme[face] in FaceTracks.WARM)
                if (held) clearAt[i] = false
                !held
            }
        }
        val confirmed = Face.entries.filter { f -> (0 until 9).all { clearAt[f.ordinal * 9 + it] } }.toSet()
        val leading = List(Stickers.COUNT) { i ->
            val v = evidence.votes[i]
            if (net[i] != null || v.sum() <= 0.0) null else CubeColor.entries[v.indices.maxBy { v[it] }]
        }
        val picture = ft.picture
        val found = faces.mapIndexed { i, face -> rulesFound(face, picture.getOrNull(i), net) }
        val placed = picture.filterNotNull().filter { (t, _) -> ft.settled(t) }.maxByOrNull { it.second.face.area }
        val main = placed?.let { (t, r) ->
            val o = ft.optionOf(t)!!
            Held(r.face, FaceOption.face(o), (r.turn + FaceOption.turn(o)) % 4)
        }
        val others = picture.filterNotNull().filter { it !== placed }.mapNotNull { (t, r) -> ft.faceOf(t)?.let { it.normal to r.face.centre } }
        // The drawing follows the largest face found: its settled face, else (only to move the drawing) the face its centre looks like.
        val anchor = faces.indices.filter { faces[it].colors[CENTRE] != null }.maxByOrNull { faces[it].area }?.let { i ->
            faces[i] to (picture.getOrNull(i)?.let { ft.faceOf(it.first) } ?: scheme.faceOf(ColorClassifier.rankedCentre(faces[i].colors[CENTRE]!!).first()))
        }
        return finish(faces, nowMillis, net, leading, confirmed, emptySet(), found, complete, clearness, main, others, anchor, ft.undecided(nowMillis) && seen.size >= HINT_SEEN)
    }

    /** The colour [sticker]'s own votes make sure ([MIN_VOTES], [MARGIN] over the next), or null. */
    private fun ownVotes(evidence: StickerEvidence, sticker: Int): CubeColor? {
        val v = evidence.votes[sticker]
        val lead = v.indices.maxBy { v[it] }
        val second = v.indices.filter { it != lead }.maxOf { v[it] }
        return CubeColor.entries[lead].takeIf { v[lead] >= MIN_VOTES && v[lead] >= MARGIN * second }
    }

    /** [face] as the rules scanner took it: names in reading order, its places known in [net] once its face and turn are known. */
    private fun rulesFound(face: FaceReading, entry: Pair<Track, TrackReading>?, net: List<CubeColor?>): FoundFace {
        val (track, r) = entry ?: return FoundFace(face, List(9) { null }, List(9) { false })
        val side = tracks.faceOf(track) ?: return FoundFace(face, r.names.mapIndexed { n, c -> if (n == CENTRE) null else c }, List(9) { false })
        val names = r.names.mapIndexed { n, c -> if (n == CENTRE) scheme[side] else c }
        val option = tracks.optionOf(track) ?: return FoundFace(face, names, List(9) { false })
        val recognised = MutableList(9) { false }
        val known = MutableList<CubeColor?>(9) { null }
        for (n in 0 until 9) {
            val at = r.at(RotationSearch.turnIndex(n, FaceOption.turn(option)))
            known[at] = net[side.ordinal * 9 + n]
            recognised[at] = known[at] != null
        }
        return FoundFace(face, names, recognised, known)
    }

    /** [outcome] of the rules scanner. */
    private fun rulesOutcome(): ScanOutcome {
        val known = state.stickers
        val net = known.mapIndexed { i, c -> c ?: if (i % 9 == CENTRE) scheme[Face.entries[i / 9]] else state.leading[i] }
        val editor = CubeEditor(net, scheme)
        val turnedUnsure = if (state.complete) emptySet() else tracks.seenFaces - tracks.settledFaces
        val uncertain = known.indices.filter { (known[it] == null || Face.entries[it / 9] in turnedUnsure) && it % 9 != CENTRE }.toSet()
        val inferred = known.indices.filter { known[it] != null && it % 9 != CENTRE && tracks.evidence.sure(it) != known[it] }.toSet()
        val readings = tracks.assignedReadings()
        val samples = List(Stickers.COUNT) { i ->
            val face = Face.entries[i / 9]
            val list = readings[face].orEmpty().mapNotNull { (r, turn, _) ->
                val at = r.at(RotationSearch.turnIndex(i % 9, turn))
                r.face.colors[at]?.let { rgb -> rgb to r.names[at] }
            }
            val agreeing = list.filter { it.second == net[i] }.ifEmpty { list }.map { it.first }
            if (agreeing.isEmpty()) null else Rgb(FrameSampler.median(agreeing.map { it.r }), FrameSampler.median(agreeing.map { it.g }), FrameSampler.median(agreeing.map { it.b }))
        }
        val cube = editor.toCube()
        return ScanOutcome(
            editor = editor,
            uncertain = uncertain,
            validity = cube?.let { CubeCheck.validity(it, scheme) } ?: Validity.WrongColorCount(editor.counts().filterValues { it != 9 }),
            samples = if (samples.all { it != null }) samples.map { it!! } else emptyList(),
            rotations = tracks.rotations(),
            inferred = inferred,
        )
    }

    companion object {
        private const val CENTRE = 4

        /**
         * A face joins a pile whose centre is at most this far from its own ([ColorClassifier.scaled]
         * Lab): one face's centres spread up to ~22 across a video, white and blue lie 37 or more apart
         * (`scan-centre-naming` findings; results the same from 25 to 35).
         */
        const val JOIN_WITHIN = 30.0

        /** A centre's palette name is clear when the next colour is at least this much further. */
        const val NAME_CLEAR = 4.0

        /** Piles whose centres come this close are one face. */
        const val MERGE_WITHIN = 8.0

        /** A pile is doubtful while naming it otherwise costs less than this much more. */
        const val DOUBT_MARGIN = 5.0

        /**
         * How much of a sticker's distance to a colour is measured without brightness ([Tone]): a
         * colour seen in dimmer light than its centre (olive yellow beside a washed-out yellow centre)
         * reads right; all of it would read red in glare as white (`scan-centre-naming` findings).
         */
        const val BARE_WEIGHT = 0.7

        /** Readings that must agree before a sticker counts (`video-scan-spike`: wrong readings lasted at most 2 frames, 5 for one sticker). */
        const val MIN_VOTES = 3

        /** Faces settled before an open one asks to turn the cube: not while a first face is still followed. */
        const val HINT_SEEN = 4

        /** The leading colour needs this many times the votes of the next. */
        const val MARGIN = 2

        /** A reading belongs with a face's others when this many of its nine stickers agree (wrong lattices: ≤ 6). */
        const val MIN_AGREE = 7

        /** Corner views needed before a face's rotation is taken from them. */
        const val MIN_EVIDENCE = 2

        /** Most readings kept per face. */
        const val MAX_READINGS = 40

        /** A projection is held at most this long after it was built from a settled face. */
        const val HOLD_MILLIS = 1_500L

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

        /** Too dark this long: stall (each stall 5 s later than at first, so the notice does not come too eagerly; user, 2026-10-05). */
        const val DARK_MILLIS = 8_000L

        /** No face found this long: stall. */
        const val NO_CUBE_MILLIS = 13_000L

        /** No more stickers known this long with the cube in view: stall. */
        const val STUCK_MILLIS = 20_000L

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
    }
}

/**
 * A sticker's colour for naming it: as read ([plain]) and without its brightness ([bare],
 * [ColorClassifier.scaled]). Their distances are blended by [VideoScan.BARE_WEIGHT].
 */
internal class Tone(val plain: Lab, val bare: Lab) {
    constructor(rgb: Rgb) : this(rgb.toLab(), ColorClassifier.scaled(rgb).toLab())

    fun distance(o: Tone): Double = plain.distance(o.plain) * (1 - VideoScan.BARE_WEIGHT) + bare.distance(o.bare) * VideoScan.BARE_WEIGHT

    companion object {
        fun mean(list: List<Tone>): Tone = Tone(Lab.mean(list.map { it.plain }), Lab.mean(list.map { it.bare }))
    }
}

internal fun faceWithNormal(n: Vec3): Face = Face.entries.first { it.normal == n }
