package id.kalen3r.ui.detail

import id.kalen3r.core.BaliCalendar
import id.kalen3r.core.TithiType
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DetailFormatTest {
    private val cal = BaliCalendar.default
    private fun day(s: String) = cal.day(LocalDate.parse(s))

    @Test fun nov1Wewaran() {
        val d = day("2026-11-01")
        assertEquals("5+9 = 14", DetailFormat.urip(d))
        assertEquals("-", DetailFormat.ekawara(d))
        assertEquals("Menga", DetailFormat.dwiwara(d))
        assertEquals("Sri", DetailFormat.caturwara(d))
        assertEquals("Tungleh", DetailFormat.sadwara(d))
        assertEquals("Sri", DetailFormat.astawara(d))
        assertEquals("Dangu", DetailFormat.sangawara(d))
        assertEquals("Sri", DetailFormat.dasawara(d))
        assertEquals(TithiType.PANGELONG, DetailFormat.tithi(d).type)
        assertEquals(7, DetailFormat.tithi(d).number)
        assertNull(DetailFormat.tithi(d).special)
    }

    @Test fun nov2Wewaran() {
        val d = day("2026-11-02")
        assertEquals("4+7 = 11", DetailFormat.urip(d))
        assertEquals("Luang", DetailFormat.ekawara(d))
        assertEquals("Pepet", DetailFormat.dwiwara(d))
        assertEquals("Laba", DetailFormat.caturwara(d))
        assertEquals("Indra", DetailFormat.astawara(d))
        assertEquals("Dangu", DetailFormat.sangawara(d))
        assertEquals("Pati", DetailFormat.dasawara(d))
        assertEquals(TithiType.PANGELONG, DetailFormat.tithi(d).type)
        assertEquals(8, DetailFormat.tithi(d).number)
    }

    @Test fun ekawaraDwiwaraFollowUripParityEveryDay() {
        var d = LocalDate.of(2026, 11, 1)
        repeat(420) {
            val day = cal.day(d)
            val odd = day.urip % 2 == 1
            assertEquals(if (odd) "Luang" else "-", DetailFormat.ekawara(day), "$d")
            assertEquals(if (odd) "Pepet" else "Menga", DetailFormat.dwiwara(day), "$d")
            d = d.plusDays(1)
        }
    }

    @Test fun purnamaAndTilemGetSpecialSuffix() {
        val p = DetailFormat.tithi(day("2026-11-24"))
        assertEquals(TithiType.PENANGGAL, p.type); assertEquals(15, p.number)
        assertEquals(TithiSpecial.PURNAMA, p.special)
        val t = DetailFormat.tithi(day("2026-11-09"))
        assertEquals(TithiType.PANGELONG, t.type)
        assertEquals(TithiSpecial.TILEM, t.special)
    }

    @Test fun everyDayOfNovember2026HasOneTithi() {
        for (d in cal.month(2026, 11)) {
            val t = DetailFormat.tithi(d)
            assertEquals(d.tithi.number, t.number)
            assertEquals(d.tithi.type, t.type)
        }
    }
}
