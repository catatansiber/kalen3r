package id.kalen3r.core

import java.time.LocalDate

/**
 * The date ranges covered by golden (reference) data: 2024-01-01..2024-06-30 and 2026-11-01..2026-11-30.
 * [BaliDay.verified] is true only for dates inside these ranges. A test ties this list to
 * calendar-core/src/test/resources/golden/coverage.json so the two cannot drift; when the owner adds golden
 * data, update coverage.json, GoldenLoader.DAY_FILES and this list together.
 *
 * Pawukon-based values (wewaran, urip, wuku) are exact everywhere; `verified` concerns the lunar-based values
 * (tithi, sasih, Purnama, Tilem and the holy days derived from them).
 */
val GOLDEN_COVERAGE_RANGES: List<ClosedRange<LocalDate>> = listOf(
    LocalDate.of(2024, 1, 1)..LocalDate.of(2024, 6, 30),
    LocalDate.of(2026, 11, 1)..LocalDate.of(2026, 11, 30),
)

internal fun isGoldenVerified(date: LocalDate): Boolean = GOLDEN_COVERAGE_RANGES.any { date in it }

/** One fully resolved day. Lunar fields (sasih, tithi, purnama/tilem, rerainan) are the CORRECTED values. */
data class BaliDay(
    val date: LocalDate,
    /** Wuku number 1..30. */
    val wuku: Int,
    /** Pawukon cycle index 0..209. */
    val index: Int,
    val saptawara: Saptawara,
    val pancawara: Pancawara,
    val triwara: Triwara,
    /** 0-based index into [Pawukon.CATURWARA_NAMES]. */
    val caturwara: Int,
    /** 0-based index into [Pawukon.SADWARA_NAMES]. */
    val sadwara: Int,
    /** 0-based index into [Pawukon.ASTAWARA_NAMES]. */
    val astawara: Int,
    /** 0-based index into [Pawukon.SANGAWARA_NAMES]. */
    val sangawara: Int,
    /** 0-based index into [Pawukon.DASAWARA_NAMES]. */
    val dasawara: Int,
    val urip: Int,
    val cakaYear: Int,
    val sasih: Sasih,
    /** True on the intercalary repeat of [sasih]. */
    val isNampih: Boolean,
    val tithi: Tithi,
    val isPurnama: Boolean,
    val isTilem: Boolean,
    val rerainan: List<Rerainan>,
    /** True when [date] lies within [GOLDEN_COVERAGE_RANGES] (lunar-based values checked against golden data). */
    val verified: Boolean,
)

interface BaliCalendar {
    fun day(date: LocalDate): BaliDay

    /** All days of [month] (1..12) of [year]; throws IllegalArgumentException for a month outside 1..12. */
    fun month(year: Int, month: Int): List<BaliDay>
    fun otonan(birth: LocalDate, count: Int, from: LocalDate): List<LocalDate>
    fun findByWewaran(wuku: Int, saptawara: Saptawara, pancawara: Pancawara, from: LocalDate, count: Int): List<LocalDate>
    fun odalanByPawukon(wuku: Int, sapta: Saptawara, panca: Pancawara, from: LocalDate, to: LocalDate): List<LocalDate>

    /** All dates in Gregorian [year] whose corrected sasih and tithi match (both occurrences for a nampih sasih). */
    fun odalanBySasih(sasih: Sasih, tithi: Tithi, year: Int): List<LocalDate>

    /** Holy days in [from]..[to] inclusive of the given [kinds], in ascending date order. */
    fun rerainan(from: LocalDate, to: LocalDate, kinds: Set<RerainanKind>): List<Rerainan>

    companion object {
        val default: BaliCalendar by lazy { DefaultBaliCalendar() }
    }
}

