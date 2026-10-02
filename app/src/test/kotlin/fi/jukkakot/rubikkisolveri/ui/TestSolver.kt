package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.ui.solve.Planner
import fi.jukkakot.rubikkisolveri.ui.solve.plan

/** Plans on the calling thread, so screen tests do not depend on background threads. */
val INLINE_PLANNER: Planner = { cube, method -> plan(cube, method) }
