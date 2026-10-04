package com.mindurset.app.ui.reflection

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mindurset.app.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

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

