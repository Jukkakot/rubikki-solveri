package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.Vec3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * How the real cube is turned, as a rotation matrix [m] (row by row) from cube coordinates (x
 * right, y up, z front; see [Vec3]) to camera coordinates (x right, y up, z towards the viewer).
 * The rows are where the camera's right, up and viewing axes point in the cube.
 */
/** How [Orientation.chosen] picked between the mirror answers: [ONLY] one (or both alike), a [CUE] from another face, the [PREVIOUS] orientation, or a [GUESS]. */
enum class Choice { ONLY, CUE, PREVIOUS, GUESS }

data class Orientation(val m: List<Double>) {
    init {
        require(m.size == 9)
    }

    /** The cube vector (x, y, z) in camera coordinates. */
    fun apply(x: Double, y: Double, z: Double): Triple<Double, Double, Double> = Triple(
        m[0] * x + m[1] * y + m[2] * z,
        m[3] * x + m[4] * y + m[5] * z,
        m[6] * x + m[7] * y + m[8] * z,
    )

    /** The angle (radians) of the rotation between this and [other]. */
    fun angleTo(other: Orientation): Double {
        val trace = m.indices.sumOf { m[it] * other.m[it] }
        return acos(((trace - 1) / 2).coerceIn(-1.0, 1.0))
    }

    companion object {
        /**
         * The rotations that show cube vectors [a] and [b] (one sticker step each, along the face's
         * rows and columns) as the steps [u] and [v] in the frame (pixels, y down), in a
         * weak-perspective camera: the third axis's on-screen step follows from the camera's rows
         * being orthogonal and of equal length. That has two answers, mirror images tilted towards
         * or away from the camera (both looking at it); they coincide for a face seen straight on.
         * Empty when the steps cannot be a face (parallel or zero).
         */
        fun candidates(u: Point, v: Point, a: Vec3, b: Vec3): List<Orientation> {
            if (abs(u.cross(v)) < 0.05 * u.length * v.length) return emptyList()
            val c = a cross b
            val ux = u.x
            val uy = -u.y
            val vx = v.x
            val vy = -v.y
            val pq = -(ux * uy + vx * vy)
            val diff = (uy * uy + vy * vy) - (ux * ux + vx * vx)
            // (p + iq)² = diff + 2i·pq
            val r = hypot(diff, 2 * pq)
            val p = sqrt(((r + diff) / 2).coerceAtLeast(0.0))
            val q = sqrt(((r - diff) / 2).coerceAtLeast(0.0)) * if (pq < 0) -1 else 1
            val signs = if (p == 0.0 && q == 0.0) listOf(1.0) else listOf(1.0, -1.0)
            return signs.mapNotNull { sign -> of(row(ux, vx, sign * p, a, b, c), row(uy, vy, sign * q, a, b, c)) }
        }

        /**
         * Of the [candidates] for a face with normal [normal] centred at [centre], the one that best
         * fits the faces [others] seen in the same frame (their normal and centre: where the
         * neighbour's centre lies tells which way the face tilts), else the one closest to
         * [previous], else the first.
         */
        fun choose(
            candidates: List<Orientation>,
            normal: Vec3,
            centre: Point,
            others: List<Pair<Vec3, Point>>,
            previous: Orientation?,
        ): Orientation? = chosen(candidates, normal, centre, others, previous)?.first

        /** [choose], with how it chose (`scan-paint-steady`: only a sure tilt draws the cube's other sides). */
        fun chosen(
            candidates: List<Orientation>,
            normal: Vec3,
            centre: Point,
            others: List<Pair<Vec3, Point>>,
            previous: Orientation?,
        ): Pair<Orientation, Choice>? {
            if (candidates.size < 2) return candidates.firstOrNull()?.let { it to Choice.ONLY }
            // Seen nearly straight on, the two mirror answers hardly differ: either will do.
            if (candidates[0].angleTo(candidates[1]) < ALIKE_RADIANS) return candidates[0] to Choice.ONLY
            val cues = others.filter { (n, _) -> n != normal && n != normal * -1 }
            if (cues.isNotEmpty()) {
                return candidates.minBy { o -> cues.sumOf { (n, at) -> o.offsetError(n, normal, at - centre) } } to Choice.CUE
            }
            if (previous != null) return candidates.minBy { it.angleTo(previous) } to Choice.PREVIOUS
            return candidates.first() to Choice.GUESS
        }

        /** Mirror answers closer than this (radians, about 15°) count as one. */
        const val ALIKE_RADIANS = 0.26

        /** Direction mismatch between the on-screen offset of face [n]'s centre from face [from]'s and the observed [d]. */
        private fun Orientation.offsetError(n: Vec3, from: Vec3, d: Point): Double {
            val (x, y, _) = apply((n.x - from.x).toDouble(), (n.y - from.y).toDouble(), (n.z - from.z).toDouble())
            val dx = d.x
            val dy = -d.y
            val len = hypot(x, y) * hypot(dx, dy)
            return if (len == 0.0) 1.0 else 1 - (x * dx + y * dy) / len
        }

        private fun row(along: Double, down: Double, depth: Double, a: Vec3, b: Vec3, c: Vec3): DoubleArray =
            DoubleArray(3) { i -> along * a[i] + down * b[i] + depth * c[i] }

        private operator fun Vec3.get(i: Int): Int = when (i) {
            0 -> x
            1 -> y
            else -> z
        }

        private fun of(row1: DoubleArray, row2: DoubleArray): Orientation? {
            val l1 = sqrt(row1.sumOf { it * it })
            val l2 = sqrt(row2.sumOf { it * it })
            if (l1 < 1e-9 || l2 < 1e-9) return null
            val r1 = row1.map { it / l1 }
            val r2 = row2.map { it / l2 }
            if (abs(r1.indices.sumOf { r1[it] * r2[it] }) > 0.2) return null
            val r3 = listOf(r1[1] * r2[2] - r1[2] * r2[1], r1[2] * r2[0] - r1[0] * r2[2], r1[0] * r2[1] - r1[1] * r2[0])
            return Orientation(r1 + r2 + r3)
        }
    }
}
