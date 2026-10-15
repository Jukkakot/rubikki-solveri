package fi.jukkakot.rubikkisolveri.ui.guide

import fi.jukkakot.rubikkisolveri.cube.Move

/**
 * How long handsfree gives for a quarter turn, counted from when the move appears; slowest first, the
 * order of the speed slider. Stored by name, so the names stay (VERY_SLOW and BRISK came later).
 */
enum class HandsfreeSpeed(val quarterMs: Long) {
    VERY_SLOW(5_000),
    SLOW(3_600),
    NORMAL(2_100),
    BRISK(1_500),
    FAST(1_000),
    ;

    /** The time to do [move]: a half turn gets [HALF_TURN_FACTOR] times a quarter turn's. */
    fun waitMs(move: Move): Long = if (move.quarterTurns == 2) (quarterMs * HALF_TURN_FACTOR).toLong() else quarterMs

    /** Whole seconds and tenths of [quarterMs], for "2,1 s / siirto". */
    val seconds: Pair<Long, Long> get() = (quarterMs / 1000) to (quarterMs % 1000) / 100

    companion object {
        const val HALF_TURN_FACTOR = 1.6

        /** The light vibration comes this long before the guide moves on. */
        const val WARN_BEFORE_MS = 500L

        fun fromName(name: String?): HandsfreeSpeed = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}
