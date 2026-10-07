package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.CubeColor
import kotlin.math.max
import kotlin.math.min

/**
 * One reading of a track (`scan-rules` design 2): [turn] quarter turns relate the track's frame to the
 * reading (reading index = `RotationSearch.turnIndex(track index, turn)`). [names] and [shares] (per
 * reading index) are worked out against the cube's own colours as they become known.
 */
class TrackReading(val face: FaceReading, val turn: Int, val seq: Long) {
    internal val tones: List<Tone?> = face.colors.map { it?.let(::Tone) }
    var names: List<CubeColor?> = List(9) { null }
        internal set
    internal var shares: List<DoubleArray?> = List(9) { null }

    /** The sticker at track index [m] in this reading (null where not found). */
    fun at(m: Int): Int = RotationSearch.turnIndex(m, turn)
}

/**
 * One physical face followed from picture to picture: readings that continue it by position, size,
 * in-plane turn and stickers (`scan-rules` design 2). Its frame is its first reading's; [u] and [v]
 * are one sticker step along its frame's rows and columns as last seen on screen.
 */
class Track(val id: Int) {
    val readings = ArrayList<TrackReading>()
    var lastAt = 0L
        internal set
    var centre = Point(0.0, 0.0)
        internal set
    var u = Point(1.0, 0.0)
        internal set
    var v = Point(0.0, 1.0)
        internal set

    /** Per track index, the colour most of its readings name (null without readings there). */
    var leading: List<CubeColor?> = List(9) { null }
        internal set

    val size: Int get() = readings.size

    /** Works [leading] out again from the readings' names. */
    fun updateLeading() {
        leading = List(9) { m ->
            val counts = IntArray(CubeColor.entries.size)
            for (r in readings) r.names[r.at(m)]?.let { counts[it.ordinal]++ }
            val top = counts.indices.maxBy { counts[it] }
            if (counts[top] == 0) null else CubeColor.entries[top]
        }
    }
}

/**
 * Follows the faces found in each picture as [Track]s. A full reading continues a track seen within
 * [GAP_MILLIS] whose centre is within [STEPS] sticker steps, whose step is of similar length, whose
 * rows point the same way within [MAX_TURN_COS] (which fixes the reading's turn in the track's frame)
 * and with at least [MIN_AGREE] of the eight outer stickers agreeing in that turn; otherwise it
 * starts a track. A partial reading (centre found) only continues a track, with all but one of its
 * stickers agreeing. A second lattice on a face this picture already has is left out.
 */
class Tracker {
    val tracks = ArrayList<Track>()
    private var nextId = 0
    private var seq = 0L

    /**
     * The track each of [faces] went to (null: left out), named by [name] (a sticker's colour against
     * the cube's own colours) for the agreement check.
     */
    fun onFrame(faces: List<FaceReading>, now: Long, name: (Rgb) -> CubeColor): List<Pair<Track, TrackReading>?> {
        val out = arrayOfNulls<Pair<Track, TrackReading>>(faces.size)
        val names = faces.map { f -> f.colors.map { it?.let(name) } }
        val live = tracks.filter { now - it.lastAt <= GAP_MILLIS }
        val taken = HashSet<Track>()
        val kept = keptFaces(faces)
        // Closest pairs first, each track and each face once.
        val options = ArrayList<Triple<Double, Int, Pair<Track, Int>>>()
        for (i in faces.indices) {
            if (i !in kept || faces[i].colors[CENTRE] == null) continue
            for (t in live) continuation(faces[i], names[i], t)?.let { (d, turn) -> options += Triple(d, i, t to turn) }
        }
        options.sortBy { it.first }
        for ((_, i, pick) in options) {
            val (track, turn) = pick
            if (out[i] != null || track in taken) continue
            out[i] = track to add(track, faces[i], names[i], turn, now)
            taken += track
        }
        for (i in faces.indices) if (out[i] == null && i in kept && faces[i].isFull) {
            val track = Track(nextId++)
            tracks += track
            out[i] = track to add(track, faces[i], names[i], 0, now)
        }
        return out.toList()
    }

