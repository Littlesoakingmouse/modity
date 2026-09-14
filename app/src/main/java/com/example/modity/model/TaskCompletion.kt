package com.example.modity.model

data class TaskCompletion(
    val id: Long = 0,
    val taskId: Long,
    val completedAtMillis: Long,
)
