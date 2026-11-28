package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

class BuildMoodInsightsTest {
    private val zone = ZoneId.of("Asia/Bangkok")
    private val today = LocalDate.of(2026, 11, 28)
    private val fixedClock = Clock.fixed(today.atTime(14, 0).atZone(zone).toInstant(), zone)

    @Test
    fun `seven day window returns seven chronological points`() {
        val checkIns =
            listOf(
                checkIn(id = "c1", emotion = Emotion.CALM, daysAgo = 6, hour = 9),
                checkIn(id = "c2", emotion = Emotion.HAPPY, daysAgo = 0, hour = 11),
            )

        val insights = BuildMoodInsights.build(checkIns, windowDays = 7, clock = fixedClock)

        assertEquals(7, insights.series.size)
        assertEquals(today.minusDays(6), insights.series.first().date)
        assertEquals(today, insights.series.last().date)
        assertEquals(Emotion.CALM, insights.series.first().emotion)
        assertEquals(Emotion.HAPPY, insights.series.last().emotion)
    }

    @Test
    fun `gaps are preserved as null for empty days`() {
        val checkIns =
            listOf(
                checkIn(id = "c1", emotion = Emotion.LOW, daysAgo = 4, hour = 10),
                checkIn(id = "c2", emotion = Emotion.HAPPY, daysAgo = 1, hour = 10),
            )

        val insights = BuildMoodInsights.build(checkIns, windowDays = 7, clock = fixedClock)

        assertEquals(7, insights.series.size)
        assertNull(insights.series[0].emotion)
        assertNull(insights.series[1].emotion)
        assertEquals(Emotion.LOW, insights.series[2].emotion)
        assertNull(insights.series[3].emotion)
        assertNull(insights.series[4].emotion)
        assertEquals(Emotion.HAPPY, insights.series[5].emotion)
        assertNull(insights.series[6].emotion)
    }

    @Test
    fun `latest check-in wins for a duplicate day while breakdown counts all check-ins`() {
        val checkIns =
            listOf(
                checkIn(id = "early", emotion = Emotion.STRESSED, daysAgo = 2, hour = 8),
                checkIn(id = "late", emotion = Emotion.CALM, daysAgo = 2, hour = 20),
                checkIn(id = "today", emotion = Emotion.HAPPY, daysAgo = 0, hour = 12),
            )

        val insights = BuildMoodInsights.build(checkIns, windowDays = 7, clock = fixedClock)

        val duplicateDayPoint = insights.series.first { it.date == today.minusDays(2) }
        assertEquals(Emotion.CALM, duplicateDayPoint.emotion)
        assertTrue(insights.hasDuplicateDay)
        assertEquals(3, insights.totalCheckIns)
        assertEquals(3, insights.counts.values.sum())
        assertEquals(1, insights.counts[Emotion.STRESSED])
        assertEquals(1, insights.counts[Emotion.CALM])
        assertEquals(1, insights.counts[Emotion.HAPPY])
    }

    @Test
    fun `window boundary is inclusive on start day and today and excludes outside days`() {
        val startDay = today.minusDays(6)
        val exactStartMillis = startDay.atStartOfDay(zone).toInstant().toEpochMilli()
        val justBeforeStartMillis = exactStartMillis - 1L
        val endOfTodayMillis = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
        val nextDayMillis = endOfTodayMillis + 1L

        val checkIns =
            listOf(
                CheckIn("before", "u1", Emotion.STRESSED, 5, null, justBeforeStartMillis),
                CheckIn("at-start", "u1", Emotion.LOW, 4, null, exactStartMillis),
                CheckIn("at-end", "u1", Emotion.HAPPY, 8, null, endOfTodayMillis),
                CheckIn("after", "u1", Emotion.ANXIOUS, 6, null, nextDayMillis),
            )

        val insights = BuildMoodInsights.build(checkIns, windowDays = 7, clock = fixedClock)

        assertEquals(2, insights.totalCheckIns)
        assertEquals(2, insights.counts.values.sum())
        assertEquals(Emotion.LOW, insights.series.first().emotion)
        assertEquals(Emotion.HAPPY, insights.series.last().emotion)
        assertEquals(0, insights.counts[Emotion.STRESSED])
        assertEquals(0, insights.counts[Emotion.ANXIOUS])
    }

    @Test
    fun `empty list returns empty series and zero counts without throwing`() {
        val insights = BuildMoodInsights.build(emptyList(), windowDays = 7, clock = fixedClock)

        assertTrue(insights.series.isEmpty())
        assertEquals(0, insights.totalCheckIns)
        assertEquals(0, insights.counts.values.sum())
        assertFalse(insights.hasDuplicateDay)
        assertEquals(Emotion.entries.size, insights.breakdown.size)
        assertTrue(insights.breakdown.all { it.count == 0 && it.share == 0f })
    }

    @Test
    fun `check-ins outside the window produce an empty series and zero counts`() {
        val old = checkIn(id = "old", emotion = Emotion.CALM, daysAgo = 10, hour = 12)

        val insights = BuildMoodInsights.build(listOf(old), windowDays = 7, clock = fixedClock)

        assertTrue(insights.series.isEmpty())
        assertEquals(0, insights.totalCheckIns)
        assertEquals(0, insights.counts.values.sum())
    }

    @Test
    fun `thirty day window counts check-ins across month boundary and computes shares`() {
        val checkIns =
            listOf(
                checkIn(id = "d29", emotion = Emotion.CALM, daysAgo = 29, hour = 9),
                checkIn(id = "d10", emotion = Emotion.HAPPY, daysAgo = 10, hour = 15),
                checkIn(id = "d2", emotion = Emotion.HAPPY, daysAgo = 2, hour = 18),
                checkIn(id = "d0", emotion = Emotion.LOW, daysAgo = 0, hour = 12),
                checkIn(id = "too-old", emotion = Emotion.STRESSED, daysAgo = 30, hour = 12),
            )

        val insights = BuildMoodInsights.build(checkIns, windowDays = 30, clock = fixedClock)

        assertEquals(30, insights.series.size)
        assertEquals(4, insights.totalCheckIns)
        assertEquals(4, insights.counts.values.sum())
        assertEquals(0.5f, insights.breakdown.first { it.emotion == Emotion.HAPPY }.share, 0.0001f)
        assertEquals(0.25f, insights.breakdown.first { it.emotion == Emotion.CALM }.share, 0.0001f)
        assertEquals(0.25f, insights.breakdown.first { it.emotion == Emotion.LOW }.share, 0.0001f)
    }

    private fun checkIn(id: String, emotion: Emotion, daysAgo: Long, hour: Int): CheckIn {
        val millis = today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return CheckIn(
            id = id,
            userId = "u1",
            emotion = emotion,
            intensity = 5,
            note = null,
            createdAt = millis,
        )
    }
}
