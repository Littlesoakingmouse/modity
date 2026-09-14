package com.example.modity.utils

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.applySystemBarInsets() {
    val initialPaddingLeft = paddingLeft
    val initialPaddingTop = paddingTop
    val initialPaddingRight = paddingRight
    val initialPaddingBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            initialPaddingLeft + systemBars.left,
            initialPaddingTop + systemBars.top,
            initialPaddingRight + systemBars.right,
            initialPaddingBottom + systemBars.bottom,
        )
        windowInsets
    }
}
