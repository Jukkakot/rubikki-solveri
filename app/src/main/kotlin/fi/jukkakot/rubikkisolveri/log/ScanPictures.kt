package fi.jukkakot.rubikkisolveri.log

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Small pictures of the scan grid, one per capture, kept next to the log so a shared log shows
 * what the camera saw. Only the newest [KEEP] are kept; they leave the phone only when shared.
 */
class ScanPictures(val dir: File) {

    /** Saves [argb] ([size]×[size]) for a capture of [face]; returns the file name. */
    @Synchronized
    fun save(face: String, argb: IntArray, size: Int, now: Date = Date()): String {
        dir.mkdirs()
        val name = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(now) + "-$face.png"
        val bitmap = Bitmap.createBitmap(argb, size, size, Bitmap.Config.ARGB_8888)
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        list().dropLast(KEEP).forEach { it.delete() }
        return name
    }

    /** The pictures, oldest first. */
    @Synchronized
    fun list(): List<File> = dir.listFiles { f -> f.name.endsWith(".png") }.orEmpty().sortedBy { it.name }

    @Synchronized
    fun clear() {
        list().forEach { it.delete() }
    }

    companion object {
        const val KEEP = 12

        fun of(context: Context) = ScanPictures(File(context.filesDir, "logs/scan"))
    }
}
