package id.kalen3r

import id.kalen3r.ui.AppRoutes
import kotlin.test.Test
import kotlin.test.assertEquals

class AppRoutesTest {
    @Test
    fun appNavHasFourTabs() {
        assertEquals(listOf("kalender", "cari", "alarm", "donasi"), AppRoutes.tabs.map { it.route })
        assertEquals("detail/{date}", AppRoutes.DETAIL)
        assertEquals("detail/2026-11-24", AppRoutes.detail("2026-11-24"))
    }
}
