package id.kalen3r.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PawukonTest {
    @Test
    fun anchorIsSintaRediteBPaing() {
        val d = Pawukon.of(LocalDate.of(2026, 11, 1))
        assertEquals(0, d.index)
        assertEquals(1, d.wuku)
        assertEquals(Saptawara.REDITE, d.saptawara)
        assertEquals(Pancawara.PAING, d.pancawara)
        assertEquals(Triwara.PASAH, d.triwara)
        assertEquals(14, d.urip)
        assertEquals(30, Pawukon.WUKU_NAMES.size)
        assertEquals("Sinta", Pawukon.WUKU_NAMES.first())
        assertEquals("Dungulan", Pawukon.WUKU_NAMES[10])
        assertEquals("Watugunung", Pawukon.WUKU_NAMES.last())
    }

    @Test
    fun goldenLoaderCountMatchesDeclaredCoverage() {
        assertEquals(212, GoldenLoader.allDays().size)
    }

    @Test
    fun matchesGoldenOverDeclaredRanges() {
        val bad = mutableListOf<String>()
        for (g in GoldenLoader.allDays()) {
            val p = Pawukon.of(g.date)
            val diffs = buildList {
                if (p.wuku != g.wukuNumber) add("wuku ${p.wuku}!=${g.wukuNumber}")
                if (Pawukon.WUKU_NAMES[p.wuku - 1].lowercase() != g.wukuName) add("wukuName")
                if (p.saptawara != g.saptawara) add("sapta ${p.saptawara}!=${g.saptawara}")
                if (p.pancawara != g.pancawara) add("panca ${p.pancawara}!=${g.pancawara}")
                if (p.triwara != g.triwara) add("tri ${p.triwara}!=${g.triwara}")
                if (p.caturwara != GoldenLoader.CATURWARA.indexOf(g.caturwara)) add("catur ${p.caturwara}!=${g.caturwara}")
                if (p.astawara != GoldenLoader.ASTAWARA.indexOf(g.astawara)) add("asta ${p.astawara}!=${g.astawara}")
                if (p.sangawara != GoldenLoader.SANGAWARA.indexOf(g.sangawara)) add("sanga ${p.sangawara}!=${g.sangawara}")
                if (p.dasawara != GoldenLoader.DASAWARA.indexOf(g.dasawara)) add("dasa ${p.dasawara}!=${g.dasawara}")
                if ("${Pawukon.saptawaraUrip(p.saptawara)}+${Pawukon.pancawaraUrip(p.pancawara)}" != g.urip) add("urip")
                if (p.urip != g.urip.split("+").sumOf { it.toInt() }) add("uripSum")
            }
            if (diffs.isNotEmpty()) bad += "${g.date}: $diffs"
        }
        assertTrue(bad.isEmpty(), "${bad.size} mismatches, first 20:\n" + bad.take(20).joinToString("\n"))
    }

    @Test
    fun propertiesHoldEveryDay2000To2100() {
        var date = LocalDate.of(2000, 1, 1)
        val end = LocalDate.of(2100, 12, 31)
        var prev: PawukonDay? = null
        var n = 0
        while (!date.isAfter(end)) {
            val p = Pawukon.of(date)
            val expected = Math.floorMod(ChronoUnit.DAYS.between(Pawukon.ANCHOR, date), 210L).toInt()
            assertEquals(expected, p.index, "$date")
            assertTrue(p.index in 0..209, "$date")
            assertEquals(p.index / 7 + 1, p.wuku, "$date")
            assertEquals(p.index % 7, p.saptawara.ordinal, "$date")
            assertEquals(p.index % 5, p.pancawara.ordinal, "$date")
            assertEquals(p.index % 3, p.triwara.ordinal, "$date")
            assertEquals(p.index % 6, p.sadwara, "$date")
            assertTrue(p.caturwara in 0..3 && p.astawara in 0..7 && p.sangawara in 0..8 && p.dasawara in 0..9, "$date")
            assertEquals((p.urip) % 10, p.dasawara, "$date")
            prev?.let { assertEquals((it.index + 1) % 210, p.index, "$date") }
            prev = p
            date = date.plusDays(1)
            n++
        }
        assertEquals(36_890, n)
    }

    @Test
    fun cycleRepeatsEvery210Days() {
        val a = LocalDate.of(2031, 3, 9)
        assertEquals(Pawukon.of(a), Pawukon.of(a.plusDays(210)))
        assertEquals(Pawukon.of(a), Pawukon.of(a.minusDays(210 * 7)))
    }
}
