package id.kalen3r.core

import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {
    @Test
    fun pancawaraHasFiveValues() {
        assertEquals(5, Pancawara.values().size)
        assertEquals(6, Saptawara.SANISCARA.ordinal)
    }
}
