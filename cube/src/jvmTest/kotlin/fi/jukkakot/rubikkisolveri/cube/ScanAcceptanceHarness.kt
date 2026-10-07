package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.BestCube
import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.CornerReader
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.StickerEvidence
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import java.io.File
import kotlin.test.Test

/**
 * `corner-scan-spike`: replays every fixture with a known cube through today's [VideoScan] and through
 * a corner-only assembler ([CornerReader] names and turns the faces, stickers voted, [BestCube] fit),
 * and prints the numbers for `findings.md`. Runs only with CORNER_HARNESS=1; the report also goes to
 * `build/corner-report.txt`.
 */
class CornerScanHarness {
    private val fixtures = listOf(
        VideoFixtures.ANGLED to VideoFixtures.TRUTH,
        VideoFixtures.STRAIGHT to VideoFixtures.TRUTH,
        "20261005_213729" to VideoFixtures.EVENING_TRUTH,
        "20261005_213817" to VideoFixtures.EVENING_TRUTH,
        "20261005_213850" to VideoFixtures.EVENING_TRUTH,
        "20261005_213929" to VideoFixtures.EVENING_TRUTH,
        VideoFixtures.CAMERA_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.TOUR_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.BLUE_FIRST_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.WEB_1007 to VideoFixtures.TRUTH_1007,
        VideoFixtures.STRIPED to VideoFixtures.STRIPED_TRUTH,
    )

    private val out = StringBuilder()

    private fun say(line: String) {
        println(line)
        out.appendLine(line)
    }

    @Test
    fun printNumbers() {
        assumeTrue("set CORNER_HARNESS=1 to run", System.getenv("CORNER_HARNESS") == "1")
        for ((title, harden) in listOf("as recorded" to false, "robustness: red centres towards orange, blue towards white" to true)) {
            say("== $title")
            say("video | frames | corner frames | corners right | corners wrong | faces in corners | opposite corners at | today: finished at / right | with rules: finished at / right | corners only: finished at / right")
            for ((video, truth) in fixtures) {
                val frames = VideoFixtures.load(video).map { f -> if (harden) f.copy(faces = f.faces.map(::hardened)) else f }
                say(row(video, truth, frames))
            }
        }
        File("build/corner-report.txt").writeText(out.toString())
    }

    private fun row(video: String, truth: String, frames: List<VideoFixtures.Frame>): String {
        val cube = Cube.fromColorString(truth)
        var cornerFrames = 0
        var right = 0
        var wrong = 0
        val cornersSeen = HashSet<Corner>()
        val facesCovered = HashSet<Face>()
        var opposite: Int? = null
        val votes = List(Stickers.COUNT) { DoubleArray(6) }
        val centres = HashMap<CubeColor, MutableList<Lab>>()
        var cornerDone: Pair<Int, Boolean>? = null
        var clearSince: Int? = null
        frames.forEachIndexed { i, frame ->
            val r = CornerReader.read(frame.faces) ?: return@forEachIndexed
            cornerFrames++
            // Stickers named by this picture's own centres (yellow reads green by the palette in dim light).
            val own = ColorClassifier.references(r.faces.indices.associate { k -> r.names[k] to listOf(frame.faces[r.faces[k]].colors[4]!!.toLab()) })
            val ok = r.faces.indices.all { k -> agrees(cube, frame.faces[r.faces[k]], r.sides[k], r.turns[k], own) }
            if (ok) right++ else wrong++
            Corner.entries.firstOrNull { it.faces.toSet() == r.sides.toSet() }?.let { c ->
                cornersSeen += c
                if (ok) facesCovered += c.faces
                if (opposite == null && cornersSeen.any { a -> cornersSeen.any { b -> a.faces.none { it in b.faces } } }) opposite = i
            }
            // Corner-only assembly: the centres named by the corner give the references, the stickers vote in the net.
            for (k in r.faces.indices) frame.faces[r.faces[k]].colors[4]?.let { centres.getOrPut(r.names[k]) { ArrayList() } += it.toLab() }
            val refs = ColorClassifier.references(centres)
            for (k in r.faces.indices) {
                val face = frame.faces[r.faces[k]]
                for (n in 0 until 9) {
                    if (n == 4) continue
                    val rgb = face.colors[RotationSearch.turnIndex(n, r.turns[k])] ?: continue
                    val s = ColorClassifier.shares(rgb.toLab(), refs)
                    val v = votes[r.sides[k].ordinal * 9 + n]
                    for (c in 0 until 6) v[c] += s[c]
                }
            }
            if (cornerDone == null) {
                val evidence = StickerEvidence(votes)
                val best = BestCube.solve(evidence)
                if (best != null && best.clearness(evidence) >= VideoScan.CLEAR_MARGIN) {
                    if (clearSince == null) clearSince = i
                    if (i - clearSince!! >= HOLD_FRAMES) cornerDone = i to ((0 until Stickers.COUNT).joinToString("") { best.cube[it].letter.toString() } == truth)
                } else {
                    clearSince = null
                }
            }
        }
        val today = today(frames, truth)
        val ruled = today(frames, truth, VideoScan(rules = true))
        fun done(d: Pair<Int, Boolean>?) = d?.let { "${it.first} / ${if (it.second) "right" else "WRONG"}" } ?: "never"
        return "$video | ${frames.size} | $cornerFrames | $right | $wrong | ${facesCovered.size} | ${opposite ?: "never"} | ${done(today)} | ${done(ruled)} | ${done(cornerDone)}"
    }

