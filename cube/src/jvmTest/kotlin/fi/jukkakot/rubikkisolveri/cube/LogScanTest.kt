package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ColorClassifier
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb
import fi.jukkakot.rubikkisolveri.cube.scan.ScanOutcome
import fi.jukkakot.rubikkisolveri.cube.scan.ScanSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTimedValue

/**
 * The user's scans of 2026-10-04 (web app log): two valid in daylight, two invalid (10:47 and 11:07,
 * real cube unknown) and the failed evening scan (18:30), where the dark blue centre was taken for
 * the white face.
 */
class LogScanTest {
    /** One finished scan of the log: the faces' nine readings as captured, and the logged result. */
    private class LoggedScan(val time: String, val faces: List<List<Rgb>>, val names: List<Face>, val cube: String, val valid: Boolean)

    private val scans: List<LoggedScan> by lazy {
        val lines = javaClass.getResource("/scan/scan-log-2026-10-04.txt")!!.readText().lines()
        val result = ArrayList<LoggedScan>()
        val faces = LinkedHashMap<String, List<Rgb>>()
        for (line in lines) {
            fun field(name: String) = Regex("$name=(\\S+)").find(line)?.groupValues?.get(1)
            when {
                " app.start " in line -> faces.clear()
                " scan.face " in line -> faces[field("face")!!] = field("rgb")!!.split(",").map(Rgb::fromHex)
                " scan.done " in line -> {
                    result += LoggedScan(line.substring(11, 16), faces.values.toList(), faces.keys.map(Face::valueOf), field("cube")!!, field("valid") == "true")
                    faces.clear()
                }
            }
        }
        result
    }

    private fun scan(time: String) = scans.single { it.time == time }

    /** Runs the faces through a scan session in the logged order, each captured and accepted as recognised. */
    private fun outcome(scan: LoggedScan, asLogged: Boolean = false): ScanOutcome {
        val session = ScanSession()
        for ((i, face) in scan.faces.withIndex()) {
            session.onFrame(face, i * 10_000L)
            session.captureNow()
            if (asLogged) session.choose(FaceView.of(scan.names[i]))
            session.accept()
        }
        assertTrue(session.isDone, scan.time)
        return session.outcome()
    }

    @Test
    fun theLogHasFiveScans() {
        assertEquals(listOf("09:17", "10:23", "10:47", "11:07", "18:30"), scans.map { it.time })
    }

    @Test
    fun eveningScanIsValid() {
        val outcome = outcome(scan("18:30"))
        assertTrue(outcome.validity.isValid, "${outcome.validity} ${outcome.editor.encode()}")
        println("18:30 renamed=${outcome.renamed} confident=${outcome.isConfident} cube=${outcome.editor.encode()}")
    }

    @Test
    fun eveningScanTakenWrongIsRenamed() {
        // As named live in the log: the blue face taken for the top, the white one for the back.
        val outcome = outcome(scan("18:30"), asLogged = true)
        assertTrue(outcome.validity.isValid, "${outcome.validity}")
        assertEquals("B>U,U>B", outcome.renamed)
        assertEquals(outcome(scan("18:30")).editor.encode(), outcome.editor.encode())
    }

    @Test
    fun daylightScansUnchanged() {
        for (time in listOf("09:17", "10:23")) {
            val logged = scan(time)
            assertTrue(logged.valid)
            val outcome = outcome(logged)
            assertEquals(logged.cube, outcome.editor.encode(), time)
            assertTrue(outcome.isConfident, time)
            assertEquals(null, outcome.renamed, time)
        }
    }

    @Test
    fun invalidDaytimeScansAreReported() {
        for (time in listOf("10:47", "11:07")) {
            val outcome = outcome(scan(time))
            println("$time valid=${outcome.validity.isValid} validity=${outcome.validity} uncertain=${outcome.uncertain.size} renamed=${outcome.renamed}")
        }
    }

    @Test
    fun darkBlueCentreIsTheBlueFace() {
        assertEquals(CubeColor.BLUE, ColorClassifier.rankedCentre(Rgb.fromHex("072641")).first())
        // The evening white stays white.
        assertEquals(CubeColor.WHITE, ColorClassifier.rankedCentre(Rgb.fromHex("7f7459")).first())
        val session = ScanSession()
        session.onFrame(scan("18:30").faces[0], 0)
        assertEquals(FaceView.of(Face.B), session.captureNow().let { session.reviewFace })
    }

    @Test
    fun trimmedMeanIgnoresAHighlight() {
        // A fifth of the cell is a lamp's reflection.
        val values = List(80) { 100 } + List(20) { 255 }
        assertEquals(100, FrameSampler.trimmedMean(values))
        assertEquals(100, FrameSampler.trimmedMean(List(20) { 10 } + List(80) { 100 }))
    }

    @Test
    fun worstCaseOutcomeTime() {
        // Every scan of the log, timed; an impossible one tries all namings.
        for (scan in scans) {
            val (outcome, time) = measureTimedValue { outcome(scan) }
            println("${scan.time} outcome in $time (valid=${outcome.validity.isValid})")
            assertTrue(time.inWholeMilliseconds < 5_000, "${scan.time} took $time")
        }
    }
}
