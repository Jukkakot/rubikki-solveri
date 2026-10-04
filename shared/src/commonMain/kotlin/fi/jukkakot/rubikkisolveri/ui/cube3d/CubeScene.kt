package fi.jukkakot.rubikkisolveri.ui.cube3d

import fi.jukkakot.rubikkisolveri.cube.Face
import fi.jukkakot.rubikkisolveri.cube.Layer
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.Vec3
import kotlin.math.PI

/**
 * A flat four-cornered piece of the cube: a sticker ([sticker] = its index) or plastic (-1).
 * [cubie] is where its little cube is now, [home] where it is at rest (for highlighting).
 */
class Quad(val corners: List<V3>, val normal: V3, val sticker: Int, val cubie: V3, val home: Vec3)

/**
 * A quad on screen: its 2D corners (x right, y down), how much it faces the light, and whether it
 * is outside the highlighted layer.
 */
class ProjectedQuad(val xs: FloatArray, val ys: FloatArray, val sticker: Int, val light: Float, val dimmed: Boolean = false) {
    fun contains(px: Float, py: Float): Boolean {
        var sign = 0
        for (i in 0 until 4) {
            val j = (i + 1) % 4
            val cross = (xs[j] - xs[i]) * (py - ys[i]) - (ys[j] - ys[i]) * (px - xs[i])
            val s = if (cross > 0) 1 else if (cross < 0) -1 else 0
            if (s != 0) {
                if (sign == 0) sign = s else if (s != sign) return false
            }
        }
        return true
    }
}

/**
 * The cube as 26 little cubes ("cubies") of size 1 on the −1..1 grid, each with a dark body and
 * its stickers. Pure math: no drawing, so it is tested on the JVM.
 */
object CubeScene {
    const val CAMERA_DISTANCE = 9f
    private const val STICKER_HALF = 0.42f
    private const val STICKER_LIFT = 0.505f
    private val LIGHT = V3(-0.35f, 0.65f, 0.7f).normalized()

    /** The default view: looking at the top, front and right faces. */
    val DEFAULT_VIEW: Quat = viewFor(Quat.IDENTITY)

    /**
     * The mirror on the wall behind the cube, fixed in camera space (x right, y up, z towards the
     * camera): its centre above the cube, a little to the left and behind it (so the mirror shows the
     * back nearly square on), its glass half width and height, and the frame's width. Tunable after
     * a try on the phone.
     */
    val MIRROR_CENTRE = V3(-1f, 4.4f, -4f)
    private const val MIRROR_HALF_WIDTH = 2.1f
    private const val MIRROR_HALF_HEIGHT = 1.8f
    private const val MIRROR_FRAME = 0.25f

    /** How much darker the reflection is than the cube, so the two are told apart. */
    private const val MIRROR_SHADE = 0.85f

    /** The mirror's normal: the ray from the camera to the mirror's centre reflects to the cube's centre. */
    val MIRROR_NORMAL: V3 = run {
        val toCamera = (V3(0f, 0f, CAMERA_DISTANCE) - MIRROR_CENTRE).normalized()
        val toCube = (V3.ZERO - MIRROR_CENTRE).normalized()
        (toCamera + toCube).normalized()
    }

    /** Reflects camera-space point [p] in the mirror's plane. */
    fun reflect(p: V3): V3 = p - MIRROR_NORMAL * (2f * ((p - MIRROR_CENTRE) dot MIRROR_NORMAL))

    /** Reflects camera-space direction [v] in the mirror's plane. */
    fun reflectDirection(v: V3): V3 = v - MIRROR_NORMAL * (2f * (v dot MIRROR_NORMAL))

    /** The mirror's glass, or with [frame] the outer edge of its frame, as four camera-space corners. */
    fun mirrorCorners(frame: Boolean): List<V3> {
        val right = (V3(0f, 1f, 0f) cross MIRROR_NORMAL).normalized()
        val up = MIRROR_NORMAL cross right
        val extra = if (frame) MIRROR_FRAME else 0f
        return square(MIRROR_CENTRE, right * (MIRROR_HALF_WIDTH + extra), up * (MIRROR_HALF_HEIGHT + extra), 1f)
    }

    /** The mirror's glass or frame on screen: x and y of its four corners. */
    fun projectMirror(frame: Boolean, width: Float, height: Float): Pair<FloatArray, FloatArray> {
        val fit = fit(width, height, mirror = true)
        val corners = mirrorCorners(frame)
        return FloatArray(4) { fit.x(corners[it]) } to FloatArray(4) { fit.y(corners[it]) }
    }

