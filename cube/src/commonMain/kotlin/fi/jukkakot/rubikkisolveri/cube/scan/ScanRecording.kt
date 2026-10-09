package fi.jukkakot.rubikkisolveri.cube.scan

/**
 * A video scan's input as text (`scan-recording`): what [VideoScan.onFrame] was given, picture by
 * picture, so a failed scan replays exactly on a computer.
 *
 * ```
 * # scan-recording 1 platform=<android|web> ver=<app version> started=<ISO time> t0=<scan clock ms> [cut=<ms>]
 * <ms> <FaceCodec text>      one per picture read; ms from the scan's first picture
 * <ms> reset                 the scan started over here (a fresh VideoScan)
 * # end finished <54 letters> | # end left | # end restart
 * ```
 *
 * `t0` is the scan clock at the first picture (the scan's time is `t0 + ms`), `cut` the time of the
 * first line kept when the start was dropped ([KEEP_MILLIS]).
 */
object ScanRecording {
    const val FORMAT = 1
    const val HEADER = "# scan-recording"
    const val KEEP_MILLIS = 90_000L

    sealed interface Entry {
        val ms: Long
    }

    /** One picture read: its faces as the scan got them. */
    class Picture(override val ms: Long, val found: FaceCodec.Found) : Entry

    /** The scan started over before the next picture. */
    class Reset(override val ms: Long) : Entry

    sealed interface End {
        data class Finished(val cube: String) : End
        data object Left : End
        data object Restart : End
    }

    class Recording(
        val platform: String,
        val version: String,
        val started: String,
        val t0: Long,
        val cut: Long?,
        val entries: List<Entry>,
        val end: End?,
    )

    fun endLine(end: End): String = when (end) {
        is End.Finished -> "# end finished ${end.cube}"
        End.Left -> "# end left"
        End.Restart -> "# end restart"
    }

    /** Reads a recording; a line that cannot be read fails with its line number. */
    fun read(text: String): Recording {
        val lines = text.lines()
        val head = lines.firstOrNull().orEmpty()
        require(head.startsWith("$HEADER ")) { "line 1: not a scan recording" }
        val fields = head.removePrefix("$HEADER ").split(' ').filter { it.isNotEmpty() }
        require(fields.firstOrNull()?.toIntOrNull() == FORMAT) { "line 1: format ${fields.firstOrNull()}" }
        val values = fields.drop(1).associate { it.substringBefore('=') to it.substringAfter('=', "") }
        val entries = ArrayList<Entry>()
        var end: End? = null
        for ((i, raw) in lines.withIndex().drop(1)) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val at = i + 1
            if (line.startsWith("#")) {
                if (line.startsWith("# end ")) {
                    val rest = line.removePrefix("# end ").trim()
                    end = when {
                        rest == "left" -> End.Left
                        rest == "restart" -> End.Restart
                        rest.startsWith("finished ") -> End.Finished(rest.removePrefix("finished ").trim())
                        else -> throw IllegalArgumentException("line $at: end '$rest'")
                    }
                }
                continue
            }
            val ms = line.substringBefore(' ').toLongOrNull() ?: throw IllegalArgumentException("line $at: time '${line.substringBefore(' ')}'")
            val body = line.substringAfter(' ', "")
            entries += if (body == "reset") {
                Reset(ms)
            } else {
                try {
                    Picture(ms, FaceCodec.decode(body))
                } catch (e: Exception) {
                    throw IllegalArgumentException("line $at: faces: ${e.message}", e)
                }
            }
        }
        return Recording(
            platform = values["platform"].orEmpty(),
            version = values["ver"].orEmpty(),
            started = values["started"].orEmpty(),
            t0 = values["t0"]?.toLongOrNull() ?: 0L,
            cut = values["cut"]?.toLongOrNull(),
            entries = entries,
            end = end,
        )
    }
}

/**
 * Records one video scan into a [ScanRecording], keeping the last [keepMillis] in memory: nothing is
 * written until [text] is asked for at the scan's end (`scan-recording` design 3). [platform],
 * [version] and [started] (an ISO time) go into the header.
 */
class ScanRecorder(
    private val platform: String,
    version: String,
    val started: String,
    private val keepMillis: Long = ScanRecording.KEEP_MILLIS,
) {
    private val version = version.substringBefore(' ').ifEmpty { "?" }
    private val lines = ArrayDeque<Pair<Long, String>>()
    private var t0: Long? = null
    private var cut = false
    private var picturesSinceReset = 0

    /** How many pictures are kept. */
    var pictures = 0
        private set

    val isEmpty: Boolean get() = pictures == 0

    /** Whether a picture was recorded since the start or the last [reset]. */
    val hasPicturesSinceReset: Boolean get() = picturesSinceReset > 0

    /**
     * Records the faces of a picture given to the scan at [at] (the scan's clock) and answers them as
     * read back from the recording, so the scan gets exactly what a replay will.
     */
    fun picture(at: Long, faces: List<FaceReading>, width: Int, height: Int, finderMs: Long = 0): List<FaceReading> {
        val text = FaceCodec.encode(FaceCodec.Found(faces, width, height, finderMs))
        add(at, text)
        pictures++
        picturesSinceReset++
        return FaceCodec.decode(text).faces
    }

    /** The scan started over at [at] (a fresh scan from the next picture). */
    fun reset(at: Long) {
        if (t0 == null) return
        add(at, "reset")
        picturesSinceReset = 0
    }

    private fun add(at: Long, body: String) {
        val start = t0 ?: at.also { t0 = it }
        val ms = at - start
        lines.addLast(ms to body)
        while (lines.size > 1 && ms - lines.first().first > keepMillis) {
            if (lines.removeFirst().second != "reset") pictures--
            cut = true
        }
    }

    /** The recording so far, ending with [end]. */
    fun text(end: ScanRecording.End): String = buildString {
        append("${ScanRecording.HEADER} ${ScanRecording.FORMAT} platform=$platform ver=$version started=$started t0=${t0 ?: 0}")
        if (cut) append(" cut=${lines.firstOrNull()?.first ?: 0}")
        append('\n')
        for ((ms, body) in lines) append(ms).append(' ').append(body).append('\n')
        append(ScanRecording.endLine(end)).append('\n')
    }
}
