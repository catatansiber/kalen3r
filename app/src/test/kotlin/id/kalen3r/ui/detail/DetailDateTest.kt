package id.kalen3r.ui.detail

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetailDateTest {
    @Test fun valid() = assertEquals(LocalDate.of(2026, 11, 24), parseDetailDate("2026-11-24"))
    @Test fun bounds() {
        assertEquals(LocalDate.of(2000, 1, 1), parseDetailDate("2000-01-01"))
        assertEquals(LocalDate.of(2100, 12, 31), parseDetailDate("2100-12-31"))
    }
    @Test fun invalid() {
        for (s in listOf("abc", "2026-13-40", "", "1999-12-31", "2101-01-01")) assertNull(parseDetailDate(s), s)
        assertNull(parseDetailDate(null))
    }
}
