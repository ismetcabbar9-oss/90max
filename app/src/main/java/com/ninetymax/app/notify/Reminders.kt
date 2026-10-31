package com.ninetymax.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ninetymax.app.MainActivity
import com.ninetymax.app.MaxApp
import com.ninetymax.app.R
import com.ninetymax.app.data.Reminder
import com.ninetymax.app.data.toTimeOrNull
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    const val CHANNEL_ID = "daily_reminders"
    private const val EXTRA_ID = "reminder_id"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Günlük hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Rutin, antrenman, su ve uyku hatırlatmaları"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Hatırlatmanın bir sonraki tetiklenme zamanı (hiç gün seçilmemişse null). */
    fun nextTrigger(reminder: Reminder, now: LocalDateTime): LocalDateTime? {
        val time = reminder.time.toTimeOrNull() ?: return null
        if (!reminder.enabled || reminder.days.isEmpty()) return null
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            val candidate = date.atTime(time)
            if (candidate.isAfter(now) && date.dayOfWeek.value in reminder.days) return candidate
        }
        return null
    }

    private fun pending(context: Context, id: String, flags: Int): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_ID, id).setAction("com.ninetymax.REMINDER.$id")
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, flags or PendingIntent.FLAG_IMMUTABLE)
    }

    fun scheduleAll(context: Context, reminders: List<Reminder>) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val now = LocalDateTime.now()
        reminders.forEach { r ->
            pending(context, r.id, PendingIntent.FLAG_NO_CREATE)?.let { am.cancel(it); it.cancel() }
            val next = nextTrigger(r, now) ?: return@forEach
            val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pi = pending(context, r.id, PendingIntent.FLAG_UPDATE_CURRENT) ?: return@forEach
            // Kesin alarm izni gerektirmeyen, Doze modunda da çalışan (birkaç dakika sapabilen) alarm.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(context: Context, reminder: Reminder) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(reminder.message.ifBlank { "90Max seni bekliyor." })
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), n)
        } catch (e: SecurityException) {
            // İzin geri alınmış olabilir; sessizce geç.
        }
    }

    class ReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val app = context.applicationContext as MaxApp
            val reminders = app.repository.state.value.reminders
            reminders.firstOrNull { it.id == intent.getStringExtra(EXTRA_ID) }?.let { show(context, it) }
            scheduleAll(context, reminders)
        }
    }

    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val app = context.applicationContext as MaxApp
            scheduleAll(context, app.repository.state.value.reminders)
        }
    }
}
