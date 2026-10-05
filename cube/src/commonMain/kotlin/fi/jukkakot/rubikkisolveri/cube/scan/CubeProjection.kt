package fi.jukkakot.rubikkisolveri.cube.scan

import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.Vec3

/**
 * Where every sticker of the real cube lies in the picture: [points] (net order, frame pixels) of
 * the sticker centres, [step] the pixels of one sticker step, [facing] the sides turned towards
 * the camera enough to mark (see [FACING]). A weak-perspective view: correct near the face it was
 * built from, drifting on sides seen at a steep angle.
 */
data class CubeProjection(val points: List<Point>, val step: Double, val facing: Set<Face>) {

    companion object {
        /** A side faces the camera when its normal's share towards the viewer is at least this (about 70° off). */
        const val FACING = 0.3

        /**
         * The projection from the cube's [orientation] and the face [front] found at [centre] with
         * steps [u] and [v], which show the cube vectors [a] and [b] (see [Orientation.candidates]).
         * Null when the steps do not fit the orientation (degenerate).
         */
        fun of(orientation: Orientation, front: Face, centre: Point, u: Point, v: Point, a: Vec3, b: Vec3): CubeProjection? {
            fun screen(x: Double, y: Double, z: Double): Point = orientation.apply(x, y, z).let { (sx, sy, _) -> Point(sx, -sy) }
            val shown = screen(a.x.toDouble(), a.y.toDouble(), a.z.toDouble()).length + screen(b.x.toDouble(), b.y.toDouble(), b.z.toDouble()).length
            if (shown < 1e-6) return null
            val step = (u.length + v.length) / shown
            // Sticker centres in steps: the faces lie 1.5 steps from the cube's middle.
            val origin = front.normal
            val points = Stickers.all.map { s ->
                val offset = s.position + s.normal * -1
                fun along(get: (Vec3) -> Int) = 1.5 * (get(s.normal) - get(origin)) + get(offset)
                centre + screen(along { it.x }, along { it.y }, along { it.z }) * step
            }
            val facing = Face.entries.filter { f -> orientation.apply(f.normal.x.toDouble(), f.normal.y.toDouble(), f.normal.z.toDouble()).third >= FACING }.toSet()
            return CubeProjection(points, step, facing)
        }
    }
}
