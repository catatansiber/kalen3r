package id.kalen3r.core

import java.io.File

/**
 * Diffs the raw engine against golden day CSVs and emits correction rows (test scope; run with
 * `gradle :calendar-core:generateCorrections`, which rewrites src/main/resources/corrections.json).
 * One row per differing day and field (tithi, sasih, purnama, tilem), source = "golden:<file>".
 * Rerainan are not compared: the engine has no rerainan rules yet and golden names are unmapped.
 * Pawukon is never compared or corrected.
 */
object CorrectionsGenerator {
    /** Golden facts of one day. [tithis] has two entries when the site shows two numbers ("2/3"). */
    data class GoldenLunar(val date: java.time.LocalDate, val tithis: List<Tithi>, val sasih: SasihInfo, val purnama: Boolean, val tilem: Boolean)

    fun goldenLunar(d: GoldenDay): GoldenLunar {
        val type = when (val t = d.raw.getValue("tithiType").lowercase()) {
            "penanggal" -> TithiType.PENANGGAL
            "pangelong" -> TithiType.PANGELONG
            else -> error("tithiType '$t' on ${d.date}")
        }
        val tithis = d.raw.getValue("tithiNo").split("/").map { Tithi(type, it.trim().toInt()) }
        val flags = d.raw.getValue("flags").lowercase().split(";").map { it.trim() }
        return GoldenLunar(d.date, tithis, GoldenLoader.sasih(d.raw.getValue("sasih")), "purnama" in flags, "tilem" in flags)
    }

    /** Rows that make the raw engine equal [days]. A two-number tithi cell matches if either number agrees. */
    fun diff(days: List<GoldenDay>, source: String): List<CorrectionRow> = days.flatMap { gd ->
        val g = goldenLunar(gd)
        val raw = rawLunarDay(g.date)
        buildList {
            fun row(f: CorrectionField, v: String) = add(CorrectionRow(g.date, f, v, source))
            if (raw.tithi !in g.tithis) row(CorrectionField.TITHI, Corrections.formatTithi(g.tithis.last()))
            if (SasihInfo(raw.sasih, raw.isNampihSasih) != g.sasih) row(CorrectionField.SASIH, Corrections.formatSasih(g.sasih))
            if (raw.isPurnama != g.purnama) row(CorrectionField.PURNAMA, g.purnama.toString())
            if (raw.isTilem != g.tilem) row(CorrectionField.TILEM, g.tilem.toString())
        }
    }

    fun generate(files: List<String>, base: String = "/golden/"): List<CorrectionRow> =
        files.flatMap { f -> diff(GoldenLoader.days(f, base), "golden:$f") }

    /** Sorted per-year row counts, e.g. {2024=3}. */
    fun perYear(rows: List<CorrectionRow>): Map<Int, Int> =
        rows.groupingBy { it.date.year }.eachCount().toSortedMap()

    @JvmStatic
    fun main(args: Array<String>) {
        val out = File(args.firstOrNull() ?: error("usage: CorrectionsGenerator <corrections.json path>"))
        val rows = generate(GoldenLoader.DAY_FILES)
        out.writeText(Corrections.toJson(rows))
        println("corrections written to $out: ${rows.size} rows")
        val per = perYear(rows)
        if (per.isEmpty()) println("per year: no corrections") else per.forEach { (y, n) -> println("per year: $y = $n") }
    }
}
