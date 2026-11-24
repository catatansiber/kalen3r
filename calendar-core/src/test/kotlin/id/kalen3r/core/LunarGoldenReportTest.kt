package id.kalen3r.core

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Report, not assertion: compares raw (uncorrected) tithi rules against the golden days that
 * coverage.json declares. Prints mismatch counts per covered range. Numbers go to docs/lunar-report.md.
 */
class LunarGoldenReportTest {
    /** Golden tithi for a day: one or more numbers 1..30 (a cell like "2/3" yields two). */
    private data class G(val date: LocalDate, val tithis: List<Int>, val purnama: Boolean, val tilem: Boolean)

    private fun golden(): Pair<List<CoveredRange>, List<G>> {
        val ranges = GoldenLoader.coverage()
        val rows = GoldenLoader.allDays().map { d ->
            val type = d.raw.getValue("tithiType").lowercase()
            val off = when (type) { "penanggal" -> 0; "pangelong" -> 15; else -> error("tithiType '$type' on ${d.date}") }
            val nums = d.raw.getValue("tithiNo").split("/").map { it.trim().toInt() + off }
            val flags = d.raw.getValue("flags").lowercase().split(";").map { it.trim() }
            G(d.date, nums, "purnama" in flags, "tilem" in flags)
        }.sortedBy { it.date }
        return ranges to rows
    }

    private fun tithiAtWita(d: LocalDate, t: LocalTime) = Lunar.tithiAt(d.atTime(t).toInstant(Lunar.WITA))

    private fun meanAtWita(d: LocalDate, t: LocalTime): Int {
        val e = Lunar.meanElongationDeg(d.atTime(t).toInstant(Lunar.WITA))
        return floor(e / 12.0).toInt() + 1
    }

    /** Approximate sunrise at Denpasar (8.65 S, 115.22 E) in WITA, NOAA low-precision formulae. */
    private fun sunriseDenpasar(d: LocalDate): LocalTime {
        val lat = -8.65 * PI / 180
        val lon = 115.22
        val g = 2 * PI / 365 * (d.dayOfYear - 1)
        val eqTime = 229.18 * (0.000075 + 0.001868 * cos(g) - 0.032077 * sin(g) - 0.014615 * cos(2 * g) - 0.040849 * sin(2 * g))
        val decl = 0.006918 - 0.399912 * cos(g) + 0.070257 * sin(g) - 0.006758 * cos(2 * g) +
            0.000907 * sin(2 * g) - 0.002697 * cos(3 * g) + 0.00148 * sin(3 * g)
        val ha = acos(cos(90.833 * PI / 180) / (cos(lat) * cos(decl)) - tan(lat) * tan(decl)) * 180 / PI
        val utcMin = 720 - 4 * (lon + ha) - eqTime
        val witaMin = utcMin + 480
        return LocalTime.of(0, 0).plusSeconds((witaMin * 60).toLong())
    }

    /** Tithi that ends during the civil day: last boundary crossed in [00:00, 24:00); if none, the tithi spanning the day. */
    private fun endingTithi(d: LocalDate): Int {
        val start = Lunar.tithiAt(d.atStartOfDay().toInstant(Lunar.WITA))
        val end = Lunar.tithiAt(d.plusDays(1).atStartOfDay().toInstant(Lunar.WITA))
        return if (start == end) start else (end + 28) % 30 + 1
    }

    private data class Score(val tithi: Int, val ptMissed: Int, val ptExtra: Int, val multi: Int) {
        override fun toString() = "tithi=$tithi purnamaTilemMissed=$ptMissed purnamaTilemExtra=$ptExtra"
    }

    private fun score(rows: List<G>, rule: (LocalDate) -> Int): Score {
        var tithi = 0; var missed = 0; var extra = 0; var multi = 0
        for (g in rows) {
            val n = rule(g.date)
            if (g.tithis.size > 1) multi++
            if (n !in g.tithis) tithi++
            val goldenPt = g.purnama || g.tilem
            val oursPt = n == 15 || n == 30
            if (goldenPt && !(g.purnama && n == 15 || g.tilem && n == 30)) missed++
            if (oursPt && !goldenPt) extra++
        }
        return Score(tithi, missed, extra, multi)
    }

    private fun mismatchList(rows: List<G>, rule: (LocalDate) -> Int) =
        rows.mapNotNull { g ->
            val n = rule(g.date)
            if (n in g.tithis) null else "${g.date} golden=${g.tithis.joinToString("/") { Lunar.tithiOf(it).label() }} ours=${Lunar.tithiOf(n).label()}"
        }

    private fun Tithi.label() = (if (type == TithiType.PENANGGAL) "P" else "K") + number

