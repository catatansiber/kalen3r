package id.kalen3r.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.floor
import kotlin.math.sin

enum class TithiType { PENANGGAL, PANGELONG }

/**
 * Candidate rules that assign one tithi to a civil day. Both reproduce all 212 golden days; see
 * calendar-core/docs/lunar-report.md ("Task 6: discrimination study") for why [PENGALANTAKA] is the default.
 */
enum class TithiRule {
    /** Balinese arithmetic pengalantaka (Eka Sungsang ke Paing): one tithi skipped every 63 days exactly. */
    PENGALANTAKA,

    /** Task 5 rule: mean elongation (Meeus 47.2) at [Lunar.TITHI_REFERENCE] WITA; skips every ~62.9 days. */
    MEAN_ELONGATION,
}

/** A tithi (lunar day). number is 1..15 within its half; Purnama = PENANGGAL 15, Tilem = PANGELONG 15. */
data class Tithi(val type: TithiType, val number: Int) {
    init {
        require(number in 1..15) { "tithi number $number out of 1..15" }
    }

    val isPurnama: Boolean get() = type == TithiType.PENANGGAL && number == 15
    val isTilem: Boolean get() = type == TithiType.PANGELONG && number == 15
}

/**
 * Raw (uncorrected) lunar computations.
 *
 * - Phase instants: Meeus, Astronomical Algorithms 2nd ed., ch. 49 (all periodic and additional terms).
 * - True elongation ([elongationDeg], [tithiAt]): Moon longitude from ch. 47 (table 47.A longitude terms,
 *   plus A1/A2/L'-F additive terms) minus Sun apparent longitude from ch. 25 (low precision). Nutation is
 *   common to both longitudes and cancels in the difference, so it is omitted. Used for astronomy only
 *   (checked against USNO phase times), NOT for the tithi of a civil day.
 * - Time scale: Meeus works in TT; UTC = TT - deltaT (Espenak-Meeus polynomial for 2005..2050).
 *
 * The tithi of a civil day ([tithiOn]) follows [TITHI_RULE], the single switch point:
 * - [TithiRule.PENGALANTAKA] (default): the Balinese arithmetic count, no astronomy and no fitted constant.
 *   Day count from [PENGALANTAKA_PIVOT] plus one extra tithi every 63 days.
 * - [TithiRule.MEAN_ELONGATION] (Task 5): floor(D / 12 deg) + 1 with D the MEAN elongation (Meeus 47.2)
 *   at [TITHI_REFERENCE] WITA. D advances 12.19 deg/day, so it skips a tithi every ~62.9 days.
 * Both rules match all 212 golden days. They drift apart outside the golden ranges (5 days in 2025 growing
 * to 36 in 2035). The only fact found that separates them, Tilem 2025-05-27, agrees with the pengalantaka,
 * but it comes from a web search summary whose page could not be fetched in full (low-to-medium
 * confidence); see calendar-core/docs/lunar-report.md. With the true elongation the best reference hour
 * still mismatched 55 of the 212 golden days. Remaining differences from the official calendar are the
 * correction table's job (Task 7).
 */
object Lunar {
    val WITA: ZoneOffset = ZoneOffset.ofHours(8)

    /** The rule [tithiOn] uses by default. Change only together with docs/lunar-report.md. */
    val TITHI_RULE: TithiRule = TithiRule.PENGALANTAKA

    /**
     * Reference time of day (WITA) for [TithiRule.MEAN_ELONGATION]. A FITTED constant, not a physical
     * time: the centre of the 12:45..13:03 window that gives 0 mismatches on the 212 golden days (either
     * edge is 9 minutes away). The strict 63-day pengalantaka fits the same 212 days equally well with no
     * fitted constant, and the Task 6 discrimination study made it the default; see docs/lunar-report.md.
     */
    val TITHI_REFERENCE: LocalTime = LocalTime.of(12, 54)

    /**
     * Pengalantaka pivot: 2000-01-18 is penanggal 12 (sasih Kapitu, Saka 1921), and a ngunalatri (one
     * tithi skipped) falls on every day whose distance from the pivot is 1 mod 63. Source:
     * peradnya, balinese-date-js-lib (Apache-2.0; see THIRD_PARTY_NOTICES.md), src/internal/Pivot.ts
     * (PIVOT_2000) and the day count dayTotal = sasihDay + dayDiff + ceil(dayDiff / 63) in src/BalineseDate.ts
     * (_F_CALC_SASIH_DAY for the day number; the same count opens _F_CALC_SASIH_INFO, which also holds the
     * nampih sasih rule used in Sasih.kt), "Pengalantaka Eka Sungsang ke Paing", used there from 2000-01-06. Earlier dates follow another pengalantaka (ke Pon) that is
     * not modelled here; dates before 2000 are extrapolated and unverified.
     */
    val PENGALANTAKA_PIVOT: LocalDate = LocalDate.of(2000, 1, 18)
    private const val PENGALANTAKA_PIVOT_TITHI = 12
    private const val NGUNALATRI_DAYS = 63L

