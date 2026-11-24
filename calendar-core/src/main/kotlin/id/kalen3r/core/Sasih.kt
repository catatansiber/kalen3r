package id.kalen3r.core

import java.time.LocalDate

/** Balinese lunar months, in order. KANAWA is the 9th (site spelling "Kesanga"); Nyepi starts KADASA. */
enum class Sasih { KASA, KARO, KATIGA, KAPAT, KALIMA, KANEM, KAPITU, KAWOLU, KANAWA, KADASA, DESTA, SADA }

/** A day's sasih; isNampih marks the intercalary repeat of [sasih] (site: "Mala Sadha"). */
data class SasihInfo(val sasih: Sasih, val isNampih: Boolean)

/** Sasih of [date] under [Lunar.TITHI_RULE]. */
fun sasihOn(date: LocalDate): SasihInfo = SasihCalendar.DEFAULT.sasihOn(date)

/** Çaka year of [date]: Gregorian year - 78 from Nyepi on, - 79 before it. */
fun cakaYear(date: LocalDate): Int = SasihCalendar.DEFAULT.cakaYear(date)

/** Nyepi (first day of Sasih Kadasa, the day after Tilem Kesanga) in [gregorianYear]. */
fun nyepi(gregorianYear: Int): LocalDate = SasihCalendar.DEFAULT.nyepi(gregorianYear)

/**
 * The nampih (intercalary) sasih of Çaka year [saka], or null.
 *
 * Rule (Çaka year mod 19, seven leap months per 19 years): 0, 6, 11 -> nampih DESTA; 3, 8, 14, 16 -> nampih
 * SADA; Çaka 1925 has no nampih Desta. The nampih month comes right AFTER the regular month of the same name.
 * Source: peradnya, balinese-date-js-lib, src/BalineseDate.ts, _F_CALC_SASIH_INFO (the "Nampih Sasih"
 * branch used from Çaka 1925 / 2003; the 1993-2002 "Sasih Kesinambungan" and pre-1993 "Malamasa" rules
 * are not modelled, so dates before Nyepi 2003 are unverified).
 * Checked here: golden 2024 shows Sadha then "Mala Sadha" (Çaka 1946, 1946 mod 19 = 8) and the published
 * Nyepi dates 2024-2030 imply 13-month years exactly for Çaka 1946 and 1949. The position and order of a
 * nampih DESTA (first expected in Çaka 1949, 2027) are not verified by any data in this repo.
 */
fun nampihSasihOf(saka: Int): Sasih? = when (Math.floorMod(saka, 19)) {
    0, 6, 11 -> if (saka == 1925) null else Sasih.DESTA
    3, 8, 14, 16 -> Sasih.SADA
    else -> null
}

/**
 * Sasih, Çaka year and Nyepi derived from the ordered sequence of lunar months of a tithi rule.
 *
 * A lunar month starts on the first day whose tithi number (1..30) is lower than the previous day's: the
 * day after Tilem. This holds when tithi 30 or 1 is skipped (ngunalatri), so a month may start on penanggal 2.
 * Months are named by walking from [ANCHOR] (Nyepi 2024-03-11 = first day of Kadasa, Çaka 1946,
 * golden kalenderbali.com) one month at a time, inserting the nampih month of [nampihSasihOf]; the Çaka year
 * increments on entering the regular Kadasa (Nyepi).
 *
 * [tithiNumber] is a parameter so that the Task 6 discrimination report can run both [TithiRule]s.
 */
class SasihCalendar(private val tithiNumber: (LocalDate) -> Int) {
    private data class State(val sasih: Sasih, val nampih: Boolean, val saka: Int)

    private fun isMonthStart(d: LocalDate) = tithiNumber(d) < tithiNumber(d.minusDays(1))

    private fun monthStart(date: LocalDate): LocalDate {
        var d = date
        repeat(40) { if (isMonthStart(d)) return d; d = d.minusDays(1) }
        error("no month start within 40 days before $date")
    }

    private fun nextStart(start: LocalDate): LocalDate {
        var d = start.plusDays(1)
        repeat(40) { if (isMonthStart(d)) return d; d = d.plusDays(1) }
        error("no month start within 40 days after $start")
    }

    private fun next(s: State): State {
        if (!s.nampih && nampihSasihOf(s.saka) == s.sasih) return s.copy(nampih = true)
        val n = Sasih.entries[(s.sasih.ordinal + 1) % 12]
        return State(n, false, if (n == Sasih.KADASA) s.saka + 1 else s.saka)
    }

    private fun prev(s: State): State {
        if (s.nampih) return s.copy(nampih = false)
        val p = Sasih.entries[(s.sasih.ordinal + 11) % 12]
        val saka = if (s.sasih == Sasih.KADASA) s.saka - 1 else s.saka
        return State(p, nampihSasihOf(saka) == p, saka)
    }

    /** Month (start day, state) containing [date]. */
    private fun monthOf(date: LocalDate): Pair<LocalDate, State> {
        val target = monthStart(date)
        var start = ANCHOR
        var st = State(Sasih.KADASA, false, ANCHOR_SAKA)
        if (target >= start) {
            while (true) {
                val n = nextStart(start)
                if (n > target) break
                start = n; st = next(st)
            }
        } else {
            while (start > target) {
                start = monthStart(start.minusDays(1)); st = prev(st)
            }
        }
        check(start == target) { "month walk missed $target (at $start)" }
        return start to st
    }

    fun sasihOn(date: LocalDate): SasihInfo = monthOf(date).second.let { SasihInfo(it.sasih, it.nampih) }

    fun cakaYear(date: LocalDate): Int = monthOf(date).second.saka

    fun nyepi(gregorianYear: Int): LocalDate {
        // Nyepi lies in early March to early April; the month containing April 15 is Kadasa, Desta or the
        // month after Kesanga at worst, so walk back from there to the regular Kadasa of Çaka year - 78.
        var (start, st) = monthOf(LocalDate.of(gregorianYear, 4, 15))
        val saka = gregorianYear - 78
        repeat(4) {
            if (st.sasih == Sasih.KADASA && !st.nampih && st.saka == saka) return start
            start = monthStart(start.minusDays(1)); st = prev(st)
        }
        error("no Nyepi found for $gregorianYear")
    }

    companion object {
        /** Nyepi 2024 = first day of Kadasa, Çaka 1946 (golden days-2024.csv and hari-raya.csv). */
        val ANCHOR: LocalDate = LocalDate.of(2024, 3, 11)
        const val ANCHOR_SAKA = 1946

        val DEFAULT = SasihCalendar { Lunar.tithiNumberOn(it) }

        fun forRule(rule: TithiRule) = SasihCalendar { Lunar.tithiNumberOn(it, rule) }
    }
}
