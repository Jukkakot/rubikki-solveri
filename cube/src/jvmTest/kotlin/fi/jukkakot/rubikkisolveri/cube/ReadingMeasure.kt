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
    /** One way of treating a video's frames before measuring (raw, light-corrected, …); [treat] gets the frames and the true cube. */
    class Step(val name: String, val treat: (List<VideoFixtures.Frame>, String) -> List<VideoFixtures.Frame>)

    private val videos = listOf(VideoFixtures.ANGLED to VideoFixtures.TRUTH, VideoFixtures.STRAIGHT to VideoFixtures.TRUTH) +
        VideoFixtures.EVENING.keys.map { it to VideoFixtures.EVENING_TRUTH }

    private class Sample(val truth: CubeColor, val lab: Lab, val centre: Boolean)

    /** Every sticker of every full face matched to the true cube (on the raw [frames]), with its true colour and its reading after [step]. */
    private fun samples(frames: List<VideoFixtures.Frame>, truth: String, step: Step): List<Sample> {
        val treated = step.treat(frames, truth)
        val out = ArrayList<Sample>()
        for ((frame, after) in frames.zip(treated)) for ((face, done) in frame.faces.zip(after.faces)) {
            val colors = trueColors(face, truth) ?: continue
            for (n in 0 until 9) out += Sample(colors[n], done.colors[n]!!.toLab(), n == 4)
        }
        return out
    }

    /** One table row per video for [step]. */
    fun rows(step: Step): List<String> = videos.map { (video, truth) ->
        val samples = samples(VideoFixtures.load(video), truth, step)
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
        val RAW = Step("raw") { frames, _ -> frames }

        /** Light correction from the brightest near-grey readings of each frame ([LightCorrection.likelyWhites]). */
        val LIGHT_GREY = Step("light, brightest grey") { frames, _ ->
            val light = LightCorrection()
            frames.map { f ->
                light.update(LightCorrection.likelyWhites(f.faces))
                f.copy(faces = f.faces.map { light.apply(it) })
            }
        }

        /** Light correction from the truly white stickers of each frame: the best the correction can do. */
        val LIGHT_TRUE = Step("light, true white") { frames, truth ->
            val light = LightCorrection()
            frames.map { f ->
                light.update(f.faces.flatMap { face -> trueColors(face, truth)?.let { c -> face.colors.filterIndexed { n, _ -> c[n] == CubeColor.WHITE }.filterNotNull() } ?: emptyList() })
                f.copy(faces = f.faces.map { light.apply(it) })
            }
        }

        val steps = listOf(RAW, LIGHT_GREY, LIGHT_TRUE)

        private val matches = HashMap<Pair<FaceReading, String>, List<CubeColor>?>()

        /**
         * The true colour of each of [face]'s stickers in its reading order: the true face and turn that
         * agree on most stickers (red and orange counted alike), or null below eight of nine (a partial
         * face, a wrong lattice).
         */
        fun trueColors(face: FaceReading, truth: String): List<CubeColor>? = matches.getOrPut(face to truth) {
            if (!face.isFull) return@getOrPut null
            val faces = truth.chunked(9).map { f -> f.map { CubeColor.fromLetter(it) } }
            fun same(a: CubeColor, b: CubeColor) = a == b || (a.isRedOrOrange() && b.isRedOrOrange())
            val names = face.colors.map { ColorClassifier.live(it!!) }
            var best: Triple<Int, Int, Int>? = null
            for (f in faces.indices) for (k in 0 until 4) {
                val turned = RotationSearch.turned(names, k)
                val agree = (0 until 9).count { same(turned[it], faces[f][it]) }
                if (best == null || agree > best.third) best = Triple(f, k, agree)
            }
            val (f, k, agree) = best!!
            if (agree < 8) return@getOrPut null
            val colors = arrayOfNulls<CubeColor>(9)
            for (n in 0 until 9) colors[RotationSearch.turnIndex(n, k)] = faces[f][n]
            colors.map { it!! }
        }

        private fun CubeColor.isRedOrOrange() = this == CubeColor.RED || this == CubeColor.ORANGE
    }
}
