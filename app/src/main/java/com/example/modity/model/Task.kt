package com.example.modity.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class RepeatType { NONE, DAILY, WEEKLY }

enum class Priority { NONE, LOW, MEDIUM, HIGH }

@Entity(tableName = "tasks", indices = [Index("group_id")])
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val color: Int,
    val icon: String = TaskIcon.CHECK.name,
    @ColumnInfo(name = "group_id") val groupId: Long? = null,
    val priority: Priority = Priority.NONE,
    @ColumnInfo(name = "repeat_type") val repeatType: RepeatType = RepeatType.NONE,
    /** Bit mask of weekdays, bit 0 = Monday … bit 6 = Sunday. Used when [repeatType] is WEEKLY. */
    @ColumnInfo(name = "repeat_days") val repeatDays: Int = 0,
    /** Epoch day. The task's date for one-time tasks, or the first day of the schedule. */
    @ColumnInfo(name = "start_date") val startDate: Long,
    /** Epoch day, inclusive. Null means the schedule repeats forever. */
    @ColumnInfo(name = "end_date") val endDate: Long? = null,
    /** Minutes from midnight. Null means "anytime". */
    @ColumnInfo(name = "start_time") val startTime: Int? = null,
    /** Epoch day of the deadline, one-time tasks only. Null means the scheduled day. */
    @ColumnInfo(name = "due_date") val dueDate: Long? = null,
    /** Minutes from midnight. Null means end of day. */
    @ColumnInfo(name = "due_time") val dueTime: Int? = null,
    /** Minutes before the task time. Null means no reminder. */
    @ColumnInfo(name = "reminder_minutes") val reminderMinutes: Int? = null,
    @ColumnInfo(name = "focus_enabled") val focusEnabled: Boolean = false,
    @ColumnInfo(name = "focus_minutes") val focusMinutes: Int = 25,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
) {
    val isRepeating: Boolean get() = repeatType != RepeatType.NONE

    fun occursOn(date: LocalDate): Boolean {
        val day = date.toEpochDay()
        if (day < startDate) return false
        if (endDate != null && day > endDate) return false
        return when (repeatType) {
            RepeatType.NONE -> day == startDate
            RepeatType.DAILY -> true
            RepeatType.WEEKLY -> repeatsOn(date.dayOfWeek)
        }
    }

    fun repeatsOn(dayOfWeek: DayOfWeek): Boolean = repeatDays and dayBit(dayOfWeek) != 0

    /** Moment after which a pending occurrence on [date] counts as missed. */
    fun deadline(date: LocalDate): LocalDateTime {
        val day = if (repeatType == RepeatType.NONE && dueDate != null) LocalDate.ofEpochDay(dueDate) else date
        val time = dueTime?.let { LocalTime.of(it / 60, it % 60) } ?: LocalTime.MAX
        return day.atTime(time)
    }

    /** Time used for reminders and timeline ordering. */
    fun anchorTime(): Int? = startTime ?: dueTime

    /** Most recent scheduled day on or before [date], or null if none within a year. */
    fun lastOccurrenceOnOrBefore(date: LocalDate): LocalDate? {
        var day = date
        repeat(DAYS_TO_SCAN) {
            if (day.toEpochDay() < startDate) return null
            if (occursOn(day)) return day
            day = day.minusDays(1)
        }
        return null
    }

    fun nextOccurrenceOnOrAfter(date: LocalDate): LocalDate? {
        var day = maxOf(date, LocalDate.ofEpochDay(startDate))
        repeat(DAYS_TO_SCAN) {
            if (endDate != null && day.toEpochDay() > endDate) return null
            if (occursOn(day)) return day
            day = day.plusDays(1)
        }
        return null
    }

    companion object {
        const val ALL_DAYS = 0b1111111
        private const val DAYS_TO_SCAN = 370

        fun dayBit(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)
    }
}
