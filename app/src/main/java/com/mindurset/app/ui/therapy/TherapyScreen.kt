package com.mindurset.app.ui.therapy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindurset.app.R
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.CircleIconButton
import com.mindurset.app.ui.common.FloatingNavSpace
import com.mindurset.app.ui.profile.MindTopBar
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.CanvasBlue
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.SurfaceWhite

/**
 * Per-step accent triples for the CBT skills cards, as in the mockup: each step keeps
 * its own colour — green, blue, purple. The tint is the soft card surface (chips, icon
 * box), the accent drives titles, labels, chip text and the action button.
 */
private val Step1Tint = Color(0xFFE2F2E4)
private val Step1Accent = Color(0xFF2A7047) // 5.15:1 on its tint, 5.99:1 white-on-accent
private val Step2Tint = Color(0xFFE3EDFB)
private val Step2Accent = Color(0xFF2A63AC) // 5.11:1 on its tint, 6.04:1 white-on-accent
private val Step3Tint = Color(0xFFF1EAFB)
private val Step3Accent = Color(0xFF7A4FBF)

/**
 * Static CBT skills screen matching uidesign/Product Page.jpg. All three steps are
 * presented as normal working cards — none is "Locked". The action buttons look and
 * feel enabled but have no behaviour yet; the header says so plainly.
 */
@Composable
fun TherapyScreen(onOpenDrawer: () -> Unit) {
    Scaffold(
        containerColor = CanvasBlue,
        contentWindowInsets = WindowInsets(0),
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
                .background(CanvasBlue)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = FloatingNavSpace),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.therapy_heading),
                    color = NavyDeep,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.therapy_subheading),
                    color = MutedGray,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.therapy_release_note),
                    color = MutedGray,
                    fontSize = 13.sp,
                )
            }

            StepCard(
                icon = AppIcons.Therapy,
                tint = Step1Tint,
                accent = Step1Accent,
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
            )

            StepCard(
                icon = AppIcons.Refresh,
                tint = Step2Tint,
                accent = Step2Accent,
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
                action = stringResource(R.string.therapy_continue),
            )

            StepCard(
                icon = AppIcons.Book,
                tint = Step3Tint,
                accent = Step3Accent,
                title = stringResource(R.string.therapy_step3_title),
                label = stringResource(R.string.therapy_step3_label),
                body = stringResource(R.string.therapy_step3_body),
                chipRows = listOf(
                    listOf(
                        stringResource(R.string.therapy_chip_art_journal),
                        stringResource(R.string.therapy_chip_support_network),
                    ),
                ),
                action = stringResource(R.string.therapy_open),
            )
        }
    }
}

@Composable
private fun StepCard(
    icon: ImageVector,
    tint: Color,
    accent: Color,
    title: String,
    label: String,
    body: String,
    chipRows: List<List<String>>,
    action: String,
) {
    AppCard {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = accent,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = accent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 23.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = label,
                    color = accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = body,
                    color = MutedGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
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
                            .background(tint)
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
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = SurfaceWhite,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(text = action, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
