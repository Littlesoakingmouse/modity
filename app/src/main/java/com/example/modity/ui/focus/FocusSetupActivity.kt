package com.example.modity.ui.focus

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.data.Stats
import com.example.modity.data.UserPreferences
import com.example.modity.databinding.ActivityFocusSetupBinding
import com.example.modity.model.Task
import com.example.modity.ui.common.BottomNav
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Choose a task and a duration, then start a focus session. */
class FocusSetupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFocusSetupBinding
    private lateinit var repository: ModityRepository
    private lateinit var preferences: UserPreferences

    private var tasks: List<Task> = emptyList()
    private var selectedTask: Task? = null
    private var minutes = 25
    private var occurrenceDate: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFocusSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets(bottom = false)
        repository = ModityRepository.get(this)
        preferences = UserPreferences(this)
        minutes = preferences.defaultFocusMinutes
        intent.getLongExtra(EXTRA_DATE, -1L).takeIf { it >= 0 }?.let { occurrenceDate = LocalDate.ofEpochDay(it) }

        DURATIONS.forEach { value ->
            binding.chipGroupDuration.addView(Chip(this).apply {
                id = View.generateViewId()
                tag = value
                text = getString(R.string.minutes_short, value)
                isCheckable = true
            })
        }
        binding.chipGroupDuration.setOnCheckedStateChangeListener { group, checkedIds ->
            checkedIds.firstOrNull()?.let { minutes = group.findViewById<Chip>(it).tag as Int }
            renderDuration()
        }

        binding.actTask.setOnItemClickListener { _, _, position, _ ->
            selectedTask = tasks.getOrNull(position - 1)
            selectedTask?.let { selectDuration(it.focusMinutes) }
        }
        binding.switchDnd.isChecked = preferences.focusDoNotDisturb
        binding.switchDnd.setOnCheckedChangeListener { _, checked ->
            preferences.focusDoNotDisturb = checked
            if (checked && !hasDndAccess()) {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        }
        binding.btnStart.setOnClickListener { start() }

        selectDuration(minutes)
        lifecycleScope.launch { loadTasks(intent.getLongExtra(EXTRA_TASK_ID, -1L)) }
    }

    override fun onResume() {
        super.onResume()
        BottomNav.setup(this, binding.bottomNavigation, R.id.nav_focus)
        loadTodaySummary()
    }

    private suspend fun loadTasks(preselectId: Long) {
        // Focus-enabled tasks of the day first, then everything else.
        val today = repository.dayTasks(occurrenceDate).map { it.task }
        val all = repository.tasks()
        tasks = (today.sortedByDescending { it.focusEnabled } + all).distinctBy { it.id }
        val labels = listOf(getString(R.string.focus_no_task)) + tasks.map { it.title }
        binding.actTask.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, labels))
        selectedTask = tasks.firstOrNull { it.id == preselectId }
        binding.actTask.setText(selectedTask?.title ?: labels.first(), false)
        selectedTask?.let { selectDuration(it.focusMinutes) }
    }

    private fun loadTodaySummary() {
        lifecycleScope.launch {
            val zone = ZoneId.systemDefault()
            val from = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
            val summary = Stats.focus(repository.focusSessions(from, System.currentTimeMillis()))
            binding.tvFocusToday.text = resources.getQuantityString(
                R.plurals.focus_today_summary,
                summary.sessions,
                summary.sessions,
                Formatters.duration(this@FocusSetupActivity, summary.totalSeconds),
            )
        }
    }

    private fun selectDuration(value: Int) {
        minutes = value
        val group = binding.chipGroupDuration
        val chip = (0 until group.childCount).map { group.getChildAt(it) as Chip }.firstOrNull { it.tag == value }
        if (chip != null) group.check(chip.id) else renderDuration()
    }

    private fun renderDuration() {
        binding.tvDurationPreview.text = String.format(Locale.US, "%02d:00", minutes)
    }

    private fun hasDndAccess(): Boolean =
        getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    private fun start() {
        val task = selectedTask
        startActivity(
            FocusTimerActivity.intent(
                context = this,
                taskId = task?.id,
                title = task?.title ?: getString(R.string.focus_no_task),
                minutes = minutes,
                date = occurrenceDate,
                doNotDisturb = binding.switchDnd.isChecked && hasDndAccess(),
            ),
        )
    }

    companion object {
        private const val EXTRA_TASK_ID = "task_id"
        private const val EXTRA_DATE = "date"
        private val DURATIONS = listOf(15, 25, 45, 60, 90)

        fun intent(context: Context, taskId: Long, date: LocalDate): Intent =
            Intent(context, FocusSetupActivity::class.java)
                .putExtra(EXTRA_TASK_ID, taskId)
                .putExtra(EXTRA_DATE, date.toEpochDay())
    }
}