    /** The faces kept: of two full faces whose centres lie within a step of each other, the larger. */
    private fun keptFaces(faces: List<FaceReading>): Set<Int> {
        val out = faces.indices.toMutableSet()
        for (i in faces.indices) for (j in faces.indices) {
            if (i == j || i !in out || j !in out) continue
            val a = faces[i]
            val b = faces[j]
            if ((a.centre - b.centre).length < step(a) && (b.area > a.area || (b.area == a.area && j < i))) out -= i
        }
        return out
    }

    /** How far [face] is from continuing [track] (in steps) and its turn there, or null when it does not continue it. */
    private fun continuation(face: FaceReading, names: List<CubeColor?>, track: Track): Pair<Double, Int>? {
        val s = step(face)
        val trackStep = (track.u.length + track.v.length) / 2
        if (s <= 0 || trackStep <= 0) return null
        val ratio = s / trackStep
        if (ratio < 1 / SIZE_RATIO || ratio > SIZE_RATIO) return null
        val distance = (face.centre - track.centre).length / max(s, trackStep)
        if (distance > STEPS) return null
        // The turn whose frame rows point most nearly where the track's last did.
        val turn = (0 until 4).maxBy { k -> cos(rowOf(face, k), track.u) }
        if (cos(rowOf(face, turn), track.u) < MAX_TURN_COS) return null
        var present = 0
        var agree = 0
        for (m in 0 until 9) {
            if (m == CENTRE) continue
            val mine = names[RotationSearch.turnIndex(m, turn)] ?: continue
            val theirs = track.leading[m] ?: continue
            present++
            if (mine == theirs) agree++
        }
        val ok = if (face.isFull) agree >= min(MIN_AGREE, present) else present >= 2 && agree >= present - 1
        return if (ok) distance to turn else null
    }

    private fun add(track: Track, face: FaceReading, names: List<CubeColor?>, turn: Int, now: Long): TrackReading {
        val r = TrackReading(face, turn, seq++)
        r.names = names
        track.readings += r
        if (track.readings.size > MAX_READINGS) track.readings.removeAt(0)
        track.lastAt = now
        track.centre = face.centre
        track.u = rowOf(face, turn)
        track.v = columnOf(face, turn)
        track.updateLeading()
        return r
    }

    companion object {
        private const val CENTRE = 4

        /** A track continues only with a reading this soon after its last. */
        const val GAP_MILLIS = 300L

        /** How far a face may move between pictures, in sticker steps. */
        const val STEPS = 1.5

        /** How much a face's step may grow or shrink between pictures. */
        const val SIZE_RATIO = 1.5

        /** Rows may turn at most about 60° between pictures. */
        const val MAX_TURN_COS = 0.5

        /** Outer stickers (of eight) that must agree for a full reading to continue a track. */
        const val MIN_AGREE = 6

        /** Most readings kept per track: the newest. */
        const val MAX_READINGS = 40

        fun step(face: FaceReading): Double = (face.u.length + face.v.length) / 2

        private fun position(face: FaceReading, j: Int): Point = face.centre + face.u * (j % 3 - 1.0) + face.v * (j / 3 - 1.0)

        /** One step along the track frame's rows on screen when [face] is read turned [turn]. */
        fun rowOf(face: FaceReading, turn: Int): Point =
            (position(face, RotationSearch.turnIndex(5, turn)) - position(face, RotationSearch.turnIndex(3, turn))) * 0.5

        /** One step down the track frame's columns on screen when [face] is read turned [turn]. */
        fun columnOf(face: FaceReading, turn: Int): Point =
            (position(face, RotationSearch.turnIndex(7, turn)) - position(face, RotationSearch.turnIndex(1, turn))) * 0.5

        private fun cos(a: Point, b: Point): Double {
            val l = a.length * b.length
            return if (l == 0.0) -1.0 else (a.x * b.x + a.y * b.y) / l
        }
    }
}
