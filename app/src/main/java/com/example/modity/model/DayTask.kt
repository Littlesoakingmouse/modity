package com.example.modity.model

import java.time.LocalDate

/** A task as it appears on a specific day, with that occurrence's status. */
data class DayTask(
    val task: Task,
    val date: LocalDate,
    val status: OccurrenceStatus,
) {
    val isDone: Boolean get() = status == OccurrenceStatus.COMPLETED
}

/** A home-screen entry: a group with its tasks for the day, or a single ungrouped task. */
data class GroupProgress(
    val group: TaskGroup?,
    val tasks: List<DayTask>,
) {
    val done: Int get() = tasks.count { it.isDone }
    val total: Int get() = tasks.size
    val percent: Int get() = if (total == 0) 0 else done * 100 / total
    val key: String get() = group?.let { "g${it.id}" } ?: "t${tasks.first().task.id}"
}
