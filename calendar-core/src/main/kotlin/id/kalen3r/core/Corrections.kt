package id.kalen3r.core

import java.time.DateTimeException
import java.time.LocalDate

/**
 * A lunar day as the raw engine computes it, before the correction table is applied.
 * Pawukon is deliberately not part of this type: it is exact arithmetic and is never corrected.
 */
data class RawLunarDay(
    val date: LocalDate,
    val tithi: Tithi,
    val sasih: Sasih,
    val isNampihSasih: Boolean,
    val isPurnama: Boolean,
    val isTilem: Boolean,
    val rerainanAdd: Set<String>,
    val rerainanRemove: Set<String>,
)

/**
 * Uncorrected lunar day built from the existing engine ([Lunar.tithiOn], [sasihOn]). The engine has no
 * rerainan rules yet, so both rerainan sets are empty.
 *
 * Purnama / Tilem: the day whose tithi is exactly penanggal 15 / pangelong 15. When the pengalantaka
 * SKIPS that tithi (ngunalatri, about once or twice a year; first on 2024-09-18, outside golden) no day
 * carries it. PROVISIONAL rule: the day before the skipped tithi (penanggal 14 / pangelong 14) is marked,
 * since traditional calendars label the preceding day. This is NOT verified against official data (no
 * golden covers such a day, see docs/lunar-report.md); a correction row fixes any day that is wrong.
 */
fun rawLunarDay(date: LocalDate): RawLunarDay {
    val tithi = Lunar.tithiOn(date)
    val sasih = sasihOn(date)
    val skippedTomorrow = Lunar.skippedTithiOn(date.plusDays(1))
    return RawLunarDay(
        date = date,
        tithi = tithi,
        sasih = sasih.sasih,
        isNampihSasih = sasih.isNampih,
        isPurnama = tithi.isPurnama || skippedTomorrow?.isPurnama == true,
        isTilem = tithi.isTilem || skippedTomorrow?.isTilem == true,
        rerainanAdd = emptySet(),
        rerainanRemove = emptySet(),
    )
}

/** Fields a correction row may change. There is intentionally no pawukon field. */
enum class CorrectionField(val key: String) {
    TITHI("tithi"), SASIH("sasih"), PURNAMA("purnama"), TILEM("tilem"),
    RERAINAN_ADD("rerainanAdd"), RERAINAN_REMOVE("rerainanRemove");

    companion object {
        fun ofKey(key: String): CorrectionField? = entries.firstOrNull { it.key == key }
    }
}

/** One row of corrections.json. [value] formats: see [Corrections]. */
data class CorrectionRow(val date: LocalDate, val field: CorrectionField, val value: String, val source: String)

class CorrectionsFormatException(message: String) : IllegalArgumentException(message)

/** A validated set of correction rows, applied per date. */
class CorrectionTable(val rows: List<CorrectionRow>) {
    private val byDate: Map<LocalDate, List<CorrectionRow>> = rows.groupBy { it.date }

    fun apply(day: RawLunarDay): RawLunarDay {
        val rs = byDate[day.date] ?: return day
        var d = day
        for (r in rs) {
            d = when (r.field) {
                CorrectionField.TITHI -> d.copy(tithi = Corrections.parseTithi(r.value))
                CorrectionField.SASIH -> Corrections.parseSasih(r.value).let { d.copy(sasih = it.sasih, isNampihSasih = it.isNampih) }
                CorrectionField.PURNAMA -> d.copy(isPurnama = r.value.toBooleanStrict())
                CorrectionField.TILEM -> d.copy(isTilem = r.value.toBooleanStrict())
                CorrectionField.RERAINAN_ADD -> d.copy(rerainanAdd = d.rerainanAdd + r.value)
                CorrectionField.RERAINAN_REMOVE -> d.copy(rerainanRemove = d.rerainanRemove + r.value)
            }
        }
        return d
    }
}

