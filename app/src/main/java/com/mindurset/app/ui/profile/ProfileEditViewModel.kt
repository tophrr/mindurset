package com.mindurset.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which validation rule the display name broke, if any. */
enum class NameError { REQUIRED, LENGTH }

data class ProfileEditUiState(
    val loading: Boolean = true,
    val displayName: String = "",
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 19,
    val language: String = "en",
    val nameError: NameError? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

class ProfileEditViewModel(
    private val profileRepository: ProfileRepository,
    private val sessionStore: SessionStore,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileEditUiState())
    val uiState: StateFlow<ProfileEditUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val session = sessionStore.current()
            if (session == null) {
                _uiState.update { it.copy(loading = false) }
                return@launch
            }
            profileRepository.ensureProfile(session.userId, session.email)
            val profile = profileRepository.observe(session.userId).first()
            _uiState.update {
                it.copy(
                    loading = false,
                    displayName = profile?.displayName.orEmpty(),
                    reminderEnabled = preferencesStore.reminderEnabled.first(),
                    reminderHour = preferencesStore.reminderHour.first(),
                    language = preferencesStore.language.first(),
                )
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, nameError = null) }
    }

    fun onReminderEnabledChange(enabled: Boolean) {
        _uiState.update { it.copy(reminderEnabled = enabled) }
    }

    fun onReminderHourChange(hour: Int) {
        _uiState.update { it.copy(reminderHour = hour.coerceIn(0, 23)) }
    }

    fun onLanguageChange(language: String) {
        _uiState.update { it.copy(language = language) }
    }

    fun save() {
        val current = _uiState.value
        val name = current.displayName.trim()
        val error = when {
            name.isEmpty() -> NameError.REQUIRED
            name.length > MAX_NAME_LENGTH -> NameError.LENGTH
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(nameError = error) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, nameError = null) }
            val session = sessionStore.current()
            if (session == null) {
                _uiState.update { it.copy(saving = false) }
                return@launch
            }
            profileRepository.ensureProfile(session.userId, session.email)
            val existing = profileRepository.observe(session.userId).first()
            profileRepository.upsert(
                UserProfile(
                    userId = session.userId,
                    displayName = name,
                    joinedAt = existing?.joinedAt ?: System.currentTimeMillis(),
                    reminderEnabled = current.reminderEnabled,
                    reminderHour = current.reminderHour,
                    language = current.language,
                ),
            )
            preferencesStore.setReminderEnabled(current.reminderEnabled)
            preferencesStore.setReminderHour(current.reminderHour)
            preferencesStore.setLanguage(current.language)

            // Re-read from Room / DataStore so the screen reflects what survives a restart.
            val persisted = profileRepository.observe(session.userId).first()
            _uiState.update {
                it.copy(
                    loading = false,
                    saving = false,
                    displayName = persisted?.displayName ?: name,
                    reminderEnabled = preferencesStore.reminderEnabled.first(),
                    reminderHour = preferencesStore.reminderHour.first(),
                    language = preferencesStore.language.first(),
                    saved = true,
                )
            }
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
    }
}
