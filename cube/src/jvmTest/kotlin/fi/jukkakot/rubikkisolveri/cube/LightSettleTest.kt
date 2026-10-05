package fi.jukkakot.rubikkisolveri.cube

import fi.jukkakot.rubikkisolveri.cube.scan.LightSettle
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LightSettleTest {
    @Test
    fun framesInTheSettlingSecondAreNotRead() {
        val settle = LightSettle()
        assertFalse(settle.settling(0), "nothing changed yet")
        settle.start(1_000)
        assertTrue(settle.settling(1_000))
        assertTrue(settle.settling(1_900))
        assertFalse(settle.settling(2_000), "settled after a second")
        assertFalse(settle.settling(2_100))
        // Turned off again: another second.
        settle.start(3_000)
        assertTrue(settle.settling(3_500))
    }
}
