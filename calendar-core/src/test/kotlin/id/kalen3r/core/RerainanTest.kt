package id.kalen3r.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class RerainanTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun names(date: String) = rerainanOn(d(date)).map { it.name }
    private fun on(date: LocalDate, kind: RerainanKind) = rerainanOn(date).filter { it.kind == kind }

    private fun allDays(from: LocalDate, to: LocalDate) = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }

    // ---- Brief's named tests -------------------------------------------------------------

    @Test
    fun galunganIsBudaKliwonDungulan() {
        val galungan = allDays(d("2024-01-01"), d("2035-12-31")).filter { "Hari Raya Galungan" in rerainanOn(it).map { r -> r.name } }.toList()
        assertTrue(galungan.isNotEmpty())
        for (g in galungan) {
            val p = Pawukon.of(g)
            assertEquals(11, p.wuku)
            assertEquals(Saptawara.BUDA, p.saptawara)
            assertEquals(Pancawara.KLIWON, p.pancawara)
        }
        // Every Buda Kliwon Dungulan is a Galungan, 210 days apart.
        assertEquals(galungan.size, allDays(d("2024-01-01"), d("2035-12-31")).count {
            Pawukon.of(it).let { p -> p.wuku == 11 && p.saptawara == Saptawara.BUDA && p.pancawara == Pancawara.KLIWON }
        })
        galungan.zipWithNext().forEach { (a, b) -> assertEquals(210L, b.toEpochDay() - a.toEpochDay()) }
    }

    @Test
    fun kuninganIs10DaysAfterGalungan() {
        val galungan = allDays(d("2024-01-01"), d("2035-12-31")).filter { "Hari Raya Galungan" in names(it.toString()) }.toList()
        for (g in galungan) assertTrue("Hari Raya Kuningan" in rerainanOn(g.plusDays(10)).map { it.name }, "Kuningan after $g")
        val kuningan = allDays(d("2024-01-01"), d("2035-12-31")).filter { "Hari Raya Kuningan" in names(it.toString()) }.toList()
        assertEquals(galungan.size, kuningan.size)
    }

    @Test
    fun kajengKliwonHas15DayStep() {
        val kk = allDays(d("2024-01-01"), d("2030-12-31")).filter { on(it, RerainanKind.KAJENG_KLIWON).isNotEmpty() }.toList()
        // triwara (3) and pancawara (5) align every 15 days.
        kk.zipWithNext().forEach { (a, b) -> assertEquals(15L, b.toEpochDay() - a.toEpochDay()) }
        kk.forEach { assertEquals(Pancawara.KLIWON, Pawukon.of(it).pancawara) }
    }

    @Test
    fun pagerwesiIsBudaKliwonSinta() {
        val p = allDays(d("2024-01-01"), d("2035-12-31")).filter { "Hari Raya Pagerwesi" in names(it.toString()) }.toList()
        assertTrue(p.isNotEmpty())
        for (x in p) Pawukon.of(x).let {
            assertEquals(1, it.wuku); assertEquals(Saptawara.BUDA, it.saptawara); assertEquals(Pancawara.KLIWON, it.pancawara)
        }
        assertEquals(listOf(d("2026-11-04")), p.filter { it.year == 2026 && it.monthValue == 11 })
    }

    // ---- Rule-only hand-derived tests (not covered by golden) ----------------------------

    @Test
    fun saraswatiIsSaniscaraUmanisWatugunungAndBanyuPinaruhFollows() {
        // Known published dates (hari-raya.csv): 2025-02-08, 2025-09-06, 2026-04-04, 2026-10-31.
        for (s in listOf("2025-02-08", "2025-09-06", "2026-04-04", "2026-10-31", "2024-07-13")) {
            assertTrue("Hari Raya Saraswati" in names(s), "Saraswati $s")
            assertTrue("Banyu Pinaruh" in names(d(s).plusDays(1).toString()))
            val p = Pawukon.of(d(s))
            assertEquals(30, p.wuku); assertEquals(Saptawara.SANISCARA, p.saptawara); assertEquals(Pancawara.UMANIS, p.pancawara)
        }
        // Saraswati 2026-10-31 -> Banyu Pinaruh, Soma Ribek, Sabuh Mas, Pagerwesi 2026-11-01..04 (Sabuh Mas = Anggara Wage Sinta)
        assertEquals(listOf("Soma Ribek"), names("2026-11-02"))
        assertEquals(listOf("Sabuh Mas"), names("2026-11-03"))
    }

    @Test
    fun budaCemengIsBudaWageEvery35Days() {
        // Buda Wage in wuku 3 (Ukir): 2024-01-03 (Soma 2024-01-01 is Ukir per golden).
        val bc = allDays(d("2024-01-01"), d("2024-12-31")).filter { on(it, RerainanKind.BUDA_CEMENG).isNotEmpty() }.toList()
        assertEquals(d("2024-01-03"), bc.first())
        bc.zipWithNext().forEach { (a, b) -> assertEquals(35L, b.toEpochDay() - a.toEpochDay()) }
        bc.forEach { Pawukon.of(it).let { p -> assertEquals(Saptawara.BUDA, p.saptawara); assertEquals(Pancawara.WAGE, p.pancawara) } }
        // Named for the wuku, e.g. Buda Cemeng Kulawu is wuku 28.
        assertTrue(bc.none { Pawukon.of(it).wuku % 5 != 3 })
    }

    @Test
    fun nyepiIsDayAfterTilemKesangaAndNgembakGeniNext() {
        for (y in 2024..2035) {
            val n = nyepi(y)
            assertTrue(rerainanOn(n).any { it.kind == RerainanKind.NYEPI }, "Nyepi $y")
            assertTrue(rerainanOn(n.plusDays(1)).any { it.name == "Ngembak Geni" })
            assertTrue(rerainanOn(n.minusDays(1)).any { it.kind == RerainanKind.TILEM })
        }
    }

    @Test
    fun siwaratriIsDayBeforeTilemKapitu() {
        assertTrue("Hari Siwaratri" in names("2024-01-09"))
        val all = allDays(d("2024-01-01"), d("2035-12-31")).filter { on(it, RerainanKind.SIWARATRI).isNotEmpty() }.toList()
        assertEquals(12, all.size, "one Siwaratri per year: $all")
    }

    @Test
    fun correctionRowsFlowIntoRerainan() {
        val date = d("2026-11-24")
        val raw = rawLunarDay(date)
        assertTrue(raw.isPurnama)
        fun lunar(change: (RawLunarDay) -> RawLunarDay) = { x: LocalDate -> rawLunarDay(x).let { if (x == date) change(it) else it } }
        assertTrue(RerainanRules.fromLunar(date, lunar { it.copy(isPurnama = false) }).none { it.kind == RerainanKind.PURNAMA })
        val added = RerainanRules.fromLunar(date, lunar { it.copy(rerainanAdd = setOf("Odalan X"), rerainanRemove = setOf("Purnama")) })
        assertTrue(added.any { it.name == "Odalan X" })
        assertTrue(added.none { it.name == "Purnama" })
        // Tilem flag of the day before Nyepi drives Nyepi.
        val nyepi2026 = nyepi(2026)
        val noTilem = { x: LocalDate -> rawLunarDay(x).let { if (x == nyepi2026.minusDays(1)) it.copy(isTilem = false) else it } }
        assertTrue(RerainanRules.fromLunar(nyepi2026, noTilem).none { it.kind == RerainanKind.NYEPI })
    }

    private fun withCorrection(date: LocalDate, change: (RawLunarDay) -> RawLunarDay) =
        { x: LocalDate -> rawLunarDay(x).let { if (x == date) change(it) else it } }

    @Test
    fun removeCorrectionAppliesToPawukonRerainan() {
        val galungan = d("2026-06-17")
        assertTrue("Hari Raya Galungan" in RerainanRules.on(galungan) { rawLunarDay(it) }.map { it.name })
        val got = RerainanRules.on(galungan, withCorrection(galungan) { it.copy(rerainanRemove = setOf("Hari Raya Galungan")) })
        assertTrue(got.none { it.name == "Hari Raya Galungan" }, got.toString())
    }

    @Test
    fun removeCorrectionAppliesToKajengKliwon() {
        val kk = allDays(d("2026-11-01"), d("2026-12-31")).first { on(it, RerainanKind.KAJENG_KLIWON).isNotEmpty() }
        val got = RerainanRules.on(kk, withCorrection(kk) { it.copy(rerainanRemove = setOf("Kajeng Kliwon")) })
        assertTrue(got.none { it.kind == RerainanKind.KAJENG_KLIWON }, got.toString())
    }

    @Test
    fun addCorrectionAddsOtherNameOnAnyDay() {
        val galungan = d("2026-06-17")
        val got = RerainanRules.on(galungan, withCorrection(galungan) { it.copy(rerainanAdd = setOf("Odalan Y")) })
        assertTrue(got.any { it.name == "Odalan Y" && it.kind == RerainanKind.OTHER && it.date == galungan })
        assertTrue(got.any { it.name == "Hari Raya Galungan" })
    }

    // ---- Golden (212 days) ---------------------------------------------------------------

    /**
     * Mapping from kalenderbali.com event names (golden `flags` column) to the (name, kind) this module
     * produces. Every golden flag must map here; unknown flags fail the test. Noise is listed in [GOLDEN_NOISE].
     */
    private val siteToRerainan: Map<String, Pair<String, RerainanKind>> = mapOf(
        "purnama" to ("Purnama" to RerainanKind.PURNAMA),
        "tilem" to ("Tilem" to RerainanKind.TILEM),
        "kajeng-kliwon" to ("Kajeng Kliwon" to RerainanKind.KAJENG_KLIWON),
        "anggara-kasih" to ("Anggara Kasih" to RerainanKind.ANGGARA_KASIH),
        "Tumpek Landep" to ("Tumpek Landep" to RerainanKind.TUMPEK),
        "Tumpek Uduh/Pengatag" to ("Tumpek Wariga (Uduh/Pengatag)" to RerainanKind.TUMPEK),
        "Tumpek Krulut" to ("Tumpek Krulut" to RerainanKind.TUMPEK),
        "Tumpek Kandang" to ("Tumpek Kandang (Uye)" to RerainanKind.TUMPEK),
        "Tumpek Wayang" to ("Tumpek Wayang" to RerainanKind.TUMPEK),
        "Sugihan Bali" to ("Sugihan Bali" to RerainanKind.GALUNGAN_KUNINGAN),
        "Sugimanek Jawa" to ("Sugihan Jawa" to RerainanKind.GALUNGAN_KUNINGAN),
        "Hari Penyekeban" to ("Penyekeban Galungan" to RerainanKind.GALUNGAN_KUNINGAN),
        "Penyajaan Galungan" to ("Penyajaan Galungan" to RerainanKind.GALUNGAN_KUNINGAN),
        "Penampahan Galungan" to ("Penampahan Galungan" to RerainanKind.GALUNGAN_KUNINGAN),
        "Hari Raya Galungan" to ("Hari Raya Galungan" to RerainanKind.GALUNGAN_KUNINGAN),
        "Manis Galungan" to ("Manis Galungan" to RerainanKind.GALUNGAN_KUNINGAN),
        "Pemaridan Guru" to ("Pemaridan Guru" to RerainanKind.GALUNGAN_KUNINGAN),
        "Pemacekan Agung" to ("Pemacekan Agung" to RerainanKind.GALUNGAN_KUNINGAN),
        "Hari Raya Kuningan" to ("Hari Raya Kuningan" to RerainanKind.GALUNGAN_KUNINGAN),
        "Banyu Pinaruh" to ("Banyu Pinaruh" to RerainanKind.SARASWATI_PAGERWESI),
        "Soma Ribek" to ("Soma Ribek" to RerainanKind.SARASWATI_PAGERWESI),
        "Hari Pagerwesi" to ("Hari Raya Pagerwesi" to RerainanKind.SARASWATI_PAGERWESI),
        "Hari Siwa Ratri" to ("Hari Siwaratri" to RerainanKind.SIWARATRI),
        "Hari Raya Nyepi Tahun Baru Saka 1946" to ("Hari Raya Nyepi" to RerainanKind.NYEPI),
        "Ngembak Geni" to ("Ngembak Geni" to RerainanKind.NGEMBAK_GENI),
    )

    /** Site text that is not a holy day. "Sukra Paing Dunggulan" is a filler leaked by the collector (controller ruling). */
    private val GOLDEN_NOISE = setOf("Sukra Paing Dunggulan")

    /**
     * Rule-only holy days from the spec that the golden site does not list (golden 2026-11-03 is Anggara Wage Sinta
     * with an empty flags cell). Allowed on golden days, but not verified by it.
     */
    private val RULE_ONLY_EXTRA = setOf("Sabuh Mas")

    private fun goldenFlags(raw: String): List<String> = raw.split(";").map { it.trim() }.filter { it.isNotEmpty() && it !in GOLDEN_NOISE }

    @Test
    fun allGoldenFlagsMatch2024To2035() {
        val days = GoldenLoader.allDays()
        assertEquals(212, days.size)
        val mappedKinds = siteToRerainan.values.map { it.second }.toSet()
        val mappedNames = siteToRerainan.values.map { it.first }.toSet()
        val problems = ArrayList<String>()
        var flagCount = 0
        for (g in days) {
            val flags = goldenFlags(g.raw.getValue("flags"))
            val expected = flags.map { f -> siteToRerainan[f] ?: fail("unmapped golden flag '$f' on ${g.date}") }
            flagCount += expected.size
            val got = rerainanOn(g.date)
            for ((name, kind) in expected) {
                val n = got.count { it.name == name && it.kind == kind }
                if (n != 1) problems += "${g.date}: expected exactly one '$name' got $n"
            }
            // Nothing extra for the mapped names: only the mapped holy days may appear.
            for (r in got) if (r.name in mappedNames && expected.none { it.first == r.name }) problems += "${g.date}: extra '${r.name}'"
            for (r in got) if (r.kind in mappedKinds && r.name !in mappedNames && r.name !in RULE_ONLY_EXTRA) problems += "${g.date}: unmapped '${r.name}' of mapped kind ${r.kind}"
        }
        assertEquals(55, flagCount) // non-noise flags in the 212 golden days
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    // ---- hari-raya.csv (independent sources) ---------------------------------------------

    private data class HariRaya(val date: LocalDate, val event: String, val source: String)

    private fun hariRaya(): List<HariRaya> =
        RerainanTest::class.java.getResourceAsStream("/golden/golden2/hari-raya.csv")!!
            .bufferedReader().readLines().drop(1).filter { it.isNotBlank() }.map {
                val (dt, e, s) = it.split(",")
                HariRaya(LocalDate.parse(dt), e, s)
            }

    /** SOURCES.md "Disagreements": Nyepi 2027 (03-08 vs 03-09) has no agreed value, so it is not asserted. */
    private fun disputed(r: HariRaya) = r.event == "nyepi" && r.date.year == 2027

    private val eventName = mapOf(
        "galungan" to "Hari Raya Galungan", "kuningan" to "Hari Raya Kuningan", "saraswati" to "Hari Raya Saraswati",
        "pagerwesi" to "Hari Raya Pagerwesi", "siwaratri" to "Hari Siwaratri", "nyepi" to "Hari Raya Nyepi",
        "ngembak-geni" to "Ngembak Geni", "purnama" to "Purnama", "tilem" to "Tilem",
    )

    @Test
    fun independentHariRayaDates() {
        val problems = hariRaya().filter { !disputed(it) }.mapNotNull { r ->
            val name = eventName.getValue(r.event)
            if (rerainanOn(r.date).any { it.name == name }) null else "${r.date} $name (${r.source}) not produced"
        }
        assertTrue(problems.isEmpty(), problems.joinToString("\n"))
    }

    @Test
    fun noExtraGalunganOrKuninganOutsideIndependentList() {
        // For years where hari-raya.csv lists Galungan, the engine must not produce another one in that year.
        val listed = hariRaya().filter { it.event == "galungan" && it.source == "wikipedia-galungan" }.map { it.date }.toSet()
        for (y in 2024..2030) {
            val got = allDays(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31)).filter { "Hari Raya Galungan" in names(it.toString()) }.toSet()
            assertEquals(listed.filter { it.year == y }.toSet(), got, "Galungan $y")
        }
    }
}
