package com.mindurset.app.ui.home.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.ConsentPolicy
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.ConsentRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.usecase.EvaluateTriage
import com.mindurset.app.safety.LocalRiskFloor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Holds the in-progress check-in. Because the selection, intensity and note live in
 * the ViewModel (not `remember`), they survive configuration changes such as
 * rotation. Saving writes one [CheckIn] to Room through the repository.
 */
class CheckInViewModel(
    private val checkInRepository: CheckInRepository,
    private val sessionStore: SessionStore,
    private val safetyRepository: SafetyRepository,
    private val consentRepository: ConsentRepository,
    /** Emotion already picked on the home card, so the form opens pre-filled. */
    initialEmotion: Emotion? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val _emotion = MutableStateFlow(initialEmotion)
    val emotion: StateFlow<Emotion?> = _emotion.asStateFlow()

    private val _intensity = MutableStateFlow(DEFAULT_INTENSITY)
    val intensity: StateFlow<Int> = _intensity.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _offerReframe = MutableStateFlow(false)

    /** True when the triage rule wants to follow this check-in with a reframe. */
    val offerReframe: StateFlow<Boolean> = _offerReframe.asStateFlow()

    private val _crisisLevel = MutableStateFlow<RiskLevel?>(null)

    /** Set when the offline floor found risk in the note, so the screen shows the crisis card. */
    val crisisLevel: StateFlow<RiskLevel?> = _crisisLevel.asStateFlow()

    private val _needsConsent = MutableStateFlow(false)

    /** Set when a save was refused because no consent row exists for this person. */
    val needsConsent: StateFlow<Boolean> = _needsConsent.asStateFlow()

    fun dismissConsentPrompt() {
        _needsConsent.value = false
    }

    fun selectEmotion(emotion: Emotion) {
        _emotion.value = emotion
    }

    fun setIntensity(value: Int) {
        _intensity.value = value.coerceIn(MIN_INTENSITY, MAX_INTENSITY)
    }

    fun setNote(value: String) {
        _note.value = value
    }

    /** Inserts the check-in for the signed-in user, then runs [onSaved] on the main thread. */
    fun save(onSaved: () -> Unit) {
        val selected = _emotion.value ?: return
        if (_saving.value) return
        _saving.value = true
        scope.launch {
            val userId = sessionStore.current()?.userId
            if (userId != null) {
                val consent = consentRepository.find(userId, ConsentPolicy.KIND)
                if (consent == null || consent.revokedAt != null) {
                    _saving.value = false
                    _needsConsent.value = true
                    return@launch
                }
                val noteText = _note.value.trim()
                checkInRepository.insert(
                    CheckIn(
                        id = newId(),
                        userId = userId,
                        emotion = selected,
                        intensity = _intensity.value,
                        note = noteText.ifBlank { null },
                        createdAt = clock(),
                    ),
                )
                val hit = LocalRiskFloor.match(noteText)
                if (hit != null && hit.level >= RiskLevel.L2) {
                    val at = clock()
                    safetyRepository.record(
                        SafetyEvent(
                            id = newId(),
                            userId = userId,
                            level = hit.level,
                            trigger = TRIGGER_CHECK_IN_NOTE,
                            patternId = hit.patternId,
                            createdAt = at,
                            retentionUntil = at + SAFETY_EVENT_RETENTION_MILLIS,
                        ),
                    )
                    _saving.value = false
                    _crisisLevel.value = hit.level
                    return@launch
                }
            }
            _saving.value = false
            val offer =
                EvaluateTriage.evaluate(selected, _intensity.value) == EvaluateTriage.Offer.Reframe
            _offerReframe.value = offer
            if (!offer) onSaved()
        }
    }

    /** The user declined the offer, so the screen leaves as it always did. */
    fun skipOffer(onDone: () -> Unit) {
        _offerReframe.value = false
        onDone()
    }

    /** Called when the crisis card has been acknowledged, so it is not shown twice. */
    fun acknowledgeCrisis() {
        _crisisLevel.value = null
    }

    companion object {
        const val MIN_INTENSITY = 1
        const val MAX_INTENSITY = 10
        private const val DEFAULT_INTENSITY = 5
        private const val TRIGGER_CHECK_IN_NOTE = "checkin_note"

        /** Safety events are kept for thirty days, then pruned by the data policy. */
        private const val SAFETY_EVENT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
