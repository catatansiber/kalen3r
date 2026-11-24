package id.kalen3r.core

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaliCalendarTest {
    private val cal: BaliCalendar = BaliCalendar.default
    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun november2026Has30DaysAndStartsSunday() {
        val m = cal.month(2026, 11)
        assertEquals(30, m.size)
        assertEquals(DayOfWeek.SUNDAY, m.first().date.dayOfWeek)
        assertEquals(Saptawara.REDITE, m.first().saptawara)
        assertEquals(1, m.first().wuku)
        assertEquals(Pawukon.WUKU_NAMES[0], "Sinta")
        assertEquals(d("2026-11-30"), m.last().date)
    }

    @Test
    fun day2026Nov24IsPurnamaKajengKliwonAnggaraKasih() {
        val day = cal.day(d("2026-11-24"))
        assertTrue(day.isPurnama)
        assertFalse(day.isTilem)
        assertEquals(Tithi(TithiType.PENANGGAL, 15), day.tithi)
        assertEquals(Sasih.KANEM, day.sasih)
        assertFalse(day.isNampih)
        val kinds = day.rerainan.map { it.kind }.toSet()
        assertTrue(RerainanKind.PURNAMA in kinds)
        assertTrue(RerainanKind.KAJENG_KLIWON in kinds)
        assertTrue(RerainanKind.ANGGARA_KASIH in kinds)
        assertEquals(Saptawara.ANGGARA, day.saptawara)
        assertEquals(Pancawara.KLIWON, day.pancawara)
    }

    @Test
    fun day2026Nov09IsTilemAndKajengKliwon() {
        val day = cal.day(d("2026-11-09"))
        assertTrue(day.isTilem)
        assertEquals(Tithi(TithiType.PANGELONG, 15), day.tithi)
        assertEquals(Sasih.KALIMA, day.sasih)
        assertTrue(day.rerainan.any { it.kind == RerainanKind.KAJENG_KLIWON })
        assertEquals(Tithi(TithiType.PENANGGAL, 1), cal.day(d("2026-11-10")).tithi)
        assertEquals(Sasih.KANEM, cal.day(d("2026-11-10")).sasih)
    }

    @Test
    fun dayMatchesPawukonEngine() {
        val date = d("2026-11-24")
        val p = Pawukon.of(date)
        val day = cal.day(date)
        assertEquals(p.index, day.index)
        assertEquals(p.wuku, day.wuku)
        assertEquals(p.urip, day.urip)
        assertEquals(p.dasawara, day.dasawara)
        assertEquals(cakaYear(date), day.cakaYear)
    }

    @Test
    fun verifiedFalseOutsideRange() {
        assertFalse(cal.day(d("2036-01-01")).verified)
        assertFalse(cal.day(d("2023-12-31")).verified)
    }

    @Test
    fun verifiedTrueAtEdges() {
        assertTrue(cal.day(d("2024-01-01")).verified)
        assertTrue(cal.day(d("2035-12-31")).verified)
    }

    @Test
    fun extremeYearsDoNotThrow() {
        cal.day(d("2000-01-01"))
        cal.day(d("2100-12-31"))
        assertEquals(31, cal.month(2000, 1).size)
        assertEquals(31, cal.month(2100, 12).size)
    }

    @Test
    fun everyMonth2000To2100DoesNotThrow() {
        val t0 = System.nanoTime()
        var days = 0
        for (y in 2000..2100) for (m in 1..12) days += cal.month(y, m).size
        val ms = (System.nanoTime() - t0) / 1_000_000
        println("month() loop 2000..2100: $days days in $ms ms")
        assertEquals(LocalDate.of(2000, 1, 1).datesUntil(LocalDate.of(2101, 1, 1)).count().toInt(), days)
    }

    @Test
    fun cakaYearMatchesEngineAroundNyepi() {
        for (y in listOf(2024, 2026, 2027, 2035)) {
            val n = nyepi(y)
            for (off in -2L..2L) assertEquals(cakaYear(n.plusDays(off)), cal.day(n.plusDays(off)).cakaYear, "${n.plusDays(off)}")
            assertEquals(cakaYear(LocalDate.of(y, 1, 1)), cal.day(LocalDate.of(y, 1, 1)).cakaYear)
            assertEquals(cakaYear(LocalDate.of(y, 12, 31)), cal.day(LocalDate.of(y, 12, 31)).cakaYear)
        }
    }

    @Test
    fun monthDaysEqualSingleDays() {
        for (day in cal.month(2026, 3)) assertEquals(DefaultBaliCalendar().day(day.date), day)
    }

    @Test
    fun monthRejectsInvalidMonth() {
        assertFailsWith<IllegalArgumentException> { cal.month(2026, 0) }
        assertFailsWith<IllegalArgumentException> { cal.month(2026, 13) }
    }

    @Test
    fun monthTwiceReturnsEqualLists() {
        assertEquals(cal.month(2026, 11), cal.month(2026, 11))
    }

    @Test
    fun cacheIsBoundedAndStillCorrect() {
        val fresh = DefaultBaliCalendar()
        val first = fresh.month(2026, 11)
        for (y in 2000..2025) for (m in 1..12) fresh.month(y, m) // 312 entries > cap, evicts 2026-11
        assertEquals(first, fresh.month(2026, 11))
    }

    @Test
    fun nyepiYearCakaBoundary() {
        val nyepi = nyepi(2026)
        assertEquals(2026 - 79, cal.day(nyepi.minusDays(1)).cakaYear)
        assertEquals(2026 - 78, cal.day(nyepi).cakaYear)
        assertEquals(Sasih.KADASA, cal.day(nyepi).sasih)
        assertTrue(cal.day(nyepi).rerainan.any { it.kind == RerainanKind.NYEPI })
    }

    @Test
    fun odalanBySasihReturnsOneOrTwoDatesPerYear() {
        // Purnama Kanem: Kanem is nampih in no year here -> exactly one date in 2026.
        val kanem = cal.odalanBySasih(Sasih.KANEM, Tithi(TithiType.PENANGGAL, 15), 2026)
        assertEquals(listOf(d("2026-11-24")), kanem)

        // Çaka 1946 (2024) has nampih Sada; Purnama Sada occurs twice in the Gregorian year 2024.
        val sada2024 = cal.odalanBySasih(Sasih.SADA, Tithi(TithiType.PENANGGAL, 15), 2024)
        assertTrue(sada2024.size in 1..2, "got $sada2024")
        // Every returned date really has that sasih and tithi, in ascending order.
        for (date in sada2024) {
            val day = cal.day(date)
            assertEquals(Sasih.SADA, day.sasih)
            assertEquals(Tithi(TithiType.PENANGGAL, 15), day.tithi)
            assertEquals(2024, date.year)
        }
        assertEquals(sada2024, sada2024.sorted())

        // Sampled sasih/tithi combos: always 0..2 dates, and 2 only when some day of that year is nampih in that sasih.
        for (y in listOf(2024, 2025, 2026, 2027, 2030)) {
            val nampihSasih = (0 until 366).mapNotNull { off ->
                val dt = LocalDate.of(y, 1, 1).plusDays(off.toLong())
                if (dt.year == y) cal.day(dt).takeIf { it.isNampih }?.sasih else null
            }.toSet()
            for (s in Sasih.entries) {
                val r = cal.odalanBySasih(s, Tithi(TithiType.PENANGGAL, 15), y)
                assertTrue(r.size <= 2, "$s $y -> $r")
                // Two dates happen when the sasih is nampih that year, or when a ~354-day lunar year lets a
                // sasih fall near both ends of the Gregorian year (e.g. Kapitu in January and December).
                if (r.size == 2) {
                    val gap = java.time.temporal.ChronoUnit.DAYS.between(r[0], r[1])
                    assertTrue(s in nampihSasih || gap > 300, "$s $y two dates $r neither nampih nor year-apart")
                }
            }
        }
    }

    @Test
    fun odalanBySasihTwoWhenNampihBothOccurrences() {
        // Find a (year, sasih) where nampih appears and verify both occurrences are returned.
        val y = 2024
        val nampihDays = LocalDate.of(y, 1, 1).datesUntil(LocalDate.of(y + 1, 1, 1)).filter { cal.day(it).isNampih }.toList()
        assertTrue(nampihDays.isNotEmpty())
        val s = cal.day(nampihDays.first()).sasih
        val tithi = cal.day(nampihDays.first()).tithi
        val r = cal.odalanBySasih(s, tithi, y)
        assertTrue(nampihDays.first() in r)
    }

    @Test
    fun rerainanRangeIsAscendingAndFiltered() {
        val kinds = setOf(RerainanKind.PURNAMA, RerainanKind.TILEM)
        val r = cal.rerainan(d("2026-01-01"), d("2026-12-31"), kinds)
        assertTrue(r.isNotEmpty())
        assertTrue(r.all { it.kind in kinds })
        assertEquals(r.map { it.date }, r.map { it.date }.sorted())
        assertTrue(r.any { it.date == d("2026-11-24") && it.kind == RerainanKind.PURNAMA })
        assertTrue(r.any { it.date == d("2026-11-09") && it.kind == RerainanKind.TILEM })
        assertEquals(emptyList(), cal.rerainan(d("2026-12-31"), d("2026-01-01"), kinds))
        assertEquals(emptyList(), cal.rerainan(d("2026-01-01"), d("2026-12-31"), emptySet()))
    }

    @Test
    fun delegatesPawukonSearch() {
        assertEquals(otonan(d("2000-01-01"), 3, d("2026-11-24")), cal.otonan(d("2000-01-01"), 3, d("2026-11-24")))
        assertEquals(
            findByWewaran(11, Saptawara.BUDA, Pancawara.KLIWON, d("2026-01-01"), 2),
            cal.findByWewaran(11, Saptawara.BUDA, Pancawara.KLIWON, d("2026-01-01"), 2),
        )
        assertEquals(
            odalanByPawukon(11, Saptawara.BUDA, Pancawara.KLIWON, d("2026-01-01"), d("2026-12-31")),
            cal.odalanByPawukon(11, Saptawara.BUDA, Pancawara.KLIWON, d("2026-01-01"), d("2026-12-31")),
        )
    }
}
