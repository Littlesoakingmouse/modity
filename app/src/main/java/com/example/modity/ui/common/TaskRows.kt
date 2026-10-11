package com.example.modity.ui.common

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.R
import com.example.modity.databinding.ItemSectionHeaderBinding
import com.example.modity.databinding.ItemTaskBinding
import com.example.modity.model.DayTask
import com.example.modity.model.OccurrenceStatus
import com.example.modity.utils.Formatters

interface TaskListener {
    fun onToggle(item: DayTask)
    fun onOpen(item: DayTask)
    fun onMore(item: DayTask, anchor: View)
}

object TaskRows {

    fun bind(
        binding: ItemTaskBinding,
        item: DayTask,
        listener: TaskListener,
        groupName: String? = null,
        showDate: Boolean = false,
    ) = with(binding) {
        val context = root.context
        val resolved = item.status == OccurrenceStatus.COMPLETED || item.status == OccurrenceStatus.SKIPPED

        tvTaskTitle.text = item.task.title
        tvTaskTitle.paintFlags = if (resolved) {
            tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
        tvTaskTitle.alpha = if (resolved) 0.5f else 1f

        val meta = listOfNotNull(
            if (showDate) Formatters.relativeDay(context, item.date) else null,
            Formatters.taskMeta(context, item.task, groupName).ifBlank { null },
        ).joinToString("  ·  ")
        tvTaskMeta.text = meta
        tvTaskMeta.isVisible = meta.isNotBlank()

        val showStatus = item.status == OccurrenceStatus.MISSED || item.status == OccurrenceStatus.SKIPPED
        tvTaskStatus.isVisible = showStatus
        if (showStatus) {
            tvTaskStatus.setText(Formatters.statusLabel(item.status))
            tvTaskStatus.setTextColor(ContextCompat.getColor(context, Formatters.statusColor(item.status)))
        }

        bindCheck(ivCheck, item.status, item.task.color)
        checkContainer.setOnClickListener { listener.onToggle(item) }
        btnTaskMore.setOnClickListener { listener.onMore(item, it) }
        root.setOnClickListener { listener.onOpen(item) }
        root.setOnLongClickListener {
            listener.onMore(item, btnTaskMore)
            true
        }
    }

    /** Circle check: filled with the task color when done, outlined otherwise. */
    fun bindCheck(view: ImageView, status: OccurrenceStatus, color: Int) {
        val context = view.context
        val white = ContextCompat.getColor(context, android.R.color.white)
        when (status) {
            OccurrenceStatus.COMPLETED -> {
                view.setBackgroundResource(R.drawable.bg_circle)
                view.backgroundTintList = ColorStateList.valueOf(color)
                view.setImageResource(R.drawable.ic_check)
                view.imageTintList = ColorStateList.valueOf(white)
            }
            OccurrenceStatus.SKIPPED -> {
                val muted = ContextCompat.getColor(context, R.color.status_skipped)
                view.setBackgroundResource(R.drawable.bg_ring)
                view.backgroundTintList = ColorStateList.valueOf(muted)
                view.setImageResource(R.drawable.ic_skip)
                view.imageTintList = ColorStateList.valueOf(muted)
            }
            OccurrenceStatus.MISSED -> {
                val missed = ContextCompat.getColor(context, R.color.status_missed)
                view.setBackgroundResource(R.drawable.bg_ring)
                view.backgroundTintList = ColorStateList.valueOf(missed)
                view.setImageDrawable(null)
            }
            else -> {
                view.setBackgroundResource(R.drawable.bg_ring)
                view.backgroundTintList = ColorStateList.valueOf(color)
                view.setImageDrawable(null)
            }
        }
    }
}

sealed interface TaskListItem {
    data class Header(val title: String) : TaskListItem
    data class Row(val item: DayTask, val groupName: String? = null) : TaskListItem
}

/** Generic task list with optional section headers (calendar, group detail, search). */
class TaskListAdapter(
    private val listener: TaskListener,
    private val showDate: Boolean = false,
) : ListAdapter<TaskListItem, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is TaskListItem.Header -> TYPE_HEADER
        is TaskListItem.Row -> TYPE_ROW
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false))
        } else {
            RowHolder(ItemTaskBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TaskListItem.Header -> (holder as HeaderHolder).binding.tvSectionTitle.text = item.title
            is TaskListItem.Row -> TaskRows.bind((holder as RowHolder).binding, item.item, listener, item.groupName, showDate)
        }
    }

    class HeaderHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class RowHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    private object Diff : DiffUtil.ItemCallback<TaskListItem>() {
        override fun areItemsTheSame(oldItem: TaskListItem, newItem: TaskListItem): Boolean = when {
            oldItem is TaskListItem.Header && newItem is TaskListItem.Header -> oldItem.title == newItem.title
            oldItem is TaskListItem.Row && newItem is TaskListItem.Row ->
                oldItem.item.task.id == newItem.item.task.id && oldItem.item.date == newItem.item.date
            else -> false
        }

        override fun areContentsTheSame(oldItem: TaskListItem, newItem: TaskListItem): Boolean = oldItem == newItem
    }

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ROW = 1
    }
}
