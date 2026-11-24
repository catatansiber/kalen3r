package id.kalen3r.alarm

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmSettingsCodecTest {
    @Test fun roundTrip() {
        val s = AlarmSettingsData(
            triSandya = listOf(SlotSetting(false, LocalTime.of(5, 45)), SlotSetting(true, LocalTime.of(12, 30)), SlotSetting(true, LocalTime.of(18, 0))),
            rainanGroups = setOf(RainanGroup.KAJENG_KLIWON, RainanGroup.BUDA_CEMENG),
            rainanTime = LocalTime.of(7, 5),
            sound = SoundChoice.TRI_SANDYA,
            customReminders = setOf(LocalDate.of(2026, 11, 24), LocalDate.of(2027, 1, 2)),
        )
        assertEquals(s, AlarmSettingsCodec.decode(AlarmSettingsCodec.encode(s)))
        val empty = AlarmSettingsData(rainanGroups = emptySet(), customReminders = emptySet())
        assertEquals(empty, AlarmSettingsCodec.decode(AlarmSettingsCodec.encode(empty)))
    }

    @Test fun missingOrBrokenFallsBackToDefaults() {
        assertEquals(AlarmSettingsData(), AlarmSettingsCodec.decode(null))
        assertEquals(AlarmSettingsData(), AlarmSettingsCodec.decode(""))
        assertEquals(AlarmSettingsData(), AlarmSettingsCodec.decode("garbage;;==x"))
        assertEquals(AlarmSettingsData().rainanTime, AlarmSettingsCodec.decode("rt=99:99").rainanTime)
    }

    @Test fun idSetRoundTrip() {
        assertEquals(setOf(1, 22, 333), AlarmSettingsCodec.decodeIds(AlarmSettingsCodec.encodeIds(setOf(333, 1, 22))))
        assertEquals(emptySet(), AlarmSettingsCodec.decodeIds(null))
        assertEquals(setOf(5), AlarmSettingsCodec.decodeIds("5,x,"))
    }

    @Test fun reminderOutcome() {
        val rt = LocalTime.of(6, 0)
        val now = LocalDateTime.of(2026, 11, 24, 10, 0)
        assertEquals(ReminderOutcome.ADDED, ReminderLogic.outcome(now, LocalDate.of(2026, 11, 25), rt))
        assertEquals(ReminderOutcome.PAST, ReminderLogic.outcome(now, LocalDate.of(2026, 11, 24), rt))
        assertEquals(ReminderOutcome.PAST, ReminderLogic.outcome(now, LocalDate.of(2026, 11, 1), rt))
        assertEquals(ReminderOutcome.ADDED, ReminderLogic.outcome(now.withHour(5), LocalDate.of(2026, 11, 24), rt))
    }
}
