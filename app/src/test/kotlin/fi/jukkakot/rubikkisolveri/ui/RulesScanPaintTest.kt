package fi.jukkakot.rubikkisolveri.ui

import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine

/** [ScanPaintTest] with the rules scanner (`scan-rules`). */
class RulesScanPaintTest : ScanPaintTest() {
    override val engine = ScanEngine.RULES
}
