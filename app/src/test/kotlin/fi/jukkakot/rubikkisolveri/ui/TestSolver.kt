package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.ui.solve.Planner
import fi.jukkakot.rubikkisolveri.ui.solve.TargetPlanner
import fi.jukkakot.rubikkisolveri.ui.solve.plan
import fi.jukkakot.rubikkisolveri.ui.solve.planTarget

/** Plans on the calling thread, so screen tests do not depend on background threads. */
val INLINE_PLANNER: Planner = { cube, method -> plan(cube, method) }

/** [INLINE_PLANNER] for targets other than solved. */
val INLINE_TARGET_PLANNER: TargetPlanner = { cube, target -> planTarget(cube, target) }
