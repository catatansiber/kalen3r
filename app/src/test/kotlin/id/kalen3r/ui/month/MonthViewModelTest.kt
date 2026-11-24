package id.kalen3r.ui.month

import id.kalen3r.core.BaliCalendar
import id.kalen3r.core.RerainanKind
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MonthViewModelTest {
    private val cal = BaliCalendar.default
    private fun vm(today: LocalDate) = MonthViewModel(cal) { today }

    @Test fun startsOnCurrentMonth() {
        val s = vm(LocalDate.of(2026, 11, 24)).state.value
        assertEquals(2026, s.year); assertEquals(11, s.month)
        assertEquals(30, s.days.size)
        assertEquals(LocalDate.of(2026, 11, 24), s.todayDate)
        assertFalse(s.isUnverified)
    }

    @Test fun previousAtJan2000IsNoOp() {
        val m = vm(LocalDate.of(2000, 1, 15)); m.previous()
        assertEquals(2000, m.state.value.year); assertEquals(1, m.state.value.month)
    }

    @Test fun nextAtDec2100IsNoOp() {
        val m = vm(LocalDate.of(2100, 12, 15)); m.next()
        assertEquals(2100, m.state.value.year); assertEquals(12, m.state.value.month)
    }

    @Test fun startClampsOutOfRangeToday() {
        assertEquals(2100, vm(LocalDate.of(2150, 3, 1)).state.value.year)
        assertEquals(2000, vm(LocalDate.of(1990, 3, 1)).state.value.year)
    }

    @Test fun unverifiedMonthSetsWarning() {
        assertTrue(vm(LocalDate.of(2023, 12, 1)).state.value.isUnverified)
        assertTrue(vm(LocalDate.of(2036, 1, 1)).state.value.isUnverified)
        assertFalse(vm(LocalDate.of(2024, 1, 1)).state.value.isUnverified)
    }

    @Test fun nextThenPreviousReturnsToSameMonth() {
        val m = vm(LocalDate.of(2026, 12, 5)); m.next()
        assertEquals(2027, m.state.value.year); assertEquals(1, m.state.value.month)
        m.previous()
        assertEquals(2026, m.state.value.year); assertEquals(12, m.state.value.month)
    }

    @Test fun november2026GoldenFacts() {
        val days = vm(LocalDate.of(2026, 11, 1)).state.value.days
        val d24 = days.first { it.date.dayOfMonth == 24 }
        val d9 = days.first { it.date.dayOfMonth == 9 }
        assertTrue(d24.isPurnama); assertTrue(d9.isTilem)
        assertTrue(d24.rerainan.any { it.kind == RerainanKind.KAJENG_KLIWON })
        assertTrue(d24.rerainan.any { it.name == "Anggara Kasih" })
        assertTrue(d9.rerainan.any { it.kind == RerainanKind.KAJENG_KLIWON })
    }

    @Test fun legendListsNotableDays() {
        val s = vm(LocalDate.of(2026, 11, 1)).state.value
        val l = MonthFormat.legend(s.days)
        val e = l.first { it.date == LocalDate.of(2026, 11, 24) }
        assertEquals(listOf("Purnama", "Kajeng Kliwon", "Anggara Kasih"), e.names)
        assertTrue(l.map { it.date }.zipWithNext().all { (a, b) -> a < b })
    }
}
