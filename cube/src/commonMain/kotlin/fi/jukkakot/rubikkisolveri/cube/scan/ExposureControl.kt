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
 *
 * `scan-camera-pick`: a darker step is judged once the stickers' brightness has moved
 * [STEP_CHANGE] (or their washed-out share fell [STEP_WASHED_CHANGE]) or after [STEP_MAX_MILLIS] (a slowly adjusting camera got extra steps); after the lock,
 * stickers clearly darker than at the lock for [DARK_MILLIS] undo it one step lighter, and the camera
 * is not made darker than that again until the torch changes.
 */
class ExposureControl(var maxDarker: Int = MAX_DARKER) {
    enum class Phase { SEARCHING, METERING, LOCKED }

    enum class Kind { DARKER, LIGHTER, LOCK }

    /** A step or the lock, for the log: the step it left the camera at, the stickers' brightness and washed-out share then. */
    data class Event(val kind: Kind, val darker: Int, val light: Int?, val washed: Double?)

    /** What became of a frame: the [event] it caused, if any. */
    data class Frame(val event: Event? = null) {
        /** The share of washed-out readings when it locked the camera. */
        val lockedWashed: Double? get() = event?.takeIf { it.kind == Kind.LOCK }?.washed
    }

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

    /** A darker step waits for the picture to change: when it was taken and the brightness then. */
    private var stepAt: Long? = null
    private var stepLight: Int? = null
    private var stepWashed = 0.0

    /** The stickers' brightness at the lock, and since when they have been clearly darker. */
    private var lockLight: Int? = null
    private var darkSince: Long? = null

    /** The most steps darker until the torch changes (lowered by a lighter step). */
    private var cap = Int.MAX_VALUE

    private val darkest get() = minOf(maxDarker, cap)

    /** The faces found in a [width]×[height] picture at [now]. */
    fun onFrame(faces: List<FaceReading>, width: Int, height: Int, now: Long): Frame {
        val at = middle(faces, width, height)
        val washed = washedShare(faces)
        val light = VideoScan.brightness(faces)
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
                val step = stepAt
                if (step != null) {
                    val before = stepLight
                    // A washed-out picture's brightness sits at the top, so fewer washed-out readings count as a change too.
                    val changed = before == null || light == null || abs(light - before) >= before * STEP_CHANGE ||
                        stepWashed - washed >= STEP_WASHED_CHANGE
                    if (!changed && now - step < STEP_MAX_MILLIS) return Frame()
                    stepAt = null
                }
                if (washed > WASHED_SHARE && settings.darker < darkest) {
                    settings = settings.copy(darker = settings.darker + 1)
                    stepUntil = now + SETTLE_MILLIS
                    stepAt = now
                    stepLight = light
                    stepWashed = washed
                    return Frame(Event(Kind.DARKER, settings.darker, light, washed))
                }
                phase = Phase.LOCKED
                settings = settings.copy(lock = true)
                washedSince = null
                lockLight = light
                darkSince = null
                lastFocus = now
                return Frame(Event(Kind.LOCK, settings.darker, light, washed))
            }
            Phase.LOCKED -> {
                if (washed != null) {
                    if (washed <= WASHED_SHARE) {
                        washedSince = null
                    } else {
                        val since = washedSince ?: now.also { washedSince = it }
                        if (now - since >= RELOCK_MILLIS && settings.darker < darkest) {
                            meterAgain(now)
                            return Frame()
                        }
                    }
                }
                val locked = lockLight
                if (light != null && locked != null) {
                    if (light < DARK_LIGHT && light < locked * DARK_SHARE) {
                        val since = darkSince ?: now.also { darkSince = it }
                        if (now - since >= DARK_MILLIS) {
                            val lighter = maxOf(0, settings.darker - 1)
                            cap = lighter
                            settings = settings.copy(darker = lighter)
                            meterAgain(now)
                            return Frame(Event(Kind.LIGHTER, lighter, light, washed))
                        }
                    } else {
                        darkSince = null
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
        cap = Int.MAX_VALUE
        if (phase != Phase.SEARCHING) meterAgain(now)
    }

    private fun meterAgain(now: Long) {
        phase = Phase.METERING
        settings = settings.copy(lock = false, meter = settings.focus ?: settings.meter)
        pointUntil = now + SETTLE_MILLIS
        stepUntil = now
        deadline = now + METER_LIMIT_MILLIS
        washedSince = null
        stepAt = null
        lockLight = null
        darkSince = null
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

        /** A darker step is judged once the stickers' brightness moved this share from the step's... */
        const val STEP_CHANGE = 0.08

        /** ...or the washed-out share fell this much... */
        const val STEP_WASHED_CHANGE = 0.15

        /** ...or this long after the step. */
        const val STEP_MAX_MILLIS = 1_200L

        /** After the lock, stickers below this share of the lock's brightness... */
        const val DARK_SHARE = 2.0 / 3

        /** ...and below this brightness (0–255, the median of max(r, g, b))... */
        const val DARK_LIGHT = 150

        /** ...this long: one step lighter (or just unlocked) and metered again. */
        const val DARK_MILLIS = 1_000L

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
