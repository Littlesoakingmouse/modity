package com.example.modity.notification

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.model.OccurrenceStatus
import com.example.modity.model.Task
import com.example.modity.ui.task.TaskDetailActivity
import com.example.modity.utils.Formatters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Shows task reminders and handles the "Mark done" notification action. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, LocalDate.now().toEpochDay()))
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ModityRepository.get(context)
                val task = repository.task(taskId) ?: return@launch
                when (intent.action) {
                    ACTION_REMIND -> {
                        if (repository.statusOf(task, date) == OccurrenceStatus.PENDING) notify(context, task, date)
                        ReminderScheduler.schedule(context, task)
                    }
                    ACTION_COMPLETE -> {
                        repository.setStatus(task.id, date, OccurrenceStatus.COMPLETED)
                        NotificationManagerCompat.from(context).cancel(task.id.toInt())
                    }
                }
            } finally {
                result.finish()
            }
        }
    }

    private fun notify(context: Context, task: Task, date: LocalDate) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            return
        }
        val openIntent = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(TaskDetailActivity.intent(context, task.id, date))
            .getPendingIntent(task.id.toInt(), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val completeIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_COMPLETE)
                .setData("modity://complete/${task.id}".toUri())
                .putExtra(EXTRA_TASK_ID, task.id)
                .putExtra(EXTRA_DATE, date.toEpochDay()),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val text = task.anchorTime()?.let { context.getString(R.string.reminder_at, Formatters.time(context, it)) }
            ?: context.getString(R.string.reminder_today)

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setColor(task.color)
            .setContentTitle(task.title)
            .setContentText(text)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(R.drawable.ic_check, context.getString(R.string.action_mark_done), completeIntent)
            .build()
        NotificationManagerCompat.from(context).notify(task.id.toInt(), notification)
    }

    companion object {
        const val ACTION_REMIND = "com.example.modity.action.REMIND"
        const val ACTION_COMPLETE = "com.example.modity.action.COMPLETE"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_DATE = "date"
    }
}
