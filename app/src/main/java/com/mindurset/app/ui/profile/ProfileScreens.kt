package com.mindurset.app.ui.profile

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.LogoutConfirmDialog
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.common.SegmentedTabs
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.DangerRed
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.PaleBlue
import com.mindurset.app.ui.theme.SurfaceWhite

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
            container.checkInRepository,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()
    val loggedOut by vm.loggedOut.collectAsStateWithLifecycle()
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var moodTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLogout()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ProfileIdentityCard(
                    displayName = state.displayName.ifBlank { stringResource(R.string.profile_name_fallback) },
                    email = state.email,
                    memberSince = state.joinedAt?.let { formatMonthYear(it) },
                )

                AppCard(contentPadding = PaddingValues(vertical = 6.dp)) {
                    ProfileMenuRow(
                        icon = AppIcons.Edit,
                        label = stringResource(R.string.profile_edit_label),
                        onClick = onEditProfile,
                    )
                    ProfileMenuRow(
                        icon = AppIcons.Settings,
                        label = stringResource(R.string.profile_settings_label),
                        onClick = onSettings,
                    )
                }

                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        SegmentedTabs(
                            options = listOf(
                                stringResource(R.string.profile_mood_weekly),
                                stringResource(R.string.profile_mood_monthly),
                            ),
                            selectedIndex = moodTab,
                            onSelect = { moodTab = it },
                        )
                        if (moodTab == 0) {
                            MoodWeeklyPlaceholder()
                        } else {
                            MoodMonthlyCalendar(
                                year = state.year,
                                monthValue = state.monthValue,
                                dayEmotions = state.dayEmotions,
                            )
                        }
                    }
                }

                AppCard(contentPadding = PaddingValues(0.dp)) {
                    ProfileMenuRow(
                        icon = AppIcons.Logout,
                        label = stringResource(R.string.profile_logout),
                        onClick = { showLogoutDialog = true },
                        labelColor = DangerRed,
                        iconTint = DangerRed,
                        showChevron = false,
                    )
                }

                Spacer(Modifier.height(FloatingNavSpace))
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
private fun ProfileIdentityCard(displayName: String, email: String, memberSince: String?) {
    val avatarDescription = stringResource(R.string.profile_avatar_description)
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PaleBlue)
                    .clearAndSetSemantics { contentDescription = avatarDescription },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialOf(displayName),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = NavyDeep,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = NavyInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (memberSince != null) {
                    Text(
                        text = stringResource(R.string.profile_member_since, memberSince),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }
            }
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
    LogoutConfirmDialog(
        title = stringResource(R.string.profile_logout_title),
        message = stringResource(R.string.profile_logout_message),
        keepLabel = stringResource(R.string.profile_logout_keep),
        cancelLabel = stringResource(R.string.profile_logout_cancel),
        deleteLabel = stringResource(R.string.profile_logout_delete),
        onKeepData = onKeepData,
        onCancel = onCancel,
        onDeleteData = onDeleteData,
    )
}


// ---------------------------------------------------------------------------
// Profile edit — display name only
// ---------------------------------------------------------------------------

@Composable
fun ProfileEditScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val container = LocalAppContainer.current
    val vm: ProfileEditViewModel = viewModel {
        ProfileEditViewModel(container.profileRepository, container.sessionStore)
    }
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                        MutedGray
                                    },
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            text = stringResource(R.string.profile_avatar_initial_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGray,
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

                Spacer(Modifier.height(FloatingNavSpace))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Settings — the only home for reminder, reminder time, language and theme
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: SettingsViewModel = viewModel { SettingsViewModel(container.preferencesStore) }
    val state by vm.uiState.collectAsStateWithLifecycle()
    var showHourPicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsSection(title = stringResource(R.string.profile_section_reminder)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_reminder_enabled),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = NavyInk,
                        )
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = vm::setReminderEnabled,
                            // The default off state sat on a near-white track, so it had no
                            // contrast against the white card; give both states real colours.
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
                    Text(
                        text = stringResource(R.string.profile_reminder_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHourPicker = true }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.profile_reminder_time),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            color = NavyInk,
                        )
                        Text(
                            text = stringResource(R.string.profile_time_value, state.reminderHour),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.profile_language_label)) {
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

                SettingsSection(title = stringResource(R.string.profile_theme_label)) {
                    Text(
                        text = themeLabel(state.theme),
                        style = MaterialTheme.typography.bodyLarge,
                        color = NavyInk,
                    )
                    Text(
                        text = stringResource(R.string.profile_theme_coming_soon),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                }

                Spacer(Modifier.height(FloatingNavSpace))
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

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    AppCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = NavyInk,
            )
            content()
        }
    }
}

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
            color = NavyInk,
        )
    }
}

@Composable
private fun themeLabel(theme: String): String = when (theme) {
    "light" -> stringResource(R.string.profile_theme_value_light)
    "dark" -> stringResource(R.string.profile_theme_value_dark)
    else -> stringResource(R.string.profile_theme_value_system)
}
