package com.example.modity.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.AppLanguage
import com.example.modity.data.HomeStyle
import com.example.modity.data.ModityRepository
import com.example.modity.data.SampleData
import com.example.modity.data.ThemeMode
import com.example.modity.data.UserPreferences
import com.example.modity.databinding.ActivitySettingsBinding
import com.example.modity.databinding.ViewSettingRowBinding
import com.example.modity.notification.ReminderScheduler
import com.example.modity.ui.group.GroupsActivity
import com.example.modity.utils.applySystemBarInsets
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** User preferences (SharedPreferences) and data tools. */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var preferences: UserPreferences
    private lateinit var repository: ModityRepository

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            binding.switchReminders.isChecked = false
            Toast.makeText(this, R.string.permission_notifications_denied, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        preferences = UserPreferences(this)
        repository = ModityRepository.get(this)

        binding.toolbar.setNavigationOnClickListener { finish() }
        setupRows()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun setupRows() = with(binding) {
        rowTheme.root.setOnClickListener {
            choose(R.string.settings_theme, ThemeMode.entries.map { getString(themeLabel(it)) }, preferences.themeMode.ordinal) {
                preferences.themeMode = ThemeMode.entries[it]
                preferences.applyTheme()
            }
        }
        rowLanguage.root.setOnClickListener {
            choose(R.string.settings_language, AppLanguage.entries.map { getString(languageLabel(it)) }, preferences.language.ordinal) {
                preferences.language = AppLanguage.entries[it]
                preferences.applyLanguage()
            }
        }
        rowHomeStyle.root.setOnClickListener {
            choose(R.string.settings_home_style, HomeStyle.entries.map { getString(styleLabel(it)) }, preferences.homeStyle.ordinal) {
                preferences.homeStyle = HomeStyle.entries[it]
            }
        }
        rowFocusDuration.root.setOnClickListener {
            val options = listOf(15, 25, 45, 60, 90)
            choose(
                R.string.settings_focus_duration,
                options.map { getString(R.string.minutes_short, it) },
                options.indexOf(preferences.defaultFocusMinutes),
            ) { preferences.defaultFocusMinutes = options[it] }
        }

        switchReminders.setOnCheckedChangeListener { _, checked ->
            if (checked == preferences.remindersEnabled) return@setOnCheckedChangeListener
            preferences.remindersEnabled = checked
            if (checked) requestNotificationPermission()
            lifecycleScope.launch { ReminderScheduler.rescheduleAll(this@SettingsActivity) }
        }
        rowExactAlarm.root.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:$packageName".toUri()))
            }
        }

        rowGroups.root.setOnClickListener { startActivity(Intent(this@SettingsActivity, GroupsActivity::class.java)) }
        rowSampleData.root.setOnClickListener {
            MaterialAlertDialogBuilder(this@SettingsActivity)
                .setTitle(R.string.settings_sample_data)
                .setMessage(R.string.sample_data_message)
                .setPositiveButton(R.string.add) { _, _ ->
                    lifecycleScope.launch {
                        SampleData.insert(this@SettingsActivity, repository)
                        ReminderScheduler.rescheduleAll(this@SettingsActivity)
                        Toast.makeText(this@SettingsActivity, R.string.sample_data_added, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
        rowClearData.root.setOnClickListener {
            MaterialAlertDialogBuilder(this@SettingsActivity)
                .setTitle(R.string.settings_clear_data)
                .setMessage(R.string.clear_data_message)
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    lifecycleScope.launch {
                        repository.tasks().forEach { ReminderScheduler.cancel(this@SettingsActivity, it.id) }
                        repository.clearAll()
                        Toast.makeText(this@SettingsActivity, R.string.data_cleared, Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun render() = with(binding) {
        bind(rowTheme, R.drawable.ic_task_moon, R.string.settings_theme, getString(themeLabel(preferences.themeMode)))
        bind(rowLanguage, R.drawable.ic_notes, R.string.settings_language, getString(languageLabel(preferences.language)))
        bind(rowHomeStyle, R.drawable.ic_view_card, R.string.settings_home_style, getString(styleLabel(preferences.homeStyle)))
        bind(rowFocusDuration, R.drawable.ic_timer, R.string.settings_focus_duration, getString(R.string.minutes_short, preferences.defaultFocusMinutes))
        bind(rowGroups, R.drawable.ic_folder, R.string.manage_groups, getString(R.string.manage_groups_desc))
        bind(rowSampleData, R.drawable.ic_add, R.string.settings_sample_data, getString(R.string.settings_sample_data_desc))
        bind(rowClearData, R.drawable.ic_delete, R.string.settings_clear_data, getString(R.string.settings_clear_data_desc))
        bind(rowVersion, R.drawable.ic_task_star, R.string.settings_version, packageManager.getPackageInfo(packageName, 0).versionName.orEmpty())

        switchReminders.isChecked = preferences.remindersEnabled && notificationsAllowed()
        val needsExactAlarm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        rowExactAlarm.root.isVisible = needsExactAlarm
        bind(rowExactAlarm, R.drawable.ic_bell, R.string.settings_exact_alarm, getString(R.string.settings_exact_alarm_desc))
    }

    private fun bind(row: ViewSettingRowBinding, @DrawableRes icon: Int, title: Int, value: String) {
        row.ivSettingIcon.setImageResource(icon)
        row.tvSettingTitle.setText(title)
        row.tvSettingValue.text = value
    }

    private fun choose(title: Int, options: List<String>, selected: Int, onChosen: (Int) -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setSingleChoiceItems(options.toTypedArray(), selected) { dialog, which ->
                onChosen(which)
                dialog.dismiss()
                render()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermission() {
        if (!notificationsAllowed()) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun themeLabel(mode: ThemeMode) = when (mode) {
        ThemeMode.SYSTEM -> R.string.theme_system
        ThemeMode.LIGHT -> R.string.theme_light
        ThemeMode.DARK -> R.string.theme_dark
    }

    private fun languageLabel(language: AppLanguage) = when (language) {
        AppLanguage.SYSTEM -> R.string.language_system
        AppLanguage.ENGLISH -> R.string.language_english
        AppLanguage.VIETNAMESE -> R.string.language_vietnamese
    }

    private fun styleLabel(style: HomeStyle) = when (style) {
        HomeStyle.CARD -> R.string.style_card
        HomeStyle.GRID -> R.string.style_grid
        HomeStyle.TIMELINE -> R.string.style_timeline
    }
}
