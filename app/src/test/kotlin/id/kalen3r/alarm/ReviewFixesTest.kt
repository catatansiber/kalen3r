package id.kalen3r.alarm

import id.kalen3r.core.BaliCalendar
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReviewFixesTest {
    private val planner = AlarmPlanner(BaliCalendar.default, StringsXmlTexts)
    private val day = LocalDate.of(2026, 11, 24) // Purnama
    private val settings = AlarmSettingsData(customReminders = setOf(day))

    @Test fun siblingAlarmsDueAtSameTimeAreNotCancelled() {
        val previous = planner.plan(LocalDateTime.of(2026, 11, 23, 12, 0), settings)
        val at6 = previous.filter { it.at == day.atTime(6, 0) }
        assertTrue(at6.size >= 3, "tri sandya + purnama + custom at 06:00")
        val r = ReschedulePlan.compute(planner, settings, previous.map { it.id }.toSet(), day.atTime(6, 0, 1))
        at6.forEach {
            assertFalse(it.id in r.toCancel, "cancelled ${it.title}")
            assertTrue(it.id in r.toPersist)
        }
        assertTrue(r.toSchedule.none { it.at <= day.atTime(6, 0, 1) })
    }

    @Test fun disabledAlarmDueTodayIsStillCancelled() {
        val previous = planner.plan(LocalDateTime.of(2026, 11, 23, 12, 0), settings)
        val off = settings.copy(customReminders = emptySet())
        val r = ReschedulePlan.compute(planner, off, previous.map { it.id }.toSet(), day.atTime(5, 0))
        assertTrue(AlarmPlanner.idFor(day, 9) in r.toCancel)
    }

    @Test fun oldIdsAreCancelledAndDroppedFromPersisted() {
        val previous = planner.plan(LocalDateTime.of(2026, 11, 23, 12, 0), settings).map { it.id }.toSet()
        val r = ReschedulePlan.compute(planner, settings, previous, day.atTime(6, 0).plus(Duration.ofHours(1)))
        assertTrue(AlarmPlanner.idFor(day, 0) in r.toCancel)
        assertFalse(AlarmPlanner.idFor(day, 0) in r.toPersist)
    }

    @Test fun corruptValuesFallBackPerField() {
        val s = AlarmSettingsCodec.decode("ts=1@zz,0@06:00,1@07:00;rg=NOPE,PURNAMA;rt=xx;sn=XX;cr=bad,2026-11-24")
        assertEquals(AlarmSettingsData.DEFAULT_SLOTS, s.triSandya)
        assertEquals(setOf(RainanGroup.PURNAMA), s.rainanGroups)
        assertEquals(LocalTime.of(6, 0), s.rainanTime)
        assertEquals(SoundChoice.DEFAULT, s.sound)
        assertEquals(setOf(day), s.customReminders)
        assertEquals(AlarmSettingsData(), AlarmSettingsCodec.decode("ts=;rg;;=;sn"))
    }

    @Test fun plannerTextKeysExistInStringsXml() {
        for (k in AlarmTextKeys.ALL) assertTrue(k in StringsXml.values, "missing string $k")
    }

    @Test fun idCodeSpaceDoesNotOverlap() {
        val tri = 0 until AlarmPlanner.TRI_SANDYA_SLOTS
        val groups = AlarmPlanner.GROUP_CODE_BASE until AlarmPlanner.GROUP_CODE_BASE + RainanGroup.entries.size
        assertTrue(tri.last < AlarmPlanner.GROUP_CODE_BASE)
        assertTrue(groups.last < AlarmPlanner.CUSTOM_CODE)
        assertTrue(AlarmPlanner.CUSTOM_CODE < AlarmPlanner.CODES_PER_DAY)
    }

    @Test fun notificationPromptAskedOnlyOnceWhenNeeded() {
        assertTrue(NotificationPrompt.shouldAsk(sdkInt = 34, granted = false, alreadyAsked = false, anyAlarmOn = true))
        assertFalse(NotificationPrompt.shouldAsk(34, false, true, true))
        assertFalse(NotificationPrompt.shouldAsk(34, true, false, true))
        assertFalse(NotificationPrompt.shouldAsk(34, false, false, false))
        assertFalse(NotificationPrompt.shouldAsk(32, false, false, true))
    }

    @Test fun bootActionAllowList() {
        assertTrue(BootActions.isAllowed("android.intent.action.BOOT_COMPLETED"))
        assertTrue(BootActions.isAllowed("android.intent.action.TIME_SET"))
        assertFalse(BootActions.isAllowed("android.intent.action.BATTERY_LOW"))
        assertFalse(BootActions.isAllowed(null))
    }

    @Test fun wallClockTimeMapsToRightInstantPerZone() {
        val at = LocalDateTime.of(2026, 11, 24, 6, 0)
        val makassar = AlarmPlanner.epochMillis(at, ZoneId.of("Asia/Makassar"))
        val la = AlarmPlanner.epochMillis(at, ZoneId.of("America/Los_Angeles"))
        assertEquals(java.time.Instant.parse("2026-11-23T22:00:00Z").toEpochMilli(), makassar)
        assertEquals(java.time.Instant.parse("2026-11-24T14:00:00Z").toEpochMilli(), la)
    }

    @Test fun reminderFailedOutcomeExists() {
        assertTrue(ReminderOutcome.entries.contains(ReminderOutcome.FAILED))
    }
}
