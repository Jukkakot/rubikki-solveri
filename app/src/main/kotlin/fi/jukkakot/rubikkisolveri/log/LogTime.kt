package fi.jukkakot.rubikkisolveri.log

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** A log time as the phone shows it: in [zone], the [locale]'s format, only the time for [today]'s lines. */
object LogTime {
    fun format(instant: Instant, today: LocalDate, zone: ZoneId, locale: Locale): String {
        val local = instant.atZone(zone)
        val formatter = if (local.toLocalDate() == today) {
            DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        } else {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)
        }
        return formatter.withLocale(locale).format(local)
    }
}
