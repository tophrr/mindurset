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

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val emailErrorKey: String? = null,
    val passwordErrorKey: String? = null,
    val confirmErrorKey: String? = null,
    val formErrorKey: String? = null,
    val isLoading: Boolean = false,
)

sealed interface RegisterEffect {
    data object Registered : RegisterEffect
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    var uiState by mutableStateOf(RegisterUiState())
        private set

    private val effectChannel = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = effectChannel.receiveAsFlow()

    fun onEmailChange(value: String) {
        uiState = uiState.copy(email = value, emailErrorKey = null, formErrorKey = null)
    }

    fun onPasswordChange(value: String) {
        uiState = uiState.copy(password = value, passwordErrorKey = null, confirmErrorKey = null, formErrorKey = null)
    }

    fun onConfirmChange(value: String) {
        uiState = uiState.copy(confirm = value, confirmErrorKey = null, formErrorKey = null)
    }

    fun submit() {
        if (uiState.isLoading) return
        val validation = ValidateCredentials.forRegister(uiState.email, uiState.password, uiState.confirm)
        if (!validation.isValid) {
            uiState = uiState.copy(
                emailErrorKey = validation.errors[ValidateCredentials.Field.EMAIL],
                passwordErrorKey = validation.errors[ValidateCredentials.Field.PASSWORD],
                confirmErrorKey = validation.errors[ValidateCredentials.Field.CONFIRM],
                formErrorKey = null,
            )
            return
        }

        uiState = uiState.copy(isLoading = true, formErrorKey = null)
        viewModelScope.launch {
            // register() hashes the password with PBKDF2; isLoading covers that window.
            val account = try {
                authRepository.register(uiState.email.trim(), uiState.password)
            } catch (e: IllegalArgumentException) {
                uiState = uiState.copy(isLoading = false, emailErrorKey = AuthErrors.EMAIL_TAKEN)
                return@launch
            } catch (e: Exception) {
                uiState = uiState.copy(isLoading = false, formErrorKey = AuthErrors.GENERIC)
                return@launch
            }

            sessionStore.signIn(SessionInfo(account.id, account.email))
            uiState = uiState.copy(isLoading = false)
            effectChannel.send(RegisterEffect.Registered)
        }
    }
}
