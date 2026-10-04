package fi.jukkakot.rubikkisolveri.cube.scan

import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.sqrt

/** A camera reading, 0..255 per channel. */
data class Rgb(val r: Int, val g: Int, val b: Int) {
    fun toHex(): String = listOf(r, g, b).joinToString("") { it.toString(16).padStart(2, '0') }

    fun toLab(): Lab {
        fun linear(c: Int): Double {
            val v = c / 255.0
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        val rl = linear(r)
        val gl = linear(g)
        val bl = linear(b)
        // sRGB (D65) to XYZ, normalised by the D65 white point.
        val x = (0.4124 * rl + 0.3576 * gl + 0.1805 * bl) / 0.95047
        val y = 0.2126 * rl + 0.7152 * gl + 0.0722 * bl
        val z = (0.0193 * rl + 0.1192 * gl + 0.9505 * bl) / 1.08883
        fun f(t: Double) = if (t > 216.0 / 24389) cbrt(t) else (24389.0 / 27 * t + 16) / 116
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)
        return Lab(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    companion object {
        fun fromHex(hex: String): Rgb = Rgb(hex.substring(0, 2).toInt(16), hex.substring(2, 4).toInt(16), hex.substring(4, 6).toInt(16))
    }
}

/** CIE L*a*b*: [l] lightness 0..100, [a] green–red, [b] blue–yellow. */
data class Lab(val l: Double, val a: Double, val b: Double) {
    /**
     * Colour difference with lightness weighted by [LIGHTNESS_WEIGHT]: lighting changes lightness
     * the most, while the hue is what tells cube colours apart.
     */
    fun distance(o: Lab): Double {
        val dl = (l - o.l) * LIGHTNESS_WEIGHT
        val da = a - o.a
        val db = b - o.b
        return sqrt(dl * dl + da * da + db * db)
    }

    companion object {
        const val LIGHTNESS_WEIGHT = 0.5

        fun mean(labs: List<Lab>): Lab =
            Lab(labs.sumOf { it.l } / labs.size, labs.sumOf { it.a } / labs.size, labs.sumOf { it.b } / labs.size)
    }
}
