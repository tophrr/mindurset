package com.mindurset.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindurset.app.ui.common.CircleIconButton
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.NavyInk

/**
 * Lightweight top bar shared by the profile, therapy, companion and info screens.
 * The leading action is a white circular monoline button: either the drawer menu
 * ([AppIcons.Menu]) or a back arrow ([AppIcons.ArrowBack]). No text glyphs.
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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            onBack != null -> CircleIconButton(
                icon = AppIcons.ArrowBack,
                contentDescription = navDescription,
                onClick = onBack,
            )

            onOpenDrawer != null -> CircleIconButton(
                icon = AppIcons.Menu,
                contentDescription = navDescription,
                onClick = onOpenDrawer,
            )

            else -> Spacer(Modifier.size(42.dp))
        }

        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = NavyInk,
        )

        Spacer(Modifier.size(42.dp))
    }
}
