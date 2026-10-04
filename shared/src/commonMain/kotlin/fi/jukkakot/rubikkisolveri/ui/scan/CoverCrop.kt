package fi.jukkakot.rubikkisolveri.ui.scan

import kotlin.math.roundToInt

/** A rectangle in video pixels. */
data class CropRect(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * The part of a [videoWidth]×[videoHeight] video that a [boxWidth]×[boxHeight] view shows when it
 * fills the view and cuts off the rest (CSS `object-fit: cover`), centred.
 */
fun coverCrop(videoWidth: Int, videoHeight: Int, boxWidth: Int, boxHeight: Int): CropRect {
    if (boxWidth <= 0 || boxHeight <= 0) return CropRect(0, 0, videoWidth, videoHeight)
    val videoAspect = videoWidth.toDouble() / videoHeight
    val boxAspect = boxWidth.toDouble() / boxHeight
    return if (videoAspect > boxAspect) {
        val w = (videoHeight * boxAspect).roundToInt().coerceIn(1, videoWidth)
        CropRect((videoWidth - w) / 2, 0, w, videoHeight)
    } else {
        val h = (videoWidth / boxAspect).roundToInt().coerceIn(1, videoHeight)
        CropRect(0, (videoHeight - h) / 2, videoWidth, h)
    }
}

/** The centred square of [crop]'s shorter side: what the scan grid is read from. */
fun centredSquare(crop: CropRect): CropRect {
    val side = minOf(crop.width, crop.height)
    return CropRect(crop.x + (crop.width - side) / 2, crop.y + (crop.height - side) / 2, side, side)
}
