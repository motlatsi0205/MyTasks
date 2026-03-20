package com.example.mytasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
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
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
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

    private lateinit var editTask: TextInputEditText
    private lateinit var editDate: TextInputEditText
    private lateinit var editTime: TextInputEditText
    private lateinit var rgPriority: RadioGroup
    private lateinit var btnAdd: MaterialButton
    private lateinit var listViewTasks: ListView
    private lateinit var taskList: ArrayList<Task>
    private lateinit var adapter: ArrayAdapter<Task>
    private lateinit var sharedPreferences: SharedPreferences

    private val checkHandler = Handler(Looper.getMainLooper())
    private lateinit var checkRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("MyTasksPrefs", Context.MODE_PRIVATE)

        editTask = findViewById(R.id.editTask)
        editDate = findViewById(R.id.editDate)
        editTime = findViewById(R.id.editTime)
        rgPriority = findViewById(R.id.rgPriority)
        btnAdd = findViewById(R.id.btnAdd)
        listViewTasks = findViewById(R.id.listViewTasks)

        editDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                val dateString = String.format(Locale.getDefault(), "%02d/%02d/%d", day, month + 1, year)
                editDate.setText(dateString)
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        editTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                val timeString = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                editTime.setText(timeString)
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        taskList = loadTasks()
        adapter = object : ArrayAdapter<Task>(this, R.layout.task_item, taskList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.task_item, parent, false)
                val task = getItem(position)!!

                val tvName = view.findViewById<TextView>(R.id.tvTaskName)
                val tvDateTime = view.findViewById<TextView>(R.id.tvTaskDateTime)
                val indicator = view.findViewById<View>(R.id.priorityIndicator)

                tvName.text = task.name
                indicator.setBackgroundColor(task.priority.color)
                
                tvDateTime.text = buildString {
                    if (task.date.isNotEmpty()) append("Date: ${task.date}")
                    if (task.time.isNotEmpty()) {
                        if (isNotEmpty()) append(" | ")
                        append("Time: ${task.time}")
                    }
                    if (isEmpty()) append("No schedule set")
                }
                return view
            }
        }
        listViewTasks.adapter = adapter

        btnAdd.setOnClickListener {
            val name = editTask.text.toString().trim()
            if (name.isNotEmpty()) {
                val priority = when (rgPriority.checkedRadioButtonId) {
                    R.id.rbMedium -> Priority.MEDIUM
                    R.id.rbHigh -> Priority.HIGH
                    else -> Priority.LOW
                }
                
                val newTask = Task(name, editDate.text.toString(), editTime.text.toString(), priority)
                taskList.add(newTask)
                taskList.sortBy { it.getTimestamp() }
                saveTasks()
                adapter.notifyDataSetChanged()
                
                editTask.text?.clear()
                editDate.text?.clear()
                editTime.text?.clear()
                rgPriority.check(R.id.rbLow)
                Toast.makeText(this, "Task added!", Toast.LENGTH_SHORT).show()
            } else {
                editTask.error = "Description required"
            }
        }

        listViewTasks.setOnItemClickListener { _, _, position, _ ->
            taskList.removeAt(position)
            saveTasks()
            adapter.notifyDataSetChanged()
            Toast.makeText(this, "Task Completed!", Toast.LENGTH_SHORT).show()
        }

        setupAutoRemoval()
    }

    private fun saveTasks() {
        val jsonArray = JSONArray()
        taskList.forEach { jsonArray.put(it.toJsonObject()) }
        sharedPreferences.edit().putString("tasks", jsonArray.toString()).apply()
    }

    private fun loadTasks(): ArrayList<Task> {
        val saved = sharedPreferences.getString("tasks", null) ?: return ArrayList()
        val list = ArrayList<Task>()
        val jsonArray = JSONArray(saved)
        for (i in 0 until jsonArray.length()) {
            list.add(Task.fromJsonObject(jsonArray.getJSONObject(i)))
        }
        return list
    }

    private fun setupAutoRemoval() {
        checkRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                if (taskList.removeAll { it.getTimestamp() != Long.MAX_VALUE && it.getTimestamp() < currentTime }) {
                    saveTasks()
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this@MainActivity, "Expired tasks cleared", Toast.LENGTH_SHORT).show()
                }
                checkHandler.postDelayed(this, 10000)
            }
        }
        checkHandler.post(checkRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        checkHandler.removeCallbacks(checkRunnable)
    }
}
