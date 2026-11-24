package com.mindurset.app.ui.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.data.remote.GenerationResponse
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.OfflineExercise
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.OfflineExerciseLibrary
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.usecase.SelectOfflineExercise
import com.mindurset.app.safety.AsqFlow
import com.mindurset.app.safety.RiskScreening
import com.mindurset.app.safety.Screening
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import java.util.UUID

/** The same retention the check-in and journal paths apply to a safety event. */
private const val SAFETY_EVENT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000

/** What the local floor alone saw. */
private const val TRIGGER_MESSAGE = "companion_message"

/** What the Worker's classifier raised the level to. */
private const val TRIGGER_CLASSIFIER = "companion_classifier"

/** A call that could not be made, escalated one tier. */
private const val TRIGGER_CLASSIFIER_UNAVAILABLE = "companion_classifier_unavailable"

/** A positive ASQ screen following an ambiguous L2 signal. */
private const val TRIGGER_ASQ_POSITIVE = "asq_positive"

class CompanionViewModel(
    private val generationClient: GenerationClient,
    private val safetyRepository: SafetyRepository,
    private val sessionStore: SessionStore,
    private val checkInRepository: CheckInRepository? = null,
    private val preferencesStore: PreferencesStore? = null,
    private val exerciseLibrary: OfflineExerciseLibrary? = null,
    private val deviceLanguage: () -> String = { Locale.getDefault().language },
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = externalScope ?: viewModelScope
    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()

    /** Set when the offline floor or ASQ found L3 risk, so the screen shows the crisis card. */
    private val _crisisLevel = MutableStateFlow<RiskLevel?>(null)
    val crisisLevel: StateFlow<RiskLevel?> = _crisisLevel.asStateFlow()

    /**
     * Set when the floor or classifier found ambiguous hopelessness (L2): the conversation holds
     * while the four screening items are answered, because a yes reaches the crisis card and four
     * no answers return to an offline exercise with the helpline one tap away.
     */
    private val _asq = MutableStateFlow<AsqFlow.State?>(null)
    val asq: StateFlow<AsqFlow.State?> = _asq.asStateFlow()

    private var lastRequest: PendingRequest? = null
    private var pendingAsq: PendingAsq? = null

    fun selectMode(mode: CompanionSupportMode) {
        if (_uiState.value.replyState != CompanionReplyState.Sending && _asq.value == null) {
            _uiState.update { it.copy(supportMode = mode) }
        }
    }

    /**
     * The offline floor runs before anything else. An L3 line goes straight to the crisis card; an
     * L2 line opens the ASQ screening panel. Neither joins the transcript or leaves the device.
     */
    fun send(message: String) {
        val clean = message.trim()
        if (clean.isBlank() || _uiState.value.replyState == CompanionReplyState.Sending || _asq.value != null) {
            return
        }

        val screening = RiskScreening.screenLocal(clean)
        if (screening.halts) {
            lastRequest = null
            handleScreening(screening, trigger = TRIGGER_MESSAGE, heldReply = null)
            return
        }

        val request = PendingRequest(clean, _uiState.value.supportMode)
        lastRequest = request
        _uiState.update {
            it.copy(
                lines = it.lines + CompanionLine(text = clean, fromUser = true),
                replyState = CompanionReplyState.Sending,
                resourcesOffered = false,
                asqSupportOffered = false,
            )
        }
        request(request)
    }

    /** Called once the card is on screen, before navigation, so a return does not show it twice. */
    fun crisisAcknowledged() {
        _crisisLevel.value = null
    }

    /**
     * Answers the question on screen. A positive screen records an L3 safety event and hands over
     * to the crisis card; four no answers return to the conversation without sending any locally
     * flagged text to the Worker, offering the offline exercise and optional helpline banner.
     */
    fun answerAsq(yes: Boolean) {
        val state = _asq.value ?: return
        val question = AsqFlow.nextQuestion(state) ?: return
        val next = AsqFlow.answer(state, question.id, yes)
        val result = AsqFlow.result(next)
        if (result == null) {
            _asq.value = next
            return
        }
        val held = pendingAsq
        pendingAsq = null
        _asq.value = null
        if (result.level >= RiskLevel.L3) {
            recordSafetyEvent(level = RiskLevel.L3, patternId = null, trigger = TRIGGER_ASQ_POSITIVE)
            _crisisLevel.value = RiskLevel.L3
            return
        }
        when (held) {
            is PendingAsq.ClassifierReply -> {
                val response = held.response
                _uiState.update {
                    it.copy(
                        lines = it.lines + CompanionLine(
                            text = response.message,
                            fromUser = false,
                            sourceIds = response.sourceIds,
                        ),
                        replyState = CompanionReplyState.Reply(response),
                        resourcesOffered = false,
                        asqSupportOffered = true,
                    )
                }
            }

            PendingAsq.LocalFloor, null -> {
                scope.launch {
                    val exercise = selectOfflineExercise(
                        PendingRequest(message = "", mode = _uiState.value.supportMode),
                    )
                    _uiState.update {
                        it.copy(
                            lines = it.lines + CompanionLine(
                                text = null,
                                fromUser = false,
                                offline = true,
                                exercise = exercise,
                            ),
                            replyState = CompanionReplyState.Idle,
                            resourcesOffered = false,
                            asqSupportOffered = true,
                        )
                    }
                }
            }
        }
    }

    fun retry() {
        val pending = lastRequest ?: return
        if (_uiState.value.replyState == CompanionReplyState.Sending || _asq.value != null) return
        request(pending)
    }

    private fun recordSafetyEvent(level: RiskLevel, patternId: String?, trigger: String) {
        scope.launch {
            val session = sessionStore.current() ?: return@launch
            val at = now()
            safetyRepository.record(
                SafetyEvent(
                    id = newId(),
                    userId = session.userId,
                    level = level,
                    trigger = trigger,
                    patternId = patternId,
                    createdAt = at,
                    retentionUntil = at + SAFETY_EVENT_RETENTION_MILLIS,
                ),
            )
        }
    }

    private fun request(request: PendingRequest) {
        _uiState.update { it.copy(replyState = CompanionReplyState.Sending) }
        scope.launch {
            try {
                val response = generationClient.reply(request.message, request.mode.workerMode)
                // The Worker classifies too, and it may only raise the floor.
                val combined = RiskScreening.screenWith(request.message, response.classifierLevel())
                if (combined.halts) {
                    _uiState.update { it.copy(replyState = CompanionReplyState.Idle, resourcesOffered = false) }
                    handleScreening(combined, trigger = TRIGGER_CLASSIFIER, heldReply = response)
                    return@launch
                }
                _uiState.update {
                    it.copy(
                        lines = it.lines + CompanionLine(
                            text = response.message,
                            fromUser = false,
                            sourceIds = response.sourceIds,
                        ),
                        replyState = CompanionReplyState.Reply(response),
                        resourcesOffered = false,
                        asqSupportOffered = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                failClosed(request, CompanionReplyState.OfflineFallback)
            } catch (_: Exception) {
                failClosed(request, CompanionReplyState.Error)
            }
        }
    }

    /**
     * The classifier could not answer. Fail closed: one tier up from the floor, and the offline copy
     * gains a way to reach a person when the raised level is the L1 affordance.
     */
    private suspend fun failClosed(request: PendingRequest, state: CompanionReplyState) {
        val screening = RiskScreening.screenWith(request.message, classifier = null)
        if (screening.halts) {
            addOfflineFallback(state, exercise = null)
            handleScreening(screening, trigger = TRIGGER_CLASSIFIER_UNAVAILABLE, heldReply = null)
            return
        }
        val exercise = selectOfflineExercise(request)
        addOfflineFallback(state, exercise = exercise)
        if (screening.level == RiskLevel.L1) {
            _uiState.update { it.copy(resourcesOffered = true) }
        }
    }

    private fun handleScreening(screening: Screening, trigger: String, heldReply: GenerationResponse?) {
        recordSafetyEvent(level = screening.level, patternId = screening.patternId, trigger = trigger)
        if (screening.level >= RiskLevel.L3) {
            _crisisLevel.value = screening.level
        } else {
            // L2 is the ambiguous rung: the ASQ items decide, not the phrase table alone.
            pendingAsq = if (heldReply != null) PendingAsq.ClassifierReply(heldReply) else PendingAsq.LocalFloor
            _asq.value = AsqFlow.State()
        }
    }

    private suspend fun selectOfflineExercise(request: PendingRequest): OfflineExercise? {
        val library = exerciseLibrary ?: return null
        val language = resolveExerciseLanguage()
        val exercises = library.loadAll(language)
        if (exercises.isEmpty()) return null
        val emotion = latestCheckInEmotion()
        return SelectOfflineExercise.select(
            exercises = exercises,
            message = request.message,
            emotion = emotion,
            fallbackTopic = request.mode.fallbackTopic,
        )
    }

    private suspend fun resolveExerciseLanguage(): String {
        val pref = preferencesStore?.language?.firstOrNull()?.lowercase()
        val device = deviceLanguage().lowercase()
        return if (pref == "id" || device == "id" || device == "in") "id" else "en"
    }

    private suspend fun latestCheckInEmotion(): Emotion? {
        val repo = checkInRepository ?: return null
        val userId = sessionStore.current()?.userId ?: return null
        return repo.observeAll(userId).firstOrNull()?.maxByOrNull { it.createdAt }?.emotion
    }

    private fun addOfflineFallback(state: CompanionReplyState, exercise: OfflineExercise?) {
        _uiState.update {
            it.copy(
                lines = it.lines + CompanionLine(text = null, fromUser = false, offline = true, exercise = exercise),
                replyState = state,
            )
        }
    }

    private data class PendingRequest(val message: String, val mode: CompanionSupportMode)

    private sealed interface PendingAsq {
        data object LocalFloor : PendingAsq

        data class ClassifierReply(val response: GenerationResponse) : PendingAsq
    }
}

/** The reply's own level, or null when it is not one the client knows. */
private fun GenerationResponse.classifierLevel(): RiskLevel? =
    runCatching { RiskLevel.valueOf(riskLevel.level) }.getOrNull()
