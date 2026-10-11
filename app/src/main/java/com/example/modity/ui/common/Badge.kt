package com.example.modity.ui.common

import android.content.res.ColorStateList
import android.widget.ImageView
import androidx.core.graphics.ColorUtils
import com.example.modity.model.TaskIcon

/** Icon on a soft tinted background, used for groups and tasks. */
fun ImageView.bindBadge(icon: String, color: Int) {
    setImageResource(TaskIcon.from(icon).drawable)
    imageTintList = ColorStateList.valueOf(color)
    backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, BADGE_ALPHA))
}

private const val BADGE_ALPHA = 0x24
