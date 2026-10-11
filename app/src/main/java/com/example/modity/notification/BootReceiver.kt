package com.example.modity.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are cleared on reboot and app update, so they are rescheduled here. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAll(context)
            } finally {
                result.finish()
            }
        }
    }
}