/**
 * The correction table (resource /corrections.json, a JSON array of {"date","field","value","source"}).
 *
 * Value formats: tithi "penanggal:15" | "pangelong:7"; sasih the [Sasih] enum name, optionally suffixed
 * ":nampih" for the intercalary month (e.g. "SADA:nampih"); purnama/tilem "true" | "false";
 * rerainanAdd/rerainanRemove a rerainan name. Pawukon is never corrected, so a "pawukon" field is rejected.
 * Loading rejects unknown fields and keys, invalid dates, bad values, a missing/blank source, and duplicate
 * (date, field) pairs (for the two rerainan fields a date may have several names: duplicate (date, field,
 * value) is rejected instead).
 */
object Corrections {
    private const val RESOURCE = "/corrections.json"

    val default: CorrectionTable by lazy {
        val stream = Corrections::class.java.getResourceAsStream(RESOURCE)
            ?: throw CorrectionsFormatException("resource $RESOURCE missing")
        parse(stream.bufferedReader(Charsets.UTF_8).use { it.readText() })
    }

    fun apply(day: RawLunarDay): RawLunarDay = default.apply(day)

    fun parse(json: String): CorrectionTable {
        val objects = StrictJson(json).parseArrayOfStringObjects()
        val rows = objects.mapIndexed { i, o -> toRow(i, o) }
        val seen = HashSet<List<Any>>()
        for (r in rows) {
            val multi = r.field == CorrectionField.RERAINAN_ADD || r.field == CorrectionField.RERAINAN_REMOVE
            val key = if (multi) listOf(r.date, r.field, r.value) else listOf(r.date, r.field)
            if (!seen.add(key)) throw CorrectionsFormatException("duplicate correction for ${r.date} field '${r.field.key}'")
        }
        return CorrectionTable(rows)
    }

    /** Serializes rows as a valid JSON array, one row per line, sorted by date then field. */
    fun toJson(rows: List<CorrectionRow>): String {
        if (rows.isEmpty()) return "[]\n"
        val sorted = rows.sortedWith(compareBy({ it.date }, { it.field.ordinal }, { it.value }))
        return sorted.joinToString(",\n", "[\n", "\n]\n") {
            """  {"date":${q(it.date.toString())},"field":${q(it.field.key)},"value":${q(it.value)},"source":${q(it.source)}}"""
        }
    }

    private fun q(s: String) = buildString {
        append('"')
        for (c in s) when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c < ' ' -> append("\\u%04x".format(c.code))
            else -> append(c)
        }
        append('"')
    }

    fun formatTithi(t: Tithi) = "${t.type.name.lowercase()}:${t.number}"
    fun formatSasih(s: SasihInfo) = s.sasih.name + if (s.isNampih) ":nampih" else ""

    fun parseTithi(v: String): Tithi {
        val parts = v.split(":")
        val type = TithiType.entries.firstOrNull { parts.size == 2 && it.name.lowercase() == parts[0] }
        val n = parts.getOrNull(1)?.toIntOrNull()
        if (parts.size != 2 || type == null || n == null || n !in 1..15) {
            throw CorrectionsFormatException("invalid tithi value '$v' (expected penanggal:1..15 or pangelong:1..15)")
        }
        return Tithi(type, n)
    }

    fun parseSasih(v: String): SasihInfo {
        val parts = v.split(":")
        val sasih = Sasih.entries.firstOrNull { it.name == parts[0] }
        if (sasih == null || parts.size > 2 || (parts.size == 2 && parts[1] != "nampih")) {
            throw CorrectionsFormatException("invalid sasih value '$v' (expected a Sasih enum name, optionally ':nampih')")
        }
        return SasihInfo(sasih, parts.size == 2)
    }

    private fun toRow(index: Int, o: Map<String, String>): CorrectionRow {
        val where = "corrections row $index"
        val allowed = setOf("date", "field", "value", "source")
        o.keys.firstOrNull { it !in allowed }?.let { throw CorrectionsFormatException("$where: unknown key '$it'") }
        fun need(k: String) = o[k] ?: throw CorrectionsFormatException("$where: missing '$k'")
        val fieldKey = need("field")
        val field = CorrectionField.ofKey(fieldKey) ?: throw CorrectionsFormatException(
            if (fieldKey == "pawukon") "$where: field 'pawukon' is not allowed, pawukon is never corrected"
            else "$where: unknown field '$fieldKey'",
        )
        val dateText = need("date")
        val date = try {
            if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(dateText)) throw DateTimeException("format")
            LocalDate.parse(dateText)
        } catch (e: DateTimeException) {
            throw CorrectionsFormatException("$where: invalid date '$dateText'")
        }
        val value = need("value")
        val source = need("source")
        if (source.isBlank()) throw CorrectionsFormatException("$where: blank source")
        when (field) {
            CorrectionField.TITHI -> parseTithi(value)
            CorrectionField.SASIH -> parseSasih(value)
            CorrectionField.PURNAMA, CorrectionField.TILEM ->
                if (value != "true" && value != "false") throw CorrectionsFormatException("$where: value '$value' must be true or false")
            CorrectionField.RERAINAN_ADD, CorrectionField.RERAINAN_REMOVE ->
                if (value.isBlank()) throw CorrectionsFormatException("$where: blank rerainan name")
        }
        return CorrectionRow(date, field, value, source)
    }
}

