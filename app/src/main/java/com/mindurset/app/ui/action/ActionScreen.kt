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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.common.SectionTitle
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import java.time.LocalDate

@Composable
fun ActionScreen(onBack: () -> Unit) {
    val sampleActions = listOf(
        ActionItem("walk", stringResource(R.string.action_sample_walk), 10, LocalDate.now()),
        ActionItem("water", stringResource(R.string.action_sample_water), 2, LocalDate.now()),
        ActionItem("breathe", stringResource(R.string.action_sample_breathe), 1, LocalDate.now()),
    )
    val actionViewModel: ActionViewModel = viewModel {
        ActionViewModel(FakeActionRepository(sampleActions))
    }
    val state by actionViewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = AppIcons.ArrowBack,
                        contentDescription = stringResource(R.string.action_back_cd),
                        tint = NavyInk,
                    )
                }
                Text(
                    text = stringResource(R.string.action_title),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyInk,
                )
            }
        },
    ) { innerPadding ->
        when (val current = state) {
            ActionUiState.Loading -> LoadingState(
                modifier = Modifier.padding(innerPadding),
                label = stringResource(R.string.action_loading),
            )

            ActionUiState.Empty -> ActionEmptyContent(
                onAdd = actionViewModel::addAction,
                contentPadding = innerPadding,
            )

            is ActionUiState.Populated -> ActionListContent(
                state = current,
                contentPadding = innerPadding,
                onAdd = actionViewModel::addAction,
                onEdit = actionViewModel::editAction,
                onComplete = actionViewModel::completeAction,
                onSnooze = actionViewModel::snoozeAction,
            )

            ActionUiState.Error -> ActionErrorContent(
                modifier = Modifier.padding(innerPadding),
                onRetry = actionViewModel::retry,
            )
        }
    }
}

@Composable
private fun ActionEmptyContent(onAdd: (String, Int) -> Unit, contentPadding: PaddingValues) {
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
        ActionEditorDialog(onDismiss = { showEditor = false }, onSave = { title, minutes ->
            onAdd(title, minutes)
            showEditor = false
        })
    }
}

@Composable
private fun ActionListContent(
    state: ActionUiState.Populated,
    contentPadding: PaddingValues,
    onAdd: (String, Int) -> Unit,
    onEdit: (String, String, Int) -> Unit,
    onComplete: (String) -> Unit,
    onSnooze: (String) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAction by remember { mutableStateOf<ActionItem?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
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
                ActionCard(action, done = false, onEdit = {
                    editingAction = action
                }, onComplete = { onComplete(action.id) }, onSnooze = { onSnooze(action.id) })
            }
        }
        if (state.doneToday.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.action_done_group)) }
            items(state.doneToday, key = { it.id }) { action ->
                ActionCard(action, done = true, onEdit = { editingAction = action }, onComplete = {}, onSnooze = {})
            }
        }
        if (state.tomorrow.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.action_tomorrow_group)) }
            items(state.tomorrow, key = { it.id }) { action ->
                ActionCard(action, done = false, onEdit = {
                    editingAction = action
                }, onComplete = { onComplete(action.id) }, onSnooze = {})
            }
        }
    }
    if (showAddDialog) {
        ActionEditorDialog(onDismiss = { showAddDialog = false }, onSave = { title, minutes ->
            onAdd(title, minutes)
            showAddDialog = false
        })
    }
    editingAction?.let { action ->
        ActionEditorDialog(action, onDismiss = { editingAction = null }, onSave = { title, minutes ->
            onEdit(action.id, title, minutes)
            editingAction = null
        })
    }
}

@Composable
private fun ActionCard(
    action: ActionItem,
    done: Boolean,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
) {
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
                enabled = !done,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    AppIcons.Check,
                    contentDescription = stringResource(R.string.action_complete_cd, action.title),
                    tint = NavyDeep,
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                Icon(
                    AppIcons.Edit,
                    contentDescription = stringResource(R.string.action_edit_cd, action.title),
                    tint = NavyDeep,
                )
            }
            if (!done && action.scheduledDate == LocalDate.now()) {
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
private fun ActionEditorDialog(action: ActionItem? = null, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var title by remember(action?.id) { mutableStateOf(action?.title.orEmpty()) }
    var minutes by remember(action?.id) { mutableStateOf(action?.minutes?.toString().orEmpty()) }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (action ==
                        null
                    ) {
                        R.string.action_dialog_add_title
                    } else {
                        R.string.action_dialog_edit_title
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        invalid = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.action_title_label)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { value ->
                        minutes = value.filter(Char::isDigit)
                        invalid = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.action_minutes_label)) },
                    singleLine = true,
                )
                if (invalid) {
                    Text(stringResource(R.string.action_invalid), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val duration = minutes.toIntOrNull()
                    if (title.isBlank() || duration == null || duration !in 1..180) {
                        invalid = true
                    } else {
                        onSave(title.trim(), duration)
                    }
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
