package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.ScanEngine

/** [VideoScanTest] with the rules scanner (`scan-rules`). */
class RulesVideoScanTest : VideoScanTest() {
    override val engine = ScanEngine.RULES
}
