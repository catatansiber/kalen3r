package id.kalen3r.core

import java.time.LocalDate

/** One row of golden/days-*.csv. Names are kept as normalized lower-case strings. */
data class GoldenDay(
    val date: LocalDate,
    val wukuNumber: Int,
    val wukuName: String,
    val saptawara: Saptawara,
    val pancawara: Pancawara,
    val triwara: Triwara,
    val caturwara: String,
    val astawara: String,
    val sangawara: String,
    val dasawara: String,
    val urip: String,
    val raw: Map<String, String>,
)

/** Declared covered range from golden/coverage.json (inclusive). */
data class CoveredRange(val from: LocalDate, val to: LocalDate) {
    val days: Int get() = (to.toEpochDay() - from.toEpochDay() + 1).toInt()
}

object GoldenLoader {
    private const val BASE = "/golden/"

    /** Name orders used to map the CSV's name cells to the Int fields of PawukonDay. */
    val CATURWARA = Pawukon.CATURWARA_NAMES.map { it.lowercase() }
    val ASTAWARA = Pawukon.ASTAWARA_NAMES.map { it.lowercase() }
    val SANGAWARA = Pawukon.SANGAWARA_NAMES.map { it.lowercase() }
    val DASAWARA = Pawukon.DASAWARA_NAMES.map { it.lowercase() }

    private fun text(path: String): String =
        (GoldenLoader::class.java.getResourceAsStream(BASE + path)
            ?: error("golden resource missing: $path")).bufferedReader().use { it.readText() }

    fun coverage(): List<CoveredRange> {
        val re = Regex("""\{\s*"from"\s*:\s*"([\d-]+)"\s*,\s*"to"\s*:\s*"([\d-]+)"\s*\}""")
        val out = re.findAll(text("coverage.json")).map {
            CoveredRange(LocalDate.parse(it.groupValues[1]), LocalDate.parse(it.groupValues[2]))
        }.toList()
        check(out.isNotEmpty()) { "coverage.json declares no ranges" }
        return out
    }

    fun pancawara(s: String): Pancawara = when (s.lowercase()) {
        "paing" -> Pancawara.PAING
        "pon" -> Pancawara.PON
        "wage" -> Pancawara.WAGE
        "keliwon", "kliwon" -> Pancawara.KLIWON
        "umanis" -> Pancawara.UMANIS
        else -> error("unknown pancawara '$s'")
    }

    fun saptawara(s: String): Saptawara = when (s.lowercase()) {
        "redite" -> Saptawara.REDITE
        "soma" -> Saptawara.SOMA
        "anggara" -> Saptawara.ANGGARA
        "buda" -> Saptawara.BUDA
        "wraspati" -> Saptawara.WRASPATI
        "sukra" -> Saptawara.SUKRA
        "saniscara" -> Saptawara.SANISCARA
        else -> error("unknown saptawara '$s'")
    }

    fun triwara(s: String): Triwara = when (s.lowercase()) {
        "pasah" -> Triwara.PASAH
        "beteng" -> Triwara.BETENG
        "kajeng" -> Triwara.KAJENG
        else -> error("unknown triwara '$s'")
    }

    /**
     * kalenderbali.com sasih names (site spelling, e.g. Kelima, Kenam, Kesanga, Kedasa, Jiyestha, Sadha)
     * to [SasihInfo]. A "Mala " / "Nampih " prefix marks the intercalary month (e.g. "Mala Sadha").
     */
    fun sasih(s: String): SasihInfo {
        val n = s.trim().lowercase()
        val nampih = n.startsWith("mala ") || n.startsWith("nampih ")
        val base = n.substringAfter(' ').takeIf { nampih } ?: n
        val sasih = when (base) {
            "kasa" -> Sasih.KASA
            "karo" -> Sasih.KARO
            "katiga", "ketiga" -> Sasih.KATIGA
            "kapat", "kepat" -> Sasih.KAPAT
            "kalima", "kelima" -> Sasih.KALIMA
            "kanem", "kenam" -> Sasih.KANEM
            "kapitu", "kepitu" -> Sasih.KAPITU
            "kawolu", "kewulu", "kawulu", "kewolu" -> Sasih.KAWOLU
            "kasanga", "kesanga", "kanawa" -> Sasih.KANAWA
            "kadasa", "kedasa" -> Sasih.KADASA
            "desta", "destha", "jiyestha", "jyestha", "dhesta" -> Sasih.DESTA
            "sada", "sadha" -> Sasih.SADA
            else -> error("unknown sasih '$s'")
        }
        return SasihInfo(sasih, nampih)
    }

    private fun normWuku(s: String) = when (val n = s.lowercase()) {
        "dunggulan" -> "dungulan"
        else -> n
    }

    fun days(file: String): List<GoldenDay> {
        val lines = text(file).lines().filter { it.isNotBlank() }
        val header = lines.first().split(",")
        return lines.drop(1).map { line ->
            val cells = line.split(",")
            val m = header.zip(cells + List(header.size - cells.size) { "" }).toMap()
            val (num, name) = m.getValue("wuku").split(".", limit = 2)
            GoldenDay(
                date = LocalDate.parse(m.getValue("date")),
                wukuNumber = num.toInt(),
                wukuName = normWuku(name),
                saptawara = saptawara(m.getValue("saptawara")),
                pancawara = pancawara(m.getValue("pancawara")),
                triwara = triwara(m.getValue("triwara")),
                caturwara = m.getValue("caturwara").lowercase(),
                astawara = m.getValue("astawara").lowercase(),
                sangawara = m.getValue("sangawara").lowercase(),
                dasawara = m.getValue("dasawara").lowercase(),
                urip = m.getValue("urip"),
                raw = m,
            )
        }
    }

    /** All golden day rows, asserting the loaded count equals the declared covered day count. */
    fun allDays(): List<GoldenDay> {
        val ranges = coverage()
        val rows = days("days-2024.csv") + days("days-2026.csv")
        val declared = ranges.sumOf { it.days }
        check(rows.size == declared) { "golden rows ${rows.size} != declared covered days $declared" }
        check(rows.all { r -> ranges.any { r.date >= it.from && r.date <= it.to } }) { "golden row outside declared ranges" }
        check(rows.map { it.date }.toSet().size == rows.size) { "duplicate golden dates" }
        return rows
    }
}
