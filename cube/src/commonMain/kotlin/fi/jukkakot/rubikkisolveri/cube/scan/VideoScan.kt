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
    /**
     * Each sticker's colour as read steadily (`scan-feedback`), in reading order: the leading colour of
     * the face's followed readings once they count, before the scan knows which face it is; null where
     * not read yet (all of it before the face counts).
     */
    val read: List<CubeColor?>? = null,
    /**
     * Followed from an earlier picture (its track had a reading before this one; `scan-rules-only`): only
     * such a face gets marks, so a lattice found in one blurred picture shows nothing.
     */
    val followed: Boolean = false,
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
    /** The centre colours of every face read steadily in this scan, kept after it leaves the picture (`scan-feedback`). */
    val readSides: Set<CubeColor> = emptySet(),
    /** Sides whose turn is still open: seen but their turn not settled, or a face an open track could be (`scan-next-view`). */
    val openTurns: Set<Face> = emptySet(),
    /** The sides done ([ScanSides.done]): the side row's ticked balls; all six only once [complete]. */
    val doneSides: Set<Face> = emptySet(),
    /** The side worth showing next ([NextSide.choose]); null once every side is done. */
    val nextSide: Face? = null,
    /** The stickers that are part of the clear cube (not known from their own votes alone): what a done side needs. */
    val clear: Set<Int> = emptySet(),
) {
    val recognised: Int get() = stickers.count { it != null }

    companion object {
        val EMPTY = VideoScanState(List(Stickers.COUNT) { null }, emptySet(), emptyList(), null, 0, false, false)
    }
}

/** Why the video scan cannot get on: too dark, no cube in view, or nothing new known for a while. */
enum class Stall { DARK, NO_CUBE, STUCK }

/**
 * The scan from continuous video (`video-scan`): faces followed from picture to picture and known by
 * the rules of a real cube ([FaceTracks]). Their votes, turned into the net, are the evidence for
 * [BestCube]: the possible cube that fits them best, known sticker by sticker where it clearly beats
 * every other possible cube ([CLEAR_MARGIN]), so a single wrong frame changes nothing and a misread
 * sticker that fits no real piece is corrected by the rest. Pure Kotlin, one code base for the phone
 * and the browser.
 */
class VideoScan(private val scheme: ColorScheme = ColorScheme.STANDARD) {
    private var lastPose: Pose? = null
    private var lastOrientation: Orientation? = null
    private var lastOrientationAt = 0L

    /** The last orientation's tilt was sure ([Choice]): a following one chosen by it is sure too. */
    private var lastSure = false
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
    private val readSides = HashSet<CubeColor>()

    var state: VideoScanState = VideoScanState.EMPTY
        private set

    /** The faces followed from picture to picture. */
    private var tracks = FaceTracks(scheme)

    /** The face centres as read ([FaceTracks.centreLog]), for the scan log. */
    val centreLog: String get() = tracks.centreLog

    /**
     * Handles the faces found in one frame taken at [nowMillis] (a partial face, stickers missing,
     * only continues a face already followed): known stickers from the best possible cube where it is
     * clear and its faces are settled, else from their own agreeing votes; nothing on a face an open
     * track could be; complete when every counting track is settled and the cube is clear.
     */
    fun onFrame(faces: List<FaceReading>, nowMillis: Long): VideoScanState {
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
        for ((t, _) in picture.filterNotNull()) if (t.size >= FaceTracks.MIN_READINGS) t.leading[CENTRE]?.let { readSides += it }
        val found = faces.mapIndexed { i, face -> foundFace(face, picture.getOrNull(i), net) }
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
        val openTurns = if (complete) emptySet() else seen - settledFaces + unsure
        val clear = clearAt.indices.filter { clearAt[it] }.toSet()
        return finish(faces, nowMillis, net, leading, confirmed, emptySet(), found, complete, clearness, main, others, anchor, ft.undecided(nowMillis) && seen.size >= HINT_SEEN, openTurns, clear)
    }

    /** The colour [sticker]'s own votes make sure ([MIN_VOTES], [MARGIN] over the next), or null. */
    private fun ownVotes(evidence: StickerEvidence, sticker: Int): CubeColor? {
        val v = evidence.votes[sticker]
        val lead = v.indices.maxBy { v[it] }
        val second = v.indices.filter { it != lead }.maxOf { v[it] }
        return CubeColor.entries[lead].takeIf { v[lead] >= MIN_VOTES && v[lead] >= MARGIN * second }
    }

