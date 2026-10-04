package com.mindurset.app.ui.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DataConsentScreen(onBack: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text("Data and consent placeholder")
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text("About placeholder")
    }
}
