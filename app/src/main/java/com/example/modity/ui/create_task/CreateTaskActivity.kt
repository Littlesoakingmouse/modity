package com.example.modity.ui.create_task

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.modity.databinding.ActivityCreateTaskBinding
import com.example.modity.utils.applySystemBarInsets

class CreateTaskActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreateTaskBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.root.applySystemBarInsets()
        binding.toolbarCreateTask.setNavigationOnClickListener { finish() }
    }
}
