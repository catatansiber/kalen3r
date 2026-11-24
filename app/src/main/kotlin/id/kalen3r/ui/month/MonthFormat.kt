package id.kalen3r.ui.month

import id.kalen3r.core.BaliDay
import id.kalen3r.core.Pancawara
import id.kalen3r.core.RerainanKind
import id.kalen3r.core.Sasih
import id.kalen3r.core.Saptawara
import java.time.LocalDate

data class LegendEntry(val date: LocalDate, val names: List<String>, val primary: RerainanKind)

/** Pure Indonesian formatting helpers (JVM-testable; no Android dependency). */
object MonthFormat {
    private val MONTHS = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember",
    )
    private val WEEKDAYS = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    fun monthName(month: Int) = MONTHS[month - 1]
    fun monthTitle(year: Int, month: Int) = "${monthName(month)} $year"
    fun longDate(d: LocalDate) = "${WEEKDAYS[d.dayOfWeek.value - 1]}, ${d.dayOfMonth} ${monthName(d.monthValue)} ${d.year}"
    fun shortDay(d: LocalDate) = "${d.dayOfMonth} ${monthName(d.monthValue).take(3)}"

    fun sasihName(s: Sasih) = s.name.lowercase().replaceFirstChar { it.uppercase() }
    fun saptawaraName(s: Saptawara) = s.name.lowercase().replaceFirstChar { it.uppercase() }
    fun pancawaraName(p: Pancawara) = p.name.lowercase().replaceFirstChar { it.uppercase() }

    /** "Çaka 1948 · Sasih Kalima–Kanem": sasih of the first and last day (one name when equal). */
    fun sasihSubtitle(days: List<BaliDay>): String {
        val a = sasihName(days.first().sasih)
        val b = sasihName(days.last().sasih)
        val range = if (a == b) a else "$a–$b"
        val ya = days.first().cakaYear
        val yb = days.last().cakaYear
        val year = if (ya == yb) "$ya" else "$ya–$yb"
        return "Çaka $year · Sasih $range"
    }

    /** Days of the month with a rerainan; Purnama, Tilem and Kajeng Kliwon first, then the rest. */
    fun legend(days: List<BaliDay>): List<LegendEntry> = days.filter { it.rerainan.isNotEmpty() }.map { d ->
        val order = listOf(RerainanKind.PURNAMA, RerainanKind.TILEM, RerainanKind.KAJENG_KLIWON)
        val sorted = d.rerainan.sortedBy { r -> order.indexOf(r.kind).let { if (it < 0) order.size else it } }
        LegendEntry(d.date, sorted.map { it.name }.distinct(), sorted.first().kind)
    }
}
