package com.example.modity

import android.app.Application
import com.example.modity.data.UserPreferences
import com.example.modity.notification.ReminderScheduler

class ModityApp : Application() {
    override fun onCreate() {
        super.onCreate()
        UserPreferences(this).applyTheme()
        ReminderScheduler.createChannel(this)
    }
}
