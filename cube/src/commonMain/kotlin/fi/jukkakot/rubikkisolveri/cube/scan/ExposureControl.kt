package fi.jukkakot.rubikkisolveri.cube.scan

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * What the camera should do during the video scan: measure light at [meter] and focus at [focus]
 * (shares 0..1 of the upright picture; null = the whole picture), be set [darker] steps of about
 * half an EV darker than its own choice ([ExposureSteps]), and hold exposure and white balance when
 * [lock].
 */
data class CameraSettings(val meter: Point? = null, val focus: Point? = null, val darker: Int = 0, val lock: Boolean = false) {
    companion object {
        val FREE = CameraSettings()
    }
}

/**
 * The video scan's exposure (`camera-exposure` design 1): the camera meters and focuses on the cube,
 * is made darker step by step while the stickers read washed out, then locked; a torch change or
 * stickers washed out for [RELOCK_MILLIS] after the lock meter again. Every frame is read meanwhile
 * (`scan-start`): this only steers the camera. Metering ends at the latest [METER_LIMIT_MILLIS] after
 * it began, unless a darker step is still settling. [maxDarker] is how many steps darker the camera
 * can go (0 = it cannot be set; then it just meters and locks).
 */
class ExposureControl(var maxDarker: Int = MAX_DARKER) {
    enum class Phase { SEARCHING, METERING, LOCKED }

    /** What became of a frame: the share of washed-out readings when it locked the camera. */
    data class Frame(val lockedWashed: Double? = null)

    var phase: Phase = Phase.SEARCHING
        private set

    var settings: CameraSettings = CameraSettings.FREE
        private set

    /** Until then the metering point is settling (it moved). */
    private var pointUntil = 0L

    /** Until then a darker step is settling. */
    private var stepUntil = 0L

    /** Metering locks by then however the cube moves. */
    private var deadline = 0L
    private var washedSince: Long? = null
    private var lastFocus = 0L

    /** The faces found in a [width]×[height] picture at [now]. */
    fun onFrame(faces: List<FaceReading>, width: Int, height: Int, now: Long): Frame {
        val at = middle(faces, width, height)
        val washed = washedShare(faces)
        when (phase) {
            Phase.SEARCHING -> {
                if (at == null) return Frame()
                phase = Phase.METERING
                settings = settings.copy(meter = at, focus = at, lock = false)
                pointUntil = now + SETTLE_MILLIS
                stepUntil = now
                deadline = now + METER_LIMIT_MILLIS
                return Frame()
            }
            Phase.METERING -> {
                if (at != null && moved(settings.meter, at)) {
                    settings = settings.copy(meter = at, focus = at)
                    pointUntil = now + SETTLE_MILLIS
                }
                if (now < stepUntil || (now < pointUntil && now < deadline)) return Frame()
                if (washed == null) return Frame()
                if (washed > WASHED_SHARE && settings.darker < maxDarker) {
                    settings = settings.copy(darker = settings.darker + 1)
                    stepUntil = now + SETTLE_MILLIS
                    return Frame()
                }
                phase = Phase.LOCKED
                settings = settings.copy(lock = true)
                washedSince = null
                lastFocus = now
                return Frame(lockedWashed = washed)
            }
            Phase.LOCKED -> {
                if (washed != null) {
                    if (washed <= WASHED_SHARE) {
                        washedSince = null
                    } else {
                        val since = washedSince ?: now.also { washedSince = it }
                        if (now - since >= RELOCK_MILLIS && settings.darker < maxDarker) {
                            meterAgain(now)
                            return Frame()
                        }
                    }
                }
                // Measuring stays where it was locked; focus follows the cube, at most once a second.
                if (at != null && moved(settings.focus, at) && now - lastFocus >= FOCUS_MILLIS) {
                    settings = settings.copy(focus = at)
                    lastFocus = now
                }
                return Frame()
            }
        }
    }

    /** The torch was turned [on] or off at [now]: meter again (off: from the camera's own exposure). */
    fun onTorch(on: Boolean, now: Long) {
        if (!on) settings = settings.copy(darker = 0)
        if (phase != Phase.SEARCHING) meterAgain(now)
    }

    private fun meterAgain(now: Long) {
        phase = Phase.METERING
        settings = settings.copy(lock = false, meter = settings.focus ?: settings.meter)
        pointUntil = now + SETTLE_MILLIS
        stepUntil = now
        deadline = now + METER_LIMIT_MILLIS
        washedSince = null
    }

    /** The middle of all [faces] (the cube as seen) as shares of the picture, null without any. */
    private fun middle(faces: List<FaceReading>, width: Int, height: Int): Point? {
        if (faces.isEmpty()) return null
        return Point(faces.sumOf { it.centre.x } / faces.size / width, faces.sumOf { it.centre.y } / faces.size / height)
    }

    private fun moved(from: Point?, to: Point): Boolean = from == null || (to - from).length > MOVE_SHARE

    companion object {
        /** Time the camera gets to adjust after a change before its frames are judged for the lock. */
        const val SETTLE_MILLIS = 600L

        /** Metering locks at the latest this long after it began (unless a darker step is settling). */
        const val METER_LIMIT_MILLIS = 1_000L

        /** More than this share of the readings washed out: one step darker. */
        const val WASHED_SHARE = 1.0 / 3

        /** Washed out this long after the lock: meter again. */
        const val RELOCK_MILLIS = 2_000L

        /** The point follows the cube when it moves more than this share of the picture. */
        const val MOVE_SHARE = 0.15

        /** Once locked, focus follows at most this often. */
        const val FOCUS_MILLIS = 1_000L

        /** Most steps darker (−2 EV at [ExposureSteps.STEP_EV] each). */
        const val MAX_DARKER = 4

        /** Share of the readings of [faces] washed out ([VideoScan.WASHED_FROM]), null without any. */
        fun washedShare(faces: List<FaceReading>): Double? {
            val values = faces.flatMap { f -> f.colors.filterNotNull() }
            if (values.isEmpty()) return null
            return values.count { maxOf(it.r, it.g, it.b) >= VideoScan.WASHED_FROM }.toDouble() / values.size
        }
    }
}

/**
 * The controller's steps in the camera's own exposure-compensation units (`camera-exposure` design 2):
 * each step is as many of the camera's steps of [stepEv] as make about [STEP_EV].
 */
object ExposureSteps {
    const val STEP_EV = 0.5

    /** Camera steps per controller step. */
    fun per(stepEv: Double): Int = if (stepEv <= 0) 0 else maxOf(1, (STEP_EV / stepEv).roundToInt())

    /** How many steps darker a camera whose lowest compensation index is [minIndex] allows (at most [ExposureControl.MAX_DARKER]). */
    fun maxDarker(stepEv: Double, minIndex: Int): Int {
        val per = per(stepEv)
        if (per == 0 || minIndex >= 0) return 0
        return minOf(ExposureControl.MAX_DARKER, floor(abs(minIndex).toDouble() / per).toInt())
    }

    /** The camera's compensation index for [darker] steps, not below [minIndex]. */
    fun index(darker: Int, stepEv: Double, minIndex: Int): Int = maxOf(minIndex, -darker * per(stepEv)).coerceAtMost(0)
}
