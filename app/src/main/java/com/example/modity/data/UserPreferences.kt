package com.example.modity.data

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat

enum class ThemeMode(val nightMode: Int) {
    SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT(AppCompatDelegate.MODE_NIGHT_NO),
    DARK(AppCompatDelegate.MODE_NIGHT_YES),
}

enum class AppLanguage(val tag: String) { SYSTEM(""), ENGLISH("en"), VIETNAMESE("vi") }

enum class HomeStyle { CARD, GRID, TIMELINE }

/** User settings stored in SharedPreferences. */
class UserPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = enumOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM)
        set(value) = prefs.edit { putString(KEY_THEME, value.name) }

    var language: AppLanguage
        get() = enumOrDefault(prefs.getString(KEY_LANGUAGE, null), AppLanguage.SYSTEM)
        set(value) = prefs.edit { putString(KEY_LANGUAGE, value.name) }

    var homeStyle: HomeStyle
        get() = enumOrDefault(prefs.getString(KEY_HOME_STYLE, null), HomeStyle.CARD)
        set(value) = prefs.edit { putString(KEY_HOME_STYLE, value.name) }

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDERS, true)
        set(value) = prefs.edit { putBoolean(KEY_REMINDERS, value) }

    var defaultFocusMinutes: Int
        get() = prefs.getInt(KEY_FOCUS_MINUTES, 25)
        set(value) = prefs.edit { putInt(KEY_FOCUS_MINUTES, value) }

    var focusDoNotDisturb: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_DND, false)
        set(value) = prefs.edit { putBoolean(KEY_FOCUS_DND, value) }

    fun applyTheme() = AppCompatDelegate.setDefaultNightMode(themeMode.nightMode)

    /** AppCompat persists the per-app locale itself (system on API 33+, autoStoreLocales below). */
    fun applyLanguage() = AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))

    private inline fun <reified T : Enum<T>> enumOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_LANGUAGE = "language"
        const val KEY_HOME_STYLE = "home_style"
        const val KEY_REMINDERS = "reminders_enabled"
        const val KEY_FOCUS_MINUTES = "default_focus_minutes"
        const val KEY_FOCUS_DND = "focus_dnd"
    }
}
