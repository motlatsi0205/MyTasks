package com.example.mytasks

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    enum class Priority(val color: Int) {
        LOW(Color.parseColor("#4CAF50")),
        MEDIUM(Color.parseColor("#FF9800")),
        HIGH(Color.parseColor("#F44336"))
    }

    data class Task(val name: String, val date: String, val time: String, val priority: Priority = Priority.LOW) {
        fun getTimestamp(): Long {
            if (date.isEmpty() && time.isEmpty()) return Long.MAX_VALUE
            val calendar = Calendar.getInstance()
            try {
                if (date.isNotEmpty()) {
                    val parts = date.split("/")
                    calendar.set(Calendar.DAY_OF_MONTH, parts[0].toInt())
                    calendar.set(Calendar.MONTH, parts[1].toInt() - 1)
                    calendar.set(Calendar.YEAR, parts[2].toInt())
                }
                if (time.isNotEmpty()) {
                    val parts = time.split(":")
                    calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                    calendar.set(Calendar.MINUTE, parts[1].toInt())
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, 23)
                    calendar.set(Calendar.MINUTE, 59)
                }
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                return calendar.timeInMillis
            } catch (e: Exception) {
                return Long.MAX_VALUE
            }
        }

        fun toJsonObject(): JSONObject {
            val json = JSONObject()
            json.put("name", name)
            json.put("date", date)
            json.put("time", time)
            json.put("priority", priority.name)
            return json
        }

        companion object {
            fun fromJsonObject(json: JSONObject): Task {
                val priorityName = if (json.has("priority")) json.getString("priority") else "LOW"
                return Task(
                    json.getString("name"),
                    json.getString("date"),
                    json.getString("time"),
                    Priority.valueOf(priorityName)
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Default fragment
        loadFragment(TasksFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_tasks -> loadFragment(TasksFragment())
                R.id.nav_about -> loadFragment(AboutFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    fun scheduleNotification(task: Task) {
        val intent = Intent(this, TaskReminderReceiver::class.java).apply {
            putExtra("taskName", task.name)
            putExtra("taskId", task.hashCode())
        }
        val pendingIntent = PendingIntent.getBroadcast(this, task.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = task.getTimestamp()
        if (time != Long.MAX_VALUE && time > System.currentTimeMillis()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                    } else {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                }
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        }
    }

    fun cancelNotification(task: Task) {
        val intent = Intent(this, TaskReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, task.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
