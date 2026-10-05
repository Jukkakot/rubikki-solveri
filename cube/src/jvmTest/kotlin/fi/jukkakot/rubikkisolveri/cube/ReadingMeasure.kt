package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.Lab
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import org.junit.Assume.assumeTrue
import kotlin.math.sqrt
import kotlin.test.Test

/**
 * `video-scan-light` design 1: how well the readings of the test videos tell red from orange. Every
 * full face of a fixture is matched to the true cube (the face and turn that agree on most stickers,
 * red and orange counted alike, at least eight of nine); the centres of each true colour give its
 * reference (mean Lab, as the scan's). Per video: the mean distance of the true red and orange
 * readings to both references, how many lie nearer the wrong one, and the spread of all readings
 * around their own colour's reference. Runs only with MEASURE=1; prints the table for `findings.md`.
 */
class ReadingMeasure {
    /** One way of treating a video's frames before measuring (raw, light-corrected, …). */
    class Step(val name: String, val treat: (List<VideoFixtures.Frame>) -> List<VideoFixtures.Frame>)

    private val videos = listOf(VideoFixtures.ANGLED to VideoFixtures.TRUTH, VideoFixtures.STRAIGHT to VideoFixtures.TRUTH) +
        VideoFixtures.EVENING.keys.map { it to VideoFixtures.EVENING_TRUTH }

    private class Sample(val truth: CubeColor, val lab: Lab, val centre: Boolean)

    /** Every sticker of every full face matched to the true cube, with its true colour. */
    private fun samples(frames: List<VideoFixtures.Frame>, truth: String): List<Sample> {
        val faces = truth.chunked(9).map { f -> f.map { CubeColor.fromLetter(it) } }
        fun same(a: CubeColor, b: CubeColor) = a == b || (a.redOrOrange && b.redOrOrange)
        val out = ArrayList<Sample>()
        for (frame in frames) for (face in frame.faces) {
            if (!face.isFull) continue
            val names = face.colors.map { ColorClassifier.live(it!!) }
            var best: Triple<Int, Int, Int>? = null
            for (f in faces.indices) for (k in 0 until 4) {
                val turned = RotationSearch.turned(names, k)
                val agree = (0 until 9).count { same(turned[it], faces[f][it]) }
                if (best == null || agree > best.third) best = Triple(f, k, agree)
            }
            val (f, k, agree) = best!!
            if (agree < 8) continue
            val rgb = RotationSearch.turned(face.colors, k)
            for (n in 0 until 9) out += Sample(faces[f][n], rgb[n]!!.toLab(), n == 4)
        }
        return out
    }

    private val CubeColor.redOrOrange get() = this == CubeColor.RED || this == CubeColor.ORANGE

    /** One table row per video for [step]. */
    fun rows(step: Step): List<String> = videos.map { (video, truth) ->
        val samples = samples(step.treat(VideoFixtures.load(video)), truth)
        val refs = CubeColor.entries.associateWith { c -> samples.filter { it.centre && it.truth == c }.map { it.lab }.takeIf { it.isNotEmpty() }?.let(Lab::mean) }
        val red = refs[CubeColor.RED]
        val orange = refs[CubeColor.ORANGE]
        fun line(color: CubeColor): String {
            val list = samples.filter { !it.centre && it.truth == color }
            if (red == null || orange == null || list.isEmpty()) return "-"
            val toRed = list.map { it.lab.distance(red) }
            val toOrange = list.map { it.lab.distance(orange) }
            val wrong = list.indices.count { if (color == CubeColor.RED) toOrange[it] < toRed[it] else toRed[it] < toOrange[it] }
            return "%.1f / %.1f, %d %% wrong".format(toRed.average(), toOrange.average(), wrong * 100 / list.size)
        }
        val spread = samples.filter { !it.centre }.mapNotNull { s -> refs[s.truth]?.distance(s.lab) }
        val rms = sqrt(spread.sumOf { it * it } / spread.size)
        val apart = if (red != null && orange != null) "%.1f".format(red.distance(orange)) else "-"
        "${video.takeLast(6)} | ${step.name} | $apart | ${line(CubeColor.RED)} | ${line(CubeColor.ORANGE)} | ${"%.1f".format(rms)}"
    }

    @Test
    fun printRedOrange() {
        assumeTrue("set MEASURE=1 to run", System.getenv("MEASURE") == "1")
        println("video | step | red–orange refs apart | true red: to red / to orange, nearer orange | true orange: to red / to orange, nearer red | spread (rms to own ref)")
        for (step in steps) rows(step).forEach(::println)
    }

    companion object {
        val RAW = Step("raw") { it }

        val steps = listOf(RAW)
    }
}
