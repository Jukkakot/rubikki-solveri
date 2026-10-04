package fi.jukkakot.rubikkisolveri.ui.cube3d

import fi.jukkakot.rubikkisolveri.cube.Vec3
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(k: Float) = V3(x * k, y * k, z * k)
    infix fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length: Float get() = sqrt(this dot this)
    fun normalized(): V3 = this * (1f / length)

    companion object {
        val ZERO = V3(0f, 0f, 0f)
        fun of(v: Vec3) = V3(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
    }
}

/** Unit quaternion for rotations; `a * b` rotates by b first, then a. */
data class Quat(val w: Float, val x: Float, val y: Float, val z: Float) {
    operator fun times(o: Quat) = Quat(
        w * o.w - x * o.x - y * o.y - z * o.z,
        w * o.x + x * o.w + y * o.z - z * o.y,
        w * o.y - x * o.z + y * o.w + z * o.x,
        w * o.z + x * o.y - y * o.x + z * o.w,
    )

    fun rotate(v: V3): V3 {
        val u = V3(x, y, z)
        val t = (u cross v) * 2f
        return v + t * w + (u cross t)
    }

    fun normalized(): Quat {
        val n = sqrt(w * w + x * x + y * y + z * z)
        return Quat(w / n, x / n, y / n, z / n)
    }

    /** Spherical interpolation from this (t = 0) to [to] (t = 1) along the shorter way. */
    fun slerp(to: Quat, t: Float): Quat {
        var dot = w * to.w + x * to.x + y * to.y + z * to.z
        var b = to
        if (dot < 0f) {
            dot = -dot
            b = Quat(-to.w, -to.x, -to.y, -to.z)
        }
        if (dot > 0.9995f) {
            return Quat(w + (b.w - w) * t, x + (b.x - x) * t, y + (b.y - y) * t, z + (b.z - z) * t).normalized()
        }
        val theta = acos(dot.coerceIn(-1f, 1f))
        val s = sin(theta)
        val ka = sin((1 - t) * theta) / s
        val kb = sin(t * theta) / s
        return Quat(w * ka + b.w * kb, x * ka + b.x * kb, y * ka + b.y * kb, z * ka + b.z * kb)
    }

    companion object {
        val IDENTITY = Quat(1f, 0f, 0f, 0f)

        /** Rotation by [radians] about unit [axis], counter-clockwise seen from the axis tip. */
        fun axisAngle(axis: V3, radians: Float): Quat {
            val h = radians / 2
            val s = sin(h)
            return Quat(cos(h), axis.x * s, axis.y * s, axis.z * s)
        }
    }
}