    /** The default tilt applied after a hold rotation, so three faces are always visible. */
    fun viewFor(hold: Quat): Quat =
        Quat.axisAngle(V3(1f, 0f, 0f), (24 * PI / 180).toFloat()) *
            Quat.axisAngle(V3(0f, 1f, 0f), (-32 * PI / 180).toFloat()) * hold

    private val cubies: List<Vec3> = buildList {
        for (x in -1..1) for (y in -1..1) for (z in -1..1) if (x != 0 || y != 0 || z != 0) add(Vec3(x, y, z))
    }

    private val directions = listOf(
        Vec3(1, 0, 0), Vec3(-1, 0, 0), Vec3(0, 1, 0), Vec3(0, -1, 0), Vec3(0, 0, 1), Vec3(0, 0, -1),
    )

    private fun tangents(n: Vec3): Pair<V3, V3> {
        val u = if (n.x != 0) V3(0f, 1f, 0f) else V3(1f, 0f, 0f)
        val v = V3.of(n) cross u
        return u to v
    }

    private fun square(centre: V3, u: V3, v: V3, half: Float): List<V3> = listOf(
        centre + (u * -half) + (v * -half),
        centre + (u * half) + (v * -half),
        centre + (u * half) + (v * half),
        centre + (u * -half) + (v * half),
    )

    /** Base quads in world space, grouped per cubie: plastic body faces first, then stickers. */
    private val baseQuads: Map<Vec3, List<Quad>> = cubies.associateWith { cubie ->
        val centre = V3.of(cubie)
        val body = directions.map { n ->
            val (u, v) = tangents(n)
            Quad(square(centre + V3.of(n) * 0.5f, u, v, 0.5f), V3.of(n), -1, centre, cubie)
        }
        val stickers = Stickers.all.filter { it.position == cubie }.map { s ->
            val face: Face = s.face
            Quad(
                square(centre + V3.of(s.normal) * STICKER_LIFT, V3.of(face.right), V3.of(face.down), STICKER_HALF),
                V3.of(s.normal), s.index, centre, cubie,
            )
        }
        body + stickers
    }

    /** Signed angle (radians, counter-clockwise from the axis tip) of [move] at [progress] 0..1. */
    fun moveAngle(move: Move, progress: Float): Float {
        val signedTurns = if (move.quarterTurns == 3) -1 else move.quarterTurns
        return -signedTurns * (PI / 2).toFloat() * progress
    }

    /** All quads in world space with the layers of [move] turned by [progress] of the move. */
    fun quads(move: Move?, progress: Float): List<Quad> {
        if (move == null || progress == 0f) return baseQuads.values.flatten()
        val rotation = Quat.axisAngle(V3.of(move.layer.axis), moveAngle(move, progress))
        return baseQuads.flatMap { (cubie, quads) ->
            if (!move.layer.turns(cubie)) {
                quads
            } else {
                quads.map { q ->
                    Quad(q.corners.map(rotation::rotate), rotation.rotate(q.normal), q.sticker, rotation.rotate(q.cubie), q.home)
                }
            }
        }
    }

    // The farthest any cube point can project from the centre is 0.302·scale (a point at the
    // cube's corner radius 2.6, seen at the worst angle); this keeps it inside 85 % of the half size.
    private const val CUBE_REACH = 0.302f

    /** Scale and screen centre of the projection. */
    class Fit(val scale: Float, val cx: Float, val cy: Float) {
        fun x(c: V3) = cx + c.x * scale / (CAMERA_DISTANCE - c.z)
        fun y(c: V3) = cy - c.y * scale / (CAMERA_DISTANCE - c.z)
    }

    /**
     * The projection for a [width]×[height] canvas: the cube alone, or with [mirror] the cube and
     * the mirror together. It never depends on the view, so dragging does not zoom.
     */
    fun fit(width: Float, height: Float, mirror: Boolean = false): Fit {
        if (!mirror) return Fit(minOf(width, height) / 2f * 0.85f / CUBE_REACH, width / 2f, height / 2f)
        var left = -CUBE_REACH
        var right = CUBE_REACH
        var bottom = -CUBE_REACH
        var top = CUBE_REACH
        for (c in mirrorCorners(frame = true)) {
            val k = 1f / (CAMERA_DISTANCE - c.z)
            left = minOf(left, c.x * k)
            right = maxOf(right, c.x * k)
            bottom = minOf(bottom, c.y * k)
            top = maxOf(top, c.y * k)
        }
        val scale = minOf(width / (right - left), height / (top - bottom)) * 0.95f
        return Fit(scale, width / 2f - (left + right) / 2f * scale, height / 2f + (top + bottom) / 2f * scale)
    }

