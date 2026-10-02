package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.Cube
import fi.jukkakot.rubikkisolveri.cube.solve.SolveResult
import fi.jukkakot.rubikkisolveri.cube.solve.TwoPhaseSolver

/** Solves on the calling thread, so screen tests do not depend on background threads. */
val INLINE_SOLVER: suspend (Cube) -> SolveResult = { TwoPhaseSolver.solve(it) }
