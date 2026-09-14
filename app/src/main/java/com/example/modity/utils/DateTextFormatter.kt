package com.example.modity.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTextFormatter {
    fun weekday(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("EEE", locale)).uppercase(locale)

    fun dayOfMonth(date: LocalDate, locale: Locale = Locale.getDefault()): String =
        date.format(DateTimeFormatter.ofPattern("dd", locale))
}
