package id.kalen3r.ui.cari

import id.kalen3r.core.BaliCalendar
import id.kalen3r.core.Pancawara
import id.kalen3r.core.Sasih
import id.kalen3r.core.Saptawara
import id.kalen3r.core.Tithi
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CariViewModelTest {
    private val cal = BaliCalendar.default
    private val today = LocalDate.of(2026, 11, 24)
    private fun vm(c: BaliCalendar = cal) = CariViewModel(c) { today }

    private fun dates(vm: CariViewModel) = assertIs<CariResult.Dates>(vm.state.value.result)

    @Test fun otonanFromBirthReturnsNextFiveDates() {
        val vm = vm()
        val birth = LocalDate.of(2000, 5, 17)
        vm.setBirth(birth)
        vm.search()
        val items = dates(vm).items
        assertEquals(5, items.size)
        assertEquals(cal.otonan(birth, 5, today), items.map { it.date })
        assertTrue(items.all { !it.date.isBefore(today) })
    }

    @Test fun mismatchedWewaranShowsError() {
        val vm = vm()
        vm.selectOtonanMode(OtonanMode.WEWARAN)
        vm.setWuku(1); vm.setSaptawara(Saptawara.REDITE); vm.setPancawara(Pancawara.PON)
        vm.search()
        val r = assertIs<CariResult.Error>(vm.state.value.result)
        assertTrue(r.message.startsWith("Kombinasi tidak valid"), r.message)
    }

    @Test fun birthBefore1900StillWorks() {
        val vm = vm()
        val birth = LocalDate.of(1855, 3, 2)
        vm.setBirth(birth)
        vm.search()
        val items = dates(vm).items
        assertEquals(5, items.size)
        assertEquals(cal.otonan(birth, 5, today), items.map { it.date })
        assertTrue(items.first().title.contains("Wuku"))
    }

    @Test fun emptyInputShowsError() {
        val vm = vm()
        vm.search()
        val r = assertIs<CariResult.Error>(vm.state.value.result)
        assertEquals("Pilih tanggal lahir dulu", r.message)
    }

    @Test fun odalanPawukonReturnsDatesWithin3Years() {
        val vm = vm()
        vm.selectTab(CariTab.ODALAN_PAWUKON)
        vm.setWuku(1); vm.setSaptawara(Saptawara.REDITE); vm.setPancawara(Pancawara.PAING)
        vm.search()
        val items = dates(vm).items
        assertTrue(items.isNotEmpty())
        val end = today.plusYears(3)
        assertTrue(items.all { !it.date.isBefore(today) && !it.date.isAfter(end) })
        assertTrue(items.size <= CariViewModel.MAX_RESULTS)
    }

    @Test fun odalanSasihEmptyShowsNeutralMessage() {
        val empty = object : BaliCalendar by cal {
            override fun odalanBySasih(sasih: Sasih, tithi: Tithi, year: Int): List<LocalDate> = emptyList()
        }
        val vm = vm(empty)
        vm.selectTab(CariTab.ODALAN_SASIH)
        vm.search()
        val r = dates(vm)
        assertTrue(r.items.isEmpty())
    }

    @Test fun odalanSasihFindsPurnamaPreset() {
        val vm = vm()
        vm.selectTab(CariTab.ODALAN_SASIH)
        vm.setSasih(Sasih.KAPITU)
        vm.applyPurnama()
        vm.setYear(2026)
        vm.search()
        val items = dates(vm).items
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { cal.day(it.date).isPurnama })
    }

    @Test fun otonanDaysFromTodayIsCorrect() {
        val vm = vm()
        val birth = LocalDate.of(2000, 5, 17)
        vm.setBirth(birth)
        vm.search()
        val first = dates(vm).items.first()
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(today, first.date).toInt(), first.daysFromToday)
        assertTrue(first.daysFromToday in 0..209)
    }

    @Test fun findByWewaranModeUsesWewaranInputs() {
        val vm = vm()
        vm.selectOtonanMode(OtonanMode.WEWARAN)
        vm.setWuku(2); vm.setSaptawara(Saptawara.BUDA); vm.setPancawara(Pancawara.PAING)
        vm.search()
        val items = dates(vm).items
        assertEquals(cal.findByWewaran(2, Saptawara.BUDA, Pancawara.PAING, today, 5), items.map { it.date })
        assertEquals("Buda Paing, Wuku Landep", items.first().title)
    }

    @Test fun inputsAndTabSurviveInState() {
        val vm = vm()
        vm.selectTab(CariTab.ODALAN_SASIH)
        vm.setYear(3000)
        assertEquals(CariTab.ODALAN_SASIH, vm.state.value.tab)
        assertEquals(2100, vm.state.value.year)
    }
}
