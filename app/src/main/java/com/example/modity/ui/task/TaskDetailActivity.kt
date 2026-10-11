package com.example.modity.ui.task

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.data.Stats
import com.example.modity.databinding.ActivityTaskDetailBinding
import com.example.modity.databinding.ItemHistoryEntryBinding
import com.example.modity.databinding.ItemInfoRowBinding
import com.example.modity.model.DayTask
import com.example.modity.model.OccurrenceStatus
import com.example.modity.model.Priority
import com.example.modity.model.Task
import com.example.modity.ui.common.TaskActions
import com.example.modity.ui.common.bindBadge
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskDetailBinding
    private lateinit var repository: ModityRepository
    private lateinit var actions: TaskActions
    private val historyAdapter = HistoryAdapter()

    private var taskId: Long = -1
    private var requestedDate: LocalDate? = null
    private var current: DayTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()

        repository = ModityRepository.get(this)
        actions = TaskActions(this) { load() }
        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        requestedDate = intent.getLongExtra(EXTRA_DATE, -1).takeIf { it >= 0 }?.let(LocalDate::ofEpochDay)

        binding.rvHistory.adapter = historyAdapter
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            val dayTask = current ?: return@setOnMenuItemClickListener false
            when (item.itemId) {
                R.id.action_edit -> actions.edit(dayTask)
                R.id.action_delete -> actions.delete(dayTask) { finish() }
            }
            true
        }
        binding.btnComplete.setOnClickListener { current?.let(actions::onToggle) }
        binding.btnSkip.setOnClickListener {
            current?.let {
                actions.setStatus(it, if (it.status == OccurrenceStatus.SKIPPED) OccurrenceStatus.PENDING else OccurrenceStatus.SKIPPED)
            }
        }
        binding.btnFocus.setOnClickListener { current?.let(actions::startFocus) }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val task = repository.task(taskId)
            if (task == null) {
                finish()
                return@launch
            }
            val today = LocalDate.now()
            val date = requestedDate?.takeIf { task.occursOn(it) }
                ?: task.lastOccurrenceOnOrBefore(today)
                ?: task.nextOccurrenceOnOrAfter(today)
                ?: LocalDate.ofEpochDay(task.startDate)
            val dayTask = DayTask(task, date, repository.statusOf(task, date))
            if (dayTask.status == OccurrenceStatus.REMOVED) {
                // This occurrence was edited or deleted on its own.
                finish()
                return@launch
            }
            current = dayTask
            val groupName = task.groupId?.let { repository.group(it)?.name }
            val history = repository.history(task)
            val sessions = repository.focusSessionsForTask(task.id)
            render(dayTask, groupName, history)
            val focusTotal = Stats.focus(sessions)
            binding.tvFocusSummary.isVisible = focusTotal.sessions > 0
            binding.tvFocusSummary.text = resources.getQuantityString(
                R.plurals.focus_sessions_summary,
                focusTotal.sessions,
                focusTotal.sessions,
                Formatters.duration(this@TaskDetailActivity, focusTotal.totalSeconds),
            )
        }
    }

    private fun render(item: DayTask, groupName: String?, history: List<DayTask>) = with(binding) {
        val task = item.task
        val context = this@TaskDetailActivity
        ivTaskIcon.bindBadge(task.icon, task.color)
        tvTaskTitle.text = task.title
        tvTaskSubtitle.text = listOfNotNull(groupName, Formatters.repeatSummary(context, task)).joinToString("  ·  ")
        tvDescription.text = task.description
        tvDescription.isVisible = task.description.isNotBlank()

        val statusColor = ContextCompat.getColor(context, Formatters.statusColor(item.status))
        tvStatus.text = getString(R.string.status_on_day, getString(Formatters.statusLabel(item.status)), Formatters.relativeDay(context, item.date))
        tvStatus.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(statusColor, 0x33))

        btnComplete.setText(if (item.isDone) R.string.action_mark_undone else R.string.action_mark_done)
        btnComplete.setIconResource(if (item.isDone) R.drawable.ic_undo else R.drawable.ic_check)
        btnSkip.setText(if (item.status == OccurrenceStatus.SKIPPED) R.string.action_unskip else R.string.action_skip)
        btnSkip.isEnabled = !item.isDone
        btnFocus.text = getString(R.string.action_start_focus_minutes, task.focusMinutes)

        renderInfo(task)

        val streak = Stats.streak(history)
        val overview = Stats.overview(history)
        labelStreak.isVisible = task.isRepeating
        streakRow.isVisible = task.isRepeating
        tileCurrentStreak.tvTileValue.text = streak.current.toString()
        tileCurrentStreak.tvTileLabel.setText(R.string.streak_current)
        tileBestStreak.tvTileValue.text = streak.best.toString()
        tileBestStreak.tvTileLabel.setText(R.string.streak_best)
        tileRate.tvTileValue.text = getString(R.string.percent, overview.rate)
        tileRate.tvTileLabel.setText(R.string.stat_completion_rate)

        labelHistory.isVisible = history.isNotEmpty()
        historyAdapter.items = history.take(HISTORY_LIMIT)
    }

    private fun renderInfo(task: Task) {
        val context = this
        val container = binding.infoContainer
        container.removeAllViews()
        fun row(@DrawableRes icon: Int, label: Int, value: String) {
            ItemInfoRowBinding.inflate(layoutInflater, container, true).apply {
                ivInfoIcon.setImageResource(icon)
                tvInfoLabel.setText(label)
                tvInfoValue.text = value
            }
        }
        val dateLabel = if (task.isRepeating) R.string.field_start_date else R.string.field_date
        row(R.drawable.ic_calendar, dateLabel, Formatters.dateMedium(context, LocalDate.ofEpochDay(task.startDate)))
        task.startTime?.let { row(R.drawable.ic_clock, R.string.field_time, Formatters.time(context, it)) }
        if (task.isRepeating) {
            row(R.drawable.ic_repeat, R.string.repeat, Formatters.repeatSummary(context, task))
            row(
                R.drawable.ic_repeat,
                R.string.field_end_date,
                task.endDate?.let { Formatters.dateMedium(context, LocalDate.ofEpochDay(it)) } ?: getString(R.string.never),
            )
        }
        if (task.dueDate != null || task.dueTime != null) {
            val due = listOfNotNull(
                task.dueDate?.let { Formatters.dateMedium(context, LocalDate.ofEpochDay(it)) },
                task.dueTime?.let { Formatters.time(context, it) },
            ).joinToString("  ")
            row(R.drawable.ic_deadline, R.string.field_deadline, due)
        }
        row(R.drawable.ic_bell, R.string.reminder, Formatters.reminderLabel(context, task.reminderMinutes))
        if (task.priority != Priority.NONE) row(R.drawable.ic_flag, R.string.label_priority, getString(Formatters.priorityLabel(task.priority)))
        row(
            R.drawable.ic_timer,
            R.string.requires_focus,
            if (task.focusEnabled) getString(R.string.minutes_short, task.focusMinutes) else getString(R.string.off),
        )
    }

    private class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.Holder>() {
        var items: List<DayTask> = emptyList()
            set(value) {
                field = value
                @Suppress("NotifyDataSetChanged")
                notifyDataSetChanged()
            }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemHistoryEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
            val item = items[position]
            val context = root.context
            val color = ContextCompat.getColor(context, Formatters.statusColor(item.status))
            viewHistoryDot.backgroundTintList = ColorStateList.valueOf(color)
            tvHistoryDate.text = Formatters.dayShort(context, item.date)
            tvHistoryStatus.setText(Formatters.statusLabel(item.status))
        }

        class Holder(val binding: ItemHistoryEntryBinding) : RecyclerView.ViewHolder(binding.root)
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_DATE = "date"
        private const val HISTORY_LIMIT = 30

        fun intent(context: Context, taskId: Long, date: LocalDate?): Intent =
            Intent(context, TaskDetailActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_DATE, date?.toEpochDay() ?: -1L)
    }
}
