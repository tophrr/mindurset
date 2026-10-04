@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mindurset.app.ui.reflection

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.usecase.ValidateReflection
import com.mindurset.app.ui.common.EmptyState
import com.mindurset.app.ui.common.ErrorState
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.theme.DangerRed
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.PaleBlue
import com.mindurset.app.ui.theme.SurfaceWhite

// Daily Reflection CRUD module: reactive list (Room), create/edit form with
// validation, detail view, and delete behind a confirmation dialog.

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
        containerColor = PaleBlue,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reflection_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { BackIcon(onBack) },
                actions = { MenuIcon(onOpenDrawer) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                    titleContentColor = NavyInk,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                containerColor = NavyDeep,
                contentColor = SurfaceWhite,
                icon = { Text("+", style = MaterialTheme.typography.titleLarge) },
                text = {
                    Text(
                        text = stringResource(R.string.reflection_new_entry),
                        fontWeight = FontWeight.SemiBold,
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
                    EmptyState(
                        title = stringResource(R.string.reflection_empty_title),
                        body = stringResource(R.string.reflection_empty_body),
                        actionLabel = stringResource(R.string.reflection_new_entry),
                        onAction = onNew,
                    )

                is ReflectionListViewModel.UiState.Ready ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 16.dp,
                            bottom = 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = current.entries, key = { it.id }) { entry ->
                            ReflectionRow(entry = entry, onClick = { onOpen(entry.id) })
                        }
                    }
            }
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
        containerColor = PaleBlue,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(
                            if (viewModel.isEdit) R.string.reflection_form_title_edit
                            else R.string.reflection_form_title_new,
                        ),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = { BackIcon(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                    titleContentColor = NavyInk,
                ),
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
        containerColor = PaleBlue,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reflection_detail_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { BackIcon(onBack) },
                actions = {
                    TextButton(onClick = onEdit) {
                        Text(stringResource(R.string.edit), color = NavyInk)
                    }
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text(stringResource(R.string.delete), color = DangerRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                    titleContentColor = NavyInk,
                ),
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
private fun ReflectionRow(entry: JournalEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.reflection_row_challenge, entry.challenge),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.reflection_form_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

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
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.reflection_field_reframe)) },
            placeholder = { Text(stringResource(R.string.reflection_field_reframe_hint)) },
            isError = viewModel.reframeTouched && reframeError != null,
            supportingText = fieldSupportingText(viewModel.reframeTouched, reframeError),
            minLines = 2,
        )

        OutlinedTextField(
            value = viewModel.gratitude,
            onValueChange = viewModel::onGratitudeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.reflection_field_gratitude)) },
            placeholder = { Text(stringResource(R.string.reflection_field_gratitude_hint)) },
            isError = viewModel.gratitudeTouched && gratitudeError != null,
            supportingText = fieldSupportingText(viewModel.gratitudeTouched, gratitudeError),
            minLines = 2,
        )

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
            .padding(20.dp),
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (entry.updatedAt != entry.createdAt) {
            Text(
                text = stringResource(R.string.reflection_detail_updated, fullDate(entry.updatedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReflectionFieldCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = NavyInk,
            )
        }
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

@Composable
private fun BackIcon(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Text("\u2190", style = MaterialTheme.typography.titleLarge, color = NavyInk)
    }
}

@Composable
private fun MenuIcon(onOpenDrawer: () -> Unit) {
    IconButton(onClick = onOpenDrawer) {
        Text("\u2630", style = MaterialTheme.typography.titleLarge, color = NavyInk)
    }
}
