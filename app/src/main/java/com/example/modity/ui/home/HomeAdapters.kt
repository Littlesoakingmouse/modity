package com.example.modity.ui.home

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.R
import com.example.modity.databinding.ItemGroupCardBinding
import com.example.modity.databinding.ItemGroupGridBinding
import com.example.modity.databinding.ItemSectionHeaderBinding
import com.example.modity.databinding.ItemTaskBinding
import com.example.modity.databinding.ItemTimelineGapBinding
import com.example.modity.databinding.ItemTimelineTaskBinding
import com.example.modity.databinding.ItemWeekDayBinding
import com.example.modity.model.DayTask
import com.example.modity.model.GroupProgress
import com.example.modity.model.TaskGroup
import com.example.modity.ui.common.TaskListener
import com.example.modity.ui.common.TaskRows
import com.example.modity.ui.common.bindBadge
import com.example.modity.utils.Formatters
import java.time.LocalDate

class WeekStripAdapter(
    private val onDateClick: (LocalDate) -> Unit,
) : ListAdapter<LocalDate, WeekStripAdapter.Holder>(DateDiff) {

    var selectedDate: LocalDate = LocalDate.now()
        set(value) {
            val previous = currentList.indexOf(field)
            field = value
            if (previous >= 0) notifyItemChanged(previous)
            currentList.indexOf(value).takeIf { it >= 0 }?.let(::notifyItemChanged)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemWeekDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
        val date = getItem(position)
        tvWeekday.text = Formatters.weekdayNarrow(root.context, date.dayOfWeek)
        tvDayNumber.text = date.dayOfMonth.toString()
        root.isSelected = date == selectedDate
        root.isActivated = date == LocalDate.now()
        root.contentDescription = Formatters.dayLong(root.context, date)
        root.setOnClickListener { onDateClick(date) }
    }

    class Holder(val binding: ItemWeekDayBinding) : RecyclerView.ViewHolder(binding.root)

    private object DateDiff : DiffUtil.ItemCallback<LocalDate>() {
        override fun areItemsTheSame(oldItem: LocalDate, newItem: LocalDate) = oldItem == newItem
        override fun areContentsTheSame(oldItem: LocalDate, newItem: LocalDate) = oldItem == newItem
    }
}

private object GroupDiff : DiffUtil.ItemCallback<GroupProgress>() {
    override fun areItemsTheSame(oldItem: GroupProgress, newItem: GroupProgress) = oldItem.key == newItem.key
    override fun areContentsTheSame(oldItem: GroupProgress, newItem: GroupProgress) = oldItem == newItem
}

/** Card view: one card per group with its progress and tasks; ungrouped tasks get their own card. */
class GroupCardAdapter(
    private val listener: TaskListener,
    private val onGroupClick: (TaskGroup) -> Unit,
) : ListAdapter<GroupProgress, GroupCardAdapter.Holder>(GroupDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemGroupCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
        val entry = getItem(position)
        val group = entry.group
        groupHeader.isVisible = group != null
        if (group != null) {
            ivGroupIcon.bindBadge(group.icon, group.color)
            tvGroupName.text = group.name
            tvGroupCount.text = root.context.getString(R.string.progress_fraction, entry.done, entry.total)
            progressGroup.setIndicatorColor(group.color)
            progressGroup.setProgressCompat(entry.percent, false)
            groupHeader.setOnClickListener { onGroupClick(group) }
        }

        val inflater = LayoutInflater.from(root.context)
        taskContainer.removeAllViews()
        entry.tasks.forEach { item ->
            val row = ItemTaskBinding.inflate(inflater, taskContainer, true)
            TaskRows.bind(row, item, listener)
        }
    }

    class Holder(val binding: ItemGroupCardBinding) : RecyclerView.ViewHolder(binding.root)
}

/** Grid view: compact tiles; a tile opens the group (or the task when ungrouped). */
class GroupGridAdapter(
    private val listener: TaskListener,
    private val onGroupClick: (TaskGroup) -> Unit,
) : ListAdapter<GroupProgress, GroupGridAdapter.Holder>(GroupDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemGroupGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
        val entry = getItem(position)
        val context = root.context
        val group = entry.group
        val single = entry.tasks.first()
        val color = group?.color ?: single.task.color

        ivGridIcon.bindBadge(group?.icon ?: single.task.icon, color)
        tvGridName.text = group?.name ?: single.task.title
        tvGridDetail.text = if (group != null) {
            context.getString(R.string.progress_done_of, entry.done, entry.total)
        } else {
            listOfNotNull(
                single.task.startTime?.let { Formatters.time(context, it) },
                context.getString(Formatters.statusLabel(single.status)),
            ).joinToString("  ·  ")
        }
        progressGrid.setIndicatorColor(color)
        progressGrid.setProgressCompat(entry.percent, false)
        tvGridPercent.text = context.getString(R.string.percent, entry.percent)
        gridCard.setOnClickListener {
            if (group != null) onGroupClick(group) else listener.onOpen(single)
        }
        gridCard.setOnLongClickListener {
            if (group == null) listener.onMore(single, it)
            group == null
        }
        progressGrid.setOnClickListener { if (group == null) listener.onToggle(single) else onGroupClick(group) }
    }

    class Holder(val binding: ItemGroupGridBinding) : RecyclerView.ViewHolder(binding.root)
}

