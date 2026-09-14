package com.example.modity.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.modity.databinding.ActivityHomeBinding
import com.example.modity.ui.create_task.CreateTaskActivity
import com.example.modity.ui.task_detail.TaskDetailActivity
import com.example.modity.utils.applySystemBarInsets

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    private val dateAdapter = DateAdapter()
    private val taskGroupAdapter = TaskGroupAdapter(
        onTaskClick = { task ->
            startActivity(TaskDetailActivity.createIntent(this, task.id))
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarInsets()
        setupLists()
        setupListeners()
        showEmptyState()
    }

    private fun setupLists() = with(binding) {
        rvDates.layoutManager = LinearLayoutManager(
            this@HomeActivity,
            LinearLayoutManager.HORIZONTAL,
            false,
        )
        rvDates.adapter = dateAdapter

        rvTaskGroups.layoutManager = LinearLayoutManager(this@HomeActivity)
        rvTaskGroups.adapter = taskGroupAdapter
    }

    private fun setupListeners() {
        binding.btnAddTask.setOnClickListener {
            startActivity(Intent(this, CreateTaskActivity::class.java))
        }
    }

    private fun showEmptyState() {
        binding.emptyTasks.isVisible = taskGroupAdapter.currentList.isEmpty()
    }
}
