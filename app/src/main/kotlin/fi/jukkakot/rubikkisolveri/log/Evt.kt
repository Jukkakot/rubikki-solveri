package fi.jukkakot.rubikkisolveri.log

/** The fixed event catalogue: every log line names one of these, so logs can be filtered by name. */
enum class Evt(val id: String) {
    APP_START("app.start"),
    APP_CRASH("app.crash"),
    NAV_SCREEN("nav.screen"),
    SETTINGS_CHANGED("settings.changed"),
    LOG_SHARED("log.shared"),
    LOG_CLEARED("log.cleared"),
    SOLVER_READY("solver.ready"),
    SOLVE_DONE("solve.done"),
    SOLVE_FAILED("solve.failed"),
    SCAN_FACE("scan.face"),
    SCAN_DONE("scan.done"),
    SCAN_PERMISSION("scan.permission"),
    SCAN_ERROR("scan.error"),
    FOLLOW_EVENT("follow.event"),
}

enum class Level { DEBUG, INFO, WARN, ERROR }