sealed interface TimelineItem {
    data class Entry(val item: DayTask, val groupName: String?) : TimelineItem
    data class Gap(val minutes: Int) : TimelineItem
    data class Header(val title: String) : TimelineItem
}

/** Timeline view: timed tasks in order with free-time gaps, then "anytime" tasks. */
class TimelineAdapter(
    private val listener: TaskListener,
) : ListAdapter<TimelineItem, RecyclerView.ViewHolder>(TimelineDiff) {

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is TimelineItem.Entry -> TYPE_TASK
        is TimelineItem.Gap -> TYPE_GAP
        is TimelineItem.Header -> TYPE_HEADER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TASK -> TaskHolder(ItemTimelineTaskBinding.inflate(inflater, parent, false))
            TYPE_GAP -> GapHolder(ItemTimelineGapBinding.inflate(inflater, parent, false))
            else -> HeaderHolder(ItemSectionHeaderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = holder.itemView.context
        when (val item = getItem(position)) {
            is TimelineItem.Entry -> with((holder as TaskHolder).binding) {
                tvTimelineTime.text = item.item.task.anchorTime()?.let { Formatters.time(context, it) }.orEmpty()
                timelineDot.backgroundTintList = ColorStateList.valueOf(item.item.task.color)
                TaskRows.bind(timelineTask, item.item, listener, item.groupName)
            }
            is TimelineItem.Gap -> (holder as GapHolder).binding.tvGap.text =
                context.getString(R.string.timeline_free, Formatters.duration(context, item.minutes * 60L))
            is TimelineItem.Header -> (holder as HeaderHolder).binding.tvSectionTitle.text = item.title
        }
    }

    class TaskHolder(val binding: ItemTimelineTaskBinding) : RecyclerView.ViewHolder(binding.root)
    class GapHolder(val binding: ItemTimelineGapBinding) : RecyclerView.ViewHolder(binding.root)
    class HeaderHolder(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val TYPE_TASK = 0
        private const val TYPE_GAP = 1
        private const val TYPE_HEADER = 2
        private const val MIN_GAP_MINUTES = 60
        private const val DEFAULT_TASK_MINUTES = 30

        fun build(context: Context, entries: List<GroupProgress>): List<TimelineItem> {
            val rows = entries.flatMap { entry -> entry.tasks.map { it to entry.group?.name } }
            val timed = rows.filter { it.first.task.anchorTime() != null }.sortedBy { it.first.task.anchorTime() }
            val anytime = rows.filter { it.first.task.anchorTime() == null }

            val items = mutableListOf<TimelineItem>()
            var previousEnd: Int? = null
            for ((item, groupName) in timed) {
                val start = item.task.anchorTime()!!
                previousEnd?.let { end -> if (start - end >= MIN_GAP_MINUTES) items += TimelineItem.Gap(start - end) }
                items += TimelineItem.Entry(item, groupName)
                previousEnd = maxOf(previousEnd ?: 0, start + durationOf(item))
            }
            if (anytime.isNotEmpty()) {
                items += TimelineItem.Header(context.getString(R.string.timeline_anytime))
                anytime.forEach { (item, groupName) -> items += TimelineItem.Entry(item, groupName) }
            }
            return items
        }

        /** Tasks have no explicit duration: use the deadline, the focus length, or a default. */
        private fun durationOf(item: DayTask): Int {
            val task = item.task
            val start = task.startTime
            val due = task.dueTime
            return when {
                start != null && due != null && due > start && task.dueDate == null -> due - start
                task.focusEnabled -> task.focusMinutes
                else -> DEFAULT_TASK_MINUTES
            }
        }
    }

    private object TimelineDiff : DiffUtil.ItemCallback<TimelineItem>() {
        override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem) = when {
            oldItem is TimelineItem.Entry && newItem is TimelineItem.Entry -> oldItem.item.task.id == newItem.item.task.id
            else -> oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem) = oldItem == newItem
    }
}
