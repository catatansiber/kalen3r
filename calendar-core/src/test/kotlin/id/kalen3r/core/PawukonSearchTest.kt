package id.kalen3r.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PawukonSearchTest {
    private val birth = LocalDate.of(2000, 3, 15)
    private val from = LocalDate.of(2026, 11, 24)

    @Test
    fun otonanStepsAre210Days() {
        val r = otonan(birth, 5, from)
        assertEquals(5, r.size)
        r.zipWithNext().forEach { (a, b) -> assertEquals(210L, ChronoUnit.DAYS.between(a, b)) }
        assertTrue(r.first() >= from)
        assertTrue(r.first().plusDays(-210) < from)
    }

    @Test
    fun otonanHasSamePawukonIndexAsBirth() {
        val idx = Pawukon.of(birth).index
        otonan(birth, 6, from).forEach { assertEquals(idx, Pawukon.of(it).index) }
    }

    @Test
    fun otonanIncludesBirthDateWhenFromIsBirth() {
        assertEquals(listOf(birth, birth.plusDays(210)), otonan(birth, 2, birth))
    }

    @Test
    fun otonanFromBeforeBirthStartsAtBirth() {
        assertEquals(birth, otonan(birth, 1, birth.minusDays(1000)).first())
    }

    @Test
    fun otonanWorksForBirthBefore1900() {
        // 1850-01-01 has Pawukon index 93 (hardcoded, verified independently).
        val b = LocalDate.of(1850, 1, 1)
        val r = otonan(b, 3, from)
        assertEquals(listOf(LocalDate.of(2027, 2, 2), LocalDate.of(2027, 8, 31), LocalDate.of(2028, 3, 28)), r)
        r.forEach { assertEquals(93, Pawukon.of(it).index) }
    }

    @Test
    fun otonanRejectsNonPositiveCount() {
        assertFailsWith<IllegalArgumentException> { otonan(birth, 0, from) }
    }

    @Test
    fun findByWewaranRejectsMismatchedPancawara() {
        val e = assertFailsWith<InvalidWewaranException> {
            findByWewaran(1, Saptawara.REDITE, Pancawara.PON, from, 1)
        }
        assertTrue(e is IllegalArgumentException)
    }

    @Test
    fun findByWewaranRejectsInvalidWuku() {
        assertFailsWith<InvalidWewaranException> { findByWewaran(0, Saptawara.REDITE, Pancawara.PAING, from, 1) }
        assertFailsWith<InvalidWewaranException> { findByWewaran(31, Saptawara.REDITE, Pancawara.PAING, from, 1) }
    }

    @Test
    fun findByWewaranRejectsNonPositiveCount() {
        assertFailsWith<IllegalArgumentException> { findByWewaran(1, Saptawara.REDITE, Pancawara.PAING, from, 0) }
    }

    @Test
    fun findByWewaranRoundTripsOtonan() {
        val d = Pawukon.of(birth)
        assertEquals(
            otonan(birth, 4, from),
            findByWewaran(d.wuku, d.saptawara, d.pancawara, from, 4),
        )
    }

    @Test
    fun findByWewaranHardcodedAnchorSequence() {
        assertEquals(
            listOf(LocalDate.of(2026, 11, 1), LocalDate.of(2027, 5, 30), LocalDate.of(2027, 12, 26)),
            findByWewaran(1, Saptawara.REDITE, Pancawara.PAING, LocalDate.of(2026, 11, 1), 3),
        )
    }

    @Test
    fun findByWewaranHardcodedLandepSomaKliwon() {
        // Index (2-1)*7+1 = 8, 8%5=3 = Kliwon -> 2026-11-01 + 8 days.
        assertEquals(
            LocalDate.of(2026, 11, 9),
            findByWewaran(2, Saptawara.SOMA, Pancawara.KLIWON, LocalDate.of(2026, 11, 1), 1).single(),
        )
    }

    @Test
    fun odalanByPawukonBoundsInclusive() {
        val a = LocalDate.of(2026, 11, 1)
        val b = LocalDate.of(2027, 5, 30)
        assertEquals(listOf(a, b), odalanByPawukon(1, Saptawara.REDITE, Pancawara.PAING, a, b))
        assertEquals(listOf(b), odalanByPawukon(1, Saptawara.REDITE, Pancawara.PAING, a.plusDays(1), b))
        assertEquals(listOf(a), odalanByPawukon(1, Saptawara.REDITE, Pancawara.PAING, a, b.minusDays(1)))
    }

    @Test
    fun odalanByPawukonEmptyWhenToBeforeFrom() {
        assertEquals(
            emptyList(),
            odalanByPawukon(1, Saptawara.REDITE, Pancawara.PAING, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 1)),
        )
    }

    @Test
    fun odalanByPawukonRejectsMismatch() {
        assertFailsWith<InvalidWewaranException> {
            odalanByPawukon(1, Saptawara.REDITE, Pancawara.PON, from, from.plusDays(500))
        }
    }
}
