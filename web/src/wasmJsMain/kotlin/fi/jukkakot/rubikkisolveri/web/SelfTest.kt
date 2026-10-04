package fi.jukkakot.rubikkisolveri.web

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.Stickers
import fi.jukkakot.rubikkisolveri.cube.beginner.BeginnerSolver
import fi.jukkakot.rubikkisolveri.cube.scan.RotationSearch
import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver
import fi.jukkakot.rubikkisolveri.log.Evt
import fi.jukkakot.rubikkisolveri.ui.elapsedMillis

/** Fixed scrambles for the speed check (also used by the smoke test). */
private val SCRAMBLES = listOf(
    "R U R' U' F2 D L' B U2 R2 F' L D2 B' U",
    "F R2 B' U L2 D' F' R U2 L B2 D R' F U'",
    "L' D2 F U R' B2 U' L F2 D' R B U2 F' L2",
)

/**
 * `?selftest`: times the solver in this browser without the UI and prints
 * `SELFTEST ok warmup=… solve=… beginner=… rotation=…` (or `SELFTEST fail …`) to the console and the log.
 */
fun runSelfTest(services: WebServices) {
    val line = try {
        var t = elapsedMillis()
        TwoPhaseSolver.warmUp()
        val warmup = elapsedMillis() - t
        var slowest = 0L
        for (scramble in SCRAMBLES) {
            val cube = Cube.solved().apply(scramble)
            t = elapsedMillis()
            val result = TwoPhaseSolver.solve(cube)
            slowest = maxOf(slowest, elapsedMillis() - t)
            check(result is SolveResult.Solved && cube.apply(result.moves).isSolved) { "wrong solution for $scramble: $result" }
        }
        t = elapsedMillis()
        val start = Cube.solved().apply(SCRAMBLES[0])
        val steps = BeginnerSolver.solve(start)
        val beginner = elapsedMillis() - t
        check(start.apply(steps.moves).isSolved) { "beginner solver failed" }
        t = elapsedMillis()
        check(RotationSearch.search(List(Stickers.COUNT) { start[it] }).validity.isValid) { "rotation search failed" }
        val rotation = elapsedMillis() - t
        "SELFTEST ok warmup=$warmup solve=$slowest beginner=$beginner rotation=$rotation"
    } catch (e: Throwable) {
        "SELFTEST fail ${e.message}"
    }
    log("INFO", line)
    services.logger.info(Evt.SOLVER_READY, line)
}
