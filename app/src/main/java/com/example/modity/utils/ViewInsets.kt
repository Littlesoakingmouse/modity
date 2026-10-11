package com.example.modity.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Pads the view by the system bar (and keyboard) insets on top of its own padding.
 * Screens with a BottomNavigationView pass [bottom] = false because the navigation view
 * pads itself for the navigation bar.
 */
fun View.applySystemBarInsets(bottom: Boolean = true) {
    val left = paddingLeft
    val top = paddingTop
    val right = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
        view.setPadding(
            left + bars.left,
            top + bars.top,
            right + bars.right,
            initialBottom + if (bottom) bars.bottom else 0,
        )
        windowInsets
    }
}