    private const val DEG = Math.PI / 180.0
    private const val J2000 = 2451545.0
    private const val UNIX_EPOCH_JD = 2440587.5

    private fun sind(x: Double) = sin(x * DEG)
    private fun norm360(x: Double): Double = ((x % 360.0) + 360.0) % 360.0

    // ---- time conversions -------------------------------------------------------------------

    /** deltaT = TT - UTC in seconds (Espenak & Meeus 2006 polynomial, valid 2005..2050; fine nearby). */
    fun deltaTSeconds(year: Double): Double {
        val t = year - 2000.0
        return 62.92 + 0.32217 * t + 0.005589 * t * t
    }

    private fun jdUtc(i: Instant): Double = UNIX_EPOCH_JD + (i.epochSecond + i.nano / 1e9) / 86400.0

    private fun jdeOf(i: Instant): Double {
        val jd = jdUtc(i)
        val year = 2000.0 + (jd - J2000) / 365.25
        return jd + deltaTSeconds(year) / 86400.0
    }

    private fun instantOfJde(jde: Double): Instant {
        val year = 2000.0 + (jde - J2000) / 365.25
        val jdUtc = jde - deltaTSeconds(year) / 86400.0
        val ms = Math.round((jdUtc - UNIX_EPOCH_JD) * 86400000.0)
        return Instant.ofEpochMilli(ms)
    }

    // ---- Meeus ch. 49: phases ----------------------------------------------------------------

    /** New moon of lunation k (k = 0 is 2000-01-06), as a UTC instant. */
    fun newMoonUtc(k: Int): Instant = instantOfJde(phaseJde(k.toDouble(), full = false))

    /** Full moon following new moon k (Meeus k + 0.5), as a UTC instant. */
    fun fullMoonUtc(k: Int): Instant = instantOfJde(phaseJde(k + 0.5, full = true))

    private fun phaseJde(k: Double, full: Boolean): Double {
        val t = k / 1236.85
        val t2 = t * t; val t3 = t2 * t; val t4 = t3 * t
        var jde = 2451550.09766 + 29.530588861 * k + 0.00015437 * t2 - 0.000000150 * t3 + 0.00000000073 * t4
        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val m = 2.5534 + 29.10535670 * k - 0.0000014 * t2 - 0.00000011 * t3
        val mp = 201.5643 + 385.81693528 * k + 0.0107582 * t2 + 0.00001238 * t3 - 0.000000058 * t4
        val f = 160.7108 + 390.67050284 * k - 0.0016118 * t2 - 0.00000227 * t3 + 0.000000011 * t4
        val om = 124.7746 - 1.56375588 * k + 0.0020672 * t2 + 0.00000215 * t3

        jde += if (!full) {
            -0.40720 * sind(mp) + 0.17241 * e * sind(m) + 0.01608 * sind(2 * mp) + 0.01039 * sind(2 * f) +
                0.00739 * e * sind(mp - m) - 0.00514 * e * sind(mp + m) + 0.00208 * e * e * sind(2 * m)
        } else {
            -0.40614 * sind(mp) + 0.17302 * e * sind(m) + 0.01614 * sind(2 * mp) + 0.01043 * sind(2 * f) +
                0.00734 * e * sind(mp - m) - 0.00515 * e * sind(mp + m) + 0.00209 * e * e * sind(2 * m)
        }
        jde += -0.00111 * sind(mp - 2 * f) - 0.00057 * sind(mp + 2 * f) + 0.00056 * e * sind(2 * mp + m) -
            0.00042 * sind(3 * mp) + 0.00042 * e * sind(m + 2 * f) + 0.00038 * e * sind(m - 2 * f) -
            0.00024 * e * sind(2 * mp - m) - 0.00017 * sind(om) - 0.00007 * sind(mp + 2 * m) +
            0.00004 * sind(2 * mp - 2 * f) + 0.00004 * sind(3 * m) + 0.00003 * sind(mp + m - 2 * f) +
            0.00003 * sind(2 * mp + 2 * f) - 0.00003 * sind(mp + m + 2 * f) + 0.00003 * sind(mp - m + 2 * f) -
            0.00002 * sind(mp - m - 2 * f) - 0.00002 * sind(3 * mp + m) + 0.00002 * sind(4 * mp)

        val a = doubleArrayOf(
            299.77 + 0.107408 * k - 0.009173 * t2,
            251.88 + 0.016321 * k,
            251.83 + 26.651886 * k,
            349.42 + 36.412478 * k,
            84.66 + 18.206239 * k,
            141.74 + 53.303771 * k,
            207.14 + 2.453732 * k,
            154.84 + 7.306860 * k,
            34.52 + 27.261239 * k,
            207.19 + 0.121824 * k,
            291.34 + 1.844379 * k,
            161.72 + 24.198154 * k,
            239.56 + 25.513099 * k,
            331.55 + 3.592518 * k,
        )
        val c = intArrayOf(325, 165, 164, 126, 110, 62, 60, 56, 47, 42, 40, 37, 35, 23)
        for (i in a.indices) jde += c[i] * 1e-6 * sind(a[i])
        return jde
    }

