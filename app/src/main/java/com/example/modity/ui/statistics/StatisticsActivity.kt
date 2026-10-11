package com.example.modity.ui.statistics

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.data.Stats
import com.example.modity.databinding.ActivityStatisticsBinding
import com.example.modity.databinding.ViewStatTileBinding
import com.example.modity.model.DayTask
import com.example.modity.model.FocusSession
import com.example.modity.model.Task
import com.example.modity.model.TaskGroup
import com.example.modity.ui.common.BottomNav
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Overview, charts, per-group rates, focus analytics, habit streaks and daily history. */
class StatisticsActivity : AppCompatActivity() {
    private enum class Period(val days: Long) { DAY(1), WEEK(7), MONTH(30) }

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var repository: ModityRepository
    private val groupAdapter = StatRowAdapter()
    private val streakAdapter = StatRowAdapter()
    private val historyAdapter = StatRowAdapter()

    private var period = Period.WEEK
    private var groups: List<TaskGroup> = emptyList()
    private var groupFilter: Long? = null // null = all groups, UNGROUPED = tasks without a group

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets(bottom = false)
        repository = ModityRepository.get(this)

        binding.rvGroups.adapter = groupAdapter
        binding.rvStreaks.adapter = streakAdapter
        binding.rvHistory.adapter = historyAdapter

