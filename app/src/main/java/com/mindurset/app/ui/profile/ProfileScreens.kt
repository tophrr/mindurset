package com.mindurset.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.ui.common.LoadingState

// ---------------------------------------------------------------------------
// Profile
// ---------------------------------------------------------------------------

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: ProfileViewModel = viewModel {
        ProfileViewModel(
            container.profileRepository,
            container.authRepository,
            container.sessionStore,
            container.preferencesStore,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val loggedOut by vm.loggedOut.collectAsStateWithLifecycle()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLogout()
    }

    Scaffold(
        topBar = {
            MindTopBar(
                title = stringResource(R.string.profile_title),
                onOpenDrawer = onOpenDrawer,
                navDescription = stringResource(R.string.profile_open_menu),
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileHeader(
                    displayName = state.displayName.ifBlank { stringResource(R.string.profile_name_fallback) },
                    memberSince = state.joinedAt?.let { formatMonthYear(it) },
                )

                SectionCard {
                    MenuRow(
                        label = stringResource(R.string.profile_email_label),
                        value = state.email,
                    )
                    HorizontalDivider()
                    MenuRow(
                        label = stringResource(R.string.profile_edit_label),
                        onClick = onEditProfile,
                    )
                    HorizontalDivider()
                    MenuRow(
                        label = stringResource(R.string.profile_settings_label),
                        onClick = onSettings,
                    )
                }

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.profile_logout),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        LogoutDialog(
            onCancel = { showLogoutDialog = false },
            onKeepData = {
                showLogoutDialog = false
                vm.logout(deleteLocalData = false)
            },
            onDeleteData = {
                showLogoutDialog = false
                vm.logout(deleteLocalData = true)
            },
        )
    }
}

@Composable
private fun ProfileHeader(displayName: String, memberSince: String?) {
    val avatarDescription = stringResource(R.string.profile_avatar_description)
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clearAndSetSemantics {
                    contentDescription = avatarDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "\u263A",
                fontSize = 40.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (memberSince != null) {
            Text(
                text = stringResource(R.string.profile_member_since, memberSince),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Confirmation for logging out. The data-preserving option is the primary action
 * and the destructive one is an explicit, separate tap, so nothing is deleted by
 * default or by a stray confirmation.
 */
@Composable
private fun LogoutDialog(
    onCancel: () -> Unit,
    onKeepData: () -> Unit,
    onDeleteData: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.profile_logout_title)) },
        text = { Text(stringResource(R.string.profile_logout_message)) },
        confirmButton = {
            Button(onClick = onKeepData) {
                Text(stringResource(R.string.profile_logout_keep))
            }
        },
        dismissButton = {
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onDeleteData) {
                    Text(
                        text = stringResource(R.string.profile_logout_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onCancel) {
                    Text(stringResource(R.string.profile_logout_cancel))
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Profile edit
// ---------------------------------------------------------------------------

@Composable
fun ProfileEditScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: ProfileEditViewModel = viewModel {
        ProfileEditViewModel(
            container.profileRepository,
            container.sessionStore,
            container.preferencesStore,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showHourPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            MindTopBar(
                title = stringResource(R.string.profile_edit_title),
                onBack = onBack,
                navDescription = stringResource(R.string.profile_back),
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.displayName,
                            onValueChange = vm::onDisplayNameChange,
                            label = { Text(stringResource(R.string.profile_field_display_name)) },
                            isError = state.nameError != null,
                            singleLine = true,
                            supportingText = {
                                Text(
                                    text = when (state.nameError) {
                                        NameError.REQUIRED -> stringResource(R.string.profile_error_name_required)
                                        NameError.LENGTH -> stringResource(R.string.profile_error_name_length)
                                        null -> stringResource(
                                            R.string.profile_name_counter,
                                            state.displayName.length,
                                        )
                                    },
                                    color = if (state.nameError != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                SectionCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_section_reminder),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_reminder_enabled),
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = state.reminderEnabled,
                                onCheckedChange = vm::onReminderEnabledChange,
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHourPicker = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_reminder_time),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(R.string.profile_time_value, state.reminderHour),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                SectionCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_language_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LanguageRow(
                            code = "en",
                            labelRes = R.string.profile_language_en,
                            selected = state.language,
                            onSelect = vm::onLanguageChange,
                        )
                        LanguageRow(
                            code = "id",
                            labelRes = R.string.profile_language_id,
                            selected = state.language,
                            onSelect = vm::onLanguageChange,
                        )
                    }
                }

                Button(
                    onClick = vm::save,
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (state.saving) R.string.profile_saving else R.string.profile_save_button,
                        ),
                    )
                }
            }
        }
    }

    if (showHourPicker) {
        HourPickerDialog(
            initialHour = state.reminderHour,
            onConfirm = {
                showHourPicker = false
                vm.onReminderHourChange(it)
            },
            onDismiss = { showHourPicker = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container.preferencesStore) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showHourPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            MindTopBar(
                title = stringResource(R.string.profile_settings_title),
                onBack = onBack,
                navDescription = stringResource(R.string.profile_back),
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_section_reminder),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_reminder_enabled),
                                modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = state.reminderEnabled,
                                onCheckedChange = vm::setReminderEnabled,
                            )
                        }
                        Text(
                            text = stringResource(R.string.profile_reminder_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHourPicker = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.profile_reminder_time),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(R.string.profile_time_value, state.reminderHour),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                SectionCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_language_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        LanguageRow(
                            code = "en",
                            labelRes = R.string.profile_language_en,
                            selected = state.language,
                            onSelect = vm::setLanguage,
                        )
                        LanguageRow(
                            code = "id",
                            labelRes = R.string.profile_language_id,
                            selected = state.language,
                            onSelect = vm::setLanguage,
                        )
                    }
                }

                SectionCard {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.profile_theme_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = themeLabel(state.theme),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.profile_theme_coming_soon),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showHourPicker) {
        HourPickerDialog(
            initialHour = state.reminderHour,
            onConfirm = {
                showHourPicker = false
                vm.setReminderHour(it)
            },
            onDismiss = { showHourPicker = false },
        )
    }
}

// ---------------------------------------------------------------------------
// Shared bits
// ---------------------------------------------------------------------------

@Composable
private fun LanguageRow(
    code: String,
    labelRes: Int,
    selected: String,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(code) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == code, onClick = { onSelect(code) })
        Text(
            text = stringResource(labelRes),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun themeLabel(theme: String): String = when (theme) {
    "light" -> stringResource(R.string.profile_theme_value_light)
    "dark" -> stringResource(R.string.profile_theme_value_dark)
    else -> stringResource(R.string.profile_theme_value_system)
}