    // ---- Meeus ch. 47 / ch. 25: elongation ---------------------------------------------------

    /** Table 47.A longitude terms: D, M, M', F, coefficient (1e-6 degree). */
    private val MOON_LON = arrayOf(
        intArrayOf(0, 0, 1, 0, 6288774), intArrayOf(2, 0, -1, 0, 1274027), intArrayOf(2, 0, 0, 0, 658314),
        intArrayOf(0, 0, 2, 0, 213618), intArrayOf(0, 1, 0, 0, -185116), intArrayOf(0, 0, 0, 2, -114332),
        intArrayOf(2, 0, -2, 0, 58793), intArrayOf(2, -1, -1, 0, 57066), intArrayOf(2, 0, 1, 0, 53322),
        intArrayOf(2, -1, 0, 0, 45758), intArrayOf(0, 1, -1, 0, -40923), intArrayOf(1, 0, 0, 0, -34720),
        intArrayOf(0, 1, 1, 0, -30383), intArrayOf(2, 0, 0, -2, 15327), intArrayOf(0, 0, 1, 2, -12528),
        intArrayOf(0, 0, 1, -2, 10980), intArrayOf(4, 0, -1, 0, 10675), intArrayOf(0, 0, 3, 0, 10034),
        intArrayOf(4, 0, -2, 0, 8548), intArrayOf(2, 1, -1, 0, -7888), intArrayOf(2, 1, 0, 0, -6766),
        intArrayOf(1, 0, -1, 0, -5163), intArrayOf(1, 1, 0, 0, 4987), intArrayOf(2, -1, 1, 0, 4036),
        intArrayOf(2, 0, 2, 0, 3994), intArrayOf(4, 0, 0, 0, 3861), intArrayOf(2, 0, -3, 0, 3665),
        intArrayOf(0, 1, -2, 0, -2689), intArrayOf(2, 0, -1, 2, -2602), intArrayOf(2, -1, -2, 0, 2390),
        intArrayOf(1, 0, 1, 0, -2348), intArrayOf(2, -2, 0, 0, 2236), intArrayOf(0, 1, 2, 0, -2120),
        intArrayOf(0, 2, 0, 0, -2069), intArrayOf(2, -2, -1, 0, 2048), intArrayOf(2, 0, 1, -2, -1773),
        intArrayOf(2, 0, 0, 2, -1595), intArrayOf(4, -1, -1, 0, 1215), intArrayOf(0, 0, 2, 2, -1110),
        intArrayOf(3, 0, -1, 0, -892), intArrayOf(2, 1, 1, 0, -810), intArrayOf(4, -1, -2, 0, 759),
        intArrayOf(0, 2, -1, 0, -713), intArrayOf(2, 2, -1, 0, -700), intArrayOf(2, 1, -2, 0, 691),
        intArrayOf(2, -1, 0, -2, 596), intArrayOf(4, 0, 1, 0, 549), intArrayOf(0, 0, 4, 0, 537),
        intArrayOf(4, -1, 0, 0, 520), intArrayOf(1, 0, -2, 0, -487), intArrayOf(2, 1, 0, -2, -399),
        intArrayOf(0, 0, 2, -2, -381), intArrayOf(1, 1, 1, 0, 351), intArrayOf(3, 0, -2, 0, -340),
        intArrayOf(4, 0, -3, 0, 330), intArrayOf(2, -1, 2, 0, 327), intArrayOf(0, 2, 1, 0, -323),
        intArrayOf(1, 1, -1, 0, 299), intArrayOf(2, 0, 3, 0, 294),
    )

    private fun centuries(i: Instant) = (jdeOf(i) - J2000) / 36525.0

    /** Mean elongation D of the Moon (Meeus 47.2), degrees in [0, 360). */
    fun meanElongationDeg(i: Instant): Double {
        val t = centuries(i)
        return norm360(297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868.0 - t * t * t * t / 113065000.0)
    }

