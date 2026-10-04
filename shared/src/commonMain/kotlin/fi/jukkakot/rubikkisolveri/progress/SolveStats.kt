package fi.jukkakot.rubikkisolveri.progress

import kotlin.math.roundToLong

enum class Penalty { NONE, PLUS_TWO, DNF }

/** A timed solve as the statistics see it. */
data class TimedResult(val millis: Long, val penalty: Penalty = Penalty.NONE) {
    /** Time that counts: +2 adds two seconds, DNF counts as infinitely slow (null). */
    val effective: Long?
        get() = when (penalty) {
            Penalty.NONE -> millis
            Penalty.PLUS_TWO -> millis + 2000
            Penalty.DNF -> null
        }
}

/** An average: a time, DNF, or not enough solves yet. */
sealed interface Average {
    data class Time(val millis: Long) : Average
    data object Dnf : Average
    data object NotEnough : Average
}

/** Statistics over solves given newest first. */
object SolveStats {
    fun best(results: List<TimedResult>): Long? = results.mapNotNull { it.effective }.minOrNull()

    fun mean(results: List<TimedResult>): Long? =
        results.mapNotNull { it.effective }.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size }

    /**
     * Average of the newest [n] (competition style): drop one best and one worst, average the rest.
     * One DNF is the worst and is dropped; two or more make the average DNF.
     */
    fun averageOf(n: Int, results: List<TimedResult>): Average {
        if (results.size < n) return Average.NotEnough
        val last = results.take(n)
        if (last.count { it.effective == null } > 1) return Average.Dnf
        val sorted = last.map { it.effective ?: Long.MAX_VALUE }.sorted()
        val middle = sorted.subList(1, n - 1)
        return Average.Time((middle.sum().toDouble() / middle.size).roundToLong())
    }

    /** "12.34" or "1:02.50" from milliseconds (hundredths, truncated as timers do). */
    fun format(millis: Long): String {
        val hundredths = millis / 10
        val seconds = hundredths / 100
        val minutes = seconds / 60
        val rest = (hundredths % 100).toString().padStart(2, '0')
        return if (minutes > 0) "$minutes:${(seconds % 60).toString().padStart(2, '0')}.$rest" else "$seconds.$rest"
    }
}
