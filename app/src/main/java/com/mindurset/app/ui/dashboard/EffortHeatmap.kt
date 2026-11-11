package com.mindurset.app.ui.dashboard

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One day's count of check-ins, completed reframes, and completed actions. */
data class EffortDay(val date: LocalDate, val count: Int)

/** Pure mapping for the dashboard's recent effort view. */
object EffortHeatmap {
    const val DAYS = 28
    const val MAX_COUNT = 3

    fun empty(today: LocalDate): List<EffortDay> = (0 until DAYS).map { offset ->
        EffortDay(date = today.minusDays((DAYS - offset - 1).toLong()), count = 0)
    }

    fun fromTimestamps(
        checkIns: List<Long>,
        completedReframes: List<Long>,
        completedActions: List<Long>,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<EffortDay> {
        val firstDay = today.minusDays(DAYS - 1L)
        val counts = mutableMapOf<LocalDate, Int>()
        (checkIns + completedReframes + completedActions).forEach { timestamp ->
            val date = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()
            if (date in firstDay..today) {
                counts[date] = ((counts[date] ?: 0) + 1).coerceAtMost(MAX_COUNT)
            }
        }
        return empty(today).map { day -> day.copy(count = counts[day.date] ?: 0) }
    }
}
