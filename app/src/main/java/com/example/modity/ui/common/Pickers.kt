package com.example.modity.ui.common

import android.text.format.DateFormat
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

object Pickers {

    fun date(activity: AppCompatActivity, initial: LocalDate, onPicked: (LocalDate) -> Unit) {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setSelection(initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            onPicked(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
        }
        picker.show(activity.supportFragmentManager, "date_picker")
    }

    /** [initialMinutes] and the result are minutes from midnight. */
    fun time(activity: AppCompatActivity, initialMinutes: Int?, onPicked: (Int) -> Unit) {
        val initial = initialMinutes ?: (LocalTime.now().hour + 1).coerceAtMost(23) * 60
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(if (DateFormat.is24HourFormat(activity)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
            .setHour(initial / 60)
            .setMinute(initial % 60)
            .build()
        picker.addOnPositiveButtonClickListener { onPicked(picker.hour * 60 + picker.minute) }
        picker.show(activity.supportFragmentManager, "time_picker")
    }
}
