package com.example.modity.data

import com.example.modity.model.DayTask
import com.example.modity.model.FocusResult
import com.example.modity.model.FocusSession
import com.example.modity.model.OccurrenceStatus
import java.time.LocalDate

data class Overview(
    val total: Int = 0,
    val completed: Int = 0,
    val missed: Int = 0,
    val skipped: Int = 0,
    val pending: Int = 0,
) {
    val rate: Int get() = if (total == 0) 0 else completed * 100 / total
}

data class Streak(val current: Int, val best: Int)

data class FocusSummary(
    val sessions: Int,
    val completed: Int,
    val interrupted: Int,
    val totalSeconds: Long,
) {
    val averageSeconds: Long get() = if (sessions == 0) 0 else totalSeconds / sessions
    val interruptedRate: Int get() = if (sessions == 0) 0 else interrupted * 100 / sessions
}

/** Pure functions turning occurrences and focus sessions into statistics. */
object Stats {

    fun overview(items: List<DayTask>): Overview = Overview(
        total = items.size,
        completed = items.count { it.status == OccurrenceStatus.COMPLETED },
        missed = items.count { it.status == OccurrenceStatus.MISSED },
        skipped = items.count { it.status == OccurrenceStatus.SKIPPED },
        pending = items.count { it.status == OccurrenceStatus.PENDING },
    )

    fun perDay(items: List<DayTask>, from: LocalDate, to: LocalDate): List<Pair<LocalDate, Overview>> {
        val byDate = items.groupBy { it.date }
        val result = mutableListOf<Pair<LocalDate, Overview>>()
        var date = from
        while (!date.isAfter(to)) {
            result += date to overview(byDate[date].orEmpty())
            date = date.plusDays(1)
        }
        return result
    }

    /**
     * Streak over scheduled occurrences only, oldest first: completed extends it, missed resets it,
     * skipped and still-pending occurrences neither extend nor break it.
     */
    fun streak(history: List<DayTask>): Streak {
        var current = 0
        var best = 0
        for (item in history.sortedBy { it.date }) {
            when (item.status) {
                OccurrenceStatus.COMPLETED -> {
                    current++
                    best = maxOf(best, current)
                }
                OccurrenceStatus.MISSED -> current = 0
                else -> Unit
            }
        }
        return Streak(current, best)
    }

    fun focus(sessions: List<FocusSession>): FocusSummary = FocusSummary(
        sessions = sessions.size,
        completed = sessions.count { it.result == FocusResult.COMPLETED },
        interrupted = sessions.count { it.result == FocusResult.STOPPED_EARLY },
        totalSeconds = sessions.sumOf { it.actualSeconds },
    )
}
