package com.mindurset.app.ui.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk

/**
 * The gate the proposal calls "explicit, revocable consent before the first check-in". It is a
 * screen rather than a dialog because a dialog can be dismissed with the back gesture, and a
 * dismissed consent is not a consent.
 */
@Composable
fun ConsentScreen(onAgreed: () -> Unit, onLater: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ConsentViewModel = viewModel {
        ConsentViewModel(container.consentRepository, container.sessionStore)
    }
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.consent_gate_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
        )
        Text(
            text = stringResource(R.string.consent_gate_lead),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedGray,
        )
        AppCard {
            Text(
                text = stringResource(R.string.consent_gate_local),
                style = MaterialTheme.typography.bodyMedium,
                color = NavyInk,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.consent_gate_limits),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = { viewModel.grant(onAgreed) },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text(stringResource(R.string.consent_gate_agree))
        }
        TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text(stringResource(R.string.consent_gate_later))
        }
        Text(
            text = stringResource(R.string.consent_gate_later_note),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
        )
    }
}