class DefaultBaliCalendar(private val maxCachedMonths: Int = 240) : BaliCalendar {
    private val cache = object : LinkedHashMap<Int, List<BaliDay>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<BaliDay>>) = size > maxCachedMonths
    }

    private fun correctedLunar(date: LocalDate): RawLunarDay = Corrections.apply(rawLunarDay(date))

    override fun day(date: LocalDate): BaliDay {
        val memo = HashMap<LocalDate, RawLunarDay>()
        return build(date, ::cakaYear) { d -> memo.getOrPut(d) { correctedLunar(d) } }
    }

    /** [lunar] must return the corrected lunar day; it is called for [date] and its two neighbours. */
    private fun build(date: LocalDate, caka: (LocalDate) -> Int, lunar: (LocalDate) -> RawLunarDay): BaliDay {
        val p = Pawukon.of(date)
        val l = lunar(date)
        return BaliDay(
            date = date,
            wuku = p.wuku,
            index = p.index,
            saptawara = p.saptawara,
            pancawara = p.pancawara,
            triwara = p.triwara,
            caturwara = p.caturwara,
            sadwara = p.sadwara,
            astawara = p.astawara,
            sangawara = p.sangawara,
            dasawara = p.dasawara,
            urip = p.urip,
            cakaYear = caka(date),
            sasih = l.sasih,
            isNampih = l.isNampihSasih,
            tithi = l.tithi,
            isPurnama = l.isPurnama,
            isTilem = l.isTilem,
            rerainan = RerainanRules.on(date, lunar),
            verified = isGoldenVerified(date),
        )
    }

    override fun month(year: Int, month: Int): List<BaliDay> {
        require(month in 1..12) { "bulan harus 1..12, bukan $month" }
        val key = year * 12 + (month - 1)
        synchronized(cache) { cache[key]?.let { return it } }
        val first = LocalDate.of(year, month, 1)
        val n = first.lengthOfMonth()
        // Each lunar day is computed once for the month window (neighbours are needed by the Nyepi/Siwaratri rules).
        val window = HashMap<LocalDate, RawLunarDay>()
        val lunar = { d: LocalDate -> window.getOrPut(d) { correctedLunar(d) } }
        // The Çaka year only changes at Nyepi: one engine walk per month unless the month contains it.
        val last = first.plusDays(n - 1L)
        val cakaFirst = cakaYear(first)
        val sameYear = cakaYear(last) == cakaFirst
        val caka = { d: LocalDate -> if (sameYear) cakaFirst else cakaYear(d) }
        val days = List(n) { build(first.plusDays(it.toLong()), caka, lunar) }
        synchronized(cache) { cache[key] = days }
        return days
    }

    override fun otonan(birth: LocalDate, count: Int, from: LocalDate) = id.kalen3r.core.otonan(birth, count, from)

    override fun findByWewaran(wuku: Int, saptawara: Saptawara, pancawara: Pancawara, from: LocalDate, count: Int) =
        id.kalen3r.core.findByWewaran(wuku, saptawara, pancawara, from, count)

    override fun odalanByPawukon(wuku: Int, sapta: Saptawara, panca: Pancawara, from: LocalDate, to: LocalDate) =
        id.kalen3r.core.odalanByPawukon(wuku, sapta, panca, from, to)

    override fun odalanBySasih(sasih: Sasih, tithi: Tithi, year: Int): List<LocalDate> =
        (1..12).flatMap { month(year, it) }.filter { it.sasih == sasih && it.tithi == tithi }.map { it.date }

    override fun rerainan(from: LocalDate, to: LocalDate, kinds: Set<RerainanKind>): List<Rerainan> {
        if (kinds.isEmpty()) return emptyList()
        val out = ArrayList<Rerainan>()
        val window = HashMap<LocalDate, RawLunarDay>()
        val lunar = { x: LocalDate -> window.getOrPut(x) { correctedLunar(x) } }
        var d = from
        while (!d.isAfter(to)) {
            for (r in RerainanRules.on(d, lunar)) if (r.kind in kinds) out += r
            window.keys.removeAll { it < d.minusDays(1) } // keep the window small for long ranges
            d = d.plusDays(1)
        }
        return out
    }
}
