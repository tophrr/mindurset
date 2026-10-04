package com.mindurset.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.repository.AuthRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the profile screen shows, derived from the live session and Room row. */
data class ProfileUiState(
    val loading: Boolean = true,
    val signedOut: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val joinedAt: Long? = null,
    val language: String = "en",
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val preferencesStore: PreferencesStore,
) : ViewModel() {

    /** Flips to true once sign-out has actually completed, so the screen can navigate. */
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    val uiState: StateFlow<ProfileUiState> = sessionStore.session
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(ProfileUiState(loading = false, signedOut = true))
            } else {
                profileRepository.observe(session.userId).map { profile ->
                    ProfileUiState(
                        loading = false,
                        signedOut = false,
                        displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                            ?: session.email.substringBefore('@'),
                        email = session.email,
                        joinedAt = profile?.joinedAt,
                        language = profile?.language ?: "en",
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(loading = true))

    init {
        // Guarantees a Room profile row for the signed-in account (e.g. after "skip"),
        // so the header never has to fall back to invented data.
        viewModelScope.launch {
            sessionStore.session.collect { session ->
                if (session != null) {
                    profileRepository.ensureProfile(session.userId, session.email)
                }
            }
        }
    }

    /**
     * Signs out for real. When [deleteLocalData] is true the account row and all of
     * its journal/check-in/profile data are deleted first; otherwise the data stays.
     */
    fun logout(deleteLocalData: Boolean) {
        viewModelScope.launch {
            val session = sessionStore.current()
            if (deleteLocalData && session != null) {
                authRepository.deleteAccount(session.userId)
            }
            sessionStore.signOut()
            _loggedOut.value = true
        }
    }
}
