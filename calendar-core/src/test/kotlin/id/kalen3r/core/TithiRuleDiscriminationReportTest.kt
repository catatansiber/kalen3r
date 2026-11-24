package id.kalen3r.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Report, not assertion (Task 6): can any independent fact tell [TithiRule.PENGALANTAKA] from
 * [TithiRule.MEAN_ELONGATION]? Both rules run through the same [SasihCalendar]. Numbers go to
 * docs/lunar-report.md; the full text is written to build/tithi-discrimination-report.txt.
 */
class TithiRuleDiscriminationReportTest {
    private data class Fact(val date: LocalDate, val event: String, val source: String)

    private fun hariRaya(): List<Fact> =
        javaClass.getResourceAsStream("/golden/golden2/hari-raya.csv")!!.bufferedReader().readLines().drop(1)
            .filter { it.isNotBlank() }.map { val (d, e, s) = it.split(","); Fact(LocalDate.parse(d), e, s) }

    /**
     * Facts found by web search during Task 6 (not in golden/, not fetched in full: search-result summaries
     * only, so low-to-medium confidence). Kept here, not in the CSVs, so they never gate a test.
     */
    private val webFacts = listOf(
        Fact(LocalDate.of(2025, 5, 27), "tilem", "rri.co.id Singaraja 'Daftar Hari Raya Umat Hindu di Bulan Mei 2025' (search summary)"),
        Fact(LocalDate.of(2025, 4, 12), "purnama", "rove.me 'Purnama Kedasa' (travel site, search summary)"),
        Fact(LocalDate.of(2025, 7, 10), "purnama", "tirto.id 'Kalender Bali Juli 2025' (search summary)"),
        Fact(LocalDate.of(2025, 7, 25), "tilem", "tirto.id 'Kalender Bali Juli 2025' (search summary)"),
        Fact(LocalDate.of(2025, 9, 7), "purnama", "rri.co.id Singaraja September rerainan, 'Purnama Sasih Katiga' (search summary)"),
        Fact(LocalDate.of(2025, 9, 22), "tilem", "rri.co.id Singaraja September rerainan, 'Tilem Sasih Katiga' (search summary)"),
        Fact(LocalDate.of(2025, 11, 5), "purnama", "tirto.id 'Kalender Bali November 2025' (search summary)"),
        Fact(LocalDate.of(2025, 12, 4), "purnama", "sonora.id 'Kalender Bali Desember 2025' (search summary)"),
        Fact(LocalDate.of(2025, 12, 19), "tilem", "sonora.id 'Kalender Bali Desember 2025' (search summary; same page also says 5 Dec)"),
    )

    private class Derived(rule: TithiRule) {
        val n: (LocalDate) -> Int = { Lunar.tithiNumberOn(it, rule) }
        val cal = SasihCalendar.forRule(rule)

        fun isMonthStart(d: LocalDate) = n(d) < n(d.minusDays(1))

        /** Tilem day = last day of a lunar month (also when tithi 30 is skipped). */
        fun isTilem(d: LocalDate) = isMonthStart(d.plusDays(1))

        /** Purnama day = last day of the bright half (tithi <= 15 today, > 15 tomorrow). */
        fun isPurnama(d: LocalDate) = n(d) <= 15 && n(d.plusDays(1)) > 15

        /** Siwaratri = pangelong 14 of Kapitu: the day before Tilem Kapitu. */
        fun siwaratri(year: Int): LocalDate? {
            var d = LocalDate.of(year, 1, 1)
            while (d.year == year) {
                if (isTilem(d.plusDays(1)) && cal.sasihOn(d.plusDays(1)) == SasihInfo(Sasih.KAPITU, false)) return d
                d = d.plusDays(1)
            }
            return null
        }

        fun check(f: Fact): Pair<Boolean, String>? = when (f.event) {
            "nyepi" -> cal.nyepi(f.date.year).let { (it == f.date) to "ours=$it" }
            "siwaratri" -> siwaratri(f.date.year).let { (it == f.date) to "ours=$it" }
            "tilem" -> isTilem(f.date) to "n=${n(f.date)} sasih=${cal.sasihOn(f.date)}"
            "purnama" -> isPurnama(f.date) to "n=${n(f.date)} sasih=${cal.sasihOn(f.date)}"
            else -> null // Pawukon-only events (galungan, kuningan, ...) do not depend on the tithi rule.
        }
    }

