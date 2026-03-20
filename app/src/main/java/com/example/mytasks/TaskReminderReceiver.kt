package com.example.mytasks

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class TaskReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskName = intent.getStringExtra("taskName") ?: "Task Reminder"
        val taskId = intent.getIntExtra("taskId", 0)

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        createNotificationChannel(context, alarmSound)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ALARM: My Task")
            .setContentText(taskName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(alarmSound)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setAutoCancel(true)
            .setFullScreenIntent(null, true) // Helps it pop up even if locked

        // This flag makes the sound repeat until the user interacts with the notification
        val notification = builder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(taskId, notification)
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }

    private fun createNotificationChannel(context: Context, soundUri: android.net.Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Task Alarms"
            val descriptionText = "Loud alarms for scheduled tasks"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(soundUri, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "task_alarm_channel"
    }
}