    /** Today's scan: the frame it first finishes and whether the cube is the true one. */
    private fun today(frames: List<VideoFixtures.Frame>, truth: String, scan: VideoScan = VideoScan()): Pair<Int, Boolean>? {
        frames.forEachIndexed { i, f ->
            val s = scan.onFrame(f.faces, i * 100L)
            if (s.complete) return i to (s.stickers.joinToString("") { it?.letter?.toString() ?: "?" } == truth)
        }
        return null
    }

    /**
     * Whether [side] turned by [turn] is the true face and turn [face]'s stickers (named by [refs]) fit
     * best; single misread stickers (red read orange) do not make a right corner wrong.
     */
    private fun agrees(cube: Cube, face: FaceReading, side: Face, turn: Int, refs: Map<CubeColor, Lab>): Boolean {
        val names = face.colors.map { it?.let { c -> ColorClassifier.live(c, refs) } }
        fun fit(s: Face, t: Int) = (0 until 9).count { n -> n != 4 && names[RotationSearch.turnIndex(n, t)] == cube[s.ordinal * 9 + n] }
        val best = Face.entries.flatMap { s -> (0 until 4).map { t -> fit(s, t) } }.max()
        return fit(side, turn) == best && fit(side, turn) >= 5
    }

    /** A red centre faded towards orange until the palette names it orange; a blue one towards white until it names it white. */
    private fun hardened(face: FaceReading): FaceReading {
        val centre = face.colors[4] ?: return face
        val p = ColorClassifier.DEFAULT_PALETTE
        val changed = when (ColorClassifier.rankedCentre(centre).first()) {
            CubeColor.RED -> fade(centre, p.getValue(CubeColor.ORANGE), CubeColor.ORANGE)
            CubeColor.BLUE -> fade(centre, Rgb(255, 255, 255), CubeColor.WHITE)
            else -> null
        } ?: return face
        return face.copy(colors = face.colors.toMutableList().also { it[4] = changed })
    }

    private fun fade(from: Rgb, to: Rgb, until: CubeColor): Rgb? = (1..100).map { t ->
        Rgb(from.r + (to.r - from.r) * t / 100, from.g + (to.g - from.g) * t / 100, from.b + (to.b - from.b) * t / 100)
    }.firstOrNull { ColorClassifier.rankedCentre(it).first() == until }

    private companion object {
        /** Clear for this many frames in a row (half a second at 10 fps), as today's scan. */
        const val HOLD_FRAMES = 5
    }
}
