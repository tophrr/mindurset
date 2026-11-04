package com.mindurset.app.ui.therapy

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.mindurset.app.ui.profile.MindTopBar
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyInk

/**
 * Three steps, each one stored as it is typed, then one small action. Back leaves the session open,
 * which is why reopening Therapy later resumes at the same step.
 */
@Composable
fun ReframeScreen(onBack: () -> Unit, onFinished: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: ReframeViewModel = viewModel {
        ReframeViewModel(
            sessions = container.sessionRepository,
            actions = container.microActionRepository,
            sessionStore = container.sessionStore,
        )
    }
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            MindTopBar(
                title = stringResource(R.string.reframe_title),
                onBack = onBack,
                navDescription = stringResource(R.string.reframe_back_cd),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (state.finished) {
                FinishedCard(onFinished = onFinished)
                return@Column
            }

            Text(
                text =
                stringResource(
                    R.string.reframe_progress,
                    minOf(state.step + 1, ReframeViewModel.LAST_INPUT_STEP + 1),
                    ReframeViewModel.LAST_INPUT_STEP + 1,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MutedGray,
            )

            when (state.step) {
                0 ->
                    StepField(
                        title = stringResource(R.string.reframe_step1_title),
                        hint = stringResource(R.string.reframe_step1_hint),
                        value = state.thought,
                        onValueChange = vm::setThought,
                    )

                1 ->
                    StepField(
                        title = stringResource(R.string.reframe_step2_title),
                        hint = stringResource(R.string.reframe_step2_hint),
                        value = state.evidence,
                        onValueChange = vm::setEvidence,
                    )

                2 ->
                    StepField(
                        title = stringResource(R.string.reframe_step3_title),
                        hint = stringResource(R.string.reframe_step3_hint),
                        value = state.alternative,
                        onValueChange = vm::setAlternative,
                    )

                else -> {
                    Text(
                        text = stringResource(R.string.reframe_done_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = NavyInk,
                    )
                    AppCard {
                        Text(
                            text = state.alternative.ifBlank { state.thought },
                            style = MaterialTheme.typography.bodyLarge,
                            color = NavyInk,
                        )
                    }
                    Text(
                        text = stringResource(R.string.reframe_action_prompt),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedGray,
                    )
                    OutlinedTextField(
                        value = state.actionTitle,
                        onValueChange = vm::setAction,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.reframe_action_label)) },
                        placeholder = { Text(stringResource(R.string.reframe_action_placeholder)) },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 5, 10).forEach { minutes ->
                            FilterChip(
                                selected = state.minutes == minutes,
                                onClick = { vm.setMinutes(minutes) },
                                label = { Text(stringResource(R.string.reframe_minutes, minutes)) },
                            )
                        }
                    }
                }
            }

            state.error?.let { error ->
                Text(
                    text =
                    stringResource(
                        when (error) {
                            ReframeViewModel.Error.ThoughtRequired -> R.string.reframe_error_thought
                            ReframeViewModel.Error.EvidenceRequired -> R.string.reframe_error_evidence
                            ReframeViewModel.Error.AlternativeRequired -> R.string.reframe_error_alternative
                            ReframeViewModel.Error.ActionRequired -> R.string.reframe_error_action
                        },
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { if (state.step > ReframeViewModel.LAST_INPUT_STEP) vm.finish() else vm.next() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(
                    stringResource(
                        if (state.step > ReframeViewModel.LAST_INPUT_STEP) {
                            R.string.reframe_finish
                        } else {
                            R.string.reframe_continue
                        },
                    ),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StepField(title: String, hint: String, value: String, onValueChange: (String) -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = NavyInk,
    )
    Text(text = hint, style = MaterialTheme.typography.bodyMedium, color = MutedGray)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        placeholder = { Text(hint) },
    )
}

@Composable
private fun FinishedCard(onFinished: () -> Unit) {
    Text(
        text = stringResource(R.string.reframe_saved_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = NavyInk,
    )
    AppCard {
        Text(
            text = stringResource(R.string.reframe_saved_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedGray,
        )
    }
    Button(
        onClick = onFinished,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(26.dp),
    ) {
        Text(stringResource(R.string.reframe_saved_done))
    }
}
