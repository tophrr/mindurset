package com.mindurset.app.ui.reflection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mindurset.app.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** The user's local calendar date for an epoch-millis timestamp. */
internal fun localDateOf(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

/** True when the timestamp falls on the current local day. */
internal fun isToday(epochMillis: Long): Boolean =
    localDateOf(epochMillis) == LocalDate.now(ZoneId.systemDefault())

/** Relative day label ("Today" / "Yesterday" / "N days ago") from string resources. */
@Composable
internal fun relativeDayLabel(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val day = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(day, LocalDate.now(zone)).coerceAtLeast(0L)
    return when (days) {
        0L -> stringResource(R.string.reflection_day_today)
        1L -> stringResource(R.string.reflection_day_yesterday)
        else -> stringResource(R.string.reflection_days_ago, days)
    }
}

/** Short day-month, e.g. "24 Sep". */
internal fun shortDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** Full timestamp for the entry detail footer, e.g. "24 Sep 2026, 20:15". */
internal fun fullDate(epochMillis: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())
        .format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** Month heading for the calendar, e.g. "September 2026". */
internal fun monthTitle(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

/** Locale-aware short weekday, e.g. "Mon". */
internal fun weekdayShort(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))

/**
 * Range label for the current (Monday–Sunday) week, e.g. "21 - 27 September 2026".
 * When the week straddles a month boundary, both months are shown.
 */
internal fun weekRangeLabel(today: LocalDate = LocalDate.now(ZoneId.systemDefault())): String {
    val locale = Locale.getDefault()
    val monday = today.with(DayOfWeek.MONDAY)
    val sunday = monday.plusDays(6)
    return if (monday.month == sunday.month) {
        val month = sunday.format(DateTimeFormatter.ofPattern("MMMM", locale))
        "${monday.dayOfMonth} - ${sunday.dayOfMonth} $month ${sunday.year}"
    } else {
        val startMonth = monday.format(DateTimeFormatter.ofPattern("MMM", locale))
        val endMonth = sunday.format(DateTimeFormatter.ofPattern("MMMM", locale))
        "${monday.dayOfMonth} $startMonth - ${sunday.dayOfMonth} $endMonth ${sunday.year}"
    }
}
