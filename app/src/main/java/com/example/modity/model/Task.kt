package com.example.modity.model

data class Task(
    val id: Long = 0,
    val groupId: Long? = null,
    val title: String,
    val description: String = "",
    val dueAtMillis: Long? = null,
    val isCompleted: Boolean = false,
    val recurrenceRule: String? = null,
)