    @Test
    fun report() {
        val (ranges, rows) = golden()
        val byRange = ranges.map { r -> r to rows.filter { it.date >= r.from && it.date <= r.to } }
        val out = StringBuilder()
        fun line(s: String) { out.appendLine(s); println(s) }

        line("LUNAR GOLDEN REPORT: ${rows.size} golden days in ${ranges.size} ranges; " +
            "golden Purnama/Tilem days=${rows.count { it.purnama || it.tilem }}; cells with two tithi numbers=${rows.count { it.tithis.size > 1 }}")
        // Golden structure: day-to-day steps (1 = normal, 2 = skipped tithi, 0 = repeated tithi).
        for ((r, rs) in byRange) {
            val steps = rs.zipWithNext { a, b -> (b.tithis.first() - a.tithis.last() + 30) % 30 }
            val skips = rs.zipWithNext().filter { (a, b) -> (b.tithis.first() - a.tithis.last() + 30) % 30 == 2 }.map { it.second.date }
            line("range ${r.from}..${r.to}: steps=${steps.groupingBy { it }.eachCount().toSortedMap()} skipDays=$skips")
        }

        val candidates = linkedMapOf<String, (LocalDate) -> Int>()
        for (h in 0..23) {
            val t = LocalTime.of(h, 0)
            candidates["true elongation at %02d:00".format(h)] = { d -> tithiAtWita(d, t) }
        }
        candidates["true elongation at Denpasar sunrise"] = { d -> tithiAtWita(d, sunriseDenpasar(d)) }
        candidates["true elongation, tithi ending in day"] = { d -> endingTithi(d) }
        for (h in 0..23) {
            val t = LocalTime.of(h, 0)
            candidates["mean elongation at %02d:00".format(h)] = { d -> meanAtWita(d, t) }
        }
        candidates["tithiOn (TITHI_REFERENCE=${Lunar.TITHI_REFERENCE})"] = { d ->
            Lunar.tithiOn(d).let { if (it.type == TithiType.PENANGGAL) it.number else it.number + 15 }
        }

        line("")
        line("candidate | total | " + byRange.joinToString(" | ") { "${it.first.from}..${it.first.to}" })
        for ((name, rule) in candidates) {
            val total = score(rows, rule)
            line("$name | $total | " + byRange.joinToString(" | ") { score(it.second, rule).toString() })
        }

        // Width of the zero-mismatch window per model, scanned every minute over the day.
        for ((model, f) in listOf<Pair<String, (LocalDate, LocalTime) -> Int>>(
            "true elongation" to { d, t -> tithiAtWita(d, t) },
            "mean elongation" to { d, t -> meanAtWita(d, t) },
        )) {
            val scan = (0 until 1440).map { i -> LocalTime.of(0, 0).plusMinutes(i.toLong()) }
                .map { t -> t to score(rows) { d -> f(d, t) }.tithi }
            val min = scan.minOf { it.second }
            val at = scan.filter { it.second == min }.map { it.first }
            line("")
            line("$model, 1-minute scan: minimum tithi mismatches=$min of ${rows.size} at ${at.first()}..${at.last()} (${at.size} sample times)")
        }

        // Diagnostic only: a strict arithmetic rule (one tithi skipped every 63 days), best fit over phase/offset.
        var best = Int.MAX_VALUE; var bestDesc = ""
        val d0 = rows.first().date.toEpochDay()
        for (phase in 0 until 63) for (n0 in 0 until 30) {
            val rule = { d: LocalDate ->
                val x = d.toEpochDay() - d0
                (n0 + x + Math.floorDiv(x + phase, 63L)).mod(30L).toInt() + 1
            }
            val s = score(rows, rule).tithi
            if (s < best) { best = s; bestDesc = "phase=$phase n0=$n0" }
        }
        line("")
        line("diagnostic: strict 63-day skip rule, best fit ($bestDesc): tithi mismatches=$best of ${rows.size}")

        // Outside the golden ranges (no official data): how far tithiOn drifts from that strict 63-day rule,
        // and where tithiOn's Purnama/Tilem fall against the USNO astronomical WITA date.
        val bp = bestDesc.substringAfter("phase=").substringBefore(" ").toLong()
        val bn = bestDesc.substringAfter("n0=").toLong()
        fun strict(d: LocalDate): Int { val x = d.toEpochDay() - d0; return (bn + x + Math.floorDiv(x + bp, 63L)).mod(30L).toInt() + 1 }
        fun on(d: LocalDate) = Lunar.tithiOn(d).let { if (it.type == TithiType.PENANGGAL) it.number else it.number + 15 }
        val usno = LunarGoldenReportTest::class.java.getResourceAsStream("/golden/golden2/moon-phases.csv")!!
            .bufferedReader().readLines().drop(1).filter { it.isNotBlank() }.map { it.split(",") }
        line("")
        line("year | days tithiOn != strict-63 rule | Purnama/Tilem vs USNO WITA date: same / 1 day earlier / 1 day later / other")
        for (y in 2024..2035) {
            var diff = 0
            var dd = LocalDate.of(y, 1, 1)
            while (dd.year == y) { if (on(dd) != strict(dd)) diff++; dd = dd.plusDays(1) }
            val other = mutableListOf<String>()
            val offs = usno.filter { it[2].startsWith("$y-") }.map { (ph, _, wita) ->
                val astro = LocalDate.parse(wita)
                val target = if (ph == "full") 15 else 30
                val ours = (-3L..3L).map { astro.plusDays(it) }.firstOrNull { on(it) == target }
                val off = if (ours == null) 99 else (ours.toEpochDay() - astro.toEpochDay()).toInt()
                if (off !in -1..1) other += "$ph $wita:" + (if (ours == null) "tithi ${if (target == 15) 15 else 30} skipped" else "offset $off")
                off
            }
            line("$y | $diff | ${offs.count { it == 0 }} / ${offs.count { it == -1 }} / ${offs.count { it == 1 }} / ${offs.count { it !in -1..1 }} $other")
        }

        line("")
        line("tithiOn mismatches:")
        mismatchList(rows) { d -> Lunar.tithiOn(d).let { if (it.type == TithiType.PENANGGAL) it.number else it.number + 15 } }
            .forEach { line("  $it") }

        java.io.File("build/lunar-golden-report.txt").writeText(out.toString())
        assertTrue(rows.isNotEmpty())
    }
}
