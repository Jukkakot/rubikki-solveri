package fi.jukkakot.rubikkisolveri.cube.scan

/**
 * After the light changes (the torch turned on or off) the camera meters again: exposure and white
 * balance are unlocked, the frames of the next [SETTLE_MILLIS] are not read, then they lock again
 * (`video-scan-light` design 7: the torch turned on after the lock washed the picture out).
 */
class LightSettle {
    private var until: Long? = null

    /** The light changed at [now]. */
    fun start(now: Long) {
        until = now + SETTLE_MILLIS
    }

    /** Whether the camera is still settling at [now] (its frames are not read, the lock is off). */
    fun settling(now: Long): Boolean {
        val end = until ?: return false
        if (now < end) return true
        until = null
        return false
    }

    companion object {
        const val SETTLE_MILLIS = 1_000L
    }
}
