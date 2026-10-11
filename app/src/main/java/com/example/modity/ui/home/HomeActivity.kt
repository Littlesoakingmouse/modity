package com.example.modity.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.modity.R
import com.example.modity.data.HomeStyle
import com.example.modity.data.ModityRepository
import com.example.modity.data.UserPreferences
import com.example.modity.databinding.ActivityHomeBinding
import com.example.modity.model.GroupProgress
import com.example.modity.ui.common.BottomNav
import com.example.modity.ui.common.TaskActions
import com.example.modity.ui.group.GroupDetailActivity
import com.example.modity.ui.settings.SettingsActivity
import com.example.modity.ui.task.SearchActivity
import com.example.modity.ui.task.TaskEditorActivity
import com.example.modity.utils.Formatters
import com.example.modity.utils.applySystemBarInsets
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var repository: ModityRepository
    private lateinit var preferences: UserPreferences
    private lateinit var actions: TaskActions

    private lateinit var cardAdapter: GroupCardAdapter
    private lateinit var gridAdapter: GroupGridAdapter
    private lateinit var timelineAdapter: TimelineAdapter
    private val weekAdapter = WeekStripAdapter { selectDate(it) }

    private var selectedDate: LocalDate = LocalDate.now()
    private var entries: List<GroupProgress> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets(bottom = false)

        repository = ModityRepository.get(this)
        preferences = UserPreferences(this)
        actions = TaskActions(this) { load() }
        savedInstanceState?.getLong(STATE_DATE)?.let { selectedDate = LocalDate.ofEpochDay(it) }

        val openGroup = { group: com.example.modity.model.TaskGroup ->
            startActivity(GroupDetailActivity.intent(this, group.id, selectedDate))
        }
        cardAdapter = GroupCardAdapter(actions, openGroup)
        gridAdapter = GroupGridAdapter(actions, openGroup)
        timelineAdapter = TimelineAdapter(actions)

        binding.rvWeek.adapter = weekAdapter
        setupListeners()
        applyStyle(preferences.homeStyle)
        updateWeekStrip()
    }

    override fun onResume() {
        super.onResume()
        BottomNav.setup(this, binding.bottomNavigation, R.id.nav_today)
        binding.toggleHomeStyle.check(styleButton(preferences.homeStyle)) // may have changed in Settings
        load()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, selectedDate.toEpochDay())
    }

    private fun setupListeners() = with(binding) {
        btnSettings.setOnClickListener { startActivity(Intent(this@HomeActivity, SettingsActivity::class.java)) }
        btnSearch.setOnClickListener { startActivity(Intent(this@HomeActivity, SearchActivity::class.java)) }
        btnAddTask.setOnClickListener {
            startActivity(TaskEditorActivity.createIntent(this@HomeActivity, selectedDate))
        }
        tvHomeDate.setOnClickListener { showDatePicker() }
        tvHomeTitle.setOnClickListener { selectDate(LocalDate.now()) }

        toggleHomeStyle.check(styleButton(preferences.homeStyle))
        toggleHomeStyle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val style = when (checkedId) {
                R.id.btn_style_grid -> HomeStyle.GRID
                R.id.btn_style_timeline -> HomeStyle.TIMELINE
                else -> HomeStyle.CARD
            }
            // Only the presentation changes; task data and statuses are untouched.
            preferences.homeStyle = style
            applyStyle(style)
            render()
        }
    }

    private fun styleButton(style: HomeStyle) = when (style) {
        HomeStyle.CARD -> R.id.btn_style_card
        HomeStyle.GRID -> R.id.btn_style_grid
        HomeStyle.TIMELINE -> R.id.btn_style_timeline
    }

    private fun applyStyle(style: HomeStyle) = with(binding.rvHome) {
        when (style) {
            HomeStyle.CARD -> {
                layoutManager = LinearLayoutManager(this@HomeActivity)
                adapter = cardAdapter
            }
            HomeStyle.GRID -> {
                layoutManager = GridLayoutManager(this@HomeActivity, GRID_COLUMNS)
                adapter = gridAdapter
            }
            HomeStyle.TIMELINE -> {
                layoutManager = LinearLayoutManager(this@HomeActivity)
                adapter = timelineAdapter
            }
        }
    }

    private fun selectDate(date: LocalDate) {
        selectedDate = date
        updateWeekStrip()
        load()
    }

    private fun updateWeekStrip() {
        val first = selectedDate.minusDays(WEEK_STRIP_DAYS_BEFORE)
        val dates = (0 until WEEK_STRIP_SIZE).map { first.plusDays(it.toLong()) }
        weekAdapter.selectedDate = selectedDate
        weekAdapter.submitList(dates) {
            (binding.rvWeek.layoutManager as LinearLayoutManager)
                .scrollToPositionWithOffset(WEEK_STRIP_DAYS_BEFORE.toInt() - 3, 0)
        }
    }

    private fun load() {
        lifecycleScope.launch {
            entries = repository.homeGroups(selectedDate)
            render()
        }
    }

    private fun render() = with(binding) {
        val today = LocalDate.now()
        tvHomeDate.text = Formatters.dayLong(this@HomeActivity, selectedDate)
        tvHomeTitle.text = when (selectedDate) {
            today -> getString(R.string.today)
            today.plusDays(1) -> getString(R.string.tomorrow)
            today.minusDays(1) -> getString(R.string.yesterday)
            else -> Formatters.dayShort(this@HomeActivity, selectedDate)
        }

        val total = entries.sumOf { it.total }
        val done = entries.sumOf { it.done }
        val percent = if (total == 0) 0 else done * 100 / total
        tvHomeSummary.text = if (total == 0) getString(R.string.home_summary_empty) else getString(R.string.home_summary, done, total)
        tvHomePercent.text = getString(R.string.percent, percent)
        tvHomePercent.isVisible = total > 0
        progressDay.setProgressCompat(percent, true)

        emptyState.isVisible = entries.isEmpty()
        rvHome.isVisible = entries.isNotEmpty()
        cardAdapter.submitList(entries)
        gridAdapter.submitList(entries)
        timelineAdapter.submitList(TimelineAdapter.build(this@HomeActivity, entries))
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(selectedDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectDate(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        }
        picker.show(supportFragmentManager, "home_date")
    }

    private companion object {
        const val STATE_DATE = "selected_date"
        const val GRID_COLUMNS = 2
        const val WEEK_STRIP_DAYS_BEFORE = 14L
        const val WEEK_STRIP_SIZE = 45
    }
}
