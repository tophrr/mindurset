package com.mindurset.app.ui.therapy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.data.remote.GenerationResponse
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionMessage
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryScheduler
import com.mindurset.app.domain.usecase.ReframeStateMachine
import com.mindurset.app.safety.AsqFlow
import com.mindurset.app.safety.RiskScreening
import com.mindurset.app.safety.Screening
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

/**
 * The three-step reframe, stored as it goes.
 *
 * Each answer becomes a [SessionMessage] and the step counter is written to the session row, so
 * killing the process in the middle of step two resumes at step two instead of starting over. The
 * last step turns into one [MicroAction], which is the point of the exercise: leave with something
 * small to do.
 *
 * When a [GenerationClient] is present, the first step is sent to `/v1/reframe` and the answer
 * enriches the rest of the session: the pattern it names, two readings to choose between, and a
 * suggested action. A failed or ungrounded answer is not an error the person has to fix. The
 * session simply continues as the offline exercise, which is the same three fields as before.
 */
class ReframeViewModel(
    private val sessions: SessionRepository,
    private val actions: MicroActionRepository,
    private val sessionStore: SessionStore,
    private val generation: GenerationClient? = null,
    private val summaryScheduler: SessionSummaryScheduler? = null,
    private val safetyRepository: SafetyRepository? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    data class State(
        val step: Int = 0,
        val thought: String = "",
        val evidence: String = "",
        val alternative: String = "",
        val actionTitle: String = "",
        val minutes: Int = 2,
        val error: Error? = null,
        val finished: Boolean = false,
        /** True while the Worker is being asked for a reading. */
        val generating: Boolean = false,
        /** True once a grounded answer arrived; the screen then stops calling itself offline. */
        val generated: Boolean = false,
        /** The thinking pattern the Worker named, in the person's own language. */
        val distortion: String = "",
        /** Two readings to choose between, or empty on the offline path. */
        val perspectives: List<String> = emptyList(),
        /** Corpus identifiers behind the answer, shown as "based on". */
        val sources: List<String> = emptyList(),
    )

    enum class Error { ThoughtRequired, EvidenceRequired, AlternativeRequired, ActionRequired }

    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /** Set when the offline floor, classifier, or ASQ found L3 risk, so the screen shows the crisis card. */
    private val _crisisLevel = MutableStateFlow<RiskLevel?>(null)
    val crisisLevel: StateFlow<RiskLevel?> = _crisisLevel.asStateFlow()

    /**
     * Set when the floor or classifier found ambiguous hopelessness (L2): the step holds while the
     * four screening items are answered, because a yes reaches the crisis card and four no answers
     * continue with the exercise.
     */
    private val _asq = MutableStateFlow<AsqFlow.State?>(null)
    val asq: StateFlow<AsqFlow.State?> = _asq.asStateFlow()

    private var sessionId: String? = null
    private var pendingAsq: PendingAsq? = null

    init {
        scope.launch { restore() }
    }

    fun setThought(value: String) = _state.update { it.copy(thought = value, error = null) }

    fun setEvidence(value: String) = _state.update { it.copy(evidence = value, error = null) }

    fun setAlternative(value: String) = _state.update { it.copy(alternative = value, error = null) }

    fun setAction(value: String) = _state.update { it.copy(actionTitle = value, error = null) }

    fun setMinutes(value: Int) = _state.update { it.copy(minutes = value.coerceIn(1, MAX_MINUTES)) }

    /** Picks one of the readings the Worker offered; the text stays editable afterwards. */
    fun choosePerspective(index: Int) {
        val option = _state.value.perspectives.getOrNull(index) ?: return
        _state.update { it.copy(alternative = option, error = null) }
    }

    /** Called once the card is on screen, before navigation, so a return does not show it twice. */
    fun crisisAcknowledged() {
        _crisisLevel.value = null
    }

    /**
     * Advances one step, saving the answer and the step counter. At step 0, the offline floor
     * runs before anything is persisted or sent to the Worker.
     */
    fun next() {
        val current = _state.value
        if (current.step > LAST_INPUT_STEP || _asq.value != null) return
        val error = errorFor(current)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        val answer = answerFor(current)
        if (current.step == 0) {
            val screening = RiskScreening.screenLocal(answer)
            if (screening.halts) {
                handleScreening(
                    screening = screening,
                    trigger = TRIGGER_REFRAME_THOUGHT,
                    pending = PendingAsq.LocalThought(answer),
                )
                return
            }
        }
        scope.launch { persistStep(step = current.step, answer = answer) }
    }

    /**
     * Answers the ASQ item on screen. A positive screen records an L3 safety event and opens the
     * crisis card without persisting anything further; four no answers release the held thought or
     * generation reply.
     */
    fun answerAsq(yes: Boolean) {
        val currentAsq = _asq.value ?: return
        val question = AsqFlow.nextQuestion(currentAsq) ?: return
        val next = AsqFlow.answer(currentAsq, question.id, yes)
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
            is PendingAsq.LocalThought -> scope.launch { persistStep(step = 0, answer = held.thought) }
            is PendingAsq.ClassifierReply -> scope.launch { persistGeneration(held.response, held.sessionId) }
            null -> Unit
        }
    }

    /** Turns the last step into one micro action and closes the session. */
    fun finish() {
        val current = _state.value
        if (_asq.value != null) return
        if (current.actionTitle.isBlank()) {
            _state.update { it.copy(error = Error.ActionRequired) }
            return
        }
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            val at = now()
            val sid = sessionId ?: newId()
            actions.add(
                MicroAction(
                    id = newId(),
                    userId = userId,
                    sessionId = sessionId,
                    title = current.actionTitle.trim(),
                    minutes = current.minutes,
                    done = false,
                    snoozedUntil = null,
                    createdAt = at,
                    completedAt = null,
                ),
            )
            val transcript = listOf(
                current.thought.trim(),
                current.evidence.trim(),
                current.alternative.trim(),
                current.actionTitle.trim(),
            ).filter { it.isNotEmpty() }
            if (transcript.isNotEmpty()) {
                summaryScheduler?.enqueue(userId = userId, sessionId = sid, transcript = transcript)
            }
            _state.update { it.copy(finished = true, error = null) }
        }
    }

    private suspend fun persistStep(step: Int, answer: String) {
        val userId = sessionStore.current()?.userId ?: return
        val session = ensureSession(userId)
        val machine = machineFor(session)
        val next = machine.advance()
        val role = roleFor(step)
        sessions.appendMessage(
            SessionMessage(
                id = newId(),
                sessionId = session.id,
                role = role,
                content = answer,
                sequence = step,
                createdAt = now(),
            ),
        )
        sessions.save(
            session.copy(
                currentStep = next.currentStep,
                status = next.status.name.lowercase(),
                completedAt = if (next.status == ReframeStateMachine.Status.Completed) now() else null,
            ),
        )
        _state.update { it.copy(step = next.currentStep, error = null) }
        if (step == 0) requestGeneration(answer, session.id)
    }

    /**
     * Asks the Worker for a reading of the thought. Any failure, including an answer that cites no
     * source, leaves the session on the offline path instead of turning the screen into an error.
     * The classifier's level may raise the flow to L2 (ASQ) or L3 (crisis) before anything is
     * persisted or shown.
     */
    private suspend fun requestGeneration(thought: String, sessionId: String) {
        val client = generation ?: return
        _state.update { it.copy(generating = true) }
        try {
            val response = client.reframe(thought)
            val classifierLevel = response.classifierLevel()
            if (classifierLevel != null && classifierLevel >= RiskLevel.L2) {
                _state.update { it.copy(generating = false, generated = false) }
                handleScreening(
                    screening = RiskScreening.screenWith(thought, classifierLevel),
                    trigger = TRIGGER_REFRAME_CLASSIFIER,
                    pending = PendingAsq.ClassifierReply(response = response, sessionId = sessionId),
                )
                return
            }
            persistGeneration(response, sessionId)
        } catch (failure: IOException) {
            _state.update { it.copy(generating = false, generated = false) }
        } catch (failure: IllegalArgumentException) {
            _state.update { it.copy(generating = false, generated = false) }
        }
    }

    private suspend fun persistGeneration(response: GenerationResponse, sessionId: String) {
        val persisted =
            buildString {
                appendLine(response.distortion)
                response.perspectives.forEach { appendLine(it) }
            }
        sessions.appendMessage(
            SessionMessage(
                id = newId(),
                sessionId = sessionId,
                role = ROLE_GENERATION,
                content = persisted.trim(),
                sequence = GENERATION_SEQUENCE,
                createdAt = now(),
                promptVersion = response.promptVersion,
                modelVersion = response.modelVersion,
            ),
        )
        _state.update {
            it.copy(
                generating = false,
                generated = true,
                distortion = response.distortion,
                perspectives = response.perspectives.take(MAX_PERSPECTIVES),
                sources = response.sourceIds,
                minutes = response.microAction.minutes.coerceIn(1, MAX_MINUTES),
                actionTitle = it.actionTitle.ifBlank { response.microAction.title },
            )
        }
    }

    private fun handleScreening(screening: Screening, trigger: String, pending: PendingAsq) {
        recordSafetyEvent(level = screening.level, patternId = screening.patternId, trigger = trigger)
        if (screening.level >= RiskLevel.L3) {
            _crisisLevel.value = screening.level
        } else {
            // L2 is the ambiguous rung: the ASQ items decide, not the phrase table alone.
            pendingAsq = pending
            _asq.value = AsqFlow.State()
        }
    }

    private fun recordSafetyEvent(level: RiskLevel, patternId: String?, trigger: String) {
        val repo = safetyRepository ?: return
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            val at = now()
            repo.record(
                SafetyEvent(
                    id = newId(),
                    userId = userId,
                    level = level,
                    trigger = trigger,
                    patternId = patternId,
                    createdAt = at,
                    retentionUntil = at + SAFETY_EVENT_RETENTION_MILLIS,
                ),
            )
        }
    }

    private suspend fun restore() {
        val userId = sessionStore.current()?.userId ?: return
        val active = sessions.findActive(userId) ?: return
        sessionId = active.id
        val messages = sessions.messages(active.id)
        fun content(role: String) = messages.firstOrNull { it.role == role }?.content.orEmpty()
        val generationRow = messages.firstOrNull { it.role == ROLE_GENERATION }
        val lines = generationRow?.content?.lines()?.filter { it.isNotBlank() }.orEmpty()
        _state.value =
            State(
                step = active.currentStep.coerceIn(0, LAST_INPUT_STEP),
                thought = content(ROLE_THOUGHT),
                evidence = content(ROLE_EVIDENCE),
                alternative = content(ROLE_ALTERNATIVE),
                generated = generationRow != null,
                distortion = lines.firstOrNull().orEmpty(),
                perspectives = lines.drop(1),
            )
    }

    private suspend fun ensureSession(userId: String): ReframeSession {
        val existing = sessionId?.let { sessions.findById(it) }
        if (existing != null) return existing
        val started = sessions.start(userId = userId, checkInId = null, now = now())
        sessionId = started.id
        return started
    }

    private fun machineFor(session: ReframeSession): ReframeStateMachine = ReframeStateMachine.resume(
        step = session.currentStep.coerceIn(0, ReframeStateMachine.STEP_COUNT),
        status =
        ReframeStateMachine.Status.entries
            .firstOrNull { it.name.equals(session.status, ignoreCase = true) }
            ?: ReframeStateMachine.Status.Active,
    )

    private fun errorFor(state: State): Error? = when (state.step) {
        0 -> if (state.thought.isBlank()) Error.ThoughtRequired else null
        1 -> if (state.evidence.isBlank()) Error.EvidenceRequired else null
        2 -> if (state.alternative.isBlank()) Error.AlternativeRequired else null
        else -> null
    }

    private fun roleFor(step: Int): String = when (step) {
        0 -> ROLE_THOUGHT
        1 -> ROLE_EVIDENCE
        else -> ROLE_ALTERNATIVE
    }

    private fun answerFor(state: State): String = when (state.step) {
        0 -> state.thought.trim()
        1 -> state.evidence.trim()
        else -> state.alternative.trim()
    }

    private sealed interface PendingAsq {
        data class LocalThought(val thought: String) : PendingAsq

        data class ClassifierReply(val response: GenerationResponse, val sessionId: String) : PendingAsq
    }

    companion object {
        /** Steps 0, 1 and 2 collect text; step 3 is the closing card. */
        const val LAST_INPUT_STEP = 2
        const val MAX_MINUTES = 15
        const val MAX_PERSPECTIVES = 2
        const val ROLE_THOUGHT = "thought"
        const val ROLE_EVIDENCE = "evidence"
        const val ROLE_ALTERNATIVE = "alternative"
        const val ROLE_GENERATION = "generation"

        /** Sorts before the first answer so the stored transcript still reads in order. */
        const val GENERATION_SEQUENCE = -1

        private const val TRIGGER_REFRAME_THOUGHT = "reframe_thought"
        private const val TRIGGER_REFRAME_CLASSIFIER = "reframe_classifier"
        private const val TRIGGER_ASQ_POSITIVE = "asq_positive"
        private const val SAFETY_EVENT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}

/** The reply's own level, or null when it is not one the client knows. */
private fun GenerationResponse.classifierLevel(): RiskLevel? =
    runCatching { RiskLevel.valueOf(riskLevel.level) }.getOrNull()
