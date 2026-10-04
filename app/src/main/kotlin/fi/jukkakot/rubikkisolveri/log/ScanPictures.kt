package fi.jukkakot.rubikkisolveri.log

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The phone's [ScanPictureStore]: PNG files in [dir], shared with the log. */
class ScanPictures(val dir: File) : ScanPictureStore {

    override fun newName(face: String): String = newName(face, Date())

    /** The file name for a capture of [face] at [now]. */
    fun newName(face: String, now: Date): String =
        SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.ROOT).format(now) + "-$face.png"

    /** Writes the picture [name] (slow enough to keep off the main thread) and drops the oldest. */
    @Synchronized
    override fun write(name: String, argb: IntArray, size: Int) {
        dir.mkdirs()
        val bitmap = Bitmap.createBitmap(argb, size, size, Bitmap.Config.ARGB_8888)
        File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        list().dropLast(ScanPictureStore.KEEP).forEach { it.delete() }
    }

    /** The pictures, oldest first. */
    @Synchronized
    fun list(): List<File> = dir.listFiles { f -> f.name.endsWith(".png") }.orEmpty().sortedBy { it.name }

    @Synchronized
    override fun clear() {
        list().forEach { it.delete() }
    }

    companion object {
        fun of(context: Context) = ScanPictures(File(context.filesDir, "logs/scan"))
    }
}
