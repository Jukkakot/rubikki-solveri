package fi.jukkakot.rubikkisolveri.log

/**
 * Small pictures of the scan grid, one per capture, kept next to the log so a shared log shows what
 * the camera saw. Only the newest [KEEP] are kept; they leave the device only when shared.
 */
interface ScanPictureStore {
    /** The name for a new capture of [face]; sorts after every earlier name. */
    fun newName(face: String): String

    /** Stores the square ARGB picture [name] of [size]² pixels and drops the oldest beyond [KEEP]. */
    fun write(name: String, argb: IntArray, size: Int)

    fun clear()

    companion object {
        const val KEEP = 12
    }
}

/** Keeps nothing: previews and tests. */
object NoScanPictures : ScanPictureStore {
    override fun newName(face: String) = "$face.png"
    override fun write(name: String, argb: IntArray, size: Int) = Unit
    override fun clear() = Unit
}
