package com.example.modity.data

import android.content.Context
import com.example.modity.R
import com.example.modity.model.FocusResult
import com.example.modity.model.FocusSession
import com.example.modity.model.OccurrenceStatus
import com.example.modity.model.Priority
import com.example.modity.model.RepeatType
import com.example.modity.model.Task
import com.example.modity.model.TaskColors
import com.example.modity.model.TaskGroup
import com.example.modity.model.TaskIcon
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** Inserts demo groups, tasks and three weeks of history (used from Settings). */
object SampleData {

    suspend fun insert(context: Context, repository: ModityRepository) {
        val today = LocalDate.now()
        val start = today.minusDays(21).toEpochDay()
        val colors = TaskColors.palette(context)

        val morning = repository.saveGroup(
            TaskGroup(name = context.getString(R.string.sample_group_morning), color = colors[3], icon = TaskIcon.SUN.name),
        )
        val study = repository.saveGroup(
            TaskGroup(name = context.getString(R.string.sample_group_study), color = colors[0], icon = TaskIcon.SCHOOL.name),
        )
        val work = repository.saveGroup(
            TaskGroup(name = context.getString(R.string.sample_group_work), color = colors[1], icon = TaskIcon.WORK.name),
        )

        val weekdays = listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
            .fold(0) { mask, day -> mask or Task.dayBit(day) }
        val gymDays = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
            .fold(0) { mask, day -> mask or Task.dayBit(day) }

        val tasks = listOf(
            Task(title = context.getString(R.string.sample_task_water), color = colors[1], icon = TaskIcon.WATER.name,
                groupId = morning, repeatType = RepeatType.DAILY, repeatDays = Task.ALL_DAYS, startDate = start, startTime = 6 * 60 + 30),
            Task(title = context.getString(R.string.sample_task_meditate), color = colors[3], icon = TaskIcon.SUN.name,
                groupId = morning, repeatType = RepeatType.DAILY, repeatDays = Task.ALL_DAYS, startDate = start, startTime = 6 * 60 + 45,
                focusEnabled = true, focusMinutes = 15),
            Task(title = context.getString(R.string.sample_task_read), color = colors[0], icon = TaskIcon.BOOK.name,
                groupId = study, repeatType = RepeatType.WEEKLY, repeatDays = gymDays, startDate = start, startTime = 20 * 60,
                focusEnabled = true, focusMinutes = 25, priority = Priority.MEDIUM),
            Task(title = context.getString(R.string.sample_task_english), color = colors[6], icon = TaskIcon.SCHOOL.name,
                groupId = study, repeatType = RepeatType.DAILY, repeatDays = Task.ALL_DAYS, startDate = start, startTime = 21 * 60,
                focusEnabled = true, focusMinutes = 45, priority = Priority.HIGH),
            Task(title = context.getString(R.string.sample_task_inbox), color = colors[1], icon = TaskIcon.WORK.name,
                groupId = work, repeatType = RepeatType.WEEKLY, repeatDays = weekdays, startDate = start, startTime = 9 * 60,
                dueTime = 9 * 60 + 30),
            Task(title = context.getString(R.string.sample_task_report), color = colors[4], icon = TaskIcon.CODE.name,
                groupId = work, startDate = today.toEpochDay(), startTime = 14 * 60, dueDate = today.plusDays(2).toEpochDay(),
                dueTime = 17 * 60, priority = Priority.HIGH, focusEnabled = true, focusMinutes = 60),
            Task(title = context.getString(R.string.sample_task_gym), color = colors[2], icon = TaskIcon.FITNESS.name,
                repeatType = RepeatType.WEEKLY, repeatDays = gymDays, startDate = start, startTime = 17 * 60 + 30),
            Task(title = context.getString(R.string.sample_task_groceries), color = colors[5], icon = TaskIcon.CART.name,
                startDate = today.toEpochDay(), priority = Priority.LOW),
        )

        val random = Random(42)
        val zone = ZoneId.systemDefault()
        for (task in tasks) {
            val saved = task.copy(id = repository.saveTask(task))
            var date = LocalDate.ofEpochDay(start)
            while (date.isBefore(today)) {
                if (saved.occursOn(date)) {
                    val roll = random.nextInt(100)
                    val status = when {
                        roll < 72 -> OccurrenceStatus.COMPLETED
                        roll < 82 -> OccurrenceStatus.SKIPPED
                        else -> null
                    }
                    status?.let { repository.setStatus(saved.id, date, it) }
                    if (saved.focusEnabled && status == OccurrenceStatus.COMPLETED) {
                        val startedAt = date.atTime((saved.startTime ?: 600) / 60, 0).atZone(zone).toInstant().toEpochMilli()
                        val finished = random.nextInt(100) < 80
                        val seconds = if (finished) saved.focusMinutes * 60L else random.nextLong(5, saved.focusMinutes.toLong()) * 60
                        repository.saveFocusSession(
                            FocusSession(
                                taskId = saved.id, taskTitle = saved.title, startedAt = startedAt,
                                endedAt = startedAt + seconds * 1000, plannedMinutes = saved.focusMinutes,
                                actualSeconds = seconds, pauseCount = random.nextInt(3),
                                result = if (finished) FocusResult.COMPLETED else FocusResult.STOPPED_EARLY,
                            ),
                        )
                    }
                }
                date = date.plusDays(1)
            }
        }
    }
}
