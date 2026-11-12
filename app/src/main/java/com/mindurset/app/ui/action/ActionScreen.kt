package com.mindurset.app.ui.action

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.common.SectionTitle
import com.mindurset.app.ui.profile.MindTopBar
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk

@Composable
fun ActionScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val actionViewModel: ActionViewModel = viewModel {
        ActionViewModel(
            actions = container.microActionRepository,
            sessionStore = container.sessionStore,
        )
    }
    val state by actionViewModel.uiState.collectAsStateWithLifecycle()
    val editorError by actionViewModel.editorError.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MindTopBar(
                title = stringResource(R.string.action_title),
                onBack = onBack,
                navDescription = stringResource(R.string.action_back_cd),
            )
        },
    ) { innerPadding ->
        when (val current = state) {
            ActionUiState.Loading ->
                LoadingState(
                    modifier = Modifier.padding(innerPadding),
                    label = stringResource(R.string.action_loading),
                )

            ActionUiState.Empty ->
                ActionEmptyContent(
                    contentPadding = innerPadding,
                    editorError = editorError,
                    onClearError = actionViewModel::clearEditorError,
                    onAdd = actionViewModel::add,
                )

            is ActionUiState.Populated ->
                ActionListContent(
                    state = current,
                    contentPadding = innerPadding,
                    editorError = editorError,
                    onClearError = actionViewModel::clearEditorError,
                    onAdd = actionViewModel::add,
                    onEdit = actionViewModel::edit,
                    onComplete = actionViewModel::complete,
                    onSnooze = actionViewModel::snooze,
                )

            ActionUiState.Error ->
                ActionErrorContent(
                    modifier = Modifier.padding(innerPadding),
                    onRetry = actionViewModel::retry,
                )
        }
    }
}

@Composable
private fun ActionEmptyContent(
    contentPadding: PaddingValues,
    editorError: ActionEditorError?,
    onClearError: () -> Unit,
    onAdd: (String, Int) -> Boolean,
) {
    var showEditor by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppCard {
            Text(
                text = stringResource(R.string.action_empty_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MutedGray,
            )
            Button(
                onClick = { showEditor = true },
                modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 12.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(stringResource(R.string.action_add))
            }
        }
    }
    if (showEditor) {
        ActionEditorDialog(
            error = editorError,
            onClearError = onClearError,
            onDismiss = {
                showEditor = false
                onClearError()
            },
            onSave = { title, minutes ->
                val saved = onAdd(title, minutes)
                if (saved) showEditor = false
                saved
            },
        )
    }
}

@Composable
private fun ActionListContent(
    state: ActionUiState.Populated,
    contentPadding: PaddingValues,
    editorError: ActionEditorError?,
    onClearError: () -> Unit,
    onAdd: (String, Int) -> Boolean,
    onEdit: (String, String, Int) -> Boolean,
    onComplete: (String) -> Unit,
    onSnooze: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<MicroAction?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding =
        PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 8.dp,
            end = 20.dp,
            bottom = FloatingNavSpace,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(stringResource(R.string.action_add), modifier = Modifier.padding(start = 8.dp))
            }
        }
        if (state.today.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.action_today_group)) }
            items(state.today, key = { it.id }) { action ->
                ActionCard(
                    action = action,
                    onEdit = { editingAction = action },
                    onComplete = { onComplete(action.id) },
                    onSnooze = { onSnooze(action.id) },
                )
            }
        }
        if (state.doneToday.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.action_done_group)) }
            items(state.doneToday, key = { it.id }) { action ->
                ActionCard(
                    action = action,
                    onEdit = { editingAction = action },
                    onComplete = {},
                    onSnooze = {},
                )
            }
        }
        if (state.tomorrow.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.action_tomorrow_group)) }
            items(state.tomorrow, key = { it.id }) { action ->
                ActionCard(
                    action = action,
                    onEdit = { editingAction = action },
                    onComplete = { onComplete(action.id) },
                    onSnooze = {},
                )
            }
        }
    }
    if (showAddDialog) {
        ActionEditorDialog(
            error = editorError,
            onClearError = onClearError,
            onDismiss = {
                showAddDialog = false
                onClearError()
            },
            onSave = { title, minutes ->
                val saved = onAdd(title, minutes)
                if (saved) showAddDialog = false
                saved
            },
        )
    }
    editingAction?.let { action ->
        ActionEditorDialog(
            action = action,
            error = editorError,
            onClearError = onClearError,
            onDismiss = {
                editingAction = null
                onClearError()
            },
            onSave = { title, minutes ->
                val saved = onEdit(action.id, title, minutes)
                if (saved) editingAction = null
                saved
            },
        )
    }
}

@Composable
private fun ActionCard(action: MicroAction, onEdit: () -> Unit, onComplete: () -> Unit, onSnooze: () -> Unit) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, style = MaterialTheme.typography.titleMedium, color = NavyInk)
                Text(
                    text = stringResource(R.string.action_minutes, action.minutes),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                )
            }
            IconButton(
                onClick = onComplete,
                enabled = !action.done,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    AppIcons.Check,
                    contentDescription = stringResource(R.string.action_complete_cd, action.title),
                    tint = if (action.done) MutedGray else NavyDeep,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(
                    AppIcons.Edit,
                    contentDescription = stringResource(R.string.action_edit_cd, action.title),
                    tint = NavyDeep,
                )
            }
            if (!action.done && action.snoozedUntil == null) {
                IconButton(onClick = onSnooze, modifier = Modifier.size(48.dp)) {
                    Icon(
                        AppIcons.Clock,
                        contentDescription = stringResource(R.string.action_snooze_cd, action.title),
                        tint = NavyDeep,
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionEditorDialog(
    action: MicroAction? = null,
    error: ActionEditorError?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Boolean,
) {
    var title by remember(action?.id) { mutableStateOf(action?.title.orEmpty()) }
    var minutes by remember(action?.id) { mutableStateOf(action?.minutes?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (action == null) R.string.action_dialog_add_title else R.string.action_dialog_edit_title,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        onClearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.action_title_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { value ->
                        minutes = value.filter(Char::isDigit)
                        onClearError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.action_minutes_label)) },
                    singleLine = true,
                )
                error?.let { current ->
                    Text(
                        text =
                        stringResource(
                            when (current) {
                                ActionEditorError.TitleRequired -> R.string.action_error_title
                                ActionEditorError.MinutesOutOfRange -> R.string.action_error_minutes
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(title, minutes.toIntOrNull() ?: 0)
                },
                modifier = Modifier.height(48.dp),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun ActionErrorContent(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.action_error), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry, modifier = Modifier.height(48.dp)) {
            Text(stringResource(R.string.action_retry))
        }
    }
}
