package com.mindurset.app.ui.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.profile.MindTopBar
import com.mindurset.app.ui.theme.DangerRed
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.PaleBlue
import com.mindurset.app.ui.theme.SuccessGreen
import com.mindurset.app.ui.theme.SurfaceWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * S-52 Memory manager: lets the user toggle `memory_enabled`, inspect/edit/delete the up to three
 * saved session summaries, write or clear a personal note, or wipe all saved memory.
 */
@Composable
fun MemoryManagerRoute(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: MemoryManagerViewModel = viewModel {
        MemoryManagerViewModel(
            summaryRepository = container.sessionSummaryRepository,
            preferencesStore = container.preferencesStore,
            sessionStore = container.sessionStore,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()

    MemoryManagerContent(
        state = state,
        onBack = onBack,
        onToggleMemory = vm::setMemoryEnabled,
        onNoteDraftChange = vm::onNoteDraftChange,
        onSaveNote = vm::saveNote,
        onClearNote = vm::clearNote,
        onEditSummary = vm::editSummary,
        onDeleteSummary = vm::deleteSummary,
        onClearAll = vm::clearAllMemory,
    )
}

@Composable
fun MemoryManagerContent(
    state: MemoryManagerUiState,
    onBack: () -> Unit,
    onToggleMemory: (Boolean) -> Unit,
    onNoteDraftChange: (String) -> Unit,
    onSaveNote: () -> Unit,
    onClearNote: () -> Unit,
    onEditSummary: (id: String, text: String) -> Unit,
    onDeleteSummary: (id: String) -> Unit,
    onClearAll: () -> Unit,
) {
    var editingSummary by remember { mutableStateOf<SessionSummary?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var deletingSummaryId by remember { mutableStateOf<String?>(null) }
    var confirmClearAll by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MindTopBar(
                title = stringResource(R.string.memory_title),
                onBack = onBack,
                navDescription = stringResource(R.string.memory_back),
            )
        },
    ) { innerPadding ->
        if (state.loading) {
            LoadingState(Modifier.padding(innerPadding), label = stringResource(R.string.loading))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MemoryToggleCard(
                    enabled = state.memoryEnabled,
                    onToggle = onToggleMemory,
                )

                if (!state.memoryEnabled) {
                    Text(
                        text = stringResource(R.string.memory_paused_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = NavyInk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PaleBlue, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.memory_summaries_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyInk,
                    )
                    Text(
                        text = stringResource(R.string.memory_summaries_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }

                if (state.summaries.isEmpty()) {
                    AppCard {
                        Text(
                            text = stringResource(R.string.memory_summaries_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedGray,
                        )
                    }
                } else {
                    state.summaries.forEach { summary ->
                        SummaryCard(
                            summary = summary,
                            onEdit = {
                                editingSummary = summary
                                editDraft = summary.summary
                            },
                            onDelete = { deletingSummaryId = summary.id },
                        )
                    }
                }

                UserNoteCard(
                    draft = state.noteDraft,
                    savedNote = state.userNote,
                    noteSaved = state.noteSaved,
                    onDraftChange = onNoteDraftChange,
                    onSave = onSaveNote,
                    onClear = onClearNote,
                )

                if (state.summaries.isNotEmpty() || state.userNote.isNotBlank()) {
                    OutlinedButton(
                        onClick = { confirmClearAll = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.memory_clear_all),
                            color = DangerRed,
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    editingSummary?.let { target ->
        AlertDialog(
            onDismissRequest = { editingSummary = null },
            title = { Text(stringResource(R.string.memory_edit_dialog_title)) },
            text = {
                OutlinedTextField(
                    value = editDraft,
                    onValueChange = { editDraft = it.take(MemoryPolicy.MAX_SUMMARY_LENGTH) },
                    label = { Text(stringResource(R.string.memory_edit_field_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditSummary(target.id, editDraft)
                        editingSummary = null
                    },
                    enabled = editDraft.trim().isNotEmpty(),
                ) {
                    Text(stringResource(R.string.memory_edit_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSummary = null }) {
                    Text(stringResource(R.string.memory_cancel))
                }
            },
        )
    }

    deletingSummaryId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { deletingSummaryId = null },
            title = { Text(stringResource(R.string.memory_delete_dialog_title)) },
            text = { Text(stringResource(R.string.memory_delete_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSummary(targetId)
                        deletingSummaryId = null
                    },
                ) {
                    Text(
                        text = stringResource(R.string.memory_delete_confirm),
                        color = DangerRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSummaryId = null }) {
                    Text(stringResource(R.string.memory_cancel))
                }
            },
        )
    }

    if (confirmClearAll) {
        AlertDialog(
            onDismissRequest = { confirmClearAll = false },
            title = { Text(stringResource(R.string.memory_clear_all_dialog_title)) },
            text = { Text(stringResource(R.string.memory_clear_all_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        confirmClearAll = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.memory_clear_all_confirm),
                        color = DangerRed,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearAll = false }) {
                    Text(stringResource(R.string.memory_cancel))
                }
            },
        )
    }
}

@Composable
private fun MemoryToggleCard(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.memory_toggle_title),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NavyInk,
            )
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceWhite,
                    checkedTrackColor = NavyDeep,
                    checkedBorderColor = NavyDeep,
                    uncheckedThumbColor = SurfaceWhite,
                    uncheckedTrackColor = MutedGray.copy(alpha = 0.55f),
                    uncheckedBorderColor = MutedGray,
                ),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                if (enabled) R.string.memory_toggle_hint_on else R.string.memory_toggle_hint_off,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
    }
}

@Composable
private fun SummaryCard(summary: SessionSummary, onEdit: () -> Unit, onDelete: () -> Unit) {
    val formatter = remember {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault())
    }
    val createdLabel = remember(summary.createdAt) { formatter.format(Instant.ofEpochMilli(summary.createdAt)) }
    val retentionLabel = remember(summary.retentionUntil) {
        formatter.format(Instant.ofEpochMilli(summary.retentionUntil))
    }

    AppCard {
        Text(
            text = summary.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = NavyInk,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.memory_summary_meta, createdLabel, retentionLabel),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.memory_summary_edit))
            }
            TextButton(onClick = onDelete) {
                Text(
                    text = stringResource(R.string.memory_summary_delete),
                    color = DangerRed,
                )
            }
        }
    }
}

@Composable
private fun UserNoteCard(
    draft: String,
    savedNote: String,
    noteSaved: Boolean,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
) {
    AppCard {
        Text(
            text = stringResource(R.string.memory_note_heading),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.memory_note_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            placeholder = { Text(stringResource(R.string.memory_note_placeholder)) },
            supportingText = {
                Text(
                    text = stringResource(
                        R.string.memory_note_counter,
                        draft.length,
                        MemoryPolicy.MAX_USER_NOTE_LENGTH,
                    ),
                    color = MutedGray,
                )
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = onSave,
                enabled = draft.trim() != savedNote,
                shape = RoundedCornerShape(22.dp),
            ) {
                Text(stringResource(R.string.memory_note_save))
            }
            if (savedNote.isNotBlank()) {
                TextButton(onClick = onClear) {
                    Text(
                        text = stringResource(R.string.memory_note_clear),
                        color = DangerRed,
                    )
                }
            }
            if (noteSaved) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.memory_note_saved),
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}
