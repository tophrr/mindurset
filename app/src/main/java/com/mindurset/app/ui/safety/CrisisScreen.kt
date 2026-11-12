package com.mindurset.app.ui.safety

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindurset.app.R
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.safety.CrisisResources
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk

/**
 * The card the offline floor sends someone to when a note contains a red flag.
 *
 * It is deliberately not a conversation: nothing to fill in, one number to call, one way out that
 * says the person is safe for now. The back gesture is blocked until that acknowledgement, which is
 * what "cannot be dismissed" means in practice.
 */
@Composable
fun CrisisScreen(level: RiskLevel = RiskLevel.L3, onAcknowledge: () -> Unit) {
    BackHandler(enabled = true) { /* acknowledgement, not the back gesture, leaves this screen */ }

    val direct = level >= RiskLevel.L3
    val context = LocalContext.current
    val dial = {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${CrisisResources.PHONE}"))
        context.startActivity(intent)
    }

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(if (direct) R.string.crisis_title else R.string.crisis_title_l2),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(if (direct) R.string.crisis_body else R.string.crisis_body_l2),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        AppCard {
            Text(
                text = stringResource(R.string.crisis_number_label),
                style = MaterialTheme.typography.labelLarge,
                color = MutedGray,
            )
            Text(
                text = CrisisResources.display(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NavyInk,
            )
            Spacer(Modifier.size(8.dp))
            Button(
                onClick = dial,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(stringResource(R.string.crisis_call))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.crisis_contact_note),
            style = MaterialTheme.typography.bodySmall,
            color = MutedGray,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onAcknowledge, modifier = Modifier.height(48.dp)) {
            Text(stringResource(if (direct) R.string.crisis_acknowledge else R.string.crisis_acknowledge_l2))
        }
    }
}
