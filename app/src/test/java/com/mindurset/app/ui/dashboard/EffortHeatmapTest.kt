package com.mindurset.app.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class EffortHeatmapTest {
    private val zone = ZoneId.of("Asia/Bangkok")
    private val today = LocalDate.of(2026, 11, 11)

    @Test
    fun countsCheckInsReframesAndCompletedActionsUpToThreePerDay() {
        val todayAt = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val yesterdayAt = today.minusDays(1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

        val days = EffortHeatmap.fromTimestamps(
            checkIns = listOf(todayAt, todayAt, yesterdayAt),
            completedReframes = listOf(todayAt),
            completedActions = listOf(todayAt, yesterdayAt),
            today = today,
            zoneId = zone,
        )

        assertEquals(EffortHeatmap.DAYS, days.size)
        assertEquals(today.minusDays(27), days.first().date)
        assertEquals(3, days.last().count)
        assertEquals(2, days[days.lastIndex - 1].count)
    }

    @Test
    fun ignoresEventsOutsideTheRollingWindowAndFutureEvents() {
        val start = today.minusDays(27)
        val beforeStart = start.minusDays(1).atTime(23, 59).atZone(zone).toInstant().toEpochMilli()
        val afterToday = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val days = EffortHeatmap.fromTimestamps(
            checkIns = listOf(beforeStart, afterToday),
            completedReframes = emptyList(),
            completedActions = emptyList(),
            today = today,
            zoneId = zone,
        )

        assertEquals(List(EffortHeatmap.DAYS) { 0 }, days.map { it.count })
    }
}
