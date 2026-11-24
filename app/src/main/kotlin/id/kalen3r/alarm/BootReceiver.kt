package id.kalen3r.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reschedules after reboot, app update, and clock/time-zone changes (all of which clear or shift alarms). */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AlarmChannels.ensure(app)
                runCatching { AlarmScheduler(app).rescheduleAll() }
            } finally {
                pending.finish()
            }
        }
    }
}
