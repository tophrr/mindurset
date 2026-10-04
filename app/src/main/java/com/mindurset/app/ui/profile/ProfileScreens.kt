package com.mindurset.app.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// SCAFFOLD STUBS - owned by the profile workstream. Replace bodies, keep signatures.

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onSettings: () -> Unit,
    onLogout: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Profile placeholder")
    }
}

@Composable
fun ProfileEditScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Profile edit placeholder")
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text("Settings placeholder")
    }
}
