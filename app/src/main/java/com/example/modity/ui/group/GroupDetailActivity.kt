package com.example.modity.ui.group

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.databinding.ActivityGroupDetailBinding
import com.example.modity.model.DayTask
import com.example.modity.model.GroupProgress
import com.example.modity.model.OccurrenceStatus
import com.example.modity.model.TaskGroup
import com.example.modity.ui.common.TaskActions
import com.example.modity.ui.common.TaskListAdapter
import com.example.modity.ui.common.TaskListItem
import com.example.modity.ui.common.bindBadge
import com.example.modity.ui.task.TaskEditorActivity
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate

/** A group's progress for one day plus every task in the group. */
class GroupDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGroupDetailBinding
    private lateinit var repository: ModityRepository
    private lateinit var adapter: TaskListAdapter

    private var groupId = -1L
    private var date: LocalDate = LocalDate.now()
    private var group: TaskGroup? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGroupDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        repository = ModityRepository.get(this)

        groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1L)
        date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
        adapter = TaskListAdapter(TaskActions(this) { load() })
        binding.rvTasks.adapter = adapter

        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> startActivity(GroupEditorActivity.editIntent(this, groupId))
                R.id.action_delete -> group?.let(::confirmDelete)
            }
            true
        }
        binding.btnAddTask.setOnClickListener {
            startActivity(TaskEditorActivity.createIntent(this, date, groupId))
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            val loaded = repository.group(groupId)
            if (loaded == null) {
                finish()
                return@launch
            }
            group = loaded
            val today = repository.dayTasks(date).filter { it.task.groupId == groupId }
            val todayIds = today.map { it.task.id }.toSet()
            val others = repository.tasks()
                .filter { it.groupId == groupId && it.id !in todayIds }
                .mapNotNull { task ->
                    val day = task.nextOccurrenceOnOrAfter(date) ?: task.lastOccurrenceOnOrBefore(date) ?: return@mapNotNull null
                    DayTask(task, day, repository.statusOf(task, day)).takeIf { it.status != OccurrenceStatus.REMOVED }
                }
            render(loaded, GroupProgress(loaded, today), others)
        }
    }

    private fun render(group: TaskGroup, progress: GroupProgress, others: List<DayTask>) = with(binding) {
        val context = this@GroupDetailActivity
        ivGroupIcon.bindBadge(group.icon, group.color)
        tvGroupName.text = group.name
        tvGroupDescription.text = group.description
        tvGroupDescription.isVisible = group.description.isNotBlank()
        tvGroupProgress.text = getString(
            R.string.group_progress_line,
            Formatters.relativeDay(context, date),
            progress.done,
            progress.total,
            progress.percent,
        )
        progressGroup.setIndicatorColor(group.color)
        progressGroup.setProgressCompat(progress.percent, true)

        val items = buildList {
            if (progress.tasks.isNotEmpty()) {
                add(TaskListItem.Header(Formatters.relativeDay(context, date)))
                progress.tasks.forEach { add(TaskListItem.Row(it)) }
            }
            if (others.isNotEmpty()) {
                add(TaskListItem.Header(getString(R.string.group_other_tasks)))
                others.forEach { add(TaskListItem.Row(it)) }
            }
            if (isEmpty()) add(TaskListItem.Header(getString(R.string.group_empty)))
        }
        adapter.submitList(items)
    }

    /** Lets the user keep the group's tasks (as ungrouped tasks) or delete them along with the group. */
    private fun confirmDelete(group: TaskGroup) {
        val choices = arrayOf(getString(R.string.delete_group_keep_tasks), getString(R.string.delete_group_delete_tasks))
        var choice = 0
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_group_title, group.name))
            .setSingleChoiceItems(choices, choice) { _, which -> choice = which }
            .setPositiveButton(R.string.action_delete) { _, _ ->
                lifecycleScope.launch {
                    repository.deleteGroup(group, deleteTasks = choice == 1)
                    finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        private const val EXTRA_GROUP_ID = "group_id"
        private const val EXTRA_DATE = "date"

        fun intent(context: Context, groupId: Long, date: LocalDate): Intent =
            Intent(context, GroupDetailActivity::class.java)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .putExtra(EXTRA_DATE, date.toEpochDay())
    }
}
