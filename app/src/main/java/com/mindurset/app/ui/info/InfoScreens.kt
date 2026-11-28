package com.mindurset.app.ui.info

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.profile.DataConsentViewModel
import com.mindurset.app.ui.profile.ExportDocumentWriter
import com.mindurset.app.ui.profile.ExportStatus
import com.mindurset.app.ui.profile.MindTopBar
import com.mindurset.app.ui.theme.DangerRed
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.SuccessGreen

private const val EXPORT_MIME_TYPE = "application/json"
private const val EXPORT_DEFAULT_FILENAME = "mindurset-export-v1.json"

/** Plain, honest summary of what the app stores and where, plus export and full wipe. */
@Composable
fun DataConsentScreen(onBack: () -> Unit, onWithdrawn: () -> Unit = onBack, onOpenMemory: () -> Unit = {}) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val viewModel: DataConsentViewModel = viewModel {
        DataConsentViewModel(
            exporter = container.exportLocalData,
            dataWiper = container.localDataWiper,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showRevokeDialog by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(EXPORT_MIME_TYPE),
    ) { uri ->
        if (uri != null) {
            viewModel.export { json -> ExportDocumentWriter.writeUtf8(context, uri, json) }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MindTopBar(
                title = stringResource(R.string.consent_title),
                onBack = onBack,
                navDescription = stringResource(R.string.profile_back),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.consent_heading),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = NavyInk,
                    )
                    Text(
                        text = stringResource(R.string.consent_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NavyInk,
                    )
                    BulletLine(stringResource(R.string.consent_bullet_local))
                    BulletLine(stringResource(R.string.consent_bullet_no_server))
                    BulletLine(stringResource(R.string.consent_bullet_delete))
                    Text(
                        text = stringResource(R.string.consent_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    TextButton(onClick = onOpenMemory) {
                        Text(stringResource(R.string.consent_manage_memory))
                    }
                }
            }

            AppCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { exportLauncher.launch(EXPORT_DEFAULT_FILENAME) },
                        enabled = !state.exporting && !state.wiping,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text(
                            stringResource(
                                if (state.exporting) {
                                    R.string.consent_export_in_progress
                                } else {
                                    R.string.consent_export_button
                                },
                            ),
                        )
                    }
                    Text(
                        text = stringResource(R.string.consent_export_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGray,
                    )
                    when (state.exportStatus) {
                        ExportStatus.Saved -> Text(
                            text = stringResource(R.string.consent_export_saved),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = SuccessGreen,
                        )

                        ExportStatus.Failed -> Text(
                            text = stringResource(R.string.consent_export_failed),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = DangerRed,
                        )

                        null -> Unit
                    }
                }
            }

            AppCard {
                TextButton(
                    onClick = { showRevokeDialog = true },
                    enabled = !state.wiping && !state.exporting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(
                            if (state.wiping) R.string.consent_withdrawing else R.string.consent_withdraw,
                        ),
                        color = DangerRed,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    if (showRevokeDialog) {
        WithdrawConsentDialog(
            wiping = state.wiping,
            onKeepData = { showRevokeDialog = false },
            onConfirmDelete = {
                showRevokeDialog = false
                viewModel.revokeAndWipe(onWithdrawn)
            },
        )
    }
}

@Composable
private fun WithdrawConsentDialog(wiping: Boolean, onKeepData: () -> Unit, onConfirmDelete: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!wiping) onKeepData() },
        title = { Text(stringResource(R.string.consent_withdraw_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.consent_withdraw_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onKeepData,
                    enabled = !wiping,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(stringResource(R.string.consent_withdraw_cancel))
                }
                TextButton(
                    onClick = onConfirmDelete,
                    enabled = !wiping,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.consent_withdraw_confirm),
                        color = DangerRed,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {},
    )
}

/** App identity, version taken from the installed package, and licence note. */
@Suppress("DEPRECATION")
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty().ifBlank { "0.1.0" }
    }

    Scaffold(
        topBar = {
            MindTopBar(
                title = stringResource(R.string.about_title),
                onBack = onBack,
                navDescription = stringResource(R.string.profile_back),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.about_app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.about_version_label, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.about_tagline),
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.about_license_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.about_license_code),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.about_license_corpus),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.about_license_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BulletLine(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = "\u2022", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}
