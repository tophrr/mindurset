@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mindurset.app.ui.reflection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.usecase.ValidateReflection
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.CircleIconButton
import com.mindurset.app.ui.common.EmptyState
import com.mindurset.app.ui.common.ErrorState
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.common.SegmentedTabs
import com.mindurset.app.ui.common.SectionTitle
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.CanvasBlue
import com.mindurset.app.ui.theme.CardShadow
import com.mindurset.app.ui.theme.DangerRed
import com.mindurset.app.ui.theme.Lavender
import com.mindurset.app.ui.theme.MoodAnxious
import com.mindurset.app.ui.theme.MoodCalm
import com.mindurset.app.ui.theme.MoodHappy
import com.mindurset.app.ui.theme.MoodLow
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.SurfaceWhite
import java.time.DayOfWeek
import java.time.LocalDate

// Daily Reflection CRUD module: reactive list (Room) with a Today's Entry card
// and Recent/Calendar views, a create/edit form with validation, a detail view,
// and delete behind a confirmation dialog. Room persistence is untouched.

private val EntryGreyBg = Color(0xFFF3F4F6)
private val EntryGreyInk = Color(0xFF5B6472)
private val EntryLavenderInk = Color(0xFF6D4FAF)
private val CalendarBlank = Color(0xFFE5E7EB)

private val CalendarPalette = listOf(NavyDeep, MoodAnxious, MoodCalm, MoodHappy, MoodLow)

@Composable
fun ReflectionListScreen(
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ReflectionListViewModel = viewModel {
        ReflectionListViewModel(container.journalRepository, container.sessionStore)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = CanvasBlue,
        topBar = {
            ReflectionTopBar(
                title = stringResource(R.string.reflection_title),
                left = {
                    CircleIconButton(
                        icon = AppIcons.Menu,
                        contentDescription = stringResource(R.string.reflection_menu),
                        onClick = onOpenDrawer,
                    )
                },
                right = {
                    CircleIconButton(
                        icon = AppIcons.Add,
                        contentDescription = stringResource(R.string.reflection_new_entry),
                        onClick = onNew,
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val current = state) {
                is ReflectionListViewModel.UiState.Loading ->
                    LoadingState(label = stringResource(R.string.loading))

                is ReflectionListViewModel.UiState.Error ->
                    ErrorState(
                        message = stringResource(reflectionErrorRes(current.messageKey)),
                        onRetry = viewModel::retry,
                    )

                is ReflectionListViewModel.UiState.Empty ->
                    ReflectionListContent(entries = emptyList(), onNew = onNew, onOpen = onOpen)

                is ReflectionListViewModel.UiState.Ready ->
                    ReflectionListContent(entries = current.entries, onNew = onNew, onOpen = onOpen)
            }
        }
    }
}

@Composable
private fun ReflectionListContent(
    entries: List<JournalEntry>,
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val todayEntry = entries.firstOrNull { isToday(it.createdAt) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 4.dp,
            bottom = FloatingNavSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TodayEntryCard(
                onOpen = {
                    if (todayEntry != null) onOpen(todayEntry.id) else onNew()
                },
            )
        }

        item {
            SegmentedTabs(
                options = listOf(
                    stringResource(R.string.reflection_tab_recent),
                    stringResource(R.string.reflection_tab_calendar),
                ),
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
            )
        }

        if (selectedTab == 0) {
            item {
                SectionTitle(stringResource(R.string.reflection_this_week, weekRangeLabel()))
            }
            if (entries.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.reflection_empty_title),
                        body = stringResource(R.string.reflection_empty_body),
                        actionLabel = stringResource(R.string.reflection_new_entry),
                        onAction = onNew,
                    )
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    ReflectionRow(
                        entry = entry,
                        index = entries.indexOf(entry),
                        onClick = { onOpen(entry.id) },
                    )
                }
            }
        } else {
            item {
                ReflectionCalendar(entries = entries, onOpen = onOpen)
            }
        }
    }
}

