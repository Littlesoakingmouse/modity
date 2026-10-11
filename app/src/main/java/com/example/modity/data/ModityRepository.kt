package com.example.modity.data

import android.content.Context
import androidx.room.withTransaction
import com.example.modity.model.DayTask
import com.example.modity.model.FocusSession
import com.example.modity.model.GroupProgress
import com.example.modity.model.OccurrenceStatus
import com.example.modity.model.RepeatType
import com.example.modity.model.Task
import com.example.modity.model.TaskGroup
import com.example.modity.model.TaskOccurrence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class ModityRepository private constructor(context: Context) {
    private val db = AppDatabase.get(context)
    private val groupDao = db.taskGroupDao()
    private val taskDao = db.taskDao()
    private val occurrenceDao = db.occurrenceDao()
    private val focusDao = db.focusSessionDao()

    // --- Groups ---

    suspend fun groups(): List<TaskGroup> = groupDao.getAll()

    suspend fun group(id: Long): TaskGroup? = groupDao.getById(id)

    suspend fun saveGroup(group: TaskGroup): Long =
        if (group.id == 0L) {
            groupDao.insert(group.copy(position = groupDao.maxPosition() + 1))
        } else {
            groupDao.update(listOf(group))
            group.id
        }

    suspend fun reorderGroups(ordered: List<TaskGroup>) =
        groupDao.update(ordered.mapIndexed { index, group -> group.copy(position = index) })

    /** Deletes a group, either keeping its tasks as ungrouped tasks or deleting them too. */
    suspend fun deleteGroup(group: TaskGroup, deleteTasks: Boolean) = db.withTransaction {
        if (deleteTasks) taskDao.deleteByGroup(group.id) else taskDao.ungroup(group.id)
        groupDao.delete(group)
    }

    // --- Tasks ---

    suspend fun tasks(): List<Task> = taskDao.getAll()

    suspend fun task(id: Long): Task? = taskDao.getById(id)

    suspend fun saveTask(task: Task): Long =
        if (task.id == 0L) taskDao.insert(task) else task.id.also { taskDao.update(task) }

    suspend fun deleteTask(task: Task) = taskDao.delete(task)

    /**
     * Applies [edited] to a single occurrence of a repeating task: the original occurrence is
     * removed from the series and replaced by a new one-time task that keeps its status.
     */
    suspend fun detachOccurrence(original: Task, date: LocalDate, edited: Task): Long = db.withTransaction {
        val previous = occurrenceDao.get(original.id, date.toEpochDay())
        occurrenceDao.upsert(TaskOccurrence(taskId = original.id, date = date.toEpochDay(), status = OccurrenceStatus.REMOVED))
        val newId = taskDao.insert(
            edited.copy(id = 0, repeatType = RepeatType.NONE, repeatDays = 0, endDate = null, createdAt = System.currentTimeMillis()),
        )
        if (previous != null && previous.status != OccurrenceStatus.REMOVED) {
            occurrenceDao.upsert(TaskOccurrence(taskId = newId, date = edited.startDate, status = previous.status))
        }
        newId
    }

    // --- Occurrences ---

    suspend fun setStatus(taskId: Long, date: LocalDate, status: OccurrenceStatus) {
        if (status == OccurrenceStatus.PENDING || status == OccurrenceStatus.MISSED) {
            occurrenceDao.delete(taskId, date.toEpochDay())
        } else {
            occurrenceDao.upsert(TaskOccurrence(taskId = taskId, date = date.toEpochDay(), status = status))
        }
    }

    suspend fun statusOf(task: Task, date: LocalDate): OccurrenceStatus =
        resolve(task, date, occurrenceDao.get(task.id, date.toEpochDay()), LocalDateTime.now())

    suspend fun dayTasks(date: LocalDate): List<DayTask> = rangeTasks(date, date)

    /** Every scheduled occurrence between [from] and [to] (inclusive), excluding removed ones. */
    suspend fun rangeTasks(from: LocalDate, to: LocalDate, tasks: List<Task>? = null): List<DayTask> {
        val allTasks = tasks ?: taskDao.getAll()
        val records = occurrenceDao.getInRange(from.toEpochDay(), to.toEpochDay())
            .associateBy { it.taskId to it.date }
        val now = LocalDateTime.now()
        val result = mutableListOf<DayTask>()
        var date = from
        while (!date.isAfter(to)) {
            for (task in allTasks) {
                if (!task.occursOn(date)) continue
                val status = resolve(task, date, records[task.id to date.toEpochDay()], now)
                if (status != OccurrenceStatus.REMOVED) result += DayTask(task, date, status)
            }
            date = date.plusDays(1)
        }
        return result
    }

    /** Past and current occurrences of one task, newest first. */
    suspend fun history(task: Task, days: Long = 365): List<DayTask> {
        val today = LocalDate.now()
        val from = maxOf(LocalDate.ofEpochDay(task.startDate), today.minusDays(days))
        if (from.isAfter(today)) return emptyList()
        return rangeTasks(from, today, listOf(task)).reversed()
    }

    /** Home screen entries for [date]: groups first (in user order), then ungrouped tasks. */
    suspend fun homeGroups(date: LocalDate): List<GroupProgress> {
        val dayTasks = dayTasks(date)
        val byGroup = dayTasks.groupBy { it.task.groupId }
        val grouped = groups().mapNotNull { group ->
            byGroup[group.id]?.let { GroupProgress(group, it) }
        }
        val knownGroupIds = grouped.map { it.group?.id }.toSet()
        val ungrouped = dayTasks
            .filter { it.task.groupId == null || it.task.groupId !in knownGroupIds }
            .map { GroupProgress(null, listOf(it)) }
        return grouped + ungrouped
    }

    private fun resolve(task: Task, date: LocalDate, record: TaskOccurrence?, now: LocalDateTime): OccurrenceStatus =
        record?.status ?: if (now.isAfter(task.deadline(date))) OccurrenceStatus.MISSED else OccurrenceStatus.PENDING

    // --- Focus sessions ---

    suspend fun saveFocusSession(session: FocusSession): Long = focusDao.insert(session)

    suspend fun focusSessions(fromMillis: Long, toMillis: Long): List<FocusSession> =
        focusDao.getInRange(fromMillis, toMillis)

    suspend fun focusSessionsForTask(taskId: Long): List<FocusSession> = focusDao.getForTask(taskId)

    // --- Maintenance ---

    suspend fun clearAll() = withContext(Dispatchers.IO) { db.clearAllTables() }

    companion object {
        @Volatile
        private var instance: ModityRepository? = null

        fun get(context: Context): ModityRepository = instance ?: synchronized(this) {
            instance ?: ModityRepository(context.applicationContext).also { instance = it }
        }
    }
}
