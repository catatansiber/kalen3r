package id.kalen3r

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import id.kalen3r.alarm.AlarmSettings
import id.kalen3r.alarm.NotificationPrompt
import kotlinx.coroutines.launch
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import id.kalen3r.alarm.AlarmChannels
import id.kalen3r.alarm.AlarmScheduler
import id.kalen3r.ui.AppNav
import id.kalen3r.ui.KalenTheme

class MainActivity : ComponentActivity() {
    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlarmChannels.ensure(this)
        AlarmScheduler.rescheduleAsync(this)
        lifecycleScope.launch { runCatching { askNotificationsOnce() } }
        setContent {
            KalenTheme {
                AppNav()
            }
        }
    }

    /** One system prompt per install (API 33+), only while some alarm is on; afterwards the Alarm tab shows a settings row instead. */
    private suspend fun askNotificationsOnce() {
        val store = AlarmSettings(this)
        val granted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (NotificationPrompt.shouldAsk(Build.VERSION.SDK_INT, granted, store.notificationAsked(), store.load().anyAlarmOn)) {
            store.markNotificationAsked()
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
