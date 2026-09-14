package com.example.modity.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.R
import com.example.modity.databinding.ItemTaskGroupBinding
import com.example.modity.model.Task
import com.example.modity.model.TaskGroup

class TaskGroupAdapter(
    private val onTaskClick: (Task) -> Unit = {},
    private val onTaskCompletionChanged: (Task, Boolean) -> Unit = { _, _ -> },
) : ListAdapter<TaskGroup, TaskGroupAdapter.TaskGroupViewHolder>(TaskGroupDiffCallback) {
    private val taskViewPool = RecyclerView.RecycledViewPool()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskGroupViewHolder {
        val binding = ItemTaskGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskGroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskGroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskGroupViewHolder(
        private val binding: ItemTaskGroupBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val taskAdapter = TaskAdapter(onTaskClick, onTaskCompletionChanged)

        init {
            binding.rvTasks.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = taskAdapter
                setRecycledViewPool(taskViewPool)
            }
        }

        fun bind(taskGroup: TaskGroup) = with(binding) {
            taskGroupTitle.text = taskGroup.name
            taskGroupCount.text = root.resources.getQuantityString(
                R.plurals.task_count,
                taskGroup.tasks.size,
                taskGroup.tasks.size,
            )
            taskAdapter.submitList(taskGroup.tasks)
        }
    }

    private object TaskGroupDiffCallback : DiffUtil.ItemCallback<TaskGroup>() {
        override fun areItemsTheSame(oldItem: TaskGroup, newItem: TaskGroup): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TaskGroup, newItem: TaskGroup): Boolean =
            oldItem == newItem
    }
}
