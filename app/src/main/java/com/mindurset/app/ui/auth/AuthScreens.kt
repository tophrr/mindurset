package com.mindurset.app.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// SCAFFOLD STUB - owned by the auth workstream. Replace the body, keep the signature.

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    onRegister: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Login screen placeholder")
    }
}

@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Register screen placeholder")
    }
}
