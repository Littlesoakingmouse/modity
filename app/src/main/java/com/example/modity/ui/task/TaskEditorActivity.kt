package com.example.modity.ui.task

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.data.UserPreferences
import com.example.modity.databinding.ActivityTaskEditorBinding
import com.example.modity.model.Priority
import com.example.modity.model.RepeatType
import com.example.modity.model.Task
import com.example.modity.model.TaskColors
import com.example.modity.model.TaskGroup
import com.example.modity.model.TaskIcon
import com.example.modity.notification.ReminderScheduler
import com.example.modity.ui.common.PickerAdapter
import com.example.modity.ui.common.Pickers
import com.example.modity.ui.group.GroupEditorActivity
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/** Creates a task, or edits one (optionally a single occurrence of a repeating task). */
class TaskEditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaskEditorBinding
    private lateinit var repository: ModityRepository

    private var original: Task? = null
    private var occurrenceDate: LocalDate? = null
    private var groups: List<TaskGroup> = emptyList()

    // Form state
    private lateinit var palette: List<Int>
    private var color = 0
    private var icon = TaskIcon.CHECK
    private var groupId: Long? = null
    private var repeatType = RepeatType.NONE
    private var repeatDays = 0
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate? = null
    private var startTime: Int? = null
    private var dueDate: LocalDate? = null
    private var dueTime: Int? = null

    private lateinit var colorAdapter: PickerAdapter
    private lateinit var iconAdapter: PickerAdapter

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) binding.switchReminder.isChecked = false
    }

    private val newGroupLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val newId = result.data?.getLongExtra(GroupEditorActivity.EXTRA_GROUP_ID, -1L) ?: -1L
        lifecycleScope.launch {
            loadGroups()
            if (newId > 0) groupId = newId
            renderGroup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaskEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        repository = ModityRepository.get(this)
        palette = TaskColors.palette(this)
        color = palette.first()

        intent.getLongExtra(EXTRA_DATE, -1L).takeIf { it >= 0 }?.let {
            occurrenceDate = LocalDate.ofEpochDay(it)
            startDate = LocalDate.ofEpochDay(it)
        }
        intent.getLongExtra(EXTRA_GROUP_ID, -1L).takeIf { it > 0 }?.let { groupId = it }

        setupPickers()
        setupChips()
        setupListeners()

        lifecycleScope.launch {
            loadGroups()
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
            if (taskId > 0) repository.task(taskId)?.let(::fillFrom)
            renderAll()
        }
    }

    private suspend fun loadGroups() {
        groups = repository.groups()
        val names = listOf(getString(R.string.no_group)) + groups.map { it.name } + getString(R.string.new_group_option)
        binding.actGroup.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, names))
    }

    private fun fillFrom(task: Task) {
        original = task
        binding.toolbar.setTitle(R.string.edit_task)
        binding.etTitle.setText(task.title)
        binding.etDescription.setText(task.description)
        color = task.color
        icon = TaskIcon.from(task.icon)
        groupId = task.groupId
        repeatType = task.repeatType
        repeatDays = task.repeatDays
        startDate = LocalDate.ofEpochDay(task.startDate)
        endDate = task.endDate?.let(LocalDate::ofEpochDay)
        startTime = task.startTime
        dueDate = task.dueDate?.let(LocalDate::ofEpochDay)
        dueTime = task.dueTime
        check(binding.chipGroupPriority, task.priority.ordinal)
        binding.switchReminder.isChecked = task.reminderMinutes != null
        task.reminderMinutes?.let { check(binding.chipGroupReminder, REMINDER_OPTIONS.indexOf(it).coerceAtLeast(0)) }
        binding.switchFocus.isChecked = task.focusEnabled
        check(binding.chipGroupFocus, FOCUS_OPTIONS.indexOf(task.focusMinutes).coerceAtLeast(1))
    }

    private fun setupPickers() {
        colorAdapter = PickerAdapter.colors(palette) {
            color = it
            iconAdapter.notifyItemChanged(iconAdapter.selected)
        }
        iconAdapter = PickerAdapter.icons({ color }) { icon = it }
        binding.rvColors.adapter = colorAdapter
        binding.rvIcons.adapter = iconAdapter
    }

    private fun setupChips() = with(binding) {
        DayOfWeek.entries.forEach { day ->
            chipGroupDays.addView(chip(Formatters.weekdayNarrow(this@TaskEditorActivity, day)).apply {
                tag = day
                setOnCheckedChangeListener { _, checked ->
                    repeatDays = if (checked) repeatDays or Task.dayBit(day) else repeatDays and Task.dayBit(day).inv()
                }
            })
        }
        Priority.entries.forEach { chipGroupPriority.addView(chip(getString(Formatters.priorityLabel(it)))) }
        REMINDER_OPTIONS.forEach { chipGroupReminder.addView(chip(Formatters.reminderLabel(this@TaskEditorActivity, it))) }
        FOCUS_OPTIONS.forEach { chipGroupFocus.addView(chip(getString(R.string.minutes_short, it))) }
        check(chipGroupPriority, 0)
        check(chipGroupReminder, REMINDER_OPTIONS.indexOf(DEFAULT_REMINDER))
        val defaultFocus = UserPreferences(this@TaskEditorActivity).defaultFocusMinutes
        check(chipGroupFocus, FOCUS_OPTIONS.indexOf(defaultFocus).coerceAtLeast(1))
    }

    private fun chip(text: String) = Chip(this).apply {
        id = android.view.View.generateViewId()
        this.text = text
        isCheckable = true
    }

    private fun check(group: ChipGroup, index: Int) {
        group.getChildAt(index)?.let { group.check(it.id) }
    }

    private fun checkedIndex(group: ChipGroup): Int =
        (0 until group.childCount).firstOrNull { (group.getChildAt(it) as Chip).isChecked } ?: 0

    private fun setupListeners() = with(binding) {
        toolbar.setNavigationOnClickListener { finish() }
        btnSave.setOnClickListener { save() }

        actGroup.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> groupId = null
                groups.size + 1 -> {
                    renderGroup()
                    newGroupLauncher.launch(Intent(this@TaskEditorActivity, GroupEditorActivity::class.java))
                }
                else -> groupId = groups[position - 1].id
            }
        }

        toggleRepeat.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            repeatType = when (checkedId) {
                R.id.btn_repeat_daily -> RepeatType.DAILY
                R.id.btn_repeat_weekly -> RepeatType.WEEKLY
                else -> RepeatType.NONE
            }
            if (repeatType == RepeatType.WEEKLY && repeatDays == 0) repeatDays = Task.dayBit(startDate.dayOfWeek)
            if (repeatType != RepeatType.NONE) dueDate = null
            renderSchedule()
        }

        etDate.setOnClickListener {
            Pickers.date(this@TaskEditorActivity, startDate) {
                startDate = it
                renderSchedule()
            }
        }
        etTime.setOnClickListener {
            Pickers.time(this@TaskEditorActivity, startTime) {
                startTime = it
                renderSchedule()
            }
        }
        tilTime.setEndIconOnClickListener {
            startTime = null
            renderSchedule()
        }
        etEndDate.setOnClickListener {
            Pickers.date(this@TaskEditorActivity, endDate ?: startDate.plusMonths(1)) {
                endDate = it
                renderSchedule()
            }
        }
        tilEndDate.setEndIconOnClickListener {
            endDate = null
            renderSchedule()
        }
        etDeadline.setOnClickListener { pickDeadline() }
        tilDeadline.setEndIconOnClickListener {
            dueDate = null
            dueTime = null
            renderSchedule()
        }

        switchReminder.setOnCheckedChangeListener { _, checked ->
            chipGroupReminder.isVisible = checked
            if (checked) requestNotificationPermission()
        }
        switchFocus.setOnCheckedChangeListener { _, checked -> chipGroupFocus.isVisible = checked }
    }

    private fun pickDeadline() {
        if (repeatType == RepeatType.NONE) {
            Pickers.date(this, dueDate ?: startDate) { date ->
                dueDate = date
                Pickers.time(this, dueTime ?: startTime) { time ->
                    dueTime = time
                    renderSchedule()
                }
                renderSchedule()
            }
        } else {
            Pickers.time(this, dueTime ?: startTime) {
                dueTime = it
                renderSchedule()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun renderAll() {
        colorAdapter.selected = palette.indexOf(color).coerceAtLeast(0)
        iconAdapter.selected = icon.ordinal
        binding.toggleRepeat.check(
            when (repeatType) {
                RepeatType.NONE -> R.id.btn_repeat_none
                RepeatType.DAILY -> R.id.btn_repeat_daily
                RepeatType.WEEKLY -> R.id.btn_repeat_weekly
            },
        )
        renderGroup()
        renderSchedule()
        binding.chipGroupReminder.isVisible = binding.switchReminder.isChecked
        binding.chipGroupFocus.isVisible = binding.switchFocus.isChecked
    }

    private fun renderGroup() {
        val name = groups.firstOrNull { it.id == groupId }?.name ?: getString(R.string.no_group)
        binding.actGroup.setText(name, false)
    }

    private fun renderSchedule() = with(binding) {
        val repeating = repeatType != RepeatType.NONE
        chipGroupDays.isVisible = repeatType == RepeatType.WEEKLY
        for (i in 0 until chipGroupDays.childCount) {
            val chip = chipGroupDays.getChildAt(i) as Chip
            chip.isChecked = repeatDays and Task.dayBit(chip.tag as DayOfWeek) != 0
        }
        tilDate.hint = getString(if (repeating) R.string.field_start_date else R.string.field_date)
        etDate.setText(Formatters.dateMedium(this@TaskEditorActivity, startDate))
        etTime.setText(startTime?.let { Formatters.time(this@TaskEditorActivity, it) }.orEmpty())
        tilTime.isEndIconVisible = startTime != null

        tilEndDate.isVisible = repeating
        etEndDate.setText(endDate?.let { Formatters.dateMedium(this@TaskEditorActivity, it) } ?: getString(R.string.never))
        tilEndDate.isEndIconVisible = endDate != null

        val deadline = buildList {
            if (!repeating) dueDate?.let { add(Formatters.dateMedium(this@TaskEditorActivity, it)) }
            dueTime?.let { add(Formatters.time(this@TaskEditorActivity, it)) }
        }.joinToString("  ")
        etDeadline.setText(deadline.ifEmpty { getString(R.string.none) })
        tilDeadline.isEndIconVisible = deadline.isNotEmpty()
        tilDeadline.helperText = getString(if (repeating) R.string.deadline_helper_repeating else R.string.deadline_helper)
    }

    private fun buildTask(): Task? {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        binding.tilTitle.error = null
        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.error_title_required)
            return null
        }
        if (repeatType == RepeatType.WEEKLY && repeatDays == 0) {
            toast(R.string.error_days_required)
            return null
        }
        if (repeatType != RepeatType.NONE && endDate != null && endDate!!.isBefore(startDate)) {
            toast(R.string.error_end_before_start)
            return null
        }
        if (repeatType == RepeatType.NONE && dueDate != null && dueDate!!.isBefore(startDate)) {
            toast(R.string.error_deadline_before_start)
            return null
        }
        val base = original ?: Task(title = title, color = color, startDate = startDate.toEpochDay())
        return base.copy(
            title = title,
            description = binding.etDescription.text?.toString()?.trim().orEmpty(),
            color = color,
            icon = icon.name,
            groupId = groupId,
            priority = Priority.entries[checkedIndex(binding.chipGroupPriority)],
            repeatType = repeatType,
            repeatDays = when (repeatType) {
                RepeatType.NONE -> 0
                RepeatType.DAILY -> Task.ALL_DAYS
                RepeatType.WEEKLY -> repeatDays
            },
            startDate = startDate.toEpochDay(),
            endDate = if (repeatType == RepeatType.NONE) null else endDate?.toEpochDay(),
            startTime = startTime,
            dueDate = if (repeatType == RepeatType.NONE) dueDate?.toEpochDay() else null,
            dueTime = dueTime,
            reminderMinutes = if (binding.switchReminder.isChecked) REMINDER_OPTIONS[checkedIndex(binding.chipGroupReminder)] else null,
            focusEnabled = binding.switchFocus.isChecked,
            focusMinutes = FOCUS_OPTIONS[checkedIndex(binding.chipGroupFocus)],
        )
    }

    private fun save() {
        val task = buildTask() ?: return
        val previous = original
        val date = occurrenceDate
        if (previous != null && previous.isRepeating && date != null) {
            val choices = arrayOf(getString(R.string.scope_this_occurrence), getString(R.string.scope_all_occurrences))
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.edit_repeating_title)
                .setItems(choices) { _, which ->
                    if (which == 0) persistSingle(previous, date, task) else persist(task)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            persist(task)
        }
    }

    private fun persist(task: Task) {
        lifecycleScope.launch {
            val id = repository.saveTask(task)
            ReminderScheduler.schedule(this@TaskEditorActivity, task.copy(id = id))
            finish()
        }
    }

    /** "Only this occurrence": the edited copy becomes a one-time task on that day. */
    private fun persistSingle(original: Task, date: LocalDate, edited: Task) {
        lifecycleScope.launch {
            val single = edited.copy(startDate = date.toEpochDay())
            val id = repository.detachOccurrence(original, date, single)
            ReminderScheduler.schedule(this@TaskEditorActivity, single.copy(id = id, repeatType = RepeatType.NONE, repeatDays = 0))
            finish()
        }
    }

    private fun toast(message: Int) = android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_DATE = "date"
        private const val EXTRA_GROUP_ID = "group_id"
        private const val DEFAULT_REMINDER = 10
        private val REMINDER_OPTIONS = listOf(0, 5, 10, 30, 60)
        private val FOCUS_OPTIONS = listOf(15, 25, 45, 60, 90)

        fun createIntent(context: Context, date: LocalDate, groupId: Long? = null): Intent =
            Intent(context, TaskEditorActivity::class.java)
                .putExtra(EXTRA_DATE, date.toEpochDay())
                .putExtra(EXTRA_GROUP_ID, groupId ?: -1L)

        fun editIntent(context: Context, taskId: Long, date: LocalDate?): Intent =
            Intent(context, TaskEditorActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_DATE, date?.toEpochDay() ?: -1L)
    }
}
