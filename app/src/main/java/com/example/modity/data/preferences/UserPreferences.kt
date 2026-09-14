package com.example.modity.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.example.modity.model.TaskDisplayStyle
import com.example.modity.model.ThemeMode
import com.example.modity.model.UserSettings

class UserPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): UserSettings = UserSettings(
        themeMode = enumValueOrDefault(
            preferences.getString(KEY_THEME_MODE, null),
            ThemeMode.SYSTEM,
        ),
        languageTag = preferences.getString(KEY_LANGUAGE_TAG, "").orEmpty(),
        currencyCode = preferences.getString(KEY_CURRENCY_CODE, DEFAULT_CURRENCY).orEmpty(),
        notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
        taskDisplayStyle = enumValueOrDefault(
            preferences.getString(KEY_TASK_DISPLAY_STYLE, null),
            TaskDisplayStyle.LIST,
        ),
    )

    fun save(settings: UserSettings) {
        preferences.edit {
            putString(KEY_THEME_MODE, settings.themeMode.name)
            putString(KEY_LANGUAGE_TAG, settings.languageTag)
            putString(KEY_CURRENCY_CODE, settings.currencyCode)
            putBoolean(KEY_NOTIFICATIONS_ENABLED, settings.notificationsEnabled)
            putString(KEY_TASK_DISPLAY_STYLE, settings.taskDisplayStyle.name)
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private companion object {
        const val PREFERENCES_NAME = "user_settings"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LANGUAGE_TAG = "language_tag"
        const val KEY_CURRENCY_CODE = "currency_code"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_TASK_DISPLAY_STYLE = "task_display_style"
        const val DEFAULT_CURRENCY = "USD"
    }
}
