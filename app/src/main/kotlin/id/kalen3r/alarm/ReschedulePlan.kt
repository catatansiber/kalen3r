package id.kalen3r.alarm

import java.time.Duration
import java.time.LocalDateTime

/** Outcome of one reschedule: what to arm, what to cancel, and which ids to remember for the next run. */
data class ReschedulePlan(val toSchedule: List<PlannedAlarm>, val toCancel: Set<Int>, val toPersist: Set<Int>) {
    companion object {
        /** Alarms due this recently stay wanted: they may be due but not yet delivered (Doze, inexact fallback, siblings at the same minute). */
        val LOOKBACK: Duration = Duration.ofMinutes(10)

        fun compute(planner: AlarmPlanner, settings: AlarmSettingsData, previousIds: Set<Int>, now: LocalDateTime): ReschedulePlan {
            val future = planner.plan(now, settings)
            val wanted = planner.plan(now.minus(LOOKBACK), settings).mapTo(HashSet()) { it.id } + future.map { it.id }
            return ReschedulePlan(future, previousIds - wanted, wanted)
        }
    }
}

/** First-launch prompt rule for POST_NOTIFICATIONS (API 33+). */
object NotificationPrompt {
    fun shouldAsk(sdkInt: Int, granted: Boolean, alreadyAsked: Boolean, anyAlarmOn: Boolean) =
        sdkInt >= 33 && !granted && !alreadyAsked && anyAlarmOn
}

object BootActions {
    private val allowed = setOf(
        "android.intent.action.BOOT_COMPLETED",
        "android.intent.action.MY_PACKAGE_REPLACED",
        "android.intent.action.TIMEZONE_CHANGED",
        "android.intent.action.TIME_SET",
        "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
    )
    fun isAllowed(action: String?) = action in allowed
}
