package com.example.modity.ui.task

import android.os.Bundle
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.databinding.ActivitySearchBinding
import com.example.modity.model.DayTask
import com.example.modity.model.OccurrenceStatus
import com.example.modity.model.TaskGroup
import com.example.modity.ui.common.Pickers
import com.example.modity.ui.common.TaskActions
import com.example.modity.ui.common.TaskListAdapter
import com.example.modity.ui.common.TaskListItem
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Search tasks by name; filter by group, status, type and date; sort by time, deadline, priority or name. */
class SearchActivity : AppCompatActivity() {
    private enum class Sort(val label: Int) {
        TIME(R.string.sort_time), DEADLINE(R.string.sort_deadline), PRIORITY(R.string.sort_priority), NAME(R.string.sort_name)
    }

    private enum class TypeFilter(val label: Int) {
        ALL(R.string.filter_all_types), ONE_TIME(R.string.filter_one_time), REPEATING(R.string.filter_repeating)
    }

    private lateinit var binding: ActivitySearchBinding
    private lateinit var repository: ModityRepository
    private lateinit var adapter: TaskListAdapter

    private var groups: List<TaskGroup> = emptyList()
    private var items: List<DayTask> = emptyList()

    private var query = ""
    private var sort = Sort.TIME
    private var groupFilter: Long? = null // null = all, UNGROUPED = no group, else group id
    private var statusFilter: OccurrenceStatus? = null
    private var typeFilter = TypeFilter.ALL
    private var dateFilter: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        repository = ModityRepository.get(this)

        adapter = TaskListAdapter(TaskActions(this) { load() }, showDate = true)
        binding.rvResults.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.etQuery.doAfterTextChanged {
            query = it?.toString()?.trim().orEmpty()
            render()
        }
        binding.chipSort.setOnClickListener { showSortMenu(it) }
        binding.chipGroup.setOnClickListener { showGroupMenu(it) }
        binding.chipStatus.setOnClickListener { showStatusMenu(it) }
        binding.chipType.setOnClickListener { showTypeMenu(it) }
        binding.chipDate.setOnClickListener { showDateMenu(it) }
        binding.etQuery.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    /** Without a date filter each task is shown with its most recent (or next) occurrence. */
    private fun load() {
        lifecycleScope.launch {
            groups = repository.groups()
            val date = dateFilter
            items = if (date != null) {
                repository.dayTasks(date)
            } else {
                val today = LocalDate.now()
                repository.tasks().mapNotNull { task ->
                    val day = task.lastOccurrenceOnOrBefore(today) ?: task.nextOccurrenceOnOrAfter(today) ?: return@mapNotNull null
                    DayTask(task, day, repository.statusOf(task, day)).takeIf { it.status != OccurrenceStatus.REMOVED }
                }
            }
            render()
        }
    }

    private fun render() {
        val groupNames = groups.associate { it.id to it.name }
        val filtered = items.filter { item ->
            val task = item.task
            (query.isEmpty() || task.title.contains(query, ignoreCase = true) || task.description.contains(query, ignoreCase = true)) &&
                when (groupFilter) {
                    null -> true
                    UNGROUPED -> task.groupId == null
                    else -> task.groupId == groupFilter
                } &&
                (statusFilter == null || item.status == statusFilter) &&
                when (typeFilter) {
                    TypeFilter.ALL -> true
                    TypeFilter.ONE_TIME -> !task.isRepeating
                    TypeFilter.REPEATING -> task.isRepeating
                }
        }
        val sorted = when (sort) {
            Sort.TIME -> filtered.sortedWith(compareBy({ it.date }, { it.task.anchorTime() ?: Int.MAX_VALUE }))
            Sort.DEADLINE -> filtered.sortedBy { it.task.deadline(it.date) }
            Sort.PRIORITY -> filtered.sortedWith(compareByDescending<DayTask> { it.task.priority.ordinal }.thenBy { it.date })
            Sort.NAME -> filtered.sortedBy { it.task.title.lowercase() }
        }
        adapter.submitList(sorted.map { TaskListItem.Row(it, it.task.groupId?.let(groupNames::get)) })
        binding.tvResultCount.text = resources.getQuantityString(R.plurals.search_result_count, sorted.size, sorted.size)
        renderChips()
    }

    private fun renderChips() = with(binding) {
        chipSort.text = getString(R.string.sort_by, getString(sort.label))
        chipGroup.text = when (groupFilter) {
            null -> getString(R.string.filter_all_groups)
            UNGROUPED -> getString(R.string.no_group)
            else -> groups.firstOrNull { it.id == groupFilter }?.name ?: getString(R.string.filter_all_groups)
        }
        chipStatus.text = statusFilter?.let { getString(Formatters.statusLabel(it)) } ?: getString(R.string.filter_all_statuses)
        chipType.text = getString(typeFilter.label)
        chipDate.text = dateFilter?.let { Formatters.relativeDay(this@SearchActivity, it) } ?: getString(R.string.filter_any_date)
        markActive(chipGroup, groupFilter != null)
        markActive(chipStatus, statusFilter != null)
        markActive(chipType, typeFilter != TypeFilter.ALL)
        markActive(chipDate, dateFilter != null)
    }

    private fun markActive(chip: Chip, active: Boolean) {
        chip.isCheckable = true
        chip.isChecked = active
    }

    private fun popup(anchor: View, labels: List<String>, onPick: (Int) -> Unit) {
        val menu = PopupMenu(this, anchor)
        labels.forEachIndexed { index, label -> menu.menu.add(0, index, index, label) }
        menu.setOnMenuItemClickListener {
            onPick(it.itemId)
            true
        }
        menu.setOnDismissListener { renderChips() }
        menu.show()
    }

    private fun showSortMenu(anchor: View) = popup(anchor, Sort.entries.map { getString(it.label) }) {
        sort = Sort.entries[it]
        render()
    }

    private fun showGroupMenu(anchor: View) {
        val labels = listOf(getString(R.string.filter_all_groups), getString(R.string.no_group)) + groups.map { it.name }
        popup(anchor, labels) { index ->
            groupFilter = when (index) {
                0 -> null
                1 -> UNGROUPED
                else -> groups[index - 2].id
            }
            render()
        }
    }

    private fun showStatusMenu(anchor: View) {
        val statuses = listOf(null, OccurrenceStatus.PENDING, OccurrenceStatus.COMPLETED, OccurrenceStatus.MISSED, OccurrenceStatus.SKIPPED)
        popup(anchor, statuses.map { status -> status?.let { getString(Formatters.statusLabel(it)) } ?: getString(R.string.filter_all_statuses) }) {
            statusFilter = statuses[it]
            render()
        }
    }

    private fun showTypeMenu(anchor: View) = popup(anchor, TypeFilter.entries.map { getString(it.label) }) {
        typeFilter = TypeFilter.entries[it]
        render()
    }

    private fun showDateMenu(anchor: View) {
        val labels = listOf(getString(R.string.filter_any_date), getString(R.string.today), getString(R.string.tomorrow), getString(R.string.pick_date))
        popup(anchor, labels) { index ->
            when (index) {
                0 -> setDate(null)
                1 -> setDate(LocalDate.now())
                2 -> setDate(LocalDate.now().plusDays(1))
                else -> Pickers.date(this, dateFilter ?: LocalDate.now(), ::setDate)
            }
        }
    }

    private fun setDate(date: LocalDate?) {
        dateFilter = date
        load()
    }

    private companion object {
        const val UNGROUPED = -1L
    }
}
