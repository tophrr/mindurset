package com.mindurset.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.UserProfile
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
    val nameError: NameError? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
)

/**
 * Edit profile owns only the display name. The daily reminder, its time and the
 * language selector live in Settings (backed by the DataStore PreferencesStore),
 * so they are not duplicated here. On save we preserve whatever those already stored.
 */
class ProfileEditViewModel(private val profileRepository: ProfileRepository, private val sessionStore: SessionStore) :
    ViewModel() {
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
                it.copy(loading = false, displayName = profile?.displayName.orEmpty())
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, nameError = null) }
    }

    fun save() {
        val current = _uiState.value
        val name = current.displayName.trim()
        val error =
            when {
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
                    reminderHour = existing?.reminderHour ?: DEFAULT_REMINDER_HOUR,
                    reminderEnabled = existing?.reminderEnabled ?: false,
                    language = existing?.language ?: "en",
                ),
            )
            _uiState.update { it.copy(saving = false, saved = true) }
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 40
        private const val DEFAULT_REMINDER_HOUR = 19
    }
}
