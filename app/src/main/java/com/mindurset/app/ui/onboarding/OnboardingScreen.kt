package com.mindurset.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mindurset.app.R
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.OnboardingFocus
import com.mindurset.app.domain.model.OnboardingSummaryKey
import com.mindurset.app.domain.model.SleepQuality
import com.mindurset.app.domain.model.SupportLevel
import com.mindurset.app.ui.common.AppCard
import com.mindurset.app.ui.common.SectionTitle
import com.mindurset.app.ui.common.emotionLabel
import com.mindurset.app.ui.common.tint
import com.mindurset.app.ui.theme.AppIcons
import com.mindurset.app.ui.theme.MutedGray
import com.mindurset.app.ui.theme.NavyDeep
import com.mindurset.app.ui.theme.NavyInk
import com.mindurset.app.ui.theme.SuccessGreen

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val container = LocalAppContainer.current
    val vm: OnboardingViewModel = viewModel {
        OnboardingViewModel(
            baselineRepository = container.onboardingBaselineRepository,
            preferencesStore = container.preferencesStore,
            sessionStore = container.sessionStore,
        )
    }
    val state by vm.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.step == OnboardingStep.BASELINE) {
        vm.backToIntro()
    }

    when (state.step) {
        OnboardingStep.INTRO -> IntroStep(
            saving = state.saving,
            onContinue = vm::continueFromIntro,
            onSkip = { vm.skip(onFinished) },
        )

        OnboardingStep.BASELINE -> BaselineStep(
            state = state,
            onSelectMood = vm::selectMood,
            onSelectSleep = vm::selectSleep,
            onSelectSupport = vm::selectSupport,
            onSelectFocus = vm::selectFocus,
            onSave = vm::saveBaseline,
            onBack = vm::backToIntro,
        )

        OnboardingStep.DONE -> DoneStep(
            summaryKey = state.summaryKey,
            onStart = onFinished,
        )
    }
}

@Composable
private fun IntroStep(saving: Boolean, onContinue: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.onboarding_intro_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = NavyInk,
        )
        Text(
            text = stringResource(R.string.onboarding_intro_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedGray,
        )
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                IntroBlock(
                    title = stringResource(R.string.onboarding_intro_does_title),
                    body = stringResource(R.string.onboarding_intro_does_body),
                )
                IntroBlock(
                    title = stringResource(R.string.onboarding_intro_not_title),
                    body = stringResource(R.string.onboarding_intro_not_body),
                )
                IntroBlock(
                    title = stringResource(R.string.onboarding_intro_data_title),
                    body = stringResource(R.string.onboarding_intro_data_body),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = onContinue,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text(stringResource(R.string.onboarding_intro_continue))
        }
        TextButton(
            onClick = onSkip,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(stringResource(R.string.onboarding_intro_skip))
        }
    }
}

@Composable
private fun IntroBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = NavyInk,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MutedGray,
        )
    }
}

@Composable
private fun BaselineStep(
    state: OnboardingUiState,
    onSelectMood: (Emotion) -> Unit,
    onSelectSleep: (SleepQuality) -> Unit,
    onSelectSupport: (SupportLevel) -> Unit,
    onSelectFocus: (OnboardingFocus) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.onboarding_baseline_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = NavyInk,
            )
            Text(
                text = stringResource(R.string.onboarding_baseline_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MutedGray,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.onboarding_question_mood))
            Emotion.entries.forEach { option ->
                ChoiceCard(
                    label = emotionLabel(option),
                    selected = state.recentMood == option,
                    accent = option.tint(),
                    onClick = { onSelectMood(option) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.onboarding_question_sleep))
            SleepQuality.entries.forEach { option ->
                ChoiceCard(
                    label = stringResource(sleepLabelRes(option)),
                    selected = state.sleepQuality == option,
                    onClick = { onSelectSleep(option) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.onboarding_question_support))
            SupportLevel.entries.forEach { option ->
                ChoiceCard(
                    label = stringResource(supportLabelRes(option)),
                    selected = state.supportLevel == option,
                    onClick = { onSelectSupport(option) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(stringResource(R.string.onboarding_question_focus))
            OnboardingFocus.entries.forEach { option ->
                ChoiceCard(
                    label = stringResource(focusLabelRes(option)),
                    selected = state.primaryFocus == option,
                    onClick = { onSelectFocus(option) },
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onSave,
                enabled = state.canSubmitBaseline,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                Text(stringResource(R.string.onboarding_baseline_continue))
            }
            TextButton(
                onClick = onBack,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun DoneStep(summaryKey: OnboardingSummaryKey?, onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        AppCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = SuccessGreen,
                )
                Text(
                    text = stringResource(R.string.onboarding_done_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyInk,
                )
                Text(
                    text = stringResource(onboardingSummaryRes(summaryKey)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedGray,
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Text(stringResource(R.string.onboarding_done_start))
                }
            }
        }
    }
}

@Composable
private fun ChoiceCard(label: String, selected: Boolean, accent: Color = NavyDeep, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) accent else NavyInk,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = accent,
                )
            }
        }
    }
}

@StringRes
private fun sleepLabelRes(sleepQuality: SleepQuality): Int = when (sleepQuality) {
    SleepQuality.RESTFUL -> R.string.onboarding_sleep_restful
    SleepQuality.MIXED -> R.string.onboarding_sleep_mixed
    SleepQuality.LIGHT_OR_BROKEN -> R.string.onboarding_sleep_light
    SleepQuality.TOO_LITTLE -> R.string.onboarding_sleep_too_little
}

@StringRes
private fun supportLabelRes(supportLevel: SupportLevel): Int = when (supportLevel) {
    SupportLevel.STRONG -> R.string.onboarding_support_strong
    SupportLevel.SOME -> R.string.onboarding_support_some
    SupportLevel.ALONE -> R.string.onboarding_support_alone
    SupportLevel.UNSURE -> R.string.onboarding_support_unsure
}

@StringRes
private fun focusLabelRes(focus: OnboardingFocus): Int = when (focus) {
    OnboardingFocus.CALMING_WORRY -> R.string.onboarding_focus_worry
    OnboardingFocus.SHIFTING_THOUGHTS -> R.string.onboarding_focus_thoughts
    OnboardingFocus.DAILY_RHYTHM -> R.string.onboarding_focus_rhythm
    OnboardingFocus.JUST_EXPLORING -> R.string.onboarding_focus_exploring
}

@StringRes
internal fun onboardingSummaryRes(summaryKey: OnboardingSummaryKey?): Int = when (summaryKey) {
    OnboardingSummaryKey.HAPPY -> R.string.onboarding_done_summary_happy
    OnboardingSummaryKey.CALM -> R.string.onboarding_done_summary_calm
    OnboardingSummaryKey.LOW -> R.string.onboarding_done_summary_low
    OnboardingSummaryKey.ANXIOUS -> R.string.onboarding_done_summary_anxious
    OnboardingSummaryKey.STRESSED -> R.string.onboarding_done_summary_stressed
    null -> R.string.onboarding_done_summary_default
}
