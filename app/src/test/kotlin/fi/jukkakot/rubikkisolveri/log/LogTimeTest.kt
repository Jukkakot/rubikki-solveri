package fi.jukkakot.rubikkisolveri.log

import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LogTimeTest {
    private val helsinki = ZoneId.of("Europe/Helsinki")
    private val finnish = Locale.forLanguageTag("fi")
    private val line = "2026-10-03T08:09:51Z INFO scan.done valid=true"

    @Test
    fun todaysLineShowsTheLocalTime() {
        val parsed = LogLine.parse(line)
        assertEquals(Level.INFO, parsed.level)
        assertEquals("scan.done valid=true", parsed.rest)
        assertEquals("11.09.51", LogTime.format(parsed.time!!, LocalDate.of(2026, 10, 3), helsinki, finnish))
    }

    @Test
    fun anEarlierDayShowsDateAndTime() {
        val time = Instant.parse("2026-10-01T08:09:51Z")
        val expected = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM).withLocale(finnish).format(time.atZone(helsinki))
        assertEquals(expected, LogTime.format(time, LocalDate.of(2026, 10, 3), helsinki, finnish))
        assertEquals(true, expected.contains("11.09.51"), expected)
    }

    @Test
    fun anUnparsableLineComesBackWhole() {
        val odd = "at fi.jukkakot.Something(Something.kt:12)"
        val parsed = LogLine.parse(odd)
        assertNull(parsed.time)
        assertNull(parsed.level)
        assertEquals(odd, parsed.rest)
        // A time with an unknown level is not split either.
        assertEquals("2026-10-03T08:09:51Z LOUD x", LogLine.parse("2026-10-03T08:09:51Z LOUD x").rest)
    }
}
