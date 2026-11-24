package id.kalen3r.alarm

import id.kalen3r.core.BaliCalendar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AlarmPlannerTest {
    private val planner = AlarmPlanner(BaliCalendar.default)
    private val start = LocalDate.of(2026, 11, 20)
    private val now = LocalDateTime.of(2026, 11, 20, 0, 0)
    private val noRainan = AlarmSettingsData(rainanGroups = emptySet())
    private val onlyRainan = AlarmSettingsData(
        triSandya = AlarmSettingsData.DEFAULT_SLOTS.map { it.copy(enabled = false) },
    )
    private val everything = AlarmSettingsData(rainanGroups = RainanGroup.entries.toSet())

    private fun isTri(a: PlannedAlarm) = a.isTriSandya

    @Test fun planCovers60Days() {
        val plan = planner.plan(now, noRainan)
        val days = plan.map { it.at.toLocalDate() }.distinct()
        assertEquals(60, days.size)
        assertEquals(start, days.first())
        assertEquals(start.plusDays(59), days.last())
    }

    @Test fun triSandyaGivesThreePerDay() {
        val plan = planner.plan(now, noRainan)
        assertEquals(180, plan.size)
        plan.groupBy { it.at.toLocalDate() }.values.forEach { assertEquals(3, it.size) }
        val first = plan.take(3)
        assertEquals(listOf(LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0)), first.map { it.at.toLocalTime() })
        assertEquals(listOf("Tri Sandya · Pagi", "Tri Sandya · Siang", "Tri Sandya · Sore"), first.map { it.title })
        assertEquals(plan.sortedBy { it.at }, plan)
    }

    @Test fun disabledKindProducesNothing() {
        val nothing = AlarmSettingsData(
            triSandya = AlarmSettingsData.DEFAULT_SLOTS.map { it.copy(enabled = false) },
            rainanGroups = emptySet(),
        )
        assertTrue(planner.plan(now, nothing).isEmpty())
        val withoutPurnama = onlyRainan.copy(rainanGroups = onlyRainan.rainanGroups - RainanGroup.PURNAMA)
        assertTrue(planner.plan(now, withoutPurnama).none { it.title.contains("Purnama") })
        assertTrue(planner.plan(now, onlyRainan).any { it.title.contains("Purnama") })
    }

    @Test fun idsAreStableAcrossCalls() {
        val a = planner.plan(now, everything)
        val b = planner.plan(now, everything)
        assertEquals(a, b)
        val later = planner.plan(now.plusHours(7), everything).associateBy { it.id }
        // alarms that exist in both plans keep the same id, time and text
        a.filter { it.at > now.plusHours(7) && it.at.toLocalDate() <= start.plusDays(59) }.forEach { assertEquals(it, later[it.id]) }
    }

    @Test fun pastTimesAreSkipped() {
        val n = LocalDateTime.of(2026, 11, 20, 12, 30)
        val plan = planner.plan(n, noRainan)
        assertTrue(plan.all { it.at > n })
        assertEquals(listOf(LocalTime.of(18, 0)), plan.filter { it.at.toLocalDate() == start }.map { it.at.toLocalTime() })
        // exactly "now" is not in the future either
        assertTrue(planner.plan(LocalDateTime.of(2026, 11, 20, 12, 0), noRainan).none { it.at.toLocalTime() == LocalTime.NOON && it.at.toLocalDate() == start })
    }

    @Test fun changingTimeChangesOnlyThatSlot() {
        val base = planner.plan(now, noRainan)
        val changed = planner.plan(
            now,
            noRainan.copy(triSandya = noRainan.triSandya.mapIndexed { i, s -> if (i == 1) s.copy(time = LocalTime.of(13, 15)) else s }),
        )
        val (baseSiang, baseOther) = base.partition { it.title.endsWith("Siang") }
        val (newSiang, newOther) = changed.partition { it.title.endsWith("Siang") }
        assertEquals(baseOther, newOther)
        assertEquals(baseSiang.map { it.id }, newSiang.map { it.id })
        assertTrue(newSiang.all { it.at.toLocalTime() == LocalTime.of(13, 15) })
        assertTrue(baseSiang.all { it.at.toLocalTime() == LocalTime.NOON })
    }

    @Test fun noIdCollisionsInWindow() {
        val settings = everything.copy(customReminders = (0L..59L).map { start.plusDays(it) }.toSet())
        val plan = planner.plan(now, settings)
        assertEquals(plan.size, plan.map { it.id }.distinct().size)
        assertTrue(plan.all { it.id > 0 })
        // the id function itself is collision-free for every date 2000..2100 and every code
        val seen = HashSet<Int>()
        var d = LocalDate.of(2000, 1, 1)
        while (d <= LocalDate.of(2100, 12, 31)) {
            for (code in 0 until AlarmPlanner.CODES_PER_DAY) assertTrue(seen.add(AlarmPlanner.idFor(d, code)), "$d/$code")
            d = d.plusDays(1)
        }
    }

    @Test fun purnama2026Nov24HasRainanAlarmAtSix() {
        val plan = planner.plan(LocalDateTime.of(2026, 11, 20, 10, 0), AlarmSettingsData())
        val a = plan.single { it.title == "Hari ini Purnama" && it.at.toLocalDate() == LocalDate.of(2026, 11, 24) }
        assertEquals(LocalDateTime.of(2026, 11, 24, 6, 0), a.at)
    }

    @Test fun customReminderProducesAlarm() {
        val d = LocalDate.of(2026, 11, 25)
        val plan = planner.plan(now, noRainan.copy(triSandya = noRainan.triSandya.map { it.copy(enabled = false) },
            customReminders = setOf(d), rainanTime = LocalTime.of(7, 30)))
        val a = plan.single()
        assertEquals("Pengingat", a.title)
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 30), a.at)
        assertTrue(a.body.isNotBlank())
        val purnamaDay = LocalDate.of(2026, 11, 24)
        val p = planner.plan(now, AlarmSettingsData(triSandya = noRainan.triSandya.map { it.copy(enabled = false) },
            rainanGroups = emptySet(), customReminders = setOf(purnamaDay))).single()
        assertTrue(p.body.contains("Purnama"), p.body)
    }

    @Test fun windowIsExactly60DaysNotMore() {
        val settings = everything.copy(customReminders = setOf(start.plusDays(60), start.plusDays(59)))
        val plan = planner.plan(now, settings)
        assertEquals(start.plusDays(59), plan.maxOf { it.at.toLocalDate() })
        assertTrue(plan.any { it.title == "Pengingat" && it.at.toLocalDate() == start.plusDays(59) })
        assertTrue(plan.none { it.at.toLocalDate() == start.plusDays(60) })
        // started late in the day, still 60 calendar days counted from today
        val late = planner.plan(LocalDateTime.of(2026, 11, 20, 23, 0), noRainan)
        assertEquals(start.plusDays(59), late.maxOf { it.at.toLocalDate() })
    }

    @Test fun sameSettingsSamePlanAcrossDeviceTimeZones() {
        val saved = TimeZone.getDefault()
        try {
            val plans = listOf("Asia/Makassar", "America/Los_Angeles", "Pacific/Kiritimati").map {
                TimeZone.setDefault(TimeZone.getTimeZone(it))
                AlarmPlanner(BaliCalendar.default).plan(now, AlarmSettingsData())
            }
            assertEquals(plans[0], plans[1])
            assertEquals(plans[0], plans[2])
        } finally {
            TimeZone.setDefault(saved)
        }
    }

    @Test fun availableSoundsHidesTriSandyaWhenFileMissing() {
        assertEquals(listOf(SoundChoice.DEFAULT), AlarmSounds.available(hasTriSandyaFile = false))
        assertEquals(listOf(SoundChoice.TRI_SANDYA, SoundChoice.DEFAULT), AlarmSounds.available(hasTriSandyaFile = true))
        assertEquals(SoundChoice.DEFAULT, AlarmSounds.effective(SoundChoice.TRI_SANDYA, hasTriSandyaFile = false))
        assertEquals(SoundChoice.TRI_SANDYA, AlarmSounds.effective(SoundChoice.TRI_SANDYA, hasTriSandyaFile = true))
    }

    @Test fun defaultsMatchSpec() {
        val s = AlarmSettingsData()
        assertEquals(listOf(LocalTime.of(6, 0), LocalTime.of(12, 0), LocalTime.of(18, 0)), s.triSandya.map { it.time })
        assertTrue(s.triSandya.all { it.enabled })
        assertEquals(setOf(RainanGroup.PURNAMA, RainanGroup.TILEM, RainanGroup.HARI_RAYA_BESAR), s.rainanGroups)
        assertFalse(RainanGroup.KAJENG_KLIWON in s.rainanGroups)
        assertEquals(LocalTime.of(6, 0), s.rainanTime)
        assertEquals(SoundChoice.DEFAULT, s.sound)
    }
}
