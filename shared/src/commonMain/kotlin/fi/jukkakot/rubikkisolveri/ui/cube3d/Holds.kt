package fi.jukkakot.rubikkisolveri.ui.cube3d

import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Move
import fi.jukkakot.rubikkisolveri.cube.scan.Orientation
import fi.jukkakot.rubikkisolveri.cube.scan.Pose
import kotlin.math.sqrt

/** The whole-cube rotation of [moves] (rotations only) as a quaternion. */
fun rotationOf(moves: List<Move>): Quat = moves.fold(Quat.IDENTITY) { q, m ->
    (Quat.axisAngle(V3.of(m.layer.axis), CubeScene.moveAngle(m, 1f)) * q).normalized()
}

/** The 3D view that shows [view]'s face in front, tilted so three faces are visible. */
fun viewFor(view: FaceView): Quat = CubeScene.viewFor(rotationOf(view.hold))

/**
 * The whole-cube rotation that holds the cube as [pose]: its front face towards the viewer, its up
 * face on top (the rotation matrix's rows are the right, up and front faces' normals).
 */
fun holdFor(pose: Pose): Quat = fromRows(V3.of(pose.right.normal), V3.of(pose.up.normal), V3.of(pose.front.normal))

/** The whole-cube rotation of the real cube as the video scan measured it ([Orientation]'s rows likewise). */
fun holdFor(orientation: Orientation): Quat {
    val m = orientation.m.map { it.toFloat() }
    return fromRows(V3(m[0], m[1], m[2]), V3(m[3], m[4], m[5]), V3(m[6], m[7], m[8]))
}

/** The rotation whose matrix has rows [r], [u], [f] (where the viewer's right, up and front point in the cube). */
private fun fromRows(r: V3, u: V3, f: V3): Quat {
    val trace = r.x + u.y + f.z
    val q = when {
        trace > 0 -> {
            val s = sqrt(trace + 1f) * 2
            Quat(s / 4, (f.y - u.z) / s, (r.z - f.x) / s, (u.x - r.y) / s)
        }
        r.x > u.y && r.x > f.z -> {
            val s = sqrt(1f + r.x - u.y - f.z) * 2
            Quat((f.y - u.z) / s, s / 4, (r.y + u.x) / s, (r.z + f.x) / s)
        }
        u.y > f.z -> {
            val s = sqrt(1f + u.y - r.x - f.z) * 2
            Quat((r.z - f.x) / s, (r.y + u.x) / s, s / 4, (u.z + f.y) / s)
        }
        else -> {
            val s = sqrt(1f + f.z - r.x - u.y) * 2
            Quat((u.x - r.y) / s, (r.z + f.x) / s, (u.z + f.y) / s, s / 4)
        }
    }
    return q.normalized()
}
