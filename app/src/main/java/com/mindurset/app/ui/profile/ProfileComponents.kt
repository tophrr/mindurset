package com.mindurset.app.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindurset.app.R
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.ui.common.tint
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.MoodAnxious
import com.mindurset.app.ui.theme.MoodCalm
import com.mindurset.app.ui.theme.MoodHappy
import com.mindurset.app.ui.theme.MoodLow
import com.mindurset.app.ui.theme.MoodStressed
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.PaleBlue
import com.mindurset.app.ui.theme.SurfaceWhite
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** A monoline icon + label row for the profile menu and the log-out action. */
@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    labelColor: Color = NavyInk,
    iconTint: Color = NavyDeep,
    showChevron: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = iconTint,
        )
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = labelColor,
        )
        if (showChevron) {
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MutedGray,
            )
        }
    }
}

/**
 * Weekly mood view placeholder. Per the design brief the weekly graph is intentionally
 * not drawn: a soft PaleBlue panel shows a thin dashed baseline and a muted note.
 */
@Composable
fun MoodWeeklyPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PaleBlue),
    ) {
        val baselineColor = MutedGray.copy(alpha = 0.5f)
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height * 0.40f
            drawLine(
                color = baselineColor,
                start = Offset(22f, y),
                end = Offset(size.width - 22f, y),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f), 0f),
            )
        }
        Text(
            text = stringResource(R.string.profile_mood_week_empty),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 26.dp, end = 26.dp, bottom = 22.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The monthly recap: current-month title, S M T W T F S headers, a day badge for every
 * date (filled with the dominant emotion's colour when that day has a check-in) and a
 * five-colour legend underneath.
 */
@Composable
fun MoodMonthlyCalendar(
    year: Int,
    monthValue: Int,
    dayEmotions: Map<Int, Emotion>,
    modifier: Modifier = Modifier,
) {
    val month = YearMonth.of(year, monthValue)
    val weekdays = stringArrayResource(R.array.profile_weekday_short)
    val leadingBlanks = month.atDay(1).dayOfWeek.value % 7 // Sunday-first columns
    val cells = buildList {
        repeat(leadingBlanks) { add(null) }
        for (day in 1..month.lengthOfMonth()) add(day)
        while (size % 7 != 0) add(null)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = formatMonth(year, monthValue),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
        )
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { initial ->
                Text(
                    text = initial,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGray,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day == null) {
                            Spacer(Modifier.size(36.dp))
                        } else {
                            MoodDayBadge(day, dayEmotions[day])
                        }
                    }
                }
            }
        }
        MoodLegend(Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun MoodDayBadge(day: Int, emotion: Emotion?) {
    val fill = emotion?.tint() ?: Color.Transparent
    val textColor = if (emotion != null) SurfaceWhite else NavyInk
    Box(
        modifier = Modifier
            .padding(vertical = 3.dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (emotion != null) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}

/** Maps the five mood colours to the five emotion names. */
@Composable
fun MoodLegend(modifier: Modifier = Modifier) {
    val entries = listOf(
        R.string.emotion_happy to MoodHappy,
        R.string.emotion_calm to MoodCalm,
        R.string.emotion_low to MoodLow,
        R.string.emotion_anxious to MoodAnxious,
        R.string.emotion_stressed to MoodStressed,
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                row.forEach { (labelRes, color) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyInk,
                        )
                    }
                }
            }
        }
    }
}

/** First letter of a display name, for the avatar. Falls back to "?" for a blank name. */
fun initialOf(name: String): String =
    name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

private val MonthYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/** Formats a join timestamp as "April 2026" style text for "Member since". */
fun formatMonthYear(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(MonthYearFormatter)

/** Formats a year + month as "September 2026", the monthly calendar's title. */
fun formatMonth(year: Int, monthValue: Int): String =
    YearMonth.of(year, monthValue).atDay(1).format(MonthYearFormatter)

/** Hour-of-day picker backed by a real value, used by settings. */
@Composable
fun HourPickerDialog(
    initialHour: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var hour by remember { mutableFloatStateOf(initialHour.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_hour_picker_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.profile_time_value, hour.roundToInt()),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = hour,
                    onValueChange = { hour = it },
                    valueRange = 0f..23f,
                    steps = 22,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour.roundToInt()) }) {
                Text(stringResource(R.string.profile_hour_picker_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_hour_picker_cancel))
            }
        },
    )
}

/** Small glyph chip used for tile icons (kept for the therapy screen). */
@Composable
fun GlyphTile(
    glyph: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(background, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = foreground)
    }
}
