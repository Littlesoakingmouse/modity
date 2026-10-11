package com.example.modity.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.modity.model.FocusSession
import com.example.modity.model.Task
import com.example.modity.model.TaskGroup
import com.example.modity.model.TaskOccurrence

@Dao
@JvmSuppressWildcards
interface TaskGroupDao {
    @Query("SELECT * FROM task_groups ORDER BY position, id")
    suspend fun getAll(): List<TaskGroup>

    @Query("SELECT * FROM task_groups WHERE id = :id")
    suspend fun getById(id: Long): TaskGroup?

    @Query("SELECT COALESCE(MAX(position), -1) FROM task_groups")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insert(group: TaskGroup): Long

    @Update
    suspend fun update(groups: List<TaskGroup>): Int

    @Delete
    suspend fun delete(group: TaskGroup): Int
}

@Dao
@JvmSuppressWildcards
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY start_time IS NULL, start_time, title")
    suspend fun getAll(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task): Int

    @Delete
    suspend fun delete(task: Task): Int

    @Query("UPDATE tasks SET group_id = NULL WHERE group_id = :groupId")
    suspend fun ungroup(groupId: Long): Int

    @Query("DELETE FROM tasks WHERE group_id = :groupId")
    suspend fun deleteByGroup(groupId: Long): Int
}

@Dao
@JvmSuppressWildcards
interface TaskOccurrenceDao {
    @Query("SELECT * FROM task_occurrences WHERE date BETWEEN :from AND :to")
    suspend fun getInRange(from: Long, to: Long): List<TaskOccurrence>

    @Query("SELECT * FROM task_occurrences WHERE task_id = :taskId")
    suspend fun getForTask(taskId: Long): List<TaskOccurrence>

    @Query("SELECT * FROM task_occurrences WHERE task_id = :taskId AND date = :date")
    suspend fun get(taskId: Long, date: Long): TaskOccurrence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(occurrence: TaskOccurrence): Long

    @Query("DELETE FROM task_occurrences WHERE task_id = :taskId AND date = :date")
    suspend fun delete(taskId: Long, date: Long): Int
}

@Dao
@JvmSuppressWildcards
interface FocusSessionDao {
    @Insert
    suspend fun insert(session: FocusSession): Long

    @Query("SELECT * FROM focus_sessions WHERE started_at BETWEEN :from AND :to ORDER BY started_at DESC")
    suspend fun getInRange(from: Long, to: Long): List<FocusSession>

    @Query("SELECT * FROM focus_sessions WHERE task_id = :taskId ORDER BY started_at DESC")
    suspend fun getForTask(taskId: Long): List<FocusSession>
}