    /** [face] as the scan took it: names in reading order, its places known in [net] once its face and turn are known. */
    private fun foundFace(face: FaceReading, entry: Pair<Track, TrackReading>?, net: List<CubeColor?>): FoundFace {
        val (track, r) = entry ?: return FoundFace(face, List(9) { null }, List(9) { false })
        val followed = track.size >= 2
        // Read once its track counts: the track's leading colours in this reading's order.
        val read = if (track.size < FaceTracks.MIN_READINGS) null else MutableList<CubeColor?>(9) { null }.also { l -> for (m in 0 until 9) l[r.at(m)] = track.leading[m] }
        val side = tracks.faceOf(track) ?: return FoundFace(face, r.names.mapIndexed { n, c -> if (n == CENTRE) null else c }, List(9) { false }, read = read, followed = followed)
        val names = r.names.mapIndexed { n, c -> if (n == CENTRE) scheme[side] else c }
        val option = tracks.optionOf(track) ?: return FoundFace(face, names, List(9) { false }, read = read, followed = followed)
        val recognised = MutableList(9) { false }
        val known = MutableList<CubeColor?>(9) { null }
        for (n in 0 until 9) {
            val at = r.at(RotationSearch.turnIndex(n, FaceOption.turn(option)))
            known[at] = net[side.ordinal * 9 + n]
            recognised[at] = known[at] != null
        }
        return FoundFace(face, names, recognised, known, read, followed)
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
        return outcomeOf(net, uncertain, if (samples.all { it != null }) samples.map { it!! } else emptyList(), tracks.rotations(), inferred, scheme)
    }

    /**
     * The cube's orientation from [main]'s steps (its right and bottom sides in the net give the
     * cube vectors they show), the tilt's sign from the [others] faces in view (normal, centre) or the
     * last orientation.
     */
    private fun orientationOf(main: Held, others: List<Pair<Vec3, Point>>): Pair<Orientation, Choice>? {
        val candidates = Orientation.candidates(main.face.u, main.face.v, sideVector(main.front, main.turn + 1), sideVector(main.front, main.turn + 2))
        return Orientation.chosen(candidates, main.front.normal, main.face.centre, others, lastOrientation)
    }

    /** Every sticker projected into this frame from [main] and the cube's [orientation]. */
    private fun projectionOf(main: Held, orientation: Orientation): CubeProjection? =
        CubeProjection.of(orientation, main.front, main.face.centre, main.face.u, main.face.v, sideVector(main.front, main.turn + 1), sideVector(main.front, main.turn + 2))

    /** The face a frame's pose and projection come from: its reading, which face it is and its turn into the net. */
    private class Held(val face: FaceReading, val front: Face, val turn: Int)

    /**
     * The state after a frame: the pose, orientation and projection from [main] (held
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
        openTurns: Set<Face> = emptySet(),
        clear: Set<Int> = emptySet(),
    ): VideoScanState {
        if (!complete) completeSince = null else if (completeSince == null) completeSince = nowMillis
        if (main != null) lastPose = Pose(main.front, neighbourAt(main.front, main.turn))
        val chosen = main?.let { orientationOf(it, others) }
        // Only a sure tilt draws the cube's other sides (`scan-paint-steady`): a mirror guess put them on the table.
        val sure = chosen != null && when (chosen.second) {
            Choice.ONLY, Choice.CUE -> true
            Choice.PREVIOUS -> lastSure && nowMillis - lastOrientationAt <= HOLD_MILLIS
            Choice.GUESS -> false
        }
        if (chosen != null) {
            lastOrientation = chosen.first
            lastOrientationAt = nowMillis
            lastSure = sure
        }
        val orientation = chosen?.first?.takeIf { sure }
        val built = if (main != null && orientation != null) projectionOf(main, orientation) else null
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
            readSides = readSides.toSet(),
            openTurns = openTurns,
            clear = clear,
        ).let { s -> s.copy(doneSides = ScanSides.done(s, state.nextSide), nextSide = NextSide.choose(s, state.nextSide, scheme)) }
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
        lastPose = null
        lastOrientation = null
        lastSure = false
        held = null
        lastRecognised = 0
        completeSince = null
        startedAt = null
        lastFaceAt = null
        dimSince = null
        progressAt = 0L
        mostKnown = 0
        readSides.clear()
        state = VideoScanState.EMPTY
    }

    companion object {
        private const val CENTRE = 4

        /** The outcome of a scan whose net is [net] (also rebuilt from text in the browser, [ScanStateCodec]). */
        fun outcomeOf(
            net: List<CubeColor?>,
            uncertain: Set<Int>,
            samples: List<Rgb>,
            rotations: Map<Face, Int>,
            inferred: Set<Int>,
            scheme: ColorScheme = ColorScheme.STANDARD,
        ): ScanOutcome {
            val editor = CubeEditor(net, scheme)
            val cube = editor.toCube()
            return ScanOutcome(
                editor = editor,
                uncertain = uncertain,
                validity = cube?.let { CubeCheck.validity(it, scheme) } ?: Validity.WrongColorCount(editor.counts().filterValues { it != 9 }),
                samples = samples,
                rotations = rotations,
                inferred = inferred,
            )
        }

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

        /** A projection is held at most this long after it was built from a settled face. */
        const val HOLD_MILLIS = 1_500L

        /** How long the whole cube must stay recognised before the scan finishes. */
        const val FINISH_MILLIS = 500L

        /** The best cube is clear when every place's margin reaches this ([BestCube.clearness]; chosen by simulation with soft votes, `video-scan-light` findings). */
        const val CLEAR_MARGIN = 2.0

        /**
         * How far the best cube's margins are worked out: every decision only compares them with [CLEAR_MARGIN]
         * (`scan-speed-up-4`; half a point more keeps a margin of exactly that clear of rounding). The log's
         * clearness shows at most this.
         */
        const val MARGIN_LOOK = CLEAR_MARGIN + 0.5

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
}

internal fun faceWithNormal(n: Vec3): Face = Face.entries.first { it.normal == n }