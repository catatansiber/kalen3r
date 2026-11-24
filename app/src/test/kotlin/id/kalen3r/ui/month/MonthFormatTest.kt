package id.kalen3r.ui.month

import id.kalen3r.core.BaliCalendar
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthFormatTest {
    @Test fun longDate() = assertEquals("Selasa, 24 November 2026", MonthFormat.longDate(LocalDate.of(2026, 11, 24)))
    @Test fun longDateSunday() = assertEquals("Minggu, 1 Maret 2026", MonthFormat.longDate(LocalDate.of(2026, 3, 1)))
    @Test fun monthTitle() = assertEquals("November 2026", MonthFormat.monthTitle(2026, 11))
    @Test fun shortDay() = assertEquals("24 Nov", MonthFormat.shortDay(LocalDate.of(2026, 11, 24)))

    @Test fun sasihSubtitleRange() {
        val days = BaliCalendar.default.month(2026, 11)
        val first = days.first().sasih; val last = days.last().sasih
        val expected = if (first == last) first.name else "${first.name}–${last.name}"
        val s = MonthFormat.sasihSubtitle(days)
        assertEquals("Çaka ${days.first().cakaYear} · Sasih " + expected.split("–").joinToString("–") { it.lowercase().replaceFirstChar(Char::uppercase) }, s)
    }

    @Test fun sasihNameIndonesian() {
        assertEquals("Kanem", MonthFormat.sasihName(id.kalen3r.core.Sasih.KANEM))
        assertEquals("Kadasa", MonthFormat.sasihName(id.kalen3r.core.Sasih.KADASA))
    }
}
