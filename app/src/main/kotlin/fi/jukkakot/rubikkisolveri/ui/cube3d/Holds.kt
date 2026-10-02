package fi.jukkakot.rubikkisolveri.ui.cube3d

import fi.jukkakot.rubikkisolveri.cube.FaceView
import fi.jukkakot.rubikkisolveri.cube.Move

/** The whole-cube rotation of [moves] (rotations only) as a quaternion. */
fun rotationOf(moves: List<Move>): Quat = moves.fold(Quat.IDENTITY) { q, m ->
    (Quat.axisAngle(V3.of(m.layer.axis), CubeScene.moveAngle(m, 1f)) * q).normalized()
}

/** The 3D view that shows [view]'s face in front, tilted so three faces are visible. */
fun viewFor(view: FaceView): Quat = CubeScene.viewFor(rotationOf(view.hold))
