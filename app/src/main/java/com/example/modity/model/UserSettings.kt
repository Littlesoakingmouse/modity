package com.example.modity.model

data class UserSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageTag: String = "",
    val currencyCode: String = "USD",
    val notificationsEnabled: Boolean = true,
    val taskDisplayStyle: TaskDisplayStyle = TaskDisplayStyle.LIST,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class TaskDisplayStyle {
    LIST,
    COMPACT,
}
