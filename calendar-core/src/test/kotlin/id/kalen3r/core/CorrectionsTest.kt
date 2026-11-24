package id.kalen3r.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CorrectionsTest {
    private fun row(date: String, field: String, value: String, source: String = "t") =
        """{"date":"$date","field":"$field","value":"$value","source":"$source"}"""

    private fun rejects(json: String, containing: String) {
        val e = assertFailsWith<CorrectionsFormatException> { Corrections.parse(json) }
        assertTrue(containing in e.message!!, "message '${e.message}' should contain '$containing'")
    }

    // ---- loader ----

    @Test fun loaderRejectsPawukonField() = rejects("[${row("2025-01-01", "pawukon", "1")}]", "pawukon")
    @Test fun loaderRejectsUnknownField() = rejects("[${row("2025-01-01", "wuku", "1")}]", "unknown field")
    @Test fun loaderRejectsInvalidDate() {
        rejects("[${row("2025-02-30", "purnama", "true")}]", "invalid date")
        rejects("[${row("2025-2-3", "purnama", "true")}]", "invalid date")
    }
    @Test fun loaderRejectsDuplicateDateFieldPair() =
        rejects("[${row("2025-01-01", "purnama", "true")},${row("2025-01-01", "purnama", "false")}]", "duplicate")
    @Test fun loaderRejectsMissingOrBlankSource() {
        rejects("""[{"date":"2025-01-01","field":"purnama","value":"true"}]""", "missing 'source'")
        rejects("[${row("2025-01-01", "purnama", "true", " ")}]", "blank source")
    }
    @Test fun loaderRejectsBadValues() {
        rejects("[${row("2025-01-01", "tithi", "penanggal:16")}]", "invalid tithi")
        rejects("[${row("2025-01-01", "tithi", "purnama")}]", "invalid tithi")
        rejects("[${row("2025-01-01", "sasih", "Kelima")}]", "invalid sasih")
        rejects("[${row("2025-01-01", "tilem", "yes")}]", "true or false")
    }
    @Test fun loaderRejectsMalformedJson() {
        for (bad in listOf("", "{}", "[", "[{]", "[1]", """[{"date":1}]""", "[{\"date\":\"x\"},]", "[] x", "[{\"a\":\"b\",\"a\":\"c\"}]", "[{\"a\":\"b\"")) {
            assertFailsWith<CorrectionsFormatException>(bad) { Corrections.parse(bad) }
        }
        rejects("""[{"date":"2025-01-01","field":"purnama","value":"true","source":"t","extra":"x"}]""", "unknown key")
    }
    @Test fun loaderAcceptsEmptyAndWhitespace() {
        assertEquals(0, Corrections.parse("[]").rows.size)
        assertEquals(0, Corrections.parse(" [ \n ] \n").rows.size)
    }
    @Test fun toJsonRoundTrips() {
        val rows = listOf(
            CorrectionRow(LocalDate.of(2025, 3, 1), CorrectionField.SASIH, "SADA:nampih", "golden:a \"q\".csv"),
            CorrectionRow(LocalDate.of(2025, 1, 1), CorrectionField.TITHI, "pangelong:7", "x"),
        )
        val back = Corrections.parse(Corrections.toJson(rows)).rows
        assertEquals(rows.sortedBy { it.date }, back)
        assertEquals("[]\n", Corrections.toJson(emptyList()))
    }

    // ---- apply ----

    @Test fun applyChangesOnlyListedFieldsOfListedDate() {
        val date = LocalDate.of(2025, 6, 10)
        val raw = rawLunarDay(date)
        val table = Corrections.parse(
            "[" + listOf(
                row("2025-06-10", "tithi", "pangelong:7"), row("2025-06-10", "sasih", "SADA:nampih"),
                row("2025-06-10", "purnama", "true"), row("2025-06-10", "tilem", "true"),
                row("2025-06-10", "rerainanAdd", "A"), row("2025-06-10", "rerainanAdd", "B"),
                row("2025-06-10", "rerainanRemove", "C"),
            ).joinToString(",") + "]",
        )
        val c = table.apply(raw)
        assertEquals(Tithi(TithiType.PANGELONG, 7), c.tithi)
        assertEquals(Sasih.SADA, c.sasih); assertTrue(c.isNampihSasih)
        assertTrue(c.isPurnama && c.isTilem)
        assertEquals(setOf("A", "B"), c.rerainanAdd); assertEquals(setOf("C"), c.rerainanRemove)
        val other = rawLunarDay(date.plusDays(1))
        assertEquals(other, table.apply(other))
    }

    @Test fun rawLunarDayMatchesEngine() {
        val d = LocalDate.of(2024, 1, 25)
        val r = rawLunarDay(d)
        assertEquals(Lunar.tithiOn(d), r.tithi)
        assertEquals(sasihOn(d), SasihInfo(r.sasih, r.isNampihSasih))
        assertTrue(r.isPurnama); assertFalse(r.isTilem)
        assertTrue(r.rerainanAdd.isEmpty() && r.rerainanRemove.isEmpty())
    }

    @Test fun skippedPurnamaOrTilemLabelsPrecedingDay() {
        // Provisional rule (unverified against official data): the day before a skipped tithi 15/30 is marked.
        var found = 0
        var d = LocalDate.of(2024, 1, 1)
        while (d < LocalDate.of(2031, 1, 1)) {
            val skipped = Lunar.skippedTithiOn(d)
            if (skipped != null && (skipped.isPurnama || skipped.isTilem)) {
                found++
                val before = rawLunarDay(d.minusDays(1))
                assertEquals(skipped.isPurnama, before.isPurnama, "$d"); assertEquals(skipped.isTilem, before.isTilem, "$d")
            }
            d = d.plusDays(1)
        }
        assertTrue(found >= 2, "expected skipped purnama/tilem in 2024..2030, found $found")
        // Exactly one purnama per synodic half-month: never two adjacent flagged days.
        var e = LocalDate.of(2024, 1, 1)
        while (e < LocalDate.of(2031, 1, 1)) {
            assertFalse(rawLunarDay(e).isPurnama && rawLunarDay(e.plusDays(1)).isPurnama, "adjacent purnama $e")
            assertFalse(rawLunarDay(e).isTilem && rawLunarDay(e.plusDays(1)).isTilem, "adjacent tilem $e")
            e = e.plusDays(1)
        }
    }

    // ---- generator pipeline on a synthetic golden file with deliberate disagreements ----

    private val fixtureBase = "/corrections-fixture/"

    @Test fun generatorEmitsRowPerDifferingField() {
        val rows = CorrectionsGenerator.generate(listOf("days-disagree.csv"), fixtureBase)
        val src = "golden:days-disagree.csv"
        fun d(s: String) = LocalDate.parse(s)
        val expected = setOf(
            CorrectionRow(d("2024-01-01"), CorrectionField.TITHI, "pangelong:6", src),
            CorrectionRow(d("2024-01-02"), CorrectionField.SASIH, "KALIMA", src),
            CorrectionRow(d("2024-01-02"), CorrectionField.PURNAMA, "true", src),
            CorrectionRow(d("2024-01-03"), CorrectionField.SASIH, "KAPITU:nampih", src),
            CorrectionRow(d("2024-01-04"), CorrectionField.TILEM, "true", src),
            CorrectionRow(d("2024-01-25"), CorrectionField.PURNAMA, "false", src),
        )
        assertEquals(expected, rows.toSet())
        assertEquals(rows.size, expected.size)
        assertEquals(mapOf(2024 to 6), CorrectionsGenerator.perYear(rows))
    }

    @Test fun appliedSyntheticTableEqualsGolden() {
        val days = GoldenLoader.days("days-disagree.csv", fixtureBase)
        val json = Corrections.toJson(CorrectionsGenerator.generate(listOf("days-disagree.csv"), fixtureBase))
        val table = Corrections.parse(json)
        for (gd in days) {
            val g = CorrectionsGenerator.goldenLunar(gd)
            val c = table.apply(rawLunarDay(g.date))
            assertTrue(c.tithi in g.tithis, "tithi ${g.date}")
            assertEquals(g.sasih, SasihInfo(c.sasih, c.isNampihSasih), "sasih ${g.date}")
            assertEquals(g.purnama, c.isPurnama, "purnama ${g.date}")
            assertEquals(g.tilem, c.isTilem, "tilem ${g.date}")
        }
    }

    // ---- real golden ----

    @Test fun appliedEqualsGoldenForAllCoveredDays() {
        val days = GoldenLoader.allDays() // asserts rows == declared covered days of coverage.json (212)
        assertTrue(days.isNotEmpty())
        for (gd in days) {
            val g = CorrectionsGenerator.goldenLunar(gd)
            val c = Corrections.apply(rawLunarDay(g.date))
            assertTrue(c.tithi in g.tithis, "tithi ${g.date}: ${c.tithi} vs ${g.tithis}")
            assertEquals(g.sasih, SasihInfo(c.sasih, c.isNampihSasih), "sasih ${g.date}")
            assertEquals(g.purnama, c.isPurnama, "purnama ${g.date}")
            assertEquals(g.tilem, c.isTilem, "tilem ${g.date}")
        }
    }

    @Test fun committedCorrectionsMatchGenerator() {
        val generated = CorrectionsGenerator.generate(GoldenLoader.DAY_FILES)
        assertEquals(generated.toSet(), Corrections.default.rows.toSet(),
            "corrections.json is stale: run `gradle :calendar-core:generateCorrections`")
    }

    @Test fun correctionCountPerYearIsBelowLimit() {
        val limit = 10
        for ((year, n) in CorrectionsGenerator.perYear(Corrections.default.rows)) {
            assertTrue(n <= limit,
                "$n corrections in $year (limit $limit). A large number means the raw engine needs fixing, not a bigger correction table.")
        }
    }
}
