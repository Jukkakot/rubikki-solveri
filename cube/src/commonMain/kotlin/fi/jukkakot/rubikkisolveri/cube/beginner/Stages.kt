package fi.jukkakot.rubikkisolveri.cube.beginner

import fi.jukkakot.rubikkisolveri.cube.Corner
import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.CubeColor
import fi.jukkakot.rubikkisolveri.cube.Edge
import fi.jukkakot.rubikkisolveri.cube.Face

/** The seven stages of the beginner method, in order. */
enum class Stage { WHITE_CROSS, WHITE_CORNERS, MIDDLE_LAYER, YELLOW_CROSS, YELLOW_EDGES, YELLOW_CORNERS_PLACED, YELLOW_CORNERS_TURNED }

/** Checks on a cube held any way: pieces are judged against the centres around them. */
object Checks {
    fun faceOf(cube: Cube, color: CubeColor): Face = Face.entries.first { cube.centre(it) == color }

    fun edgeSolved(cube: Cube, edge: Edge): Boolean =
        edge.faces.indices.all { cube[edge.stickers[it]] == cube.centre(edge.faces[it]) }

    fun cornerSolved(cube: Cube, corner: Corner): Boolean =
        corner.faces.indices.all { cube[corner.stickers[it]] == cube.centre(corner.faces[it]) }

    /** The corner holds the right three colours, however it is twisted. */
    fun cornerPlaced(cube: Cube, corner: Corner): Boolean =
        corner.stickers.map { cube[it] }.toSet() == corner.faces.map { cube.centre(it) }.toSet()

    fun edgesAround(face: Face): List<Edge> = Edge.entries.filter { face in it.faces }
    fun cornersAround(face: Face): List<Corner> = Corner.entries.filter { face in it.faces }

    fun whiteFace(cube: Cube) = faceOf(cube, CubeColor.WHITE)
    fun yellowFace(cube: Cube) = faceOf(cube, CubeColor.YELLOW)

    fun crossDone(cube: Cube) = edgesAround(whiteFace(cube)).all { edgeSolved(cube, it) }

    fun solvedWhiteCorners(cube: Cube) = cornersAround(whiteFace(cube)).count { cornerSolved(cube, it) }

    fun firstLayerDone(cube: Cube) = crossDone(cube) && solvedWhiteCorners(cube) == 4

    /** Middle-layer edges: those touching neither the white nor the yellow face. */
    fun middleEdges(cube: Cube): List<Edge> {
        val w = whiteFace(cube)
        val y = yellowFace(cube)
        return Edge.entries.filter { w !in it.faces && y !in it.faces }
    }

    fun solvedMiddleEdges(cube: Cube) = middleEdges(cube).count { edgeSolved(cube, it) }

    fun twoLayersDone(cube: Cube) = firstLayerDone(cube) && solvedMiddleEdges(cube) == 4

    /** Yellow stickers on the yellow face among the four edges. */
    fun yellowEdgesUp(cube: Cube): Int {
        val y = yellowFace(cube)
        return edgesAround(y).count { e -> cube[e.stickers[e.faces.indexOf(y)]] == CubeColor.YELLOW }
    }

    fun yellowCornersUp(cube: Cube): Int {
        val y = yellowFace(cube)
        return cornersAround(y).count { c -> cube[c.stickers[c.faces.indexOf(y)]] == CubeColor.YELLOW }
    }

    fun yellowCornersPlaced(cube: Cube) = cornersAround(yellowFace(cube)).count { cornerPlaced(cube, it) }

    fun yellowEdgesSolved(cube: Cube) = edgesAround(yellowFace(cube)).count { edgeSolved(cube, it) }

    fun stageDone(cube: Cube, stage: Stage): Boolean = when (stage) {
        Stage.WHITE_CROSS -> crossDone(cube)
        Stage.WHITE_CORNERS -> firstLayerDone(cube)
        Stage.MIDDLE_LAYER -> twoLayersDone(cube)
        Stage.YELLOW_CROSS -> twoLayersDone(cube) && yellowEdgesUp(cube) == 4
        Stage.YELLOW_EDGES -> twoLayersDone(cube) && yellowEdgesSolved(cube) == 4
        Stage.YELLOW_CORNERS_PLACED -> stageDone(cube, Stage.YELLOW_EDGES) && yellowCornersPlaced(cube) == 4
        Stage.YELLOW_CORNERS_TURNED -> cube.isSolved
    }
}
