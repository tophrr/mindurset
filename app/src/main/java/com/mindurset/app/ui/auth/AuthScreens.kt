package com.mindurset.app.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.auth.ValidateCredentials

@Composable
fun LoginScreen(onSignedIn: () -> Unit, onRegister: () -> Unit, onSkip: () -> Unit) {
    val container = LocalAppContainer.current
    val loginViewModel: LoginViewModel = viewModel {
        LoginViewModel(container.authRepository, container.sessionStore)
    }
    val state = loginViewModel.uiState

    LaunchedEffect(loginViewModel) {
        loginViewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.SignedIn -> onSignedIn()
                LoginEffect.Skipped -> onSkip()
            }
        }
    }

    AuthScreenFrame {
        AuthHeading(
            title = stringResource(R.string.auth_login_title),
            subtitle = stringResource(R.string.auth_login_subtitle),
        )

        AuthField(
            value = state.email,
            onValueChange = loginViewModel::onEmailChange,
            label = stringResource(R.string.auth_email_label),
            errorKey = state.emailErrorKey,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )

        AuthField(
            value = state.password,
            onValueChange = loginViewModel::onPasswordChange,
            label = stringResource(R.string.auth_password_label),
            errorKey = state.passwordErrorKey,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            onDone = loginViewModel::submit,
        )

        state.formErrorKey?.let { key ->
            AuthErrorBanner(stringResource(authErrorRes(key), ValidateCredentials.MIN_PASSWORD_LENGTH))
        }

        AuthPrimaryButton(
            label = stringResource(R.string.auth_sign_in),
            loadingLabel = stringResource(R.string.auth_signing_in),
            loading = state.isLoading,
            onClick = loginViewModel::submit,
        )

        AuthSecondaryButton(
            label = stringResource(R.string.auth_continue_without_account),
            enabled = !state.isLoading,
            onClick = loginViewModel::continueAsGuest,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.auth_no_account_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onRegister, enabled = !state.isLoading) {
                Text(
                    text = stringResource(R.string.auth_create_account),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegistered: () -> Unit, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val registerViewModel: RegisterViewModel = viewModel {
        RegisterViewModel(container.authRepository, container.sessionStore)
    }
    val state = registerViewModel.uiState

    LaunchedEffect(registerViewModel) {
        registerViewModel.effects.collect { effect ->
            when (effect) {
                RegisterEffect.Registered -> onRegistered()
            }
        }
    }

    AuthScreenFrame {
        AuthHeading(
            title = stringResource(R.string.auth_register_title),
            subtitle = stringResource(R.string.auth_register_subtitle),
        )

        AuthField(
            value = state.email,
            onValueChange = registerViewModel::onEmailChange,
            label = stringResource(R.string.auth_email_label),
            errorKey = state.emailErrorKey,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        )

        AuthField(
            value = state.password,
            onValueChange = registerViewModel::onPasswordChange,
            label = stringResource(R.string.auth_password_label),
            errorKey = state.passwordErrorKey,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next,
            isPassword = true,
        )

        AuthField(
            value = state.confirm,
            onValueChange = registerViewModel::onConfirmChange,
            label = stringResource(R.string.auth_confirm_password_label),
            errorKey = state.confirmErrorKey,
            enabled = !state.isLoading,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            isPassword = true,
            onDone = registerViewModel::submit,
        )

        state.formErrorKey?.let { key ->
            AuthErrorBanner(stringResource(authErrorRes(key), ValidateCredentials.MIN_PASSWORD_LENGTH))
        }

        AuthPrimaryButton(
            label = stringResource(R.string.auth_create_account_action),
            loadingLabel = stringResource(R.string.auth_creating_account),
            loading = state.isLoading,
            onClick = registerViewModel::submit,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.auth_have_account_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onBack, enabled = !state.isLoading) {
                Text(
                    text = stringResource(R.string.auth_back_to_login),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
