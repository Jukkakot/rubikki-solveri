package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Face
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The video scan's state and outcome as text, for the browser where the scan runs in the worker and
 * the page only draws (`scan-speed-up-2` design 5). Only what the screen and the log use: the
 * projection is left out (the paint no longer uses it).
 */
object ScanStateCodec {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private class Found(
        val colors: List<String?>,
        val at: List<Double>,
        val names: String,
        val recognised: String,
        val known: String,
        val read: String? = null,
    )

    @Serializable
    private class State(
        val stickers: String,
        val leading: String,
        val found: List<Found>,
        val pose: String? = null,
        val newStickers: Int = 0,
        val complete: Boolean = false,
        val finished: Boolean = false,
        val orientation: List<Double>? = null,
        val clearness: Double = 0.0,
        val brightness: Int? = null,
        val dim: Boolean = false,
        val stall: String? = null,
        val confirmed: String = "",
        val undecided: Boolean = false,
        val readSides: String = "",
        val contradictions: List<Int> = emptyList(),
    )

    @Serializable
    private class Outcome(
        val net: String,
        val uncertain: List<Int>,
        val inferred: List<Int>,
        val samples: List<String>,
        val rotations: String,
    )

    private fun colors(list: List<CubeColor?>): String = list.joinToString("") { it?.letter?.toString() ?: "." }
    private fun colorsOf(text: String): List<CubeColor?> = text.map { if (it == '.') null else CubeColor.fromLetter(it) }

    fun encode(state: VideoScanState): String = json.encodeToString(
        State.serializer(),
        State(
            stickers = colors(state.stickers),
            leading = colors(state.leading),
            found = state.found.map { f ->
                val r = f.reading
                Found(
                    r.colors.map { it?.toHex() },
                    listOf(r.centre.x, r.centre.y, r.u.x, r.u.y, r.v.x, r.v.y),
                    colors(f.names),
                    f.recognised.joinToString("") { if (it) "1" else "0" },
                    colors(f.known),
                    f.read?.let(::colors),
                )
            },
            pose = state.pose?.let { "${it.front.name}${it.up.name}" },
            newStickers = state.newStickers,
            complete = state.complete,
            finished = state.finished,
            orientation = state.orientation?.m,
            clearness = state.clearness,
            brightness = state.brightness,
            dim = state.dim,
            stall = state.stall?.name,
            confirmed = state.confirmed.joinToString("") { it.name },
            undecided = state.undecided,
            readSides = colors(state.readSides.toList()),
            contradictions = state.contradictions.toList(),
        ),
    )

    fun decode(text: String): VideoScanState {
        val s = json.decodeFromString(State.serializer(), text)
        return VideoScanState(
            stickers = colorsOf(s.stickers),
            contradictions = s.contradictions.toSet(),
            found = s.found.map { f ->
                FoundFace(
                    FaceReading(f.colors.map { it?.let(Rgb::fromHex) }, Point(f.at[0], f.at[1]), Point(f.at[2], f.at[3]), Point(f.at[4], f.at[5])),
                    colorsOf(f.names),
                    f.recognised.map { it == '1' },
                    colorsOf(f.known),
                    f.read?.let(::colorsOf),
                )
            },
            pose = s.pose?.let { Pose(Face.valueOf(it.substring(0, 1)), Face.valueOf(it.substring(1, 2))) },
            newStickers = s.newStickers,
            complete = s.complete,
            finished = s.finished,
            leading = colorsOf(s.leading),
            orientation = s.orientation?.let(::Orientation),
            clearness = s.clearness,
            brightness = s.brightness,
            dim = s.dim,
            stall = s.stall?.let(Stall::valueOf),
            confirmed = s.confirmed.map { Face.valueOf(it.toString()) }.toSet(),
            undecided = s.undecided,
            readSides = colorsOf(s.readSides).filterNotNull().toSet(),
        )
    }

    fun encodeOutcome(outcome: ScanOutcome): String = json.encodeToString(
        Outcome.serializer(),
        Outcome(
            net = colors(outcome.editor.colors),
            uncertain = outcome.uncertain.toList(),
            inferred = outcome.inferred.toList(),
            samples = outcome.samples.map { it.toHex() },
            rotations = Face.entries.joinToString("") { "${it.name}${outcome.rotations[it] ?: 0}" },
        ),
    )

    fun decodeOutcome(text: String): ScanOutcome {
        val o = json.decodeFromString(Outcome.serializer(), text)
        val rotations = o.rotations.chunked(2).associate { Face.valueOf(it.substring(0, 1)) to it.substring(1).toInt() }
        return VideoScan.outcomeOf(colorsOf(o.net), o.uncertain.toSet(), o.samples.map(Rgb::fromHex), rotations, o.inferred.toSet())
    }
}
