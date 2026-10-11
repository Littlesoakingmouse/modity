package com.example.modity.utils

import android.content.Context
import android.text.format.DateFormat
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.os.ConfigurationCompat
import com.example.modity.R
import com.example.modity.model.OccurrenceStatus
import com.example.modity.model.Priority
import com.example.modity.model.RepeatType
import com.example.modity.model.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

object Formatters {

    fun locale(context: Context): Locale =
        ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.getDefault()

    private fun pattern(context: Context, skeleton: String): DateTimeFormatter {
        val locale = locale(context)
        return DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
    }

    fun time(context: Context, minutes: Int): String {
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        return LocalTime.of(minutes / 60, minutes % 60).format(pattern(context, skeleton))
    }

    /** "Saturday, 11 October" */
    fun dayLong(context: Context, date: LocalDate): String = date.format(pattern(context, "EEEEdMMMM"))

    /** "Sat, 11 Oct" */
    fun dayShort(context: Context, date: LocalDate): String = date.format(pattern(context, "EEEdMMM"))

    /** "11 Oct 2026" */
    fun dateMedium(context: Context, date: LocalDate): String = date.format(pattern(context, "dMMMyyyy"))

    fun monthYear(context: Context, date: LocalDate): String = date.format(pattern(context, "MMMMyyyy"))

    fun weekdayNarrow(context: Context, day: DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, locale(context))

    /** "Today", "Tomorrow", "Yesterday" or a short date. */
    fun relativeDay(context: Context, date: LocalDate): String {
        val today = LocalDate.now()
        return when (date) {
            today -> context.getString(R.string.today)
            today.plusDays(1) -> context.getString(R.string.tomorrow)
            today.minusDays(1) -> context.getString(R.string.yesterday)
            else -> dayShort(context, date)
        }
    }

    fun duration(context: Context, seconds: Long): String {
        val totalMinutes = seconds / 60
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            seconds in 1 until 60 -> context.getString(R.string.duration_seconds, seconds)
            hours > 0 && minutes > 0 -> context.getString(R.string.duration_hours_minutes, hours, minutes)
            hours > 0 -> context.getString(R.string.duration_hours, hours)
            else -> context.getString(R.string.duration_minutes, minutes)
        }
    }

    fun repeatSummary(context: Context, task: Task): String = when (task.repeatType) {
        RepeatType.NONE -> context.getString(R.string.repeat_once_on, dayShort(context, LocalDate.ofEpochDay(task.startDate)))
        RepeatType.DAILY -> context.getString(R.string.repeat_every_day)
        RepeatType.WEEKLY -> if (task.repeatDays == Task.ALL_DAYS) {
            context.getString(R.string.repeat_every_day)
        } else {
            DayOfWeek.entries.filter { task.repeatsOn(it) }.joinToString(", ") { weekdayNarrow(context, it) }
        }
    }

    /** Short secondary line for task rows: time, deadline and repeat hints. */
    fun taskMeta(context: Context, task: Task, groupName: String? = null): String = buildList {
        task.startTime?.let { add(time(context, it)) }
        if (task.dueTime != null || task.dueDate != null) {
            val due = buildString {
                if (task.dueDate != null && task.dueDate != task.startDate) {
                    append(dayShort(context, LocalDate.ofEpochDay(task.dueDate)))
                    if (task.dueTime != null) append(' ')
                }
                task.dueTime?.let { append(time(context, it)) }
            }
            add(context.getString(R.string.meta_due, due))
        }
        groupName?.let { add(it) }
        if (task.isRepeating) add(repeatSummary(context, task))
        if (task.focusEnabled) add(context.getString(R.string.meta_focus, task.focusMinutes))
    }.joinToString("  ·  ")

    @StringRes
    fun statusLabel(status: OccurrenceStatus): Int = when (status) {
        OccurrenceStatus.PENDING -> R.string.status_pending
        OccurrenceStatus.COMPLETED -> R.string.status_completed
        OccurrenceStatus.MISSED -> R.string.status_missed
        OccurrenceStatus.SKIPPED -> R.string.status_skipped
        OccurrenceStatus.REMOVED -> R.string.status_removed
    }

    @ColorRes
    fun statusColor(status: OccurrenceStatus): Int = when (status) {
        OccurrenceStatus.COMPLETED -> R.color.status_completed
        OccurrenceStatus.MISSED -> R.color.status_missed
        OccurrenceStatus.SKIPPED, OccurrenceStatus.REMOVED -> R.color.status_skipped
        OccurrenceStatus.PENDING -> R.color.status_pending
    }

    @StringRes
    fun priorityLabel(priority: Priority): Int = when (priority) {
        Priority.NONE -> R.string.priority_none
        Priority.LOW -> R.string.priority_low
        Priority.MEDIUM -> R.string.priority_medium
        Priority.HIGH -> R.string.priority_high
    }

    fun reminderLabel(context: Context, minutes: Int?): String = when (minutes) {
        null -> context.getString(R.string.reminder_off)
        0 -> context.getString(R.string.reminder_at_time)
        else -> context.getString(R.string.reminder_before, minutes)
    }
}

/** Resolves a theme color attribute such as ?attr/colorOnSurface. */
fun Context.themeColor(@AttrRes attr: Int): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(attr))
    return try {
        typedArray.getColor(0, 0)
    } finally {
        typedArray.recycle()
    }
}
