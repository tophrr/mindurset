package com.mindurset.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight top bar shared by the profile, therapy, companion and info screens.
 * It stays a plain Row so the app does not depend on experimental Material APIs.
 * The leading action is either the drawer menu or a back arrow.
 */
@Composable
fun MindTopBar(
    title: String,
    onOpenDrawer: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    navDescription: String = title,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            onBack != null -> IconButton(
                onClick = onBack,
                modifier = Modifier.semantics { contentDescription = navDescription },
            ) {
                Text(text = "\u2190", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            onOpenDrawer != null -> IconButton(
                onClick = onOpenDrawer,
                modifier = Modifier.semantics { contentDescription = navDescription },
            ) {
                Text(text = "\u2630", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            else -> Spacer(Modifier.size(48.dp))
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.size(48.dp))
    }
}
