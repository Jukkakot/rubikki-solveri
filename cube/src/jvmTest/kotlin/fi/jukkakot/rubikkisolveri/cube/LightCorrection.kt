package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.FaceReading
import fi.jukkakot.rubikkisolveri.cube.scan.FrameSampler
import fi.jukkakot.rubikkisolveri.cube.scan.Rgb

/**
 * The colour of the light taken out of a frame's readings (von Kries): the stickers taken as white
 * give the light's colour, and every reading is scaled per channel so that white comes out grey at
 * the brightness it had. Each gain stays within [MIN_GAIN]..[MAX_GAIN] (a yellowish sticker taken
 * for white must not repaint the frame). A frame without white keeps the last correction.
 *
 * `video-scan-light` design 2, measured in [ReadingMeasure]: it does not tell red from orange better
 * on the test videos (the camera's white balance already makes white grey), so the scan does not use it.
 */
class LightCorrection {
    /** Per channel (r, g, b) the factor readings are multiplied by. */
    var gains: DoubleArray = doubleArrayOf(1.0, 1.0, 1.0)
        private set

    /** Takes the light's colour from [whites] (readings of white stickers in one frame); none: unchanged. */
    fun update(whites: List<Rgb>) {
        if (whites.isEmpty()) return
        val r = FrameSampler.median(whites.map { it.r }).toDouble()
        val g = FrameSampler.median(whites.map { it.g }).toDouble()
        val b = FrameSampler.median(whites.map { it.b }).toDouble()
        if (r <= 0 || g <= 0 || b <= 0) return
        val grey = (r + g + b) / 3
        gains = doubleArrayOf(grey / r, grey / g, grey / b).map { it.coerceIn(MIN_GAIN, MAX_GAIN) }.toDoubleArray()
    }

    fun apply(rgb: Rgb): Rgb {
        fun ch(v: Int, k: Int) = (v * gains[k]).toInt().coerceIn(0, 255)
        return Rgb(ch(rgb.r, 0), ch(rgb.g, 1), ch(rgb.b, 2))
    }

    fun apply(face: FaceReading): FaceReading = face.copy(colors = face.colors.map { it?.let(::apply) })

    fun reset() {
        gains = doubleArrayOf(1.0, 1.0, 1.0)
    }

    companion object {
        const val MIN_GAIN = 0.67
        const val MAX_GAIN = 1.5

        /** Most a white reading's channels may differ, as a share of its brightest. */
        const val GREY_SPREAD = 0.4

        /** A white reading is at least this share of the frame's brightest near-grey reading. */
        const val WHITE_SHARE = 0.75

        /** Close enough to grey to be white in some light (low saturation). */
        fun greyish(rgb: Rgb): Boolean {
            val top = maxOf(rgb.r, rgb.g, rgb.b)
            return top > 0 && (top - minOf(rgb.r, rgb.g, rgb.b)) <= GREY_SPREAD * top
        }

        /** Before anything is known: the brightest near-grey readings of [faces] are taken as white. */
        fun likelyWhites(faces: List<FaceReading>): List<Rgb> {
            val grey = faces.flatMap { f -> f.colors.filterNotNull().filter(::greyish) }
            val top = grey.maxOfOrNull { maxOf(it.r, it.g, it.b) } ?: return emptyList()
            return grey.filter { maxOf(it.r, it.g, it.b) >= WHITE_SHARE * top }
        }
    }
}
