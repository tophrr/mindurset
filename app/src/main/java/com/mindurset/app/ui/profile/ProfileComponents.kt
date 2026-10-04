package com.mindurset.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindurset.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/** Rounded surface used to group rows on the profile / settings screens. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Column(Modifier.padding(vertical = 4.dp), content = content)
    }
}

/** A labelled row; clickable and chevron-marked only when an action is supplied. */
@Composable
fun MenuRow(
    label: String,
    onClick: (() -> Unit)? = null,
    value: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (onClick != null) {
            Text(
                text = "  \u203A",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Hour-of-day picker backed by a real value, used by edit profile and settings. */
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

private val MonthYearFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

/** Formats a join timestamp as "April 2026" style text for "Member since". */
fun formatMonthYear(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(MonthYearFormatter)

/** Small glyph chip used for tile icons (kept as text so no icon dependency is needed). */
@Composable
fun GlyphTile(
    glyph: String,
    background: androidx.compose.ui.graphics.Color,
    foreground: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier.background(background, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = glyph, color = foreground)
    }
}
