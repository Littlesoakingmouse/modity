package com.example.modity.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.data.UserPreferences
import com.example.modity.model.Task
import java.time.LocalDate
import java.time.ZoneId

/** Schedules one AlarmManager alarm per task: the next upcoming reminder of its schedule. */
object ReminderScheduler {
    const val CHANNEL_ID = "task_reminders"
    private const val DEFAULT_TIME = 9 * 60

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminders),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.channel_reminders_desc) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, task: Task) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(pendingIntent(context, task.id, null))

        val minutes = task.reminderMinutes ?: return
        if (!UserPreferences(context).remindersEnabled) return
        val (date, triggerAt) = nextTrigger(task, minutes) ?: return
        val intent = pendingIntent(context, task.id, date)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, intent)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(pendingIntent(context, taskId, null))
    }

    suspend fun rescheduleAll(context: Context) {
        ModityRepository.get(context).tasks().forEach { schedule(context, it) }
    }

    /** Time a reminder fires for the occurrence on [date]. */
    fun triggerTime(task: Task, date: LocalDate, minutesBefore: Int): Long {
        val time = task.anchorTime() ?: DEFAULT_TIME
        return date.atTime(time / 60, time % 60)
            .minusMinutes(minutesBefore.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun nextTrigger(task: Task, minutesBefore: Int): Pair<LocalDate, Long>? {
        val now = System.currentTimeMillis()
        var from = LocalDate.now()
        repeat(MAX_LOOKUPS) {
            val date = task.nextOccurrenceOnOrAfter(from) ?: return null
            val trigger = triggerTime(task, date, minutesBefore)
            if (trigger > now) return date to trigger
            from = date.plusDays(1)
        }
        return null
    }

    private fun pendingIntent(context: Context, taskId: Long, date: LocalDate?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            .setData("modity://task/$taskId".toUri())
            .putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
        date?.let { intent.putExtra(ReminderReceiver.EXTRA_DATE, it.toEpochDay()) }
        return PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private const val MAX_LOOKUPS = 3
}
