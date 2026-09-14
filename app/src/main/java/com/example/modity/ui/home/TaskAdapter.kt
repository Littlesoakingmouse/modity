package com.example.modity.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.databinding.ItemTaskBinding
import com.example.modity.model.Task

class TaskAdapter(
    private val onTaskClick: (Task) -> Unit = {},
    private val onTaskCompletionChanged: (Task, Boolean) -> Unit = { _, _ -> },
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) = with(binding) {
            taskTitle.text = task.title
            taskDescription.text = task.description
            taskDescription.isVisible = task.description.isNotBlank()

            taskCompleted.setOnCheckedChangeListener(null)
            taskCompleted.isChecked = task.isCompleted
            taskCompleted.setOnCheckedChangeListener { _, isChecked ->
                onTaskCompletionChanged(task, isChecked)
            }

            root.setOnClickListener { onTaskClick(task) }
        }
    }

    private object TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
    }
}
