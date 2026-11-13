package com.mindurset.app.ui.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindurset.app.R
import com.mindurset.app.safety.AsqFlow
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk

/**
 * The four ASQ items plus the acuity probe, one at a time. It replaces the check-in form while it is
 * open: the check-in is already filled in and held back, so leaving this screen saves nothing.
 *
 * The wording is the published instrument; the Indonesian translation is ours and not clinically
 * validated (ADR 0007).
 */
@Composable
fun AsqPanel(state: AsqFlow.State, onAnswer: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val question = AsqFlow.nextQuestion(state) ?: return
    val textRes = questionText(question.id) ?: return

    Column(
        modifier =
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.asq_progress, AsqFlow.position(state), AsqFlow.total(state)),
            style = MaterialTheme.typography.labelLarge,
            color = MutedGray,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { onAnswer(true) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text(stringResource(R.string.asq_yes))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { onAnswer(false) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text(stringResource(R.string.asq_no))
        }
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.asq_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MutedGray,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The instrument's items as resources, so `AsqFlow` stays free of Android. */
private fun questionText(id: String): Int? = when (id) {
    AsqFlow.ID_Q1 -> R.string.asq_q1
    AsqFlow.ID_Q2 -> R.string.asq_q2
    AsqFlow.ID_Q3 -> R.string.asq_q3
    AsqFlow.ID_Q4 -> R.string.asq_q4
    AsqFlow.ID_ACUITY -> R.string.asq_acuity
    else -> null
}