/** Minimal strict JSON reader for exactly one shape: an array of objects whose values are all strings. */
private class StrictJson(private val s: String) {
    private var p = 0

    private fun fail(msg: String): Nothing = throw CorrectionsFormatException("corrections.json: $msg at offset $p")

    private fun ws() { while (p < s.length && s[p] in " \t\n\r") p++ }

    private fun expect(c: Char) { ws(); if (p >= s.length || s[p] != c) fail("expected '$c'"); p++ }

    fun parseArrayOfStringObjects(): List<Map<String, String>> {
        expect('[')
        val out = ArrayList<Map<String, String>>()
        ws()
        if (p < s.length && s[p] == ']') { p++ } else {
            while (true) {
                out += parseObject()
                ws()
                if (p < s.length && s[p] == ',') { p++; continue }
                expect(']')
                break
            }
        }
        ws()
        if (p != s.length) fail("trailing content after array")
        return out
    }

    private fun parseObject(): Map<String, String> {
        expect('{')
        val m = LinkedHashMap<String, String>()
        ws()
        if (p < s.length && s[p] == '}') { p++; return m }
        while (true) {
            ws()
            val k = parseString()
            expect(':')
            ws()
            if (p >= s.length || s[p] != '"') fail("value of '$k' is not a string")
            if (m.put(k, parseString()) != null) fail("duplicate key '$k'")
            ws()
            if (p < s.length && s[p] == ',') { p++; continue }
            expect('}')
            return m
        }
    }

    private fun parseString(): String {
        if (p >= s.length || s[p] != '"') fail("expected string")
        p++
        val sb = StringBuilder()
        while (true) {
            if (p >= s.length) fail("unterminated string")
            val c = s[p++]
            when {
                c == '"' -> return sb.toString()
                c < ' ' -> fail("control character in string")
                c == '\\' -> {
                    if (p >= s.length) fail("unterminated escape")
                    when (val e = s[p++]) {
                        '"', '\\', '/' -> sb.append(e)
                        'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                        'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                        'u' -> {
                            val hex = s.substring(p, minOf(p + 4, s.length))
                            if (hex.length != 4 || !hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) fail("bad \\u escape")
                            sb.append(hex.toInt(16).toChar()); p += 4
                        }
                        else -> fail("bad escape '\\$e'")
                    }
                }
                else -> sb.append(c)
            }
        }
    }
}
