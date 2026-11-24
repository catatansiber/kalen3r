package id.kalen3r.core

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.floor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LunarTest {
    private fun minutesBetween(a: Instant, b: Instant) = abs(Duration.between(a, b).seconds) / 60.0

    /** USNO rows: phase (new|full), UTC instant, WITA date. */
    private fun usno(): List<Triple<String, Instant, LocalDate>> =
        LunarTest::class.java.getResourceAsStream("/golden/golden2/moon-phases.csv")!!
            .bufferedReader().readLines().drop(1).filter { it.isNotBlank() }.map { line ->
                val (phase, utc, wita) = line.split(",")
                val t = LocalDateTime.parse(utc.removeSuffix("Z")).toInstant(ZoneOffset.UTC)
                Triple(phase, t, LocalDate.parse(wita))
            }

    /** Lunation number k of the phase nearest instant t (k = 0 is the 2000-01-06 new moon). */
    private fun kNear(t: Instant, full: Boolean): Int {
        val days = (t.epochSecond - Instant.parse("2000-01-06T18:14:00Z").epochSecond) / 86400.0
        return floor(days / 29.530588861 - (if (full) 0.5 else 0.0) + 0.5).toInt()
    }

    @Test
    fun knownNewMoon2000Jan6() {
        val t = Lunar.newMoonUtc(0)
        assertTrue(minutesBetween(t, Instant.parse("2000-01-06T18:14:00Z")) <= 5.0, "got $t")
    }

    @Test
    fun fullMoonIsHalfCycleAfterNewMoon() {
        for (k in -50..200) {
            val nm = Lunar.newMoonUtc(k)
            val next = Lunar.newMoonUtc(k + 1)
            val fm = Lunar.fullMoonUtc(k)
            val synodic = Duration.between(nm, next).seconds / 86400.0
            assertTrue(abs(synodic - 29.53) <= 0.5, "k=$k synodic=$synodic")
            val half = Duration.between(nm, fm).seconds / 86400.0
            assertTrue(half in 13.5..16.0, "k=$k new->full=$half")
        }
    }

    @Test
    fun meeusPhasesMatchUsnoWithinFiveMinutes() {
        val rows = usno()
        assertTrue(rows.size > 290, "expected ~297 USNO rows, got ${rows.size}")
        var worst = 0.0
        for ((phase, t, _) in rows) {
            val full = phase == "full"
            val k = kNear(t, full)
            val ours = if (full) Lunar.fullMoonUtc(k) else Lunar.newMoonUtc(k)
            val err = minutesBetween(ours, t)
            worst = maxOf(worst, err)
            assertTrue(err <= 5.0, "$phase $t: ours $ours, err $err min")
        }
        println("Meeus ch.49 vs USNO: ${rows.size} rows, worst error %.2f min".format(worst))
    }

    /** The elongation function used by tithiOn must cross 0 deg / 180 deg at the USNO instants. */
    @Test
    fun elongationCrossingsMatchUsnoWithinFiveMinutes() {
        var worst = 0.0
        for ((phase, t, _) in usno()) {
            val target = if (phase == "full") 180.0 else 0.0
            // signed offset of elongation from target, in (-180, 180]
            fun f(x: Instant): Double {
                var d = Lunar.elongationDeg(x) - target
                d = ((d % 360.0) + 540.0) % 360.0 - 180.0
                return d
            }
            var lo = t.minusSeconds(6 * 3600)
            var hi = t.plusSeconds(6 * 3600)
            assertTrue(f(lo) < 0 && f(hi) > 0, "$phase $t not bracketed")
            repeat(40) {
                val mid = lo.plusMillis(Duration.between(lo, hi).toMillis() / 2)
                if (f(mid) < 0) lo = mid else hi = mid
            }
            val err = minutesBetween(lo, t)
            worst = maxOf(worst, err)
            assertTrue(err <= 5.0, "$phase $t: elongation root $lo, err $err min")
        }
        println("Elongation roots vs USNO: worst error %.2f min".format(worst))
    }

    @Test
    fun tithiNumberMapping() {
        assertEquals(Tithi(TithiType.PENANGGAL, 1), Lunar.tithiOf(1))
        assertEquals(Tithi(TithiType.PENANGGAL, 15), Lunar.tithiOf(15))
        assertEquals(Tithi(TithiType.PANGELONG, 1), Lunar.tithiOf(16))
        assertEquals(Tithi(TithiType.PANGELONG, 15), Lunar.tithiOf(30))
        assertTrue(Tithi(TithiType.PENANGGAL, 15).isPurnama)
        assertTrue(Tithi(TithiType.PANGELONG, 15).isTilem)
    }

    @Test
    fun tithiAtIsElongationOverTwelve() {
        // Just after the 2000-01-06 new moon the tithi is penanggal 1; just after full moon, pangelong 1.
        assertEquals(1, Lunar.tithiAt(Lunar.newMoonUtc(0).plusSeconds(600)))
        assertEquals(30, Lunar.tithiAt(Lunar.newMoonUtc(0).minusSeconds(600)))
        assertEquals(16, Lunar.tithiAt(Lunar.fullMoonUtc(0).plusSeconds(600)))
        assertEquals(15, Lunar.tithiAt(Lunar.fullMoonUtc(0).minusSeconds(600)))
    }

    @Test
    fun tithiOnUsesReferenceTimeInWita() {
        val d = LocalDate.of(2026, 11, 24)
        val ref = d.atTime(Lunar.TITHI_REFERENCE).toInstant(Lunar.WITA)
        assertEquals(Lunar.tithiOf(Lunar.meanTithiAt(ref)), Lunar.tithiOn(d))
    }

    @Test
    fun tithiOnNeverRepeatsAndSkipsAboutEvery63Days() {
        var prev = Lunar.tithiOn(LocalDate.of(2023, 12, 31))
        val skips = mutableListOf<LocalDate>()
        var d = LocalDate.of(2024, 1, 1)
        while (d.year <= 2035) {
            val cur = Lunar.tithiOn(d)
            fun n(t: Tithi) = if (t.type == TithiType.PENANGGAL) t.number else t.number + 15
            val step = (n(cur) - n(prev) + 30) % 30
            assertTrue(step == 1 || step == 2, "$d step $step")
            if (step == 2) skips += d
            prev = cur; d = d.plusDays(1)
        }
        skips.zipWithNext { a, b -> b.toEpochDay() - a.toEpochDay() }.forEach { assertTrue(it in 62..64, "gap $it") }
    }

    @Test
    fun skippedTithiOnReportsNgunalatri() {
        assertEquals(Tithi(TithiType.PANGELONG, 14), Lunar.skippedTithiOn(LocalDate.of(2024, 1, 10)))
        assertEquals(Tithi(TithiType.PENANGGAL, 3), Lunar.skippedTithiOn(LocalDate.of(2024, 3, 13)))
        assertEquals(Tithi(TithiType.PENANGGAL, 7), Lunar.skippedTithiOn(LocalDate.of(2024, 5, 15)))
        assertEquals(null, Lunar.skippedTithiOn(LocalDate.of(2024, 5, 16)))
    }

    /** Golden anchors (kalenderbali.com, golden/days-*.csv): Purnama, Tilem, Nyepi day and a ngunalatri. */
    @Test
    fun goldenAnchors() {
        assertEquals(Tithi(TithiType.PANGELONG, 15), Lunar.tithiOn(LocalDate.of(2024, 1, 10))) // 14 skipped
        assertEquals(Tithi(TithiType.PENANGGAL, 15), Lunar.tithiOn(LocalDate.of(2024, 1, 25)))
        assertEquals(Tithi(TithiType.PENANGGAL, 1), Lunar.tithiOn(LocalDate.of(2024, 3, 11))) // Nyepi
        assertEquals(Tithi(TithiType.PENANGGAL, 4), Lunar.tithiOn(LocalDate.of(2024, 3, 13))) // 3 skipped
        assertEquals(Tithi(TithiType.PANGELONG, 15), Lunar.tithiOn(LocalDate.of(2026, 11, 9)))
        assertEquals(Tithi(TithiType.PENANGGAL, 15), Lunar.tithiOn(LocalDate.of(2026, 11, 24)))
    }
}
