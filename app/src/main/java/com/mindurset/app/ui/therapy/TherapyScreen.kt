package com.mindurset.app.ui.therapy

import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.CanvasBlue
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.PaleBlue
import com.mindurset.app.ui.theme.SurfaceWhite

/**
 * Static CBT skills screen matching uidesign/Product Page.jpg. All three steps are
 * presented as normal working cards — none is "Locked". The action buttons look and
 * feel enabled but have no behaviour yet; the header says so plainly.
 */
@Composable
fun TherapyScreen(
    onOpenDrawer: () -> Unit,
) {
    Scaffold(containerColor = CanvasBlue) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasBlue)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = FloatingNavSpace),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTopBar(
                title = stringResource(R.string.therapy_title),
                menuDescription = stringResource(R.string.therapy_open_menu),
                onOpenDrawer = onOpenDrawer,
            )

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
private fun ScreenTopBar(
    title: String,
    menuDescription: String,
    onOpenDrawer: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            icon = AppIcons.Menu,
            contentDescription = menuDescription,
            onClick = onOpenDrawer,
        )
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            color = NavyDeep,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.size(42.dp))
    }
}

@Composable
private fun StepCard(
    icon: ImageVector,
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
                    .background(PaleBlue),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = NavyDeep,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NavyInk,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 23.sp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = label,
                    color = NavyDeep,
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
                            .background(PaleBlue)
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    ) {
                        Text(
                            text = chip,
                            color = NavyInk,
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
                containerColor = NavyDeep,
                contentColor = SurfaceWhite,
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text(text = action, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
