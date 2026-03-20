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

        // Prioritize the device's default RINGTONE as requested for urgency
        val ringtoneSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        createNotificationChannel(context, ringtoneSound)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("URGENT: Task Reminder")
            .setContentText(taskName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL) // Using CALL category often makes it more bypass-ignore
            .setSound(ringtoneSound)
            .setVibrate(longArrayOf(1000, 500, 1000, 500, 1000))
            .setAutoCancel(true)

        // This makes the ringtone repeat until you dismiss it
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
            val name = "Urgent Task Reminders"
            val descriptionText = "Uses your ringtone for urgent task alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setSound(soundUri, audioAttributes)
                enableVibration(true)
            }
            
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Delete old channel if it exists to ensure new sound settings take effect
            notificationManager.deleteNotificationChannel("task_alarm_channel")
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "task_urgent_channel"
    }
}
