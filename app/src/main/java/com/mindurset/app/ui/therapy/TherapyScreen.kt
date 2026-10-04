package com.mindurset.app.ui.therapy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindurset.app.R
import com.mindurset.app.ui.profile.GlyphTile
import com.mindurset.app.ui.profile.MindTopBar
import androidx.compose.foundation.background

// Palette sampled from uidesign/Product Page.jpg.
private val Navy = Color(0xFF14213D)
private val BodyText = Color(0xFF5A6478)
private val GreenAccent = Color(0xFF2F7D4F)
private val GreenChip = Color(0xFFE2F2E4)
private val GreenButton = Color(0xFFDFF2E1)
private val BlueAccent = Color(0xFF2F6FBF)
private val BlueChip = Color(0xFFE3EDFB)
private val PurpleAccent = Color(0xFF7A4FBF)
private val PurpleChip = Color(0xFFF1EAFB)
private val LockedButton = Color(0xFFB0B0B0)
private val LockedText = Color(0xFF6E6E6E)

/**
 * Static CBT skills screen. It mirrors the product mockup: three ordered steps,
 * the first unlocked with a Begin button, the rest visibly locked. No AI, no network.
 */
@Composable
fun TherapyScreen(
    onOpenDrawer: () -> Unit,
) {
    Scaffold(
        topBar = {
            MindTopBar(
                title = stringResource(R.string.therapy_title),
                onOpenDrawer = onOpenDrawer,
                navDescription = stringResource(R.string.therapy_open_menu),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(
                    text = stringResource(R.string.therapy_heading),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy,
                )
                Text(
                    text = stringResource(R.string.therapy_subheading),
                    fontSize = 14.sp,
                    color = BodyText,
                )
            }

            StepCard(
                glyph = "\u2698",
                title = stringResource(R.string.therapy_step1_title),
                label = stringResource(R.string.therapy_step1_label),
                body = stringResource(R.string.therapy_step1_body),
                chipRows = listOf(
                    listOf(
                        stringResource(R.string.therapy_chip_emotion_log),
                        stringResource(R.string.therapy_chip_finger_breathing),
                    ),
                    listOf(stringResource(R.string.therapy_chip_body_scan)),
                ),
                action = stringResource(R.string.therapy_begin),
                accent = GreenAccent,
                chipBg = GreenChip,
                buttonBg = GreenButton,
                buttonText = GreenAccent,
                enabled = true,
            )

            StepCard(
                glyph = "\u21BB",
                title = stringResource(R.string.therapy_step2_title),
                label = stringResource(R.string.therapy_step2_label),
                body = stringResource(R.string.therapy_step2_body),
                chipRows = listOf(
                    listOf(
                        stringResource(R.string.therapy_chip_thought_triangle),
                        stringResource(R.string.therapy_chip_anxiety_box),
                    ),
                    listOf(stringResource(R.string.therapy_chip_dopamine_menu)),
                ),
                action = stringResource(R.string.therapy_locked),
                accent = BlueAccent,
                chipBg = BlueChip,
                buttonBg = LockedButton,
                buttonText = LockedText,
                enabled = false,
            )

            StepCard(
                glyph = "\u2611",
                title = stringResource(R.string.therapy_step3_title),
                label = stringResource(R.string.therapy_step3_label),
                body = stringResource(R.string.therapy_step3_body),
                chipRows = listOf(
                    listOf(
                        stringResource(R.string.therapy_chip_art_journal),
                        stringResource(R.string.therapy_chip_support_network),
                    ),
                ),
                action = stringResource(R.string.therapy_locked),
                accent = PurpleAccent,
                chipBg = PurpleChip,
                buttonBg = LockedButton,
                buttonText = LockedText,
                enabled = false,
            )
        }
    }
}

@Composable
private fun StepCard(
    glyph: String,
    title: String,
    label: String,
    body: String,
    chipRows: List<List<String>>,
    action: String,
    accent: Color,
    chipBg: Color,
    buttonBg: Color,
    buttonText: Color,
    enabled: Boolean,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlyphTile(
                    glyph = glyph,
                    background = chipBg,
                    foreground = accent,
                    modifier = Modifier.size(56.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Navy,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 24.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = label,
                        color = accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(text = body, color = BodyText, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            Spacer(Modifier.height(14.dp))

            chipRows.forEach { row ->
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(chipBg)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        ) {
                            Text(
                                text = chip,
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {},
                enabled = enabled,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBg,
                    contentColor = buttonText,
                    disabledContainerColor = buttonBg,
                    disabledContentColor = buttonText,
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) {
                Text(text = action, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
