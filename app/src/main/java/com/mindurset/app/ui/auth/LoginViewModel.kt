package com.mindurset.app.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.auth.ValidateCredentials
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.AuthRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailErrorKey: String? = null,
    val passwordErrorKey: String? = null,
    val formErrorKey: String? = null,
    val isLoading: Boolean = false,
)

/** One-shot navigation signals for the login screen; never stored in UI state. */
sealed interface LoginEffect {
    data object SignedIn : LoginEffect

    data object Skipped : LoginEffect
}

class LoginViewModel(private val authRepository: AuthRepository, private val sessionStore: SessionStore) :
    ViewModel() {
    var uiState by mutableStateOf(LoginUiState())
        private set

    private val effectChannel = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = effectChannel.receiveAsFlow()

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, emailErrorKey = null, formErrorKey = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, passwordErrorKey = null, formErrorKey = null)
    }

    fun submit() {
        if (uiState.isLoading) return
        val validation = ValidateCredentials.forLogin(uiState.email, uiState.password)
        if (!validation.isValid) {
            uiState =
                uiState.copy(
                    emailErrorKey = validation.errors[ValidateCredentials.Field.EMAIL],
                    passwordErrorKey = validation.errors[ValidateCredentials.Field.PASSWORD],
                    formErrorKey = null,
                )
            return
        }

        uiState = uiState.copy(isLoading = true, formErrorKey = null)
        viewModelScope.launch {
            // PBKDF2 verification happens inside signIn; isLoading stays true until it returns.
            val account =
                try {
                    authRepository.signIn(uiState.email.trim(), uiState.password)
                } catch (e: Exception) {
                    uiState = uiState.copy(isLoading = false, formErrorKey = AuthErrors.GENERIC)
                    return@launch
                }

            if (account == null) {
                // Deliberately generic: never disclose whether the email address exists.
                uiState = uiState.copy(isLoading = false, formErrorKey = AuthErrors.INVALID_CREDENTIALS)
                return@launch
            }

            sessionStore.signIn(SessionInfo(account.id, account.email))
            uiState = uiState.copy(isLoading = false)
            effectChannel.send(LoginEffect.SignedIn)
        }
    }

    /**
     * Signs in the single shared on-device guest account, creating it on first use so the
     * app stays usable without the user setting up credentials.
     */
    fun continueAsGuest() {
        if (uiState.isLoading) return
        uiState = uiState.copy(isLoading = true, formErrorKey = null)
        viewModelScope.launch {
            val account =
                try {
                    authRepository.findAccount(GUEST_EMAIL) ?: authRepository.register(GUEST_EMAIL, GUEST_PASSWORD)
                } catch (e: IllegalArgumentException) {
                    // Rare race: the guest account appeared between find and register.
                    runCatching { authRepository.signIn(GUEST_EMAIL, GUEST_PASSWORD) }.getOrNull()
                } catch (e: Exception) {
                    null
                }

            if (account == null) {
                uiState = uiState.copy(isLoading = false, formErrorKey = AuthErrors.GENERIC)
                return@launch
            }

            sessionStore.signIn(SessionInfo(account.id, account.email))
            uiState = uiState.copy(isLoading = false)
            effectChannel.send(LoginEffect.Skipped)
        }
    }

    companion object {
        const val GUEST_EMAIL = "guest@mindurset.local"

        /** Fixed strong password for the shared guest account; never shown to the user. */
        const val GUEST_PASSWORD = "MindUrSet-guest-2026-local!"
    }
}
