package com.mindurset.app.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// SCAFFOLD STUB - owned by the dashboard workstream. Replace the body, keep the signature.

@Composable
fun DashboardScreen(
    onOpenCheckIn: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenCompanion: () -> Unit,
    onOpenTherapy: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Dashboard placeholder")
    }
}