    /**
     * Screen position (x right, y down) of world point [p] seen through [view]; [reflected] gives
     * its image in the mirror.
     */
    fun projectPoint(p: V3, view: Quat, width: Float, height: Float, mirror: Boolean = false, reflected: Boolean = false): Pair<Float, Float> {
        val fit = fit(width, height, mirror || reflected)
        val c = view.rotate(p).let { if (reflected) reflect(it) else it }
        return fit.x(c) to fit.y(c)
    }

    /**
     * Projects [quads] seen through [view] onto a canvas of [width]×[height], dropping faces that
     * look away from the camera, in drawing order (far cubies first). Quads outside [highlight]'s
     * layers are marked dimmed. [mirror] fits the projection to the cube and the mirror;
     * [reflected] projects the cube's image in the mirror instead (a little darker).
     */
    fun project(
        quads: List<Quad>, view: Quat, width: Float, height: Float, highlight: Move? = null,
        mirror: Boolean = false, reflected: Boolean = false,
    ): List<ProjectedQuad> {
        val camera = V3(0f, 0f, CAMERA_DISTANCE)
        val fit = fit(width, height, mirror || reflected)
        val visible = ArrayList<Triple<Float, Int, ProjectedQuad>>(quads.size)
        for ((order, q) in quads.withIndex()) {
            var normal = view.rotate(q.normal)
            var corners = q.corners.map(view::rotate)
            var cubie = view.rotate(q.cubie)
            if (reflected) {
                // A reflection flips handedness: reversing the corners keeps their winding on screen.
                normal = reflectDirection(normal)
                corners = corners.map(::reflect).asReversed()
                cubie = reflect(cubie)
            }
            val mid = (corners[0] + corners[2]) * 0.5f
            if ((normal dot (camera - mid)) <= 0f) continue
            val xs = FloatArray(4) { fit.x(corners[it]) }
            val ys = FloatArray(4) { fit.y(corners[it]) }
            val light = (0.8f + 0.2f * maxOf(0f, normal dot LIGHT)) * (if (reflected) MIRROR_SHADE else 1f)
            val depth = (camera - cubie).length
            val dimmed = highlight != null && !highlight.layer.turns(q.home)
            visible += Triple(depth, order, ProjectedQuad(xs, ys, q.sticker, light, dimmed))
        }
        // Far cubies first; inside a cubie keep the body before its stickers.
        visible.sortWith(compareByDescending<Triple<Float, Int, ProjectedQuad>> { it.first }.thenBy { it.second })
        return visible.map { it.third }
    }

    /**
     * The direction arrow for [move] seen through [view], in world space: an arc in the plane of the
     * turning face just above the stickers, its middle on the side facing the camera, traced in the
     * move's turning direction over its full angle (a quarter or half circle). With [reflected] the
     * middle is on the side the mirror shows.
     */
    fun arrow(move: Move, view: Quat, reflected: Boolean = false, segments: Int = 24): List<V3> {
        val axis = V3.of(move.layer.axis)
        val (offset, radius) = when (move.layer.kind) {
            Layer.Kind.FACE, Layer.Kind.WIDE -> 1.56f to 1.05f
            else -> 0f to 2.0f
        }
        val centre = axis * offset
        val inverse = Quat(view.w, -view.x, -view.y, -view.z)
        // The mirror shows the cube as seen from the camera's image behind the glass.
        val seenFrom = if (reflected) reflect(V3(0f, 0f, CAMERA_DISTANCE)).normalized() else V3(0f, 0f, 1f)
        val towardsCamera = inverse.rotate(seenFrom)
        var u = towardsCamera - axis * (axis dot towardsCamera)
        if (u.length < 1e-3f) u = if (axis.y != 0f) V3(0f, 0f, 1f) else V3(0f, 1f, 0f)
        u = u.normalized()
        val v = axis cross u
        val sweep = moveAngle(move, 1f)
        return (0..segments).map { k ->
            val theta = -sweep / 2 + sweep * k / segments
            centre + (u * kotlin.math.cos(theta) + v * kotlin.math.sin(theta)) * radius
        }
    }

    /** The sticker under the point, looking from the front (nearest first), or null. */
    fun hitTest(projected: List<ProjectedQuad>, x: Float, y: Float): Int? =
        projected.asReversed().firstOrNull { it.contains(x, y) }?.let { if (it.sticker >= 0) it.sticker else null }
}
