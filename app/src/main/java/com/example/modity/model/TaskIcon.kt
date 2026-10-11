package com.example.modity.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.example.modity.R

enum class TaskIcon(@DrawableRes val drawable: Int) {
    CHECK(R.drawable.ic_task_check),
    STAR(R.drawable.ic_task_star),
    SUN(R.drawable.ic_task_sun),
    MOON(R.drawable.ic_task_moon),
    BOOK(R.drawable.ic_task_book),
    SCHOOL(R.drawable.ic_task_school),
    WORK(R.drawable.ic_task_work),
    CODE(R.drawable.ic_task_code),
    FITNESS(R.drawable.ic_task_fitness),
    HEART(R.drawable.ic_task_heart),
    WATER(R.drawable.ic_task_water),
    FOOD(R.drawable.ic_task_food),
    MUSIC(R.drawable.ic_task_music),
    CART(R.drawable.ic_task_cart),
    HOME(R.drawable.ic_task_home);

    companion object {
        fun from(name: String): TaskIcon = entries.firstOrNull { it.name == name } ?: CHECK
    }
}

/** Colors users can give tasks and groups; stored as ARGB ints. */
object TaskColors {
    private val resources = listOf(
        R.color.task_indigo,
        R.color.task_sky,
        R.color.task_emerald,
        R.color.task_amber,
        R.color.task_red,
        R.color.task_pink,
        R.color.task_violet,
        R.color.task_slate,
    )

    fun palette(context: Context): List<Int> = resources.map { ContextCompat.getColor(context, it) }
}
