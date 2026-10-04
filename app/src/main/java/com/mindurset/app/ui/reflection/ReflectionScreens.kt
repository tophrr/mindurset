package com.mindurset.app.ui.reflection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// SCAFFOLD STUBS - owned by the reflection CRUD workstream. Replace bodies, keep signatures.

@Composable
fun ReflectionListScreen(
    onNew: () -> Unit,
    onOpen: (String) -> Unit,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Reflection list placeholder")
    }
}

@Composable
fun ReflectionFormScreen(
    entryId: String?,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Reflection form placeholder")
    }
}

@Composable
fun ReflectionDetailScreen(
    entryId: String,
    onEdit: () -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.padding(24.dp)) {
        Text("Reflection detail placeholder")
    }
}
