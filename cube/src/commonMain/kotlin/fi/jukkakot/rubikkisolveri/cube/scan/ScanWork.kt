package fi.jukkakot.rubikkisolveri.cube.scan

/**
 * The scan's work as running totals (`scan-work-counters`), for tests and harnesses: the same on every machine,
 * unlike milliseconds. A caller samples them per picture. Never shown in the app.
 */
internal class ScanWork {
    /** Sticker colours named against the references. */
    var namings = 0L

    /** Nodes of the joint face assignment's branch and bound. */
    var nodes = 0L

    /** Best cubes worked out ([BestCube.solve], and [BestCube.cost] not found in the memo). */
    var solves = 0L

    /** Tracks the pictures' work went over, summed per picture worked out. */
    var tracks = 0L

    fun copy(): ScanWork = ScanWork().also { w ->
        w.namings = namings
        w.nodes = nodes
        w.solves = solves
        w.tracks = tracks
    }

    override fun toString(): String = "namings=$namings nodes=$nodes solves=$solves tracks=$tracks"
}
