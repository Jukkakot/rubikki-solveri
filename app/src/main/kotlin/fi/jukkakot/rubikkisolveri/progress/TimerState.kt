package fi.jukkakot.rubikkisolveri.progress

/**
 * The speedcubing timer: hold a finger down until ready (green), release to start, tap to stop.
 * [now] gives milliseconds; injected so tests control time.
 */
class TimerState(private val now: () -> Long) {
    enum class Phase { IDLE, HOLDING, READY, RUNNING, STOPPED }

    var phase: Phase = Phase.IDLE
        private set
    private var pressedAt = 0L
    private var startedAt = 0L
    var result: Long? = null
        private set

    /** Milliseconds to show right now. */
    fun display(): Long = when (phase) {
        Phase.RUNNING -> now() - startedAt
        Phase.STOPPED -> result ?: 0
        else -> 0
    }

    /** Finger down: stops a running timer, otherwise starts holding. Returns the time if it stopped. */
    fun press(): Long? {
        when (phase) {
            Phase.RUNNING -> {
                result = now() - startedAt
                phase = Phase.STOPPED
                return result
            }
            Phase.IDLE, Phase.STOPPED -> {
                pressedAt = now()
                phase = Phase.HOLDING
            }
            else -> Unit
        }
        return null
    }

    /** Called while the finger stays down: becomes ready after [HOLD_MS]. */
    fun tick() {
        if (phase == Phase.HOLDING && now() - pressedAt >= HOLD_MS) phase = Phase.READY
    }

    /** Finger up: starts when ready, otherwise cancels the hold. */
    fun release() {
        tick()
        when (phase) {
            Phase.READY -> {
                startedAt = now()
                phase = Phase.RUNNING
            }
            Phase.HOLDING -> phase = if (result != null) Phase.STOPPED else Phase.IDLE
            else -> Unit
        }
    }

    companion object {
        const val HOLD_MS = 500L
    }
}