@Composable
private fun TodayEntryCard(onOpen: () -> Unit) {
    AppCard {
        Text(
            text = stringResource(R.string.reflection_today_entry_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
        )
        Text(
            text = stringResource(R.string.reflection_today_entry_body),
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
        Button(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 14.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyDeep,
                contentColor = SurfaceWhite,
            ),
        ) {
            Text(
                text = stringResource(R.string.reflection_today_entry_button),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ReflectionRow(entry: JournalEntry, index: Int, onClick: () -> Unit) {
    val lavender = index % 2 == 1
    val background = if (lavender) Lavender else EntryGreyBg
    val bodyInk = if (lavender) EntryLavenderInk else EntryGreyInk
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(shape)
            .background(background)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = relativeDayLabel(entry.createdAt),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = NavyInk,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = shortDate(entry.createdAt),
                style = MaterialTheme.typography.labelMedium,
                color = bodyInk,
            )
        }
        Text(
            text = stringResource(R.string.reflection_row_challenge, entry.challenge),
            style = MaterialTheme.typography.bodyMedium,
            color = bodyInk,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (entry.reframe.isNotBlank()) {
            Text(
                text = stringResource(R.string.reflection_row_reframe, entry.reframe),
                style = MaterialTheme.typography.bodyMedium,
                color = bodyInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ReflectionCalendar(entries: List<JournalEntry>, onOpen: (String) -> Unit) {
    val today = LocalDate.now()
    val first = today.withDayOfMonth(1)
    val daysInMonth = first.lengthOfMonth()
    val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val trailing = (7 - (leading + daysInMonth) % 7) % 7
    val byDay: Map<LocalDate, JournalEntry> =
        entries.groupBy { localDateOf(it.createdAt) }.mapValues { it.value.first() }
    val weekdays = (0..6).map { weekdayShort(first.with(DayOfWeek.MONDAY).plusDays(it.toLong())) }

    val cells = mutableListOf<LocalDate?>()
    repeat(leading) { cells.add(null) }
    for (day in 1..daysInMonth) cells.add(first.withDayOfMonth(day))
    repeat(trailing) { cells.add(null) }

    AppCard {
        Text(
            text = monthTitle(first),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
            modifier = Modifier.padding(bottom = 10.dp),
        )
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MutedGray,
                    textAlign = TextAlign.Center,
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            date == null -> Box(
                                Modifier.size(36.dp).clip(CircleShape).background(CalendarBlank),
                            )

                            byDay[date] != null -> {
                                val entry = byDay.getValue(date)
                                val color = CalendarPalette[date.dayOfYear % CalendarPalette.size]
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { onOpen(entry.id) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SurfaceWhite,
                                    )
                                }
                            }

                            else -> Box(
                                Modifier.size(36.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = NavyInk,
                                )
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            CalendarPalette.forEach { color ->
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            }
            Text(
                text = stringResource(R.string.reflection_calendar_legend),
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
            )
        }
    }
}

@Composable
fun ReflectionFormScreen(
    entryId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ReflectionFormViewModel = viewModel {
        ReflectionFormViewModel(entryId, container.journalRepository, container.sessionStore)
    }

    Scaffold(
        containerColor = CanvasBlue,
        topBar = {
            ReflectionTopBar(
                title = stringResource(
                    if (viewModel.isEdit) R.string.reflection_form_title_edit
                    else R.string.reflection_form_title_new,
                ),
                left = {
                    CircleIconButton(
                        icon = AppIcons.ArrowBack,
                        contentDescription = stringResource(R.string.reflection_back),
                        onClick = onBack,
                    )
                },
                right = {},
            )
        },
    ) { innerPadding ->
        when {
            viewModel.isLoading -> LoadingState(label = stringResource(R.string.loading))

            viewModel.notFound -> ErrorState(
                message = stringResource(R.string.reflection_detail_not_found),
            )

            else -> ReflectionFormBody(
                viewModel = viewModel,
                contentPadding = innerPadding,
                onSaved = onSaved,
            )
        }
    }
}

@Composable
fun ReflectionDetailScreen(
    entryId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ReflectionDetailViewModel = viewModel {
        ReflectionDetailViewModel(entryId, container.journalRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = CanvasBlue,
        topBar = {
            ReflectionTopBar(
                title = stringResource(R.string.reflection_detail_title),
                left = {
                    CircleIconButton(
                        icon = AppIcons.ArrowBack,
                        contentDescription = stringResource(R.string.reflection_back),
                        onClick = onBack,
                    )
                },
                right = {
                    CircleIconButton(
                        icon = AppIcons.Edit,
                        contentDescription = stringResource(R.string.reflection_action_edit),
                        onClick = onEdit,
                    )
                    CircleIconButton(
                        icon = AppIcons.Trash,
                        contentDescription = stringResource(R.string.reflection_action_delete),
                        onClick = { showDeleteDialog = true },
                    )
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val current = state) {
                is ReflectionDetailViewModel.UiState.Loading ->
                    LoadingState(label = stringResource(R.string.loading))

                is ReflectionDetailViewModel.UiState.NotFound ->
                    EmptyState(
                        title = stringResource(R.string.reflection_detail_not_found),
                        body = stringResource(R.string.reflection_detail_not_found_body),
                        actionLabel = stringResource(R.string.back),
                        onAction = onBack,
                    )

                is ReflectionDetailViewModel.UiState.Error ->
                    ErrorState(message = stringResource(reflectionErrorRes(current.messageKey)))

                is ReflectionDetailViewModel.UiState.Ready ->
                    ReflectionDetailBody(entry = current.entry)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.reflection_delete_confirm_title)) },
            text = { Text(stringResource(R.string.reflection_delete_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(onBack)
                    },
                ) {
                    Text(stringResource(R.string.delete), color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ReflectionFormBody(
    viewModel: ReflectionFormViewModel,
    contentPadding: PaddingValues,
    onSaved: () -> Unit,
) {
    val errors = viewModel.validation.errors
    val challengeError = errors[ValidateReflection.Field.CHALLENGE]
    val reframeError = errors[ValidateReflection.Field.REFRAME]
    val gratitudeError = errors[ValidateReflection.Field.GRATITUDE]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = FloatingNavSpace),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.reflection_form_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )

        AppCard {
            OutlinedTextField(
                value = viewModel.challenge,
                onValueChange = viewModel::onChallengeChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.reflection_field_challenge)) },
                placeholder = { Text(stringResource(R.string.reflection_field_challenge_hint)) },
                isError = viewModel.challengeTouched && challengeError != null,
                supportingText = fieldSupportingText(viewModel.challengeTouched, challengeError),
                minLines = 3,
            )

            OutlinedTextField(
                value = viewModel.reframe,
                onValueChange = viewModel::onReframeChange,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                label = { Text(stringResource(R.string.reflection_field_reframe)) },
                placeholder = { Text(stringResource(R.string.reflection_field_reframe_hint)) },
                isError = viewModel.reframeTouched && reframeError != null,
                supportingText = fieldSupportingText(viewModel.reframeTouched, reframeError),
                minLines = 2,
            )

            OutlinedTextField(
                value = viewModel.gratitude,
                onValueChange = viewModel::onGratitudeChange,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                label = { Text(stringResource(R.string.reflection_field_gratitude)) },
                placeholder = { Text(stringResource(R.string.reflection_field_gratitude_hint)) },
                isError = viewModel.gratitudeTouched && gratitudeError != null,
                supportingText = fieldSupportingText(viewModel.gratitudeTouched, gratitudeError),
                minLines = 2,
            )
        }

        Button(
            onClick = {
                if (viewModel.isValid) viewModel.save(onSaved) else viewModel.markAllTouched()
            },
            enabled = !viewModel.isSaving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NavyDeep,
                contentColor = SurfaceWhite,
            ),
        ) {
            Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReflectionDetailBody(entry: JournalEntry) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = FloatingNavSpace),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = relativeDayLabel(entry.createdAt),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
        )
        ReflectionFieldCard(
            label = stringResource(R.string.reflection_detail_section_challenge),
            value = entry.challenge,
        )
        if (entry.reframe.isNotBlank()) {
            ReflectionFieldCard(
                label = stringResource(R.string.reflection_detail_section_reframe),
                value = entry.reframe,
            )
        }
        if (entry.gratitude.isNotBlank()) {
            ReflectionFieldCard(
                label = stringResource(R.string.reflection_detail_section_gratitude),
                value = entry.gratitude,
            )
        }
        Text(
            text = stringResource(R.string.reflection_detail_created, fullDate(entry.createdAt)),
            style = MaterialTheme.typography.labelSmall,
            color = MutedGray,
        )
        if (entry.updatedAt != entry.createdAt) {
            Text(
                text = stringResource(R.string.reflection_detail_updated, fullDate(entry.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MutedGray,
            )
        }
    }
}

@Composable
private fun ReflectionFieldCard(label: String, value: String) {
    AppCard {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MutedGray,
        )
        Text(
            text = value,
            modifier = Modifier.padding(top = 6.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = NavyInk,
        )
    }
}

/** Shared top bar: a monoline circle on the left, the title centred, actions on the right. */
@Composable
private fun ReflectionTopBar(
    title: String,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.CenterStart,
            ) { left() }
            Box(
                modifier = Modifier.height(42.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) { right() }
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = NavyInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun fieldSupportingText(show: Boolean, errorKey: String?): (@Composable () -> Unit)? =
    if (show && errorKey != null) {
        {
            Text(
                text = stringResource(reflectionErrorRes(errorKey)),
                color = MaterialTheme.colorScheme.error,
            )
        }
    } else {
        null
    }
