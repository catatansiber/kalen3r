package id.kalen3r.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import id.kalen3r.MainActivity
import id.kalen3r.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AlarmChannels {
    const val DEFAULT = "pengingat"
    const val TRI_SANDYA = "pengingat_tri_sandya"

    fun triSandyaResId(context: Context): Int = context.resources.getIdentifier("tri_sandya", "raw", context.packageName)
    fun hasTriSandyaFile(context: Context): Boolean = triSandyaResId(context) != 0

    /** Channel sound cannot change after creation, so the Tri Sandya sound has its own channel (only if the file exists). */
    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(DEFAULT, context.getString(R.string.channel_name), NotificationManager.IMPORTANCE_HIGH)
                .apply { description = context.getString(R.string.channel_description) },
        )
        val res = triSandyaResId(context)
        if (res != 0) {
            val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            nm.createNotificationChannel(
                NotificationChannel(TRI_SANDYA, context.getString(R.string.channel_name_tri_sandya), NotificationManager.IMPORTANCE_HIGH).apply {
                    description = context.getString(R.string.channel_description)
                    setSound(Uri.parse("android.resource://${context.packageName}/$res"), attrs)
                },
            )
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val id = intent.getIntExtra(EXTRA_ID, 0)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val tri = intent.getBooleanExtra(EXTRA_TRI, false)
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                runCatching {
                    val sound = AlarmSounds.effective(AlarmSettings(app).load().sound, AlarmChannels.hasTriSandyaFile(app))
                    post(app, id, title, body, useTriSandyaChannel = tri && sound == SoundChoice.TRI_SANDYA)
                }
                // extend the rolling window even if the app is never opened
                runCatching { AlarmScheduler(app).rescheduleAll() }
            } finally {
                pending.finish()
            }
        }
    }

    private fun post(context: Context, id: Int, title: String, body: String, useTriSandyaChannel: Boolean) {
        if (title.isEmpty()) return
        AlarmChannels.ensure(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val channel = if (useTriSandyaChannel) AlarmChannels.TRI_SANDYA else AlarmChannels.DEFAULT
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
        const val EXTRA_TRI = "tri"
    }
}
