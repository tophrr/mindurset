package com.mindurset.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.repository.PreferencesStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val language: String = "en",
    val theme: String = "system",
)

/**
 * Settings are read and written straight through the DataStore-backed
 * [PreferencesStore]; there are no local-only switches here.
 */
class SettingsViewModel(private val preferencesStore: PreferencesStore) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> =
        combine(
            preferencesStore.reminderEnabled,
            preferencesStore.reminderHour,
            preferencesStore.language,
            preferencesStore.theme,
        ) { enabled, hour, language, theme ->
            SettingsUiState(
                loading = false,
                reminderEnabled = enabled,
                reminderHour = hour,
                language = language,
                theme = theme,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(loading = true))

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setReminderEnabled(enabled) }
    }

    fun setReminderHour(hour: Int) {
        viewModelScope.launch { preferencesStore.setReminderHour(hour) }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { preferencesStore.setLanguage(language) }
    }
}
