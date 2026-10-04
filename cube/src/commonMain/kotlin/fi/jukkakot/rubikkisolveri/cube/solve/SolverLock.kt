package fi.jukkakot.rubikkisolveri.cube.solve

/**
 * Runs [block] holding [lock] where threads exist (the JVM: the app warms the solver up on one
 * thread and solves on another); the browser has a single thread.
 */
internal expect inline fun <T> withSolverLock(lock: Any, block: () -> T): T
