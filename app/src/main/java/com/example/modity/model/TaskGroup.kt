package com.example.modity.model

data class TaskGroup(
    val id: Long = 0,
    val name: String,
    val tasks: List<Task> = emptyList(),
)
