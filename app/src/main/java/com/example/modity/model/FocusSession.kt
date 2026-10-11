package com.example.modity.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class FocusResult { COMPLETED, STOPPED_EARLY }

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["task_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("task_id"), Index("started_at")],
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "task_id") val taskId: Long?,
    /** Snapshot of the task title so history survives task deletion. */
    @ColumnInfo(name = "task_title") val taskTitle: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "planned_minutes") val plannedMinutes: Int,
    @ColumnInfo(name = "actual_seconds") val actualSeconds: Long,
    @ColumnInfo(name = "pause_count") val pauseCount: Int,
    val result: FocusResult,
)