        binding.togglePeriod.check(R.id.btn_period_week)
        binding.togglePeriod.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            period = when (checkedId) {
                R.id.btn_period_day -> Period.DAY
                R.id.btn_period_month -> Period.MONTH
                else -> Period.WEEK
            }
            load()
        }
        binding.actGroupFilter.setOnItemClickListener { _, _, position, _ ->
            groupFilter = when (position) {
                0 -> null
                1 -> UNGROUPED
                else -> groups[position - 2].id
            }
            load()
        }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.setup(this, binding.bottomNavigation, R.id.nav_stats)
        load()
    }

    private fun matchesGroup(task: Task): Boolean = when (groupFilter) {
        null -> true
        UNGROUPED -> task.groupId == null
        else -> task.groupId == groupFilter
    }

    private fun load() {
        lifecycleScope.launch {
            groups = repository.groups()
            setupGroupFilter()

            val today = LocalDate.now()
            val from = today.minusDays(period.days - 1)
            val tasks = repository.tasks().filter(::matchesGroup)
            val occurrences = repository.rangeTasks(from, today, tasks)

            val zone = ZoneId.systemDefault()
            val taskIds = tasks.map { it.id }.toSet()
            val sessions = repository.focusSessions(
                from.atStartOfDay(zone).toInstant().toEpochMilli(),
                System.currentTimeMillis(),
            ).filter { groupFilter == null || it.taskId in taskIds }

            val streaks = tasks.filter { it.isRepeating }.map { it to Stats.streak(repository.history(it)) }

            renderOverview(occurrences, sessions)
            renderCharts(occurrences, from, today)
            renderGroups(occurrences)
            renderFocus(sessions)
            streakAdapter.submitList(
                streaks.sortedByDescending { it.second.current }.map { (task, streak) ->
                    StatRow(
                        key = "s${task.id}",
                        title = task.title,
                        value = getString(R.string.streak_value, streak.current),
                        subtitle = getString(R.string.streak_best_value, streak.best),
                        color = task.color,
                        icon = task.icon,
                    )
                }.ifEmpty { listOf(emptyRow(R.string.stats_no_habits)) },
            )
            renderHistory(occurrences, from, today)
        }
    }

    private fun setupGroupFilter() {
        val labels = listOf(getString(R.string.filter_all_groups), getString(R.string.no_group)) + groups.map { it.name }
        binding.actGroupFilter.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
        val current = when (groupFilter) {
            null -> labels[0]
            UNGROUPED -> labels[1]
            else -> groups.firstOrNull { it.id == groupFilter }?.name ?: labels[0]
        }
        binding.actGroupFilter.setText(current, false)
    }

    private fun tile(tile: ViewStatTileBinding, value: String, label: Int) {
        tile.tvTileValue.text = value
        tile.tvTileLabel.setText(label)
    }

    private fun renderOverview(occurrences: List<DayTask>, sessions: List<FocusSession>) = with(binding) {
        val overview = Stats.overview(occurrences)
        tile(tileRate, getString(R.string.percent, overview.rate), R.string.stat_completion_rate)
        tile(tileScheduled, overview.total.toString(), R.string.stat_scheduled)
        tile(tileCompleted, overview.completed.toString(), R.string.status_completed)
        tile(tileMissed, overview.missed.toString(), R.string.status_missed)
        tile(tileSkipped, overview.skipped.toString(), R.string.status_skipped)
        tile(tileFocusTime, Formatters.duration(this@StatisticsActivity, Stats.focus(sessions).totalSeconds), R.string.stat_focus_time)
    }

    private fun renderCharts(occurrences: List<DayTask>, from: LocalDate, to: LocalDate) = with(binding) {
        sectionCharts.isVisible = period != Period.DAY
        if (period == Period.DAY) return@with
        val days = Stats.perDay(occurrences, from, to)
        val labels = days.map { (date, _) ->
            if (period == Period.WEEK) Formatters.weekdayNarrow(this@StatisticsActivity, date.dayOfWeek) else date.dayOfMonth.toString()
        }
        chartCompleted.setData(days.map { it.second.completed.toFloat() }, labels)
        chartRate.setData(days.map { (_, overview) -> if (overview.total == 0) null else overview.rate.toFloat() }, labels)
    }

    private fun renderGroups(occurrences: List<DayTask>) {
        val byGroup = occurrences.groupBy { it.task.groupId }
        val rows = groups.mapNotNull { group ->
            val items = byGroup[group.id] ?: return@mapNotNull null
            val overview = Stats.overview(items)
            StatRow(
                key = "g${group.id}",
                title = group.name,
                value = getString(R.string.percent, overview.rate),
                subtitle = getString(R.string.progress_done_of, overview.completed, overview.total),
                progress = overview.rate,
                color = group.color,
                icon = group.icon,
            )
        }.sortedByDescending { it.progress }.toMutableList()
        byGroup[null]?.let { items ->
            val overview = Stats.overview(items)
            rows += StatRow(
                key = "g0",
                title = getString(R.string.no_group),
                value = getString(R.string.percent, overview.rate),
                subtitle = getString(R.string.progress_done_of, overview.completed, overview.total),
                progress = overview.rate,
            )
        }
        groupAdapter.submitList(rows.ifEmpty { listOf(emptyRow(R.string.stats_no_data)) })
    }

    private fun renderFocus(sessions: List<FocusSession>) = with(binding) {
        val focus = Stats.focus(sessions)
        tile(tileSessions, getString(R.string.progress_fraction, focus.completed, focus.sessions), R.string.stat_sessions_completed)
        tile(tileAvgSession, Formatters.duration(this@StatisticsActivity, focus.averageSeconds), R.string.stat_avg_session)
        tile(tileInterrupted, getString(R.string.percent, focus.interruptedRate), R.string.stat_interrupted)
    }

    private fun renderHistory(occurrences: List<DayTask>, from: LocalDate, to: LocalDate) {
        val rows = Stats.perDay(occurrences, from, to)
            .filter { it.second.total > 0 }
            .reversed()
            .map { (date, overview) ->
                StatRow(
                    key = "d$date",
                    title = Formatters.dayShort(this, date),
                    value = getString(R.string.progress_fraction, overview.completed, overview.total),
                    subtitle = listOfNotNull(
                        overview.missed.takeIf { it > 0 }?.let { getString(R.string.history_missed, it) },
                        overview.skipped.takeIf { it > 0 }?.let { getString(R.string.history_skipped, it) },
                    ).joinToString("  ·  ").ifBlank { null },
                    progress = overview.rate,
                )
            }
        historyAdapter.submitList(rows.ifEmpty { listOf(emptyRow(R.string.stats_no_data)) })
    }

    private fun emptyRow(message: Int) = StatRow(key = "empty$message", title = getString(message), value = "")

    private companion object {
        const val UNGROUPED = -1L
    }
}
