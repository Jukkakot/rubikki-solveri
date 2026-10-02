package fi.jukkakot.rubikkisolveri.cube

/** Integer 3D vector: x to the right, y up, z towards the viewer (front). */
data class Vec3(val x: Int, val y: Int, val z: Int) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(k: Int) = Vec3(x * k, y * k, z * k)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)

    /** Rotates a quarter turn clockwise as seen looking from the tip of the unit [axis]. */
    fun quarterTurn(axis: Vec3): Vec3 = axis * (axis dot this) + (axis cross this) * -1
}

/**
 * One of the 54 stickers. Index order is URFDLB, each face row by row in the standard net:
 * U1..U9 = 0..8, R1..R9 = 9..17, F = 18..26, D = 27..35, L = 36..44, B = 45..53.
 * [position] is the centre of the little cube (each coordinate -1..1) and [normal] the direction
 * the sticker faces.
 */
data class Sticker(val index: Int, val face: Face, val row: Int, val col: Int, val position: Vec3, val normal: Vec3)

object Stickers {
    const val COUNT = 54

    val all: List<Sticker> = Face.entries.flatMap { face ->
        (0 until 9).map { i ->
            val row = i / 3
            val col = i % 3
            val position = face.normal + face.right * (col - 1) + face.down * (row - 1)
            Sticker(face.ordinal * 9 + i, face, row, col, position, face.normal)
        }
    }

    private val byPlace: Map<Pair<Vec3, Vec3>, Int> = all.associate { (it.position to it.normal) to it.index }

    fun indexAt(position: Vec3, normal: Vec3): Int =
        byPlace[position to normal] ?: throw IllegalArgumentException("No sticker at $position facing $normal")

    /** Index of the sticker on [face] at 1-based [n] (1..9) in the net, e.g. index(Face.U, 9) for U9. */
    fun index(face: Face, n: Int): Int = face.ordinal * 9 + n - 1

    fun centre(face: Face): Int = index(face, 5)
}
