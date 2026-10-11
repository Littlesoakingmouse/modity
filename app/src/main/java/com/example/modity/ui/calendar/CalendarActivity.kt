package com.example.modity.ui.calendar

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.databinding.ActivityCalendarBinding
import com.example.modity.databinding.ItemCalendarDayBinding
import com.example.modity.model.DayTask
import com.example.modity.ui.common.BottomNav
import com.example.modity.ui.common.TaskActions
import com.example.modity.ui.common.TaskListAdapter
import com.example.modity.ui.common.TaskListItem
import com.example.modity.ui.task.TaskEditorActivity
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import com.example.modity.utils.themeColor
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** Plan overview by day, week or month. */
class CalendarActivity : AppCompatActivity() {
    private enum class Mode { DAY, WEEK, MONTH }

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var repository: ModityRepository
    private lateinit var agendaAdapter: TaskListAdapter
    private val dayAdapter = CalendarDayAdapter { date ->
        selected = date
        load()
    }

    private var mode = Mode.MONTH
    private var selected: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets(bottom = false)
        repository = ModityRepository.get(this)

        agendaAdapter = TaskListAdapter(TaskActions(this) { load() })
        binding.rvAgenda.adapter = agendaAdapter
        binding.rvCalendar.layoutManager = GridLayoutManager(this, DAYS_IN_WEEK)
        binding.rvCalendar.adapter = dayAdapter
        buildWeekdayHeader()

        binding.toggleMode.check(R.id.btn_mode_month)
        binding.toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            mode = when (checkedId) {
                R.id.btn_mode_day -> Mode.DAY
                R.id.btn_mode_week -> Mode.WEEK
                else -> Mode.MONTH
            }
            load()
        }
        binding.btnPrev.setOnClickListener { shift(-1) }
        binding.btnNext.setOnClickListener { shift(1) }
        binding.tvCalendarTitle.setOnClickListener {
            selected = LocalDate.now()
            load()
        }
        binding.btnAddTask.setOnClickListener { startActivity(TaskEditorActivity.createIntent(this, selected)) }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.setup(this, binding.bottomNavigation, R.id.nav_calendar)
        load()
    }

    private fun buildWeekdayHeader() {
        DayOfWeek.entries.forEach { day ->
            binding.weekdayHeader.addView(TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                gravity = Gravity.CENTER
                text = Formatters.weekdayNarrow(this@CalendarActivity, day)
                setTextAppearance(R.style.TextAppearance_Modity_Caption)
            })
        }
    }

    private fun shift(step: Long) {
        selected = when (mode) {
            Mode.DAY -> selected.plusDays(step)
            Mode.WEEK -> selected.plusWeeks(step)
            Mode.MONTH -> selected.plusMonths(step)
        }
        load()
    }

    private fun load() {
        val weekStart = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val (gridDays, rangeStart, rangeEnd) = when (mode) {
            Mode.DAY -> Triple(emptyList<LocalDate?>(), selected, selected)
            Mode.WEEK -> Triple((0L until 7L).map { weekStart.plusDays(it) }, weekStart, weekStart.plusDays(6))
            Mode.MONTH -> {
                val first = selected.withDayOfMonth(1)
                val leading = first.dayOfWeek.value - 1
                val days = List(leading) { null } + (0 until selected.lengthOfMonth()).map { first.plusDays(it.toLong()) }
                Triple(days, first, first.plusDays(selected.lengthOfMonth() - 1L))
            }
        }

        binding.tvCalendarTitle.text = when (mode) {
            Mode.DAY -> Formatters.dayLong(this, selected)
            else -> Formatters.monthYear(this, selected)
        }
        binding.weekdayHeader.isVisible = mode != Mode.DAY
        binding.rvCalendar.isVisible = mode != Mode.DAY

        lifecycleScope.launch {
            val occurrences = repository.rangeTasks(rangeStart, rangeEnd)
            val byDate = occurrences.groupBy { it.date }
            dayAdapter.submit(gridDays, selected, byDate)

            val agendaDays = if (mode == Mode.WEEK) (0L until 7L).map { weekStart.plusDays(it) } else listOf(selected)
            val items = buildList {
                agendaDays.forEach { date ->
                    val tasks = byDate[date].orEmpty().sortedBy { it.task.anchorTime() ?: Int.MAX_VALUE }
                    if (tasks.isEmpty() && mode == Mode.WEEK) return@forEach
                    add(TaskListItem.Header(Formatters.dayLong(this@CalendarActivity, date)))
                    tasks.forEach { add(TaskListItem.Row(it)) }
                }
            }
            agendaAdapter.submitList(items)
            binding.tvAgendaEmpty.isVisible = items.none { it is TaskListItem.Row }
        }
    }

    private class CalendarDayAdapter(
        private val onClick: (LocalDate) -> Unit,
    ) : RecyclerView.Adapter<CalendarDayAdapter.Holder>() {
        private var days: List<LocalDate?> = emptyList()
        private var selected: LocalDate = LocalDate.now()
        private var occurrences: Map<LocalDate, List<DayTask>> = emptyMap()

        @Suppress("NotifyDataSetChanged")
        fun submit(days: List<LocalDate?>, selected: LocalDate, occurrences: Map<LocalDate, List<DayTask>>) {
            this.days = days
            this.selected = selected
            this.occurrences = occurrences
            notifyDataSetChanged()
        }

        override fun getItemCount() = days.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int): Unit = with(holder.binding) {
            val date = days[position]
            val context = root.context
            tvCalendarDay.isInvisible = date == null
            viewCalendarDot.isInvisible = true
            root.setOnClickListener(null)
            if (date == null) return@with

            tvCalendarDay.text = date.dayOfMonth.toString()
            tvCalendarDay.isSelected = date == selected
            tvCalendarDay.isActivated = date == LocalDate.now()
            root.contentDescription = Formatters.dayLong(context, date)
            root.setOnClickListener { onClick(date) }

            val tasks = occurrences[date].orEmpty()
            if (tasks.isNotEmpty()) {
                val color = if (tasks.all { it.isDone }) {
                    ContextCompat.getColor(context, R.color.status_completed)
                } else {
                    context.themeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
                }
                viewCalendarDot.backgroundTintList = ColorStateList.valueOf(color)
                viewCalendarDot.isInvisible = false
            }
        }

        class Holder(val binding: ItemCalendarDayBinding) : RecyclerView.ViewHolder(binding.root)
    }

    private companion object {
        const val DAYS_IN_WEEK = 7
    }
}
