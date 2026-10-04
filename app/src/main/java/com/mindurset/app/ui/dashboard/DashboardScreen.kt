package com.mindurset.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.usecase.DashboardSummary
import com.mindurset.app.ui.common.EmptyState
import com.mindurset.app.ui.common.ErrorState
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.theme.MoodAnxious
import com.mindurset.app.ui.theme.MoodCalm
import com.mindurset.app.ui.theme.MoodHappy
import com.mindurset.app.ui.theme.MoodLow
import com.mindurset.app.ui.theme.MoodStressed
import java.text.DateFormat
import java.util.Date

/**
 * Home dashboard. Reads real Room data through [DashboardViewModel] (StateFlow) and
 * renders loading / empty / content / error. Every value updates live because the
 * ViewModel observes Room Flows, so writing a check-in or reflection refreshes it
 * without a restart.
 */
@Composable
fun DashboardScreen(
    onOpenCheckIn: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenTherapy: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: DashboardViewModel = viewModel {
        DashboardViewModel(
            container.journalRepository,
            container.checkInRepository,
            container.profileRepository,
            container.sessionStore,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DashboardTopBar(onOpenDrawer = onOpenDrawer)
        Box(modifier = Modifier.fillMaxSize()) {
            when (val current = state) {
                is DashboardUiState.Loading ->
                    LoadingState(label = stringResource(R.string.dashboard_loading))

                is DashboardUiState.Error ->
                    ErrorState(
                        message = current.message
                            ?: stringResource(R.string.dashboard_error_body),
                    )

                is DashboardUiState.Empty ->
                    EmptyState(
                        title = stringResource(R.string.dashboard_empty_title),
                        body = stringResource(R.string.dashboard_empty_body),
                        actionLabel = stringResource(R.string.dashboard_empty_action),
                        onAction = onOpenCheckIn,
                    )

                is DashboardUiState.Content ->
                    DashboardBody(
                        summary = current.summary,
                        onOpenCheckIn = onOpenCheckIn,
                        onOpenReflection = onOpenReflection,
                        onOpenCompanion = onOpenCompanion,
                        onOpenTherapy = onOpenTherapy,
                    )
            }
        }
    }
}

@Composable
private fun DashboardTopBar(onOpenDrawer: () -> Unit) {
    val menuLabel = stringResource(R.string.menu)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onOpenDrawer,
            modifier = Modifier.semantics { contentDescription = menuLabel },
        ) {
            Text(
                text = "\u2630",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.nav_home),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.size(48.dp))
    }
}

@Composable
private fun DashboardBody(
    summary: DashboardSummary,
    onOpenCheckIn: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenTherapy: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { GreetingHeader(displayName = summary.displayName) }
        item { StatsRow(summary = summary) }
        item { CheckInCard(today = summary.todayCheckIn, onOpenCheckIn = onOpenCheckIn) }
        item { ShortcutRow(onOpenCompanion = onOpenCompanion, onOpenReflection = onOpenReflection) }
        item { TherapyShortcut(onOpenTherapy = onOpenTherapy) }
        item {
            Text(
                text = stringResource(R.string.dashboard_reflections_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (summary.latestReflections.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.dashboard_reflections_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(items = summary.latestReflections, key = { it.id }) { entry ->
                ReflectionCard(entry = entry, onClick = onOpenReflection)
            }
        }
    }
}

@Composable
private fun GreetingHeader(displayName: String) {
    val hour = remember { java.time.LocalTime.now().hour }
    val greeting = if (displayName.isBlank()) {
        stringResource(R.string.dashboard_greeting_generic)
    } else {
        val pattern = when {
            hour < 12 -> R.string.dashboard_greeting_morning
            hour < 18 -> R.string.dashboard_greeting_afternoon
            else -> R.string.dashboard_greeting_evening
        }
        stringResource(pattern, displayName)
    }
    val initial = remember(displayName) {
        displayName.trim().take(1).uppercase().ifBlank { "\u263A" }
    }
    val avatarCd = stringResource(R.string.dashboard_avatar_cd)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .semantics { contentDescription = avatarCd },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.dashboard_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsRow(summary: DashboardSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            value = summary.journalCount,
            label = stringResource(R.string.dashboard_stats_journal),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value = summary.checkInCount,
            label = stringResource(R.string.dashboard_stats_checkins),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(value: Int, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheckInCard(today: CheckIn?, onOpenCheckIn: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.dashboard_checkin_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (today == null) {
                Text(
                    text = stringResource(R.string.dashboard_checkin_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenCheckIn, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.dashboard_checkin_cta))
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(moodColor(today.emotion)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(
                                R.string.dashboard_checkin_logged,
                                emotionText(today.emotion),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(
                                R.string.dashboard_checkin_intensity,
                                today.intensity,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                OutlinedButton(onClick = onOpenCheckIn, modifier = Modifier.fillMaxWidth()) {
                    Text(text = stringResource(R.string.dashboard_checkin_update))
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(onOpenCompanion: () -> Unit, onOpenReflection: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShortcutCard(
            title = stringResource(R.string.dashboard_shortcut_companion_title),
            body = stringResource(R.string.dashboard_shortcut_companion_body),
            cta = stringResource(R.string.dashboard_shortcut_companion_cta),
            onClick = onOpenCompanion,
            modifier = Modifier.weight(1f),
        )
        ShortcutCard(
            title = stringResource(R.string.dashboard_shortcut_reflection_title),
            body = stringResource(R.string.dashboard_shortcut_reflection_body),
            cta = stringResource(R.string.dashboard_shortcut_reflection_cta),
            onClick = onOpenReflection,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ShortcutCard(
    title: String,
    body: String,
    cta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
                Text(text = cta)
            }
        }
    }
}

@Composable
private fun TherapyShortcut(onOpenTherapy: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTherapy),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.dashboard_shortcut_therapy_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.dashboard_shortcut_therapy_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.dashboard_shortcut_therapy_cta),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ReflectionCard(entry: JournalEntry, onClick: () -> Unit) {
    val date = remember(entry.createdAt) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(entry.createdAt))
    }
    val title = entry.challenge
        .ifBlank { entry.reframe }
        .ifBlank { stringResource(R.string.dashboard_reflection_untitled) }
    val snippet = listOf(entry.reframe, entry.gratitude)
        .firstOrNull { it.isNotBlank() && it != title }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (snippet != null) {
                Text(
                    text = snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun emotionText(emotion: Emotion): String = stringResource(
    when (emotion) {
        Emotion.HAPPY -> R.string.emotion_happy
        Emotion.CALM -> R.string.emotion_calm
        Emotion.LOW -> R.string.emotion_low
        Emotion.ANXIOUS -> R.string.emotion_anxious
        Emotion.STRESSED -> R.string.emotion_stressed
    },
)

private fun moodColor(emotion: Emotion): Color = when (emotion) {
    Emotion.HAPPY -> MoodHappy
    Emotion.CALM -> MoodCalm
    Emotion.LOW -> MoodLow
    Emotion.ANXIOUS -> MoodAnxious
    Emotion.STRESSED -> MoodStressed
}
