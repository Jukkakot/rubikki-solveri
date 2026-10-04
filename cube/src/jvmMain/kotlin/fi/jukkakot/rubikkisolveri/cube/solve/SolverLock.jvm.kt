package fi.jukkakot.rubikkisolveri.cube.solve

internal actual inline fun <T> withSolverLock(lock: Any, block: () -> T): T = synchronized(lock, block)
