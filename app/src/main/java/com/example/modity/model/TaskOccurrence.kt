package com.example.modity.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Status of one scheduled occurrence of a task.
 * Only COMPLETED, SKIPPED and REMOVED are stored; PENDING and MISSED are derived
 * from the schedule and the task deadline.
 */
enum class OccurrenceStatus { PENDING, COMPLETED, MISSED, SKIPPED, REMOVED }

@Entity(
    tableName = "task_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["task_id", "date"], unique = true), Index("date")],
)
data class TaskOccurrence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long,
    /** Epoch day of the scheduled occurrence. */
    val date: Long,
    val status: OccurrenceStatus,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)
