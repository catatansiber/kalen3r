package id.kalen3r.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class SasihTest {
    private data class HariRaya(val date: LocalDate, val event: String, val source: String)

    private fun hariRaya(): List<HariRaya> =
        SasihTest::class.java.getResourceAsStream("/golden/golden2/hari-raya.csv")!!
            .bufferedReader().readLines().drop(1).filter { it.isNotBlank() }.map {
                val (d, e, s) = it.split(",")
                HariRaya(LocalDate.parse(d), e, s)
            }

    /**
     * Nyepi years whose independent sources disagree (golden/SOURCES.md, "Disagreements"): no assertion,
     * listed in the task-6 report instead. 2027: 03-08 vs 03-09.
     * Nyepi 2028 (2028-03-26) IS asserted although SOURCES.md lists it in the disagreements table: its rival
     * value there is "none" (only one value was found, from two sources), so no source contradicts it.
     */
    private val disputedNyepiYears = setOf(2027)

    @Test
    fun cakaYear2026Nov1Is1948() {
        assertEquals(1948, cakaYear(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun cakaYearChangesAfterNyepi() {
        val n = nyepi(2026)
        assertEquals(1947, cakaYear(n.minusDays(1)))
        assertEquals(1948, cakaYear(n))
        assertEquals(1948, cakaYear(n.plusDays(1)))
        // Golden: Nyepi 2024-03-11 itself carries the new year.
        assertEquals(1945, cakaYear(LocalDate.of(2024, 3, 10)))
        assertEquals(1946, cakaYear(LocalDate.of(2024, 3, 11)))
    }

    @Test
    fun cakaYearIsGregorianMinus78AfterNyepiAnd79Before() {
        for (y in 2024..2035) {
            val n = nyepi(y)
            assertEquals(y - 79, cakaYear(n.minusDays(1)), "$y day before Nyepi")
            assertEquals(y - 78, cakaYear(n), "$y Nyepi")
            assertEquals(y - 79, cakaYear(LocalDate.of(y, 1, 1)), "$y-01-01")
            assertEquals(y - 78, cakaYear(LocalDate.of(y, 12, 31)), "$y-12-31")
        }
    }

    @Test
    fun cakaMatchesGolden() {
        val bad = GoldenLoader.allDays().filter { cakaYear(it.date) != it.raw.getValue("caka").toInt() }
            .map { "${it.date} golden=${it.raw["caka"]} ours=${cakaYear(it.date)}" }
        assertTrue(bad.isEmpty(), "caka mismatches (${bad.size}):\n" + bad.joinToString("\n"))
    }

    @Test
    fun nyepiMatchesGolden2024To2035() {
        val rows = hariRaya().filter { it.event == "nyepi" }
        assertTrue(rows.isNotEmpty())
        val checked = rows.filter { it.date.year !in disputedNyepiYears }
        val bad = checked.filter { nyepi(it.date.year) != it.date }
            .map { "${it.date} (${it.source}) ours=${nyepi(it.date.year)}" }
        assertTrue(bad.isEmpty(), "Nyepi mismatches:\n" + bad.joinToString("\n"))
        // Golden days: Nyepi 2024 = first day of Kedasa on kalenderbali.com.
        assertEquals(LocalDate.of(2024, 3, 11), nyepi(2024))
        println("Nyepi checked against ${checked.size} rows (${checked.map { it.date.year }.toSortedSet()}); " +
            "disputed, not asserted: " + rows.filter { it.date.year in disputedNyepiYears }.map { "${it.date} ${it.source}" } +
            "; ours for disputed years: " + disputedNyepiYears.map { nyepi(it) })
    }

    @Test
    fun nyepiIsFirstDayOfKadasaAndDayAfterTilemKesanga() {
        for (y in 2024..2035) {
            val n = nyepi(y)
            assertEquals(SasihInfo(Sasih.KADASA, false), sasihOn(n), "$y")
            assertEquals(SasihInfo(Sasih.KANAWA, false), sasihOn(n.minusDays(1)), "$y")
            assertEquals(y, n.year)
        }
    }

    @Test
    fun sasihMatchesGolden() {
        val days = GoldenLoader.allDays()
        assertEquals(212, days.size)
        val bad = days.mapNotNull { d ->
            val want = GoldenLoader.sasih(d.raw.getValue("sasih"))
            val got = sasihOn(d.date)
            if (got == want) null else "${d.date} golden=${d.raw["sasih"]} ($want) ours=$got"
        }
        if (bad.isNotEmpty()) fail("sasih mismatches (${bad.size} of ${days.size}):\n" + bad.joinToString("\n"))
    }

    @Test
    fun sasihChangesOnlyAtNewMonthAndFollowsOrder() {
        var prev = sasihOn(LocalDate.of(2023, 12, 31))
        var d = LocalDate.of(2024, 1, 1)
        var changes = 0
        while (d.year <= 2035) {
            val cur = sasihOn(d)
            if (cur != prev) {
                changes++
                val expected = if (!prev.isNampih && cur.isNampih) prev.sasih else Sasih.entries[(prev.sasih.ordinal + 1) % 12]
                assertEquals(expected, cur.sasih, "$d: $prev -> $cur")
            }
            prev = cur; d = d.plusDays(1)
        }
        assertTrue(changes in 145..150, "month changes 2024..2035: $changes")
    }

    @Test
    fun nampihRuleSakaMod19() {
        // Saka 1946 (2024): nampih Sada (golden "Mala Sadha" 2024-06-07..). Saka 1949 (2027): nampih Desta.
        assertEquals(Sasih.SADA, nampihSasihOf(1946))
        assertEquals(Sasih.DESTA, nampihSasihOf(1949))
        assertEquals(null, nampihSasihOf(1947))
        assertEquals(null, nampihSasihOf(1948))
        // 7 nampih years in every 19.
        assertEquals(7, (1930 until 1949).count { nampihSasihOf(it) != null })
        // A Çaka year has 13 months exactly when it has a nampih sasih: Nyepi-to-Nyepi spans 383-385 days.
        for (y in 2024..2034) {
            val len = nyepi(y + 1).toEpochDay() - nyepi(y).toEpochDay()
            if (nampihSasihOf(y - 78) != null) assertTrue(len in 383..385, "$y len $len")
            else assertTrue(len in 353..356, "$y len $len")
        }
    }
}
