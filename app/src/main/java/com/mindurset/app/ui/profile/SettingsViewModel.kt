package com.mindurset.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.data.prefs.AppLocale
import com.mindurset.app.domain.repository.PreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val loading: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val language: String = AppLocale.SYSTEM,
    val theme: String = "system",
    val memoryEnabled: Boolean = true,
)

/**
 * Settings are read and written straight through the DataStore-backed
 * [PreferencesStore]; there are no local-only switches here.
 */
class SettingsViewModel(private val preferencesStore: PreferencesStore, externalScope: CoroutineScope? = null) :
    ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope

    val uiState: StateFlow<SettingsUiState> =
        combine(
            preferencesStore.reminderEnabled,
            preferencesStore.reminderHour,
            preferencesStore.language,
            preferencesStore.theme,
            preferencesStore.memoryEnabled,
        ) { enabled, hour, language, theme, memoryEnabled ->
            SettingsUiState(
                loading = false,
                reminderEnabled = enabled,
                reminderHour = hour,
                language = AppLocale.normalizeStoredLanguage(language),
                theme = theme,
                memoryEnabled = memoryEnabled,
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(loading = true))

    fun setReminderEnabled(enabled: Boolean) {
        scope.launch { preferencesStore.setReminderEnabled(enabled) }
    }

    fun setReminderHour(hour: Int) {
        scope.launch { preferencesStore.setReminderHour(hour) }
    }

    fun setLanguage(language: String, onPersisted: () -> Unit = {}) {
        val normalized = AppLocale.normalizeStoredLanguage(language)
        scope.launch {
            preferencesStore.setLanguage(normalized)
            onPersisted()
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        scope.launch { preferencesStore.setMemoryEnabled(enabled) }
    }
}