    /** True elongation (apparent Moon longitude minus apparent Sun longitude), degrees in [0, 360). */
    fun elongationDeg(i: Instant): Double {
        val t = centuries(i)
        val t2 = t * t; val t3 = t2 * t; val t4 = t3 * t
        val lp = 218.3164477 + 481267.88123421 * t - 0.0015786 * t2 + t3 / 538841.0 - t4 / 65194000.0
        val d = 297.8501921 + 445267.1114034 * t - 0.0018819 * t2 + t3 / 545868.0 - t4 / 113065000.0
        val m = 357.5291092 + 35999.0502909 * t - 0.0001536 * t2 + t3 / 24490000.0
        val mp = 134.9633964 + 477198.8675055 * t + 0.0087414 * t2 + t3 / 69699.0 - t4 / 14712000.0
        val f = 93.2720950 + 483202.0175233 * t - 0.0036539 * t2 - t3 / 3526000.0 + t4 / 863310000.0
        val e = 1.0 - 0.002516 * t - 0.0000074 * t2
        val a1 = 119.75 + 131.849 * t
        val a2 = 53.09 + 479264.290 * t
        var sl = 0.0
        for (row in MOON_LON) {
            val eFactor = when (kotlin.math.abs(row[1])) { 1 -> e; 2 -> e * e; else -> 1.0 }
            sl += row[4] * eFactor * sind(row[0] * d + row[1] * m + row[2] * mp + row[3] * f)
        }
        sl += 3958 * sind(a1) + 1962 * sind(lp - f) + 318 * sind(a2)
        val moonLon = lp + sl / 1e6

        // Sun, Meeus ch. 25 (low precision): geometric longitude + equation of centre, then aberration.
        val l0 = 280.46646 + 36000.76983 * t + 0.0003032 * t2
        val ms = 357.52911 + 35999.05029 * t - 0.0001537 * t2
        val c = (1.914602 - 0.004817 * t - 0.000014 * t2) * sind(ms) +
            (0.019993 - 0.000101 * t) * sind(2 * ms) + 0.000289 * sind(3 * ms)
        val sunLon = l0 + c - 0.00569
        return norm360(moonLon - sunLon)
    }

    // ---- tithi ------------------------------------------------------------------------------

    /** Tithi number 1..30 current at instant i: floor(elongation / 12 deg) + 1. */
    fun tithiAt(i: Instant): Int = (floor(elongationDeg(i) / 12.0).toInt()).coerceIn(0, 29) + 1

    /** Maps 1..30 to penanggal 1..15 / pangelong 1..15 (15 = Purnama, 30 = Tilem). */
    fun tithiOf(n: Int): Tithi {
        require(n in 1..30) { "tithi $n out of 1..30" }
        return if (n <= 15) Tithi(TithiType.PENANGGAL, n) else Tithi(TithiType.PANGELONG, n - 15)
    }

    /** Tithi number 1..30 by MEAN elongation at instant i: floor(D / 12 deg) + 1. */
    fun meanTithiAt(i: Instant): Int = (floor(meanElongationDeg(i) / 12.0).toInt()).coerceIn(0, 29) + 1

    /** Tithi number 1..30 of a civil day under the pengalantaka (see [PENGALANTAKA_PIVOT]). */
    fun pengalantakaTithiNumber(date: LocalDate): Int {
        val dd = date.toEpochDay() - PENGALANTAKA_PIVOT.toEpochDay()
        val skips = -Math.floorDiv(-dd, NGUNALATRI_DAYS) // ceil(dd / 63), also for dd < 0
        return (PENGALANTAKA_PIVOT_TITHI - 1 + dd + skips).mod(30L).toInt() + 1
    }

    /** Tithi number 1..30 of a civil day under [rule]. */
    fun tithiNumberOn(date: LocalDate, rule: TithiRule = TITHI_RULE): Int = when (rule) {
        TithiRule.PENGALANTAKA -> pengalantakaTithiNumber(date)
        TithiRule.MEAN_ELONGATION -> meanTithiAt(date.atTime(TITHI_REFERENCE).toInstant(WITA))
    }

    /** Raw tithi of a civil day under [rule] (default [TITHI_RULE]). */
    fun tithiOn(date: LocalDate, rule: TithiRule = TITHI_RULE): Tithi = tithiOf(tithiNumberOn(date, rule))

    /**
     * The tithi skipped (ngunalatri) between the previous day and [date], or null if none. A skipped
     * PENANGGAL 15 / PANGELONG 15 means the raw output has no Purnama/Tilem day in that half-month;
     * how the official calendar labels such days is left to the correction table (Task 7).
     */
    fun skippedTithiOn(date: LocalDate, rule: TithiRule = TITHI_RULE): Tithi? {
        val prev = tithiNumberOn(date.minusDays(1), rule)
        val cur = tithiNumberOn(date, rule)
        return if ((cur - prev + 30) % 30 == 2) tithiOf(prev % 30 + 1) else null
    }
}
