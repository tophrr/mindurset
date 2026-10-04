package com.mindurset.app.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindurset.app.R
import com.mindurset.app.ui.profile.MindTopBar

/**
 * Static, honest preview of the companion chat. It renders two sample bubbles and
 * a disabled input row; it does not pretend to talk to an AI, because none is wired.
 */
@Composable
fun CompanionScreen(
    onOpenDrawer: () -> Unit,
) {
    val sendDescription = stringResource(R.string.companion_send)
    Scaffold(
        topBar = {
            MindTopBar(
                title = stringResource(R.string.companion_title),
                onOpenDrawer = onOpenDrawer,
                navDescription = stringResource(R.string.companion_open_menu),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TopicChip(text = stringResource(R.string.companion_chip_personal), selected = true)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CompanionBubble(
                    text = stringResource(R.string.companion_message_greeting),
                    time = stringResource(R.string.companion_time_first),
                    fromUser = false,
                )
                CompanionBubble(
                    text = stringResource(R.string.companion_message_user),
                    time = stringResource(R.string.companion_time_second),
                    fromUser = true,
                )
            }

            Text(
                text = stringResource(R.string.companion_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    enabled = false,
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.companion_input_placeholder)) },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {},
                    enabled = false,
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = sendDescription },
                ) {
                    Text(text = "\u27A4", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun CompanionBubble(text: String, time: String, fromUser: Boolean) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            shadowElevation = if (fromUser) 0.dp else 2.dp,
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (fromUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TopicChip(text: String, selected: Boolean) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val foreground = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(text = text, color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
