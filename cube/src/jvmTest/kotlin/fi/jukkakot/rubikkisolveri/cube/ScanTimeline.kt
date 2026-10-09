package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceOption
import fi.jukkakot.rubikkisolveri.cube.scan.FaceTracks
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScan
import fi.jukkakot.rubikkisolveri.cube.scan.VideoScanState

/** The timeline lines [RulesTimeline] and [RecordingReplay] print: the state, then every track. */
object ScanTimeline {
    private val tracks = VideoScan::class.java.getDeclaredField("tracks").apply { isAccessible = true }

    /** [label] (frame or time), the [faces] found, [s] and the tracks of [scan]; [truth] (URFDLB) marks a track whose best true fit differs (`!`). */
    fun print(label: String, scan: VideoScan, faces: Int, s: VideoScanState, truth: String?) {
        val ft = tracks.get(scan) as FaceTracks
        println("$label faces=$faces known=${s.recognised} clear=%.2f complete=${s.complete} undecided=${s.undecided}".format(s.clearness))
        for (t in ft.snapshot()) {
            val o = t.option ?: t.assigned
            val mine = if (o == FaceOption.NONE) "none" else "${FaceOption.face(o)}${FaceOption.turn(o)}"
            val state = if (t.option != null) "=" else if (t.face != null) "${t.face}?" else "?"
            val read = t.leading.joinToString("") { it?.letter?.toString() ?: "." }
            val fit = truth?.let { best(it, t.leading) }
            val flag = if (fit != null && o != FaceOption.NONE && fit.first != o) " !" else ""
            if (System.getenv("TIMELINE_COSTS") == "1") println("      " + ft.costsOf(t.id))
            println("   #${t.id}x${t.size}${if (t.live) "*" else ""} $state$mine $read" + (fit?.let { " true ${FaceOption.face(it.first)}${FaceOption.turn(it.first)} (${it.second}/8)$flag" } ?: ""))
        }
    }

    /** The true face and turn whose stickers [leading] (track frame) matches on the most outer stickers. */
    private fun best(truth: String, leading: List<CubeColor?>): Pair<Int, Int> =
        (0 until FaceOption.FACES).map { o ->
            val face = FaceOption.face(o)
            o to (0 until 9).count { n -> n != 4 && leading[RotationSearch.turnIndex(n, FaceOption.turn(o))]?.letter == truth[face.ordinal * 9 + n] }
        }.maxBy { it.second }
}
