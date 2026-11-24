package id.kalen3r.alarm

import id.kalen3r.core.BaliCalendar
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class PlannedAlarm(
    val id: Int,
    val at: LocalDateTime,
    val title: String,
    val body: String,
    val isTriSandya: Boolean = false,
)

/**
 * Pure planner: turns settings into the future alarms of a rolling 60-day window (today + 59 days).
 * Depends only on the passed [now] (device wall-clock time), never on the JVM default time zone.
 *
 * IDs are deterministic and collision-free by construction (no hashing): id = epochDay * 16 + code,
 * where code 0..2 = Tri Sandya slot, 3 + RainanGroup.ordinal = rainan group, 9 = custom reminder.
 * Rescheduling therefore reuses the same PendingIntent request code for the same alarm.
 */
class AlarmPlanner(private val calendar: BaliCalendar) {

    fun plan(now: LocalDateTime, settings: AlarmSettingsData): List<PlannedAlarm> {
        val from = now.toLocalDate()
        val to = from.plusDays(WINDOW_DAYS - 1L)
        val out = ArrayList<PlannedAlarm>()

        fun add(a: PlannedAlarm) { if (a.at.isAfter(now)) out += a }

        var d = from
        while (!d.isAfter(to)) {
            settings.triSandya.forEachIndexed { i, slot ->
                if (slot.enabled) {
                    add(PlannedAlarm(idFor(d, i), d.atTime(slot.time), "Tri Sandya · ${SLOT_NAMES[i]}", "Waktunya Tri Sandya ${SLOT_NAMES[i].lowercase()}.", isTriSandya = true))
                }
            }
            d = d.plusDays(1)
        }

        if (settings.rainanGroups.isNotEmpty()) {
            val kinds = settings.rainanGroups.flatMapTo(HashSet()) { it.kinds }
            val byDate = calendar.rerainan(from, to, kinds).groupBy { it.date }
            for ((date, list) in byDate) {
                for (group in settings.rainanGroups) {
                    val names = list.filter { it.kind in group.kinds }.map { it.name }.distinct()
                    if (names.isEmpty()) continue
                    add(PlannedAlarm(idFor(date, GROUP_CODE_BASE + group.ordinal), date.atTime(settings.rainanTime), "Hari ini ${names.first()}", "Rerainan hari ini: ${names.joinToString(" · ")}"))
                }
            }
        }

        for (date in settings.customReminders) {
            if (date.isBefore(from) || date.isAfter(to)) continue
            val names = runCatching { calendar.day(date).rerainan.map { it.name }.distinct() }.getOrDefault(emptyList())
            val body = if (names.isEmpty()) "Hari yang kamu tandai." else "Rerainan: ${names.joinToString(" · ")}"
            add(PlannedAlarm(idFor(date, CUSTOM_CODE), date.atTime(settings.rainanTime), "Pengingat", body))
        }
        return out.sortedWith(compareBy({ it.at }, { it.id }))
    }

    companion object {
        const val WINDOW_DAYS = 60
        const val CODES_PER_DAY = 16
        private const val GROUP_CODE_BASE = 3
        private const val CUSTOM_CODE = 9
        private val SLOT_NAMES = listOf("Pagi", "Siang", "Sore")

        /** Stable positive id for (date, code); dates from 1970-01-02 to ~year 2243 (Int range). */
        fun idFor(date: LocalDate, code: Int): Int {
            require(code in 0 until CODES_PER_DAY)
            val day = date.toEpochDay()
            require(day in 1..100_000_000L) { "date out of range: $date" }
            return (day * CODES_PER_DAY + code).toInt()
        }
    }
}
