package id.kalen3r.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import id.kalen3r.CalendarProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Schedules the rolling 60-day plan with AlarmManager. Safe to call as often as needed: the same alarm always
 * gets the same request code, and ids from the previous run that are no longer planned are cancelled.
 */
class AlarmScheduler(context: Context) {
    private val app = context.applicationContext
    private val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val settings = AlarmSettings(app)
    private val planner = AlarmPlanner(CalendarProvider.calendar)

    /** False on API 31+ when the user has revoked exact alarms (API 31-32: SCHEDULE_EXACT_ALARM; 33+: USE_EXACT_ALARM is granted at install). */
    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    suspend fun rescheduleAll(now: LocalDateTime = LocalDateTime.now()) = lock.withLock {
        val plan = planner.plan(now, settings.load())
        val planned = plan.map { it.id }.toSet()
        for (id in settings.scheduledIds() - planned) cancel(id)
        val exact = canScheduleExact()
        val zone = ZoneId.systemDefault()
        for (a in plan) schedule(a, a.at.atZone(zone).toInstant().toEpochMilli(), exact)
        settings.saveScheduledIds(planned)
    }

    private fun intentFor(id: Int, title: String = "", body: String = "", tri: Boolean = false) =
        Intent(app, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_ID, id)
            .putExtra(AlarmReceiver.EXTRA_TITLE, title)
            .putExtra(AlarmReceiver.EXTRA_BODY, body)
            .putExtra(AlarmReceiver.EXTRA_TRI, tri)

    private fun schedule(a: PlannedAlarm, atMillis: Long, exact: Boolean) {
        val pi = PendingIntent.getBroadcast(
            app, a.id, intentFor(a.id, a.title, a.body, a.isTriSandya),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        try {
            if (exact) alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            else alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } catch (_: SecurityException) {
            // permission revoked between the check and the call: inexact still beats no alarm
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun cancel(id: Int) {
        val pi = PendingIntent.getBroadcast(app, id, intentFor(id), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    companion object {
        private val lock = Mutex()
        private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Fire-and-forget reschedule that survives the caller (activity, view model) going away. */
        fun rescheduleAsync(context: Context) {
            val app = context.applicationContext
            appScope.launch { runCatching { AlarmScheduler(app).rescheduleAll() } }
        }

        /** Applies [transform] to the stored settings, then reschedules. */
        fun updateSettings(context: Context, transform: (AlarmSettingsData) -> AlarmSettingsData) {
            val app = context.applicationContext
            appScope.launch {
                AlarmSettings(app).update(transform)
                runCatching { AlarmScheduler(app).rescheduleAll() }
            }
        }

        /** Adds [date] to the custom reminders unless its reminder time already passed. */
        suspend fun addReminder(context: Context, date: LocalDate, now: LocalDateTime = LocalDateTime.now()): ReminderOutcome {
            val app = context.applicationContext
            val store = AlarmSettings(app)
            val outcome = ReminderLogic.outcome(now, date, store.load().rainanTime)
            if (outcome == ReminderOutcome.ADDED) {
                store.update { it.copy(customReminders = it.customReminders + date) }
                AlarmScheduler(app).rescheduleAll(now)
            }
            return outcome
        }
    }
}
