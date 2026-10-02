package fi.jukkakot.rubikkisolveri.log

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.Executor
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val clock = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC)
    private val direct = Executor { it.run() }

    @Test
    fun lineHasTimeLevelEventFieldsAndMessageInOrder() {
        val line = LogLine.format(clock.instant(), Level.INFO, Evt.APP_START, mapOf("ver" to "0.1.0", "sdk" to 34), "hello there")
        assertEquals("2026-10-02T12:00:00Z INFO app.start ver=0.1.0 sdk=34 msg=\"hello there\"", line)
    }

    @Test
    fun stackTraceStaysOnOneLine() {
        val line = LogLine.format(clock.instant(), Level.ERROR, Evt.APP_CRASH, mapOf("stack" to LogLine.stackOf(IllegalStateException("boom"))), null)
        assertFalse(line.contains('\n'))
        assertTrue(line.contains("IllegalStateException: boom\\n"))
    }

    @Test
    fun nullFieldsAreLeftOut() {
        val line = LogLine.format(clock.instant(), Level.INFO, Evt.NAV_SCREEN, mapOf("screen" to null), null)
        assertEquals("2026-10-02T12:00:00Z INFO nav.screen", line)
    }

    @Test
    fun eventRecorded() {
        val file = LogFile(File(tmp.root, "logs/app.log"))
        val sunk = mutableListOf<String>()
        val logger = Logger(file, { _, line -> sunk += line }, direct, clock)
        logger.info(Evt.APP_START, null, "ver" to "0.1.0-abc1234")
        assertEquals(listOf("2026-10-02T12:00:00Z INFO app.start ver=0.1.0-abc1234"), file.readLines())
        assertEquals(file.readLines(), sunk)
    }

    @Test
    fun logStaysSmall() {
        val file = LogFile(File(tmp.root, "app.log"), maxBytes = 1_000)
        repeat(200) { file.append("line %03d %s".format(it, "x".repeat(20))) }
        val lines = file.readLines()
        assertTrue(file.file.length() <= 1_000, "size ${file.file.length()}")
        assertEquals("line 199 ${"x".repeat(20)}", lines.last())
        assertFalse(lines.any { it.startsWith("line 000") })
    }

    @Test
    fun clearEmptiesTheFile() {
        val file = LogFile(File(tmp.root, "app.log"))
        file.append("a")
        file.clear()
        assertEquals(emptyList(), file.readLines())
    }

    @Test
    fun crashBecomesOneLineAndSetsTheMarker() {
        val file = LogFile(File(tmp.root, "app.log"))
        val marker = File(tmp.root, "crash.marker")
        var handedOver: Throwable? = null
        val previous = Thread.UncaughtExceptionHandler { _, e -> handedOver = e }
        val logger = Logger(file, { _, _ -> }, { /* never runs: crash path must not need the executor */ }, clock)
        val error = IllegalStateException("boom")

        CrashHandler(logger, marker, previous).uncaughtException(Thread.currentThread(), error)

        val lines = file.readLines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].startsWith("2026-10-02T12:00:00Z ERROR app.crash"))
        assertTrue(lines[0].contains("stack="))
        assertEquals(error, handedOver)
        assertTrue(CrashHandler.consumeCrashMarker(marker))
        assertFalse(CrashHandler.consumeCrashMarker(marker), "the notice is shown only once")
    }
}
