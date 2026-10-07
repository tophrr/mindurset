package com.mindurset.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.usecase.DashboardSummary
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.CircleIconButton
import com.mindurset.app.ui.common.EmptyState
import com.mindurset.app.ui.common.ErrorState
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.common.SectionTitle
import com.mindurset.app.ui.common.emotionLabel
import com.mindurset.app.ui.common.tint
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.PaleBlue
import java.text.DateFormat
import java.util.Date

/**
 * Home dashboard. Reads real Room data through [DashboardViewModel] (StateFlow) and
 * renders loading / empty / content / error. Every value updates live because the
 * ViewModel observes Room Flows, so writing a check-in or reflection refreshes it
 * without a restart. The layout mirrors the UI mockup: monoline top-bar icons, a
 * greeting block, the daily check-in card, two shortcut cards, a recommendation
 * checklist and the latest reflections - all on the CanvasBlue background.
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
        when (val current = state) {
            is DashboardUiState.Loading ->
                LoadingState(label = stringResource(R.string.dashboard_loading))

            is DashboardUiState.Error ->
                ErrorState(
                    message = current.message
                        ?: stringResource(R.string.dashboard_error_body),
                )

            // Nothing recorded yet: show the whole dashboard with zero counts rather than
            // replacing it with a placeholder page.
            is DashboardUiState.Empty ->
                DashboardBody(
                    summary = DashboardSummary.EMPTY,
                    onOpenCheckIn = onOpenCheckIn,
                    onOpenReflection = onOpenReflection,
                    onOpenCompanion = onOpenCompanion,
                    onOpenTherapy = onOpenTherapy,
                    onOpenDrawer = onOpenDrawer,
                )

            is DashboardUiState.Content ->
                DashboardBody(
                    summary = current.summary,
                    onOpenCheckIn = onOpenCheckIn,
                    onOpenReflection = onOpenReflection,
                    onOpenCompanion = onOpenCompanion,
                    onOpenTherapy = onOpenTherapy,
                    onOpenDrawer = onOpenDrawer,
                )
        }
    }
}

@Composable
private fun DashboardBody(
    summary: DashboardSummary,
    onOpenCheckIn: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenTherapy: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = FloatingNavSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { DashboardHeader(displayName = summary.displayName, onOpenDrawer = onOpenDrawer) }
        item { StatChips(summary = summary) }
        item { DailyCheckInCard(today = summary.todayCheckIn, onOpenCheckIn = onOpenCheckIn) }
        item { ShortcutRow(onOpenCompanion = onOpenCompanion, onOpenReflection = onOpenReflection) }
        item { RecommendCard() }
        item {
            ReflectionsHeader(onSeeAll = onOpenReflection)
        }
        if (summary.latestReflections.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.dashboard_reflections_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
        } else {
            items(items = summary.latestReflections, key = { it.id }) { entry ->
                ReflectionCard(entry = entry, onClick = onOpenReflection)
            }
        }
        item { TherapyShortcut(onOpenTherapy = onOpenTherapy) }
    }
}

/* ---------------------------------------------------------------- header */

@Composable
private fun DashboardHeader(displayName: String, onOpenDrawer: () -> Unit) {
    val menuLabel = stringResource(R.string.menu)
    val bellLabel = stringResource(R.string.dashboard_notifications_cd)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = AppIcons.Menu,
                contentDescription = menuLabel,
                onClick = onOpenDrawer,
            )
            Spacer(modifier = Modifier.weight(1f))
            CircleIconButton(
                icon = AppIcons.Bell,
                contentDescription = bellLabel,
                onClick = {},
            )
        }
        GreetingBlock(displayName = displayName)
    }
}

@Composable
private fun GreetingBlock(displayName: String) {
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
        displayName.trim().take(1).uppercase().ifBlank { "\u2022" }
    }
    val avatarCd = stringResource(R.string.dashboard_avatar_cd)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(PaleBlue)
                .semantics { contentDescription = avatarCd },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
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
                color = MutedGray,
            )
        }
    }
}

/* ------------------------------------------------------------- stat chips */

@Composable
private fun StatChips(summary: DashboardSummary) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatChip(
            count = summary.checkInCount,
            label = stringResource(R.string.dashboard_stats_checkins),
        )
        StatChip(
            count = summary.journalCount,
            label = stringResource(R.string.dashboard_stats_journal),
        )
    }
}

