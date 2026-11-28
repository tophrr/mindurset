package com.mindurset.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.OnboardingBaseline
import com.mindurset.app.domain.model.OnboardingFocus
import com.mindurset.app.domain.model.OnboardingSummaryKey
import com.mindurset.app.domain.model.SleepQuality
import com.mindurset.app.domain.model.SupportLevel
import com.mindurset.app.domain.model.ValidateOnboardingBaseline
import com.mindurset.app.domain.repository.OnboardingBaselineRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class OnboardingStep {
    INTRO,
    BASELINE,
    DONE,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.INTRO,
    val recentMood: Emotion? = null,
    val sleepQuality: SleepQuality? = null,
    val supportLevel: SupportLevel? = null,
    val primaryFocus: OnboardingFocus? = null,
    val saving: Boolean = false,
    val savedBaseline: OnboardingBaseline? = null,
    val summaryKey: OnboardingSummaryKey? = null,
) {
    val canSubmitBaseline: Boolean
        get() =
            !saving &&
                ValidateOnboardingBaseline.isComplete(
                    recentMood = recentMood,
                    sleepQuality = sleepQuality,
                    supportLevel = supportLevel,
                    primaryFocus = primaryFocus,
                )
}

/**
 * Drives the three-step first-run onboarding flow. Selections and saved answers are never logged
 * (NFR-08), and skipping records completion without writing a baseline row.
 */
class OnboardingViewModel(
    private val baselineRepository: OnboardingBaselineRepository,
    private val preferencesStore: PreferencesStore,
    private val sessionStore: SessionStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun continueFromIntro() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(step = OnboardingStep.BASELINE) }
    }

    fun backToIntro() {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(step = OnboardingStep.INTRO) }
    }

    fun selectMood(emotion: Emotion) {
        _uiState.update { it.copy(recentMood = emotion) }
    }

    fun selectSleep(sleepQuality: SleepQuality) {
        _uiState.update { it.copy(sleepQuality = sleepQuality) }
    }

    fun selectSupport(supportLevel: SupportLevel) {
        _uiState.update { it.copy(supportLevel = supportLevel) }
    }

    fun selectFocus(focus: OnboardingFocus) {
        _uiState.update { it.copy(primaryFocus = focus) }
    }

    fun skip(onFinished: () -> Unit) {
        if (_uiState.value.saving) return
        _uiState.update { it.copy(saving = true) }
        scope.launch {
            preferencesStore.setOnboardingCompleted(true)
            _uiState.update { it.copy(saving = false) }
            onFinished()
        }
    }

    fun saveBaseline() {
        val current = _uiState.value
        val mood = current.recentMood ?: return
        val sleep = current.sleepQuality ?: return
        val support = current.supportLevel ?: return
        val focus = current.primaryFocus ?: return
        if (current.saving) return

        _uiState.update { it.copy(saving = true) }
        scope.launch {
            val userId = sessionStore.current()?.userId
            val baseline =
                if (userId != null) {
                    val existing = baselineRepository.findForUser(userId)
                    OnboardingBaseline(
                        id = existing?.id ?: newId(),
                        userId = userId,
                        recentMood = mood,
                        sleepQuality = sleep,
                        supportLevel = support,
                        primaryFocus = focus,
                        createdAt = clock(),
                    ).also { baselineRepository.save(it) }
                } else {
                    null
                }
            preferencesStore.setOnboardingCompleted(true)
            _uiState.update {
                it.copy(
                    saving = false,
                    step = OnboardingStep.DONE,
                    savedBaseline = baseline,
                    summaryKey = OnboardingSummaryKey.fromEmotion(mood),
                )
            }
        }
    }
}
