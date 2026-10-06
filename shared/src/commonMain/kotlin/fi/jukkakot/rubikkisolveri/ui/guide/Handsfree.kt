package fi.jukkakot.rubikkisolveri.ui.guide

import fi.jukkakot.rubikkisolveri.cube.Move

/** How long handsfree gives for a quarter turn after its demo has ended. */
enum class HandsfreeSpeed(val quarterMs: Long) {
    SLOW(3_600),
    NORMAL(2_100),
    FAST(1_000),
    ;

    /** The time to do [move]: a half turn gets [HALF_TURN_FACTOR] times a quarter turn's. */
    fun waitMs(move: Move): Long = if (move.quarterTurns == 2) (quarterMs * HALF_TURN_FACTOR).toLong() else quarterMs

    companion object {
        const val HALF_TURN_FACTOR = 1.6

        /** The light vibration comes this long before the guide moves on. */
        const val WARN_BEFORE_MS = 500L

        fun fromName(name: String?): HandsfreeSpeed = entries.firstOrNull { it.name == name } ?: NORMAL
    }
}