@Composable
private fun StatChip(count: Int, label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = PaleBlue,
    ) {
        Text(
            text = stringResource(R.string.dashboard_stat_chip, count, label),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/* ---------------------------------------------------------- check-in card */

@Composable
private fun DailyCheckInCard(today: CheckIn?, onOpenCheckIn: () -> Unit) {
    // The five moods stay on screen in both states; when a check-in already exists the
    // logged emotion simply starts out selected (see Daily check in mood 2.0).
    var selected by remember(today?.emotion) { mutableStateOf(today?.emotion) }

    AppCard {
        Text(
            text = stringResource(R.string.dashboard_checkin_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = stringResource(R.string.dashboard_checkin_prompt),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.size(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Emotion.entries.forEach { option ->
                MoodOption(
                    emotion = option,
                    selected = option == selected,
                    onClick = { selected = option },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (today != null) {
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.dashboard_checkin_logged, emotionLabel(today.emotion)) +
                    "  ·  " + stringResource(R.string.dashboard_checkin_intensity, today.intensity),
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
            )
        }
        Spacer(modifier = Modifier.size(18.dp))
        Button(
            onClick = onOpenCheckIn,
            enabled = selected != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text(
                text = stringResource(
                    if (today == null) R.string.dashboard_checkin_cta else R.string.dashboard_checkin_update,
                ),
            )
        }
    }
}

/** One mood option: its own monoline face, its own colour, name and hint underneath. */
@Composable
private fun MoodOption(
    emotion: Emotion,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = emotion.tint()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else Color.Transparent,
        border = if (selected) BorderStroke(1.5.dp, accent) else null,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = moodIcon(emotion),
                contentDescription = emotionLabel(emotion),
                modifier = Modifier.size(30.dp),
                tint = if (selected) accent else NavyInk,
            )
            Text(
                text = emotionLabel(emotion),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) accent else MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = emotionHint(emotion),
                // The longest hint ("Overwhelmed") has to fit a fifth of the card width:
                // 8 sp fits on one line, and maxLines 2 is only a safety net so a longer
                // translation wraps instead of being clipped.
                fontSize = 8.sp,
                lineHeight = 11.sp,
                color = if (selected) accent else MutedGray,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

private fun moodIcon(emotion: Emotion): ImageVector = when (emotion) {
    Emotion.HAPPY -> AppIcons.FaceHappy
    Emotion.CALM -> AppIcons.FaceCalm
    Emotion.LOW -> AppIcons.FaceLow
    Emotion.ANXIOUS -> AppIcons.FaceAnxious
    Emotion.STRESSED -> AppIcons.FaceStressed
}

/* ------------------------------------------------------------- shortcuts */

@Composable
private fun ShortcutRow(onOpenCompanion: () -> Unit, onOpenReflection: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShortcutCard(
            icon = AppIcons.Sparkles,
            title = stringResource(R.string.dashboard_shortcut_companion_title),
            body = stringResource(R.string.dashboard_shortcut_companion_body),
            cta = stringResource(R.string.dashboard_shortcut_companion_cta),
            onClick = onOpenCompanion,
            modifier = Modifier.weight(1f),
        )
        ShortcutCard(
            icon = AppIcons.Book,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    cta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppCard(modifier = modifier, contentPadding = PaddingValues(16.dp)) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(PaleBlue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.size(2.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.size(12.dp))
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = cta)
        }
    }
}

@Composable
private fun TherapyShortcut(onOpenTherapy: () -> Unit) {
    AppCard(onClick = onOpenTherapy) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PaleBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Therapy,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.dashboard_shortcut_therapy_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.dashboard_shortcut_therapy_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedGray,
                    maxLines = 2,
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

/* ------------------------------------------------------------ recommend */

private val RecommendLabels = listOf(
    R.string.dashboard_recommend_1,
    R.string.dashboard_recommend_2,
    R.string.dashboard_recommend_3,
    R.string.dashboard_recommend_4,
    R.string.dashboard_recommend_5,
)

@Composable
private fun RecommendCard() {
    // Local-only: the checklist is a gentle nudge, not persisted data.
    val checked = remember { mutableStateListOf(false, false, false, false, false) }
    AppCard {
        SectionTitle(text = stringResource(R.string.dashboard_recommend_title))
        Spacer(modifier = Modifier.size(8.dp))
        RecommendLabels.forEachIndexed { index, labelRes ->
            RecommendRow(
                label = stringResource(labelRes),
                checked = checked[index],
                onToggle = { checked[index] = !checked[index] },
            )
        }
    }
}

@Composable
private fun RecommendRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PaleBlue)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = if (checked) tint else MaterialTheme.colorScheme.surface,
            border = if (checked) null else BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
            onClick = onToggle,
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
    Spacer(modifier = Modifier.size(8.dp))
}

/* ----------------------------------------------------------- reflections */

@Composable
private fun ReflectionsHeader(onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionTitle(text = stringResource(R.string.dashboard_reflections_title))
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onSeeAll) {
            Text(
                text = stringResource(R.string.dashboard_reflections_seeall),
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

    AppCard(onClick = onClick, contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
            )
        }
        if (snippet != null) {
            Spacer(modifier = Modifier.size(6.dp))
            Text(
                text = snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/* ---------------------------------------------------------------- helpers */

@Composable
private fun emotionHint(emotion: Emotion): String = stringResource(
    when (emotion) {
        Emotion.HAPPY -> R.string.checkin_hint_happy
        Emotion.CALM -> R.string.checkin_hint_calm
        Emotion.LOW -> R.string.checkin_hint_low
        Emotion.ANXIOUS -> R.string.checkin_hint_anxious
        Emotion.STRESSED -> R.string.checkin_hint_stressed
    },
)
