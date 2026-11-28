package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** One day in a mood trend series; [emotion] is null when no check-in occurred on [date]. */
data class MoodTrendPoint(val date: LocalDate, val emotion: Emotion?)

/** Count and proportional share (`0f..1f`) for one [Emotion] inside a window. */
data class EmotionBreakdownItem(val emotion: Emotion, val count: Int, val share: Float)

/**
 * Derived mood trend and emotion distribution for a rolling day window.
 *
 * [series] is empty when no check-ins fall in the window so callers can show a plain
 * empty state instead of rendering an empty chart.
 */
data class MoodInsights(
    val windowDays: Int,
    val series: List<MoodTrendPoint>,
    val counts: Map<Emotion, Int>,
    val breakdown: List<EmotionBreakdownItem>,
    val totalCheckIns: Int,
    val hasDuplicateDay: Boolean,
) {
    companion object {
        fun empty(windowDays: Int): MoodInsights = MoodInsights(
            windowDays = windowDays,
            series = emptyList(),
            counts = Emotion.entries.associateWith { 0 },
            breakdown = Emotion.entries.map { EmotionBreakdownItem(it, count = 0, share = 0f) },
            totalCheckIns = 0,
            hasDuplicateDay = false,
        )
    }
}

/**
 * Computes a daily mood trend series and emotion breakdown over a rolling window.
 *
 * Window boundary: inclusive of `[today.minusDays(windowDays - 1), today]` in [clock]'s
 * time zone, where `today` is the local date of `clock.instant()`. Check-ins outside that
 * date range are excluded. When multiple check-ins fall on the same local date, the latest
 * check-in wins for that day's trend point, while all in-window check-ins contribute to
 * [MoodInsights.counts] and [MoodInsights.breakdown].
 */
object BuildMoodInsights {
    const val WEEKLY_WINDOW_DAYS = 7
    const val MONTHLY_WINDOW_DAYS = 30

    fun build(checkIns: List<CheckIn>, windowDays: Int, clock: Clock = Clock.systemDefaultZone()): MoodInsights {
        if (windowDays <= 0) {
            return MoodInsights.empty(windowDays = 0)
        }

        val zone = clock.zone
        val today = Instant.ofEpochMilli(clock.millis()).atZone(zone).toLocalDate()
        val startDate = today.minusDays((windowDays - 1).toLong())

        val inWindowByDate = mutableMapOf<LocalDate, MutableList<CheckIn>>()
        val emotionCounts = Emotion.entries.associateWith { 0 }.toMutableMap()
        var total = 0

        for (checkIn in checkIns) {
            val date = Instant.ofEpochMilli(checkIn.createdAt).atZone(zone).toLocalDate()
            if (date in startDate..today) {
                inWindowByDate.getOrPut(date) { mutableListOf() }.add(checkIn)
                emotionCounts[checkIn.emotion] = (emotionCounts[checkIn.emotion] ?: 0) + 1
                total++
            }
        }

        if (total == 0) {
            return MoodInsights.empty(windowDays = windowDays)
        }

        val series =
            (0 until windowDays).map { offset ->
                val date = startDate.plusDays(offset.toLong())
                val latest =
                    inWindowByDate[date]?.maxWithOrNull(
                        compareBy<CheckIn> { it.createdAt }.thenBy { it.id },
                    )
                MoodTrendPoint(date = date, emotion = latest?.emotion)
            }

        val hasDuplicateDay = inWindowByDate.values.any { it.size > 1 }
        val breakdown =
            Emotion.entries.map { emotion ->
                val count = emotionCounts[emotion] ?: 0
                EmotionBreakdownItem(
                    emotion = emotion,
                    count = count,
                    share = count.toFloat() / total.toFloat(),
                )
            }

        return MoodInsights(
            windowDays = windowDays,
            series = series,
            counts = emotionCounts.toMap(),
            breakdown = breakdown,
            totalCheckIns = total,
            hasDuplicateDay = hasDuplicateDay,
        )
    }
}
