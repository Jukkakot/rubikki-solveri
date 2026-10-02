package fi.jukkakot.rubikkisolveri.cube

import kotlin.test.Test
import kotlin.test.assertEquals

class CubeModuleTest {
    @Test
    fun moduleIsOnTheClasspath() {
        assertEquals("cube", CubeModule.NAME)
    }
}
