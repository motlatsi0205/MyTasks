package com.example.mytasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class TasksFragment : Fragment() {

    private lateinit var editTask: TextInputEditText
    private lateinit var editDate: TextInputEditText
    private lateinit var editTime: TextInputEditText
    private lateinit var rgPriority: RadioGroup
    private lateinit var btnAdd: MaterialButton
    private lateinit var listViewTasks: ListView
    private lateinit var taskList: ArrayList<MainActivity.Task>
    private lateinit var adapter: ArrayAdapter<MainActivity.Task>
    private lateinit var sharedPreferences: SharedPreferences

    private val checkHandler = Handler(Looper.getMainLooper())
    private lateinit var checkRunnable: Runnable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tasks, container, false)
        setHasOptionsMenu(true)

        sharedPreferences = requireActivity().getSharedPreferences("MyTasksPrefs", Context.MODE_PRIVATE)

        editTask = view.findViewById(R.id.editTask)
        editDate = view.findViewById(R.id.editDate)
        editTime = view.findViewById(R.id.editTime)
        rgPriority = view.findViewById(R.id.rgPriority)
        btnAdd = view.findViewById(R.id.btnAdd)
        listViewTasks = view.findViewById(R.id.listViewTasks)

        setupPickers()
        setupList()

        btnAdd.setOnClickListener {
            val name = editTask.text.toString().trim()
            if (name.isNotEmpty()) {
                val priority = when (rgPriority.checkedRadioButtonId) {
                    R.id.rbMedium -> MainActivity.Priority.MEDIUM
                    R.id.rbHigh -> MainActivity.Priority.HIGH
                    else -> MainActivity.Priority.LOW
                }
                val newTask = MainActivity.Task(name, editDate.text.toString(), editTime.text.toString(), priority)
                taskList.add(newTask)
                taskList.sortBy { it.getTimestamp() }
                saveTasks()
                adapter.notifyDataSetChanged()
                (activity as? MainActivity)?.scheduleNotification(newTask)

                editTask.text?.clear()
                editDate.text?.clear()
                editTime.text?.clear()
                rgPriority.check(R.id.rbLow)
                Toast.makeText(context, "Task Added", Toast.LENGTH_SHORT).show()
            }
        }

        listViewTasks.setOnItemClickListener { _, _, position, _ ->
            val task = taskList[position]
            (activity as? MainActivity)?.cancelNotification(task)
            taskList.removeAt(position)
            saveTasks()
            adapter.notifyDataSetChanged()
            Toast.makeText(context, "Task Removed", Toast.LENGTH_SHORT).show()
        }

        setupAutoRemoval()
        return view
    }

    private fun setupPickers() {
        editDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(requireContext(), { _, y, m, d ->
                editDate.setText(String.format(Locale.getDefault(), "%02d/%02d/%d", d, m + 1, y))
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
        editTime.setOnClickListener {
            val c = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, h, min ->
                editTime.setText(String.format(Locale.getDefault(), "%02d:%02d", h, min))
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show()
        }
    }

    private fun setupList() {
        taskList = loadTasks()
        adapter = object : ArrayAdapter<MainActivity.Task>(requireContext(), R.layout.task_item, taskList) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: layoutInflater.inflate(R.layout.task_item, parent, false)
                val task = getItem(position)!!
                v.findViewById<TextView>(R.id.tvTaskName).text = task.name
                
                val dateTimeText = buildString {
                    if (task.date.isNotEmpty()) append(task.date)
                    if (task.time.isNotEmpty()) {
                        if (isNotEmpty()) append(" | ")
                        append(task.time)
                    }
                }
                v.findViewById<TextView>(R.id.tvTaskDateTime).text = dateTimeText
                v.findViewById<View>(R.id.priorityIndicator).setBackgroundColor(task.priority.color)
                return v
            }
        }
        listViewTasks.adapter = adapter
    }

    private fun saveTasks() {
        val jsonArray = JSONArray()
        taskList.forEach { jsonArray.put(it.toJsonObject()) }
        sharedPreferences.edit().putString("tasks", jsonArray.toString()).apply()
    }

    private fun loadTasks(): ArrayList<MainActivity.Task> {
        val saved = sharedPreferences.getString("tasks", null) ?: return ArrayList()
        val list = ArrayList<MainActivity.Task>()
        try {
            val jsonArray = JSONArray(saved)
            for (i in 0 until jsonArray.length()) {
                list.add(MainActivity.Task.fromJsonObject(jsonArray.getJSONObject(i)))
            }
        } catch (e: Exception) { e.printStackTrace() }
        return list
    }

    private fun setupAutoRemoval() {
        checkRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                // Use the exact timestamp comparison we had before
                val removedAny = taskList.removeAll { task ->
                    val taskTime = task.getTimestamp()
                    taskTime != Long.MAX_VALUE && taskTime < currentTime
                }
                
                if (removedAny) {
                    saveTasks()
                    activity?.runOnUiThread {
                        adapter.notifyDataSetChanged()
                        Toast.makeText(context, "Expired tasks cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                checkHandler.postDelayed(this, 10000)
            }
        }
        checkHandler.post(checkRunnable)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.tasks_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_clear_all) {
            taskList.forEach { (activity as? MainActivity)?.cancelNotification(it) }
            taskList.clear()
            saveTasks()
            adapter.notifyDataSetChanged()
            Toast.makeText(context, "All tasks cleared", Toast.LENGTH_SHORT).show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Re-run auto-removal when coming back to the fragment
        checkHandler.post(checkRunnable)
    }

    override fun onPause() {
        super.onPause()
        checkHandler.removeCallbacks(checkRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        checkHandler.removeCallbacks(checkRunnable)
    }
}
