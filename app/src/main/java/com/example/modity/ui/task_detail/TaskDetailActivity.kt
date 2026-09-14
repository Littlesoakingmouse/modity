package com.example.modity.ui.task_detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.modity.databinding.ActivityTaskDetailBinding
import com.example.modity.utils.applySystemBarInsets

class TaskDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarInsets()
        binding.toolbarTaskDetail.setNavigationOnClickListener { finish() }
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"

        fun createIntent(context: Context, taskId: Long): Intent =
            Intent(context, TaskDetailActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
    }
}
