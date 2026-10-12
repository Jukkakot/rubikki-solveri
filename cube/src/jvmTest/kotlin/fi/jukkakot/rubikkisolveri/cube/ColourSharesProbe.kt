package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceOption
import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.ScanRecording
import fi.jukkakot.rubikkisolveri.cube.scan.Track
import fi.jukkakot.rubikkisolveri.cube.scan.Tracker
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import org.junit.Assume.assumeTrue
import kotlin.test.Test

/**
 * `scan-colour-shares-spike`: per picture and counting track sticker, the leading colour (a plurality of the
 * readings' hard names, `Track.updateLeading`) against the leader of the colour shares (the track's votes) and the
 * true cube. Runs only with COLOUR_SHARES=1; prints one row per source for the findings.
 */
class ColourSharesProbe {
    private val tracksField = VideoScan::class.java.getDeclaredField("tracks").apply { isAccessible = true }
    private val trackerField = FaceTracks::class.java.getDeclaredField("tracker").apply { isAccessible = true }
    private val statesField = FaceTracks::class.java.getDeclaredField("states").apply { isAccessible = true }

    private class Stat {
        var pictures = 0
        var n = 0
        var diff = 0
        var diffWarm = 0
        var judged = 0
        var leadRight = 0
        var shareRight = 0
        var close = 0
        var closeJudged = 0
        var closeLeadWrong = 0
        var flipsLead = 0
        var flipsShare = 0
    }

    /** The true face and turn whose stickers [colours] (track frame) match on the most outer stickers. */
    private fun fit(truth: String, colours: List<CubeColor?>): Pair<Int, Int> = (0 until FaceOption.FACES).map { o ->
        val face = FaceOption.face(o)
        o to (0 until 9).count { n -> n != CENTRE && colours[RotationSearch.turnIndex(n, FaceOption.turn(o))]?.letter == truth[face.ordinal * 9 + n] }
    }.maxBy { it.second }

    /** [pictures]: time and faces, null faces for a reset. */
    private fun probe(truth: String, pictures: List<Pair<Long, List<FaceReading>?>>): Stat {
        val st = Stat()
        var scan = VideoScan()
        val lastLead = HashMap<Pair<Int, Int>, CubeColor?>()
        val lastShare = HashMap<Pair<Int, Int>, Int>()
        for ((ms, faces) in pictures) {
            if (faces == null) {
                scan = VideoScan()
                lastLead.clear()
                lastShare.clear()
                continue
            }
            scan.onFrame(faces, ms)
            st.pictures++
            val ft = tracksField.get(scan) as FaceTracks
            val tracker = trackerField.get(ft) as Tracker

            @Suppress("UNCHECKED_CAST")
            val states = statesField.get(ft) as Map<Track, Any>
            for (t in tracker.tracks) {
                if (t.size < FaceTracks.MIN_READINGS) continue
                val s = states[t] ?: continue

                @Suppress("UNCHECKED_CAST")
                val votes = s.javaClass.getDeclaredField("votes").apply { isAccessible = true }.get(s) as List<DoubleArray>
                val share = List(9) { m -> votes[m].let { v -> if (v.sum() <= 0) null else v.indices.maxBy { v[it] } } }
                // The track's true face and turn, where its leading colours and its shares agree on it and fit well.
                val byLead = fit(truth, t.leading)
                val byShare = fit(truth, share.map { it?.let { c -> CubeColor.entries[c] } })
                val trueOption = if (byLead.first == byShare.first && byLead.second >= 6) byLead.first else null
                for (m in 0 until 9) {
                    if (m == CENTRE) continue
                    val sc = share[m] ?: continue
                    val lc = t.leading[m]
                    st.n++
                    val sorted = votes[m].sortedDescending()
                    val close = sorted[1] > 0.6 * sorted[0]
                    if (close) st.close++
                    val key = t.id to m
                    if (key in lastLead && lastLead[key] != lc) st.flipsLead++
                    if (key in lastShare && lastShare[key] != sc) st.flipsShare++
                    lastLead[key] = lc
                    lastShare[key] = sc
                    val different = lc?.ordinal != sc
                    if (different) {
                        st.diff++
                        if (lc in WARM && CubeColor.entries[sc] in WARM) st.diffWarm++
                    }
                    if (trueOption == null) continue
                    val n = (0 until 9).first { RotationSearch.turnIndex(it, FaceOption.turn(trueOption)) == m }
                    val tc = truth[FaceOption.face(trueOption).ordinal * 9 + n]
                    if (close) {
                        st.closeJudged++
                        if (lc?.letter != tc) st.closeLeadWrong++
                    }
                    if (different) {
                        st.judged++
                        if (lc?.letter == tc) st.leadRight++
                        if (CubeColor.entries[sc].letter == tc) st.shareRight++
                    }
                }
            }
        }
        return st
    }

    @Test
    fun printTable() {
        assumeTrue("set COLOUR_SHARES=1 to run", System.getenv("COLOUR_SHARES") == "1")
        val sources = ArrayList<Triple<String, String, List<Pair<Long, List<FaceReading>?>>>>()
        val recordings = FinishDelayHarness.RECORDINGS + listOf(
            "web_20261011_100403" to ColourSpikeHarness.TRUTH_1011,
            "web_20261011_101618" to ColourSpikeHarness.TRUTH_1011,
        )
        for ((name, truth) in recordings) {
            val r = VideoFixtures.loadRecording(name)
            sources += Triple(
                "rec $name", truth,
                r.entries.map { e ->
                    when (e) {
                        is ScanRecording.Reset -> 0L to null
                        is ScanRecording.Picture -> (r.t0 + e.ms) to e.found.faces
                    }
                },
            )
        }
        val videos = listOf(
            VideoFixtures.ANGLED to VideoFixtures.TRUTH,
            VideoFixtures.STRAIGHT to VideoFixtures.TRUTH,
            VideoFixtures.STRIPED to VideoFixtures.STRIPED_TRUTH,
            "web_121505" to VideoFixtures.TRUTH_1008C,
        ) + VideoFixtures.EVENING.keys.map { it to VideoFixtures.EVENING_TRUTH }
        for ((v, truth) in videos) sources += Triple(v, truth, VideoFixtures.load(v).mapIndexed { i, f -> i * 100L to f.faces })
        fun pct(a: Int, b: Int) = if (b == 0) "-" else "%.1f".format(100.0 * a / b)
        println("source | pictures | sticker-pictures | lead≠share % | of them R/O % | judged | lead right | share right | close % | lead wrong when close % | lead flips /100 pictures | share flips /100 pictures")
        for ((name, truth, pictures) in sources) {
            val s = probe(truth, pictures)
            println(
                "$name | ${s.pictures} | ${s.n} | ${pct(s.diff, s.n)} | ${pct(s.diffWarm, s.diff)} | ${s.judged} | ${s.leadRight} | ${s.shareRight} | " +
                    "${pct(s.close, s.n)} | ${pct(s.closeLeadWrong, s.closeJudged)} | ${"%.1f".format(100.0 * s.flipsLead / s.pictures)} | ${"%.1f".format(100.0 * s.flipsShare / s.pictures)}",
            )
        }
    }

    private companion object {
        const val CENTRE = 4
        val WARM = setOf(CubeColor.RED, CubeColor.ORANGE)
    }
}
