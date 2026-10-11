package com.example.modity.ui.common

import android.app.Activity
import android.content.Intent
import android.os.Build
import com.example.modity.R
import com.example.modity.ui.calendar.CalendarActivity
import com.example.modity.ui.focus.FocusSetupActivity
import com.example.modity.ui.home.HomeActivity
import com.example.modity.ui.statistics.StatisticsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Top-level navigation. Home stays at the bottom of the back stack; every other tab
 * replaces the current one so Back always returns to Home.
 */
object BottomNav {

    fun setup(activity: Activity, navigation: BottomNavigationView, currentItem: Int) {
        navigation.setOnItemSelectedListener(null)
        navigation.selectedItemId = currentItem
        navigation.setOnItemSelectedListener { item ->
            if (item.itemId == currentItem) return@setOnItemSelectedListener true
            val target = when (item.itemId) {
                R.id.nav_today -> HomeActivity::class.java
                R.id.nav_calendar -> CalendarActivity::class.java
                R.id.nav_focus -> FocusSetupActivity::class.java
                R.id.nav_stats -> StatisticsActivity::class.java
                else -> return@setOnItemSelectedListener false
            }
            activity.startActivity(
                Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
            if (activity !is HomeActivity) activity.finish()
            disableTransition(activity)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun disableTransition(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            activity.overridePendingTransition(0, 0)
        }
    }
}
