package com.mindurset.app.ui.auth

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mindurset.app.R
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.domain.auth.ValidateCredentials

/** Stable error keys produced by the auth ViewModels and mapped to string resources here. */
object AuthErrors {
    const val INVALID_CREDENTIALS = "auth_invalid_credentials"
    const val EMAIL_TAKEN = "auth_email_taken"
    const val GENERIC = "auth_generic"
}

/**
 * Turns any validation/field error key into a user-visible string resource. Keeping the
 * mapping in one place means the domain layer never ships raw copy to the UI.
 */
@StringRes
fun authErrorRes(key: String): Int = when (key) {
    ValidateCredentials.ERROR_EMAIL_EMPTY -> R.string.auth_error_email_empty
    ValidateCredentials.ERROR_EMAIL_FORMAT -> R.string.auth_error_email_format
    ValidateCredentials.ERROR_PASSWORD_EMPTY -> R.string.auth_error_password_empty
    ValidateCredentials.ERROR_PASSWORD_SHORT -> R.string.auth_error_password_short
    ValidateCredentials.ERROR_PASSWORD_MISMATCH -> R.string.auth_error_password_mismatch
    AuthErrors.INVALID_CREDENTIALS -> R.string.auth_error_invalid_credentials
    AuthErrors.EMAIL_TAKEN -> R.string.auth_error_email_taken
    else -> R.string.auth_error_generic
}

/**
 * Pale-blue canvas with the app wordmark on top and a soft white rounded card holding the
 * form, mirroring the project mockups (white cards floating on a tinted background).
 */
@Composable
fun AuthScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun AuthHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Inline banner for non-field errors (wrong credentials, duplicate email, unexpected failure). */
@Composable
fun AuthErrorBanner(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorKey: String?,
    enabled: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    isPassword: Boolean = false,
    onDone: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    val errorText = errorKey?.let { stringResource(authErrorRes(it), ValidateCredentials.MIN_PASSWORD_LENGTH) }

    val trailing: (@Composable () -> Unit)? = if (isPassword) {
        {
            IconButton(
                onClick = { visible = !visible },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (visible) AppIcons.EyeOff else AppIcons.Eye,
                    contentDescription = stringResource(
                        if (visible) R.string.auth_hide_password else R.string.auth_show_password,
                    ),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    } else {
        null
    }
    val supporting: (@Composable () -> Unit)? = if (errorText != null) {
        { Text(text = errorText, style = MaterialTheme.typography.bodySmall) }
    } else {
        null
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        isError = errorText != null,
        shape = RoundedCornerShape(16.dp),
        label = { Text(text = label) },
        visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = if (onDone != null) { { onDone() } } else null),
        trailingIcon = trailing,
        supportingText = supporting,
    )
}

@Composable
fun AuthPrimaryButton(
    label: String,
    loadingLabel: String,
    loading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current,
            )
            Text(
                text = loadingLabel,
                modifier = Modifier.padding(start = 10.dp),
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Text(text = label, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AuthSecondaryButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(text = label, fontWeight = FontWeight.SemiBold)
    }
}
