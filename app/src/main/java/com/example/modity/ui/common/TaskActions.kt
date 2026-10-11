package com.example.modity.ui.common

import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.modity.R
import com.example.modity.data.ModityRepository
import com.example.modity.model.DayTask
import com.example.modity.model.OccurrenceStatus
import com.example.modity.notification.ReminderScheduler
import com.example.modity.ui.focus.FocusSetupActivity
import com.example.modity.ui.task.TaskEditorActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/** Task actions shared by every screen that lists tasks. [onChanged] reloads the screen. */
class TaskActions(
    private val activity: AppCompatActivity,
    private val onChanged: () -> Unit,
) : TaskListener {
    private val repository = ModityRepository.get(activity)

    override fun onToggle(item: DayTask) {
        setStatus(item, if (item.isDone) OccurrenceStatus.PENDING else OccurrenceStatus.COMPLETED)
    }

    override fun onOpen(item: DayTask) {
        activity.startActivity(com.example.modity.ui.task.TaskDetailActivity.intent(activity, item.task.id, item.date))
    }

    override fun onMore(item: DayTask, anchor: View) {
        val popup = PopupMenu(activity, anchor)
        val menu = popup.menu
        menu.add(0, MENU_TOGGLE, 0, if (item.isDone) R.string.action_mark_undone else R.string.action_mark_done)
        if (item.status == OccurrenceStatus.SKIPPED) {
            menu.add(0, MENU_UNSKIP, 1, R.string.action_unskip)
        } else if (!item.isDone) {
            menu.add(0, MENU_SKIP, 1, R.string.action_skip)
        }
        menu.add(0, MENU_FOCUS, 2, R.string.action_start_focus)
        menu.add(0, MENU_EDIT, 3, R.string.action_edit)
        menu.add(0, MENU_DELETE, 4, R.string.action_delete)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                MENU_TOGGLE -> onToggle(item)
                MENU_SKIP -> setStatus(item, OccurrenceStatus.SKIPPED)
                MENU_UNSKIP -> setStatus(item, OccurrenceStatus.PENDING)
                MENU_FOCUS -> startFocus(item)
                MENU_EDIT -> edit(item)
                MENU_DELETE -> delete(item)
            }
            true
        }
        popup.show()
    }

    fun setStatus(item: DayTask, status: OccurrenceStatus) {
        activity.lifecycleScope.launch {
            repository.setStatus(item.task.id, item.date, status)
            onChanged()
        }
    }

    fun edit(item: DayTask) {
        activity.startActivity(TaskEditorActivity.editIntent(activity, item.task.id, item.date))
    }

    fun startFocus(item: DayTask) {
        activity.startActivity(FocusSetupActivity.intent(activity, item.task.id, item.date))
    }

    /** Repeating tasks ask whether to delete one occurrence or the whole series. */
    fun delete(item: DayTask, onDeleted: () -> Unit = onChanged) {
        val task = item.task
        if (task.isRepeating) {
            val choices = arrayOf(
                activity.getString(R.string.scope_this_occurrence),
                activity.getString(R.string.scope_all_occurrences),
            )
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_repeating_title)
                .setItems(choices) { _, which ->
                    activity.lifecycleScope.launch {
                        if (which == 0) {
                            repository.setStatus(task.id, item.date, OccurrenceStatus.REMOVED)
                        } else {
                            repository.deleteTask(task)
                            ReminderScheduler.cancel(activity, task.id)
                        }
                        onDeleted()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.delete_task_title)
                .setMessage(activity.getString(R.string.delete_task_message, task.title))
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    activity.lifecycleScope.launch {
                        repository.deleteTask(task)
                        ReminderScheduler.cancel(activity, task.id)
                        onDeleted()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private companion object {
        const val MENU_TOGGLE = 1
        const val MENU_SKIP = 2
        const val MENU_UNSKIP = 3
        const val MENU_FOCUS = 4
        const val MENU_EDIT = 5
        const val MENU_DELETE = 6
    }
}