    @Test
    fun report() {
        val out = StringBuilder()
        fun line(s: String) { out.appendLine(s); println(s) }
        val rules = TithiRule.entries.associateWith { Derived(it) }
        val golden = GoldenLoader.allDays()

        line("TITHI RULE DISCRIMINATION (Task 6). Rules: ${TithiRule.entries}")
        line("")
        line("1. Golden days (${golden.size}, coverage.json ranges): tithi / sasih / caka mismatches")
        for ((rule, r) in rules) {
            var t = 0; var s = 0; var c = 0
            for (g in golden) {
                val off = if (g.raw.getValue("tithiType").lowercase() == "penanggal") 0 else 15
                if (r.n(g.date) !in g.raw.getValue("tithiNo").split("/").map { it.trim().toInt() + off }) t++
                if (r.cal.sasihOn(g.date) != GoldenLoader.sasih(g.raw.getValue("sasih"))) s++
                if (r.cal.cakaYear(g.date) != g.raw.getValue("caka").toInt()) c++
            }
            line("   $rule: tithi=$t sasih=$s caka=$c")
        }

        line("")
        line("2. golden2/hari-raya.csv lunar rows (nyepi, siwaratri, purnama, tilem): per-rule result")
        val lunarRows = hariRaya().filter { it.event in setOf("nyepi", "siwaratri", "purnama", "tilem") }
        val tally = rules.keys.associateWith { 0 }.toMutableMap()
        var separating = 0
        for (f in lunarRows) {
            val res = rules.mapValues { it.value.check(f)!! }
            res.forEach { (k, v) -> if (v.first) tally[k] = tally.getValue(k) + 1 }
            val differ = res.values.map { it.first }.toSet().size > 1
            if (differ) separating++
            if (differ || res.values.any { !it.first }) {
                line("   ${f.date} ${f.event} [${f.source}] " + res.entries.joinToString("; ") { "${it.key}: ${if (it.value.first) "match" else "MISMATCH"} ${it.value.second}" })
            }
        }
        line("   rows=${lunarRows.size} matched: $tally; rows that separate the rules: $separating")

        line("")
        line("3. Web-search facts (not in golden/, low-medium confidence)")
        for (f in webFacts) {
            val res = rules.mapValues { it.value.check(f)!! }
            line("   ${f.date} ${f.event}: " + res.entries.joinToString("; ") { "${it.key}=${if (it.value.first) "match" else "MISMATCH"} (${it.value.second})" } + "  <- ${f.source}")
        }

        line("")
        line("4. Nyepi per rule, 2024-2035 (a '*' marks Nyepi not on penanggal 1 or Tilem not on pangelong 15)")
        for (y in 2024..2035) {
            line("   $y: " + rules.entries.joinToString("  ") { (k, r) ->
                val ny = r.cal.nyepi(y)
                val mark = if (r.n(ny) != 1 || r.n(ny.minusDays(1)) != 30) "*(n=${r.n(ny.minusDays(1))}->${r.n(ny)})" else ""
                "$k=$ny$mark"
            })
        }

        line("")
        line("5. Where the rules disagree, 2024-2035: differing days per year; differing Purnama/Tilem/sasih/Nyepi dates")
        val a = rules.getValue(TithiRule.PENGALANTAKA); val b = rules.getValue(TithiRule.MEAN_ELONGATION)
        for (y in 2024..2035) {
            var diff = 0
            val events = mutableListOf<String>()
            var d = LocalDate.of(y, 1, 1)
            while (d.year == y) {
                if (a.n(d) != b.n(d)) diff++
                if (a.isTilem(d) != b.isTilem(d)) events += "tilem $d ${if (a.isTilem(d)) "PENGALANTAKA" else "MEAN"}"
                if (a.isPurnama(d) != b.isPurnama(d)) events += "purnama $d ${if (a.isPurnama(d)) "PENGALANTAKA" else "MEAN"}"
                d = d.plusDays(1)
            }
            val sasihDiff = (0 until LocalDate.of(y, 12, 31).dayOfYear).map { LocalDate.of(y, 1, 1).plusDays(it.toLong()) }
                .count { a.cal.sasihOn(it) != b.cal.sasihOn(it) }
            line("   $y: days=$diff sasihDays=$sasihDiff ${events}")
        }

        java.io.File("build/tithi-discrimination-report.txt").writeText(out.toString())
        assertTrue(lunarRows.isNotEmpty())
    }
}
