package com.mindurset.app.ui.therapy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.SessionMessage
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.usecase.ReframeStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * The three-step reframe, stored as it goes.
 *
 * Each answer becomes a [SessionMessage] and the step counter is written to the session row, so
 * killing the process in the middle of step two resumes at step two instead of starting over. The
 * last step turns into one [MicroAction], which is the point of the exercise: leave with something
 * small to do.
 */
class ReframeViewModel(
    private val sessions: SessionRepository,
    private val actions: MicroActionRepository,
    private val sessionStore: SessionStore,
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
    )

    enum class Error { ThoughtRequired, EvidenceRequired, AlternativeRequired, ActionRequired }

    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var sessionId: String? = null

    init {
        scope.launch { restore() }
    }

    fun setThought(value: String) = _state.update { it.copy(thought = value, error = null) }

    fun setEvidence(value: String) = _state.update { it.copy(evidence = value, error = null) }

    fun setAlternative(value: String) = _state.update { it.copy(alternative = value, error = null) }

    fun setAction(value: String) = _state.update { it.copy(actionTitle = value, error = null) }

    fun setMinutes(value: Int) = _state.update { it.copy(minutes = value.coerceIn(1, MAX_MINUTES)) }

    /** Advances one step, saving the answer and the step counter. */
    fun next() {
        val current = _state.value
        if (current.step > LAST_INPUT_STEP) return
        val error = errorFor(current)
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            val session = ensureSession(userId)
            val machine = machineFor(session)
            val next = machine.advance()
            val role = roleFor(current.step)
            val answer = answerFor(current)
            sessions.appendMessage(
                SessionMessage(
                    id = newId(),
                    sessionId = session.id,
                    role = role,
                    content = answer,
                    sequence = current.step,
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
        }
    }

    /** Turns the last step into one micro action and closes the session. */
    fun finish() {
        val current = _state.value
        if (current.actionTitle.isBlank()) {
            _state.update { it.copy(error = Error.ActionRequired) }
            return
        }
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            val at = now()
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
            _state.update { it.copy(finished = true, error = null) }
        }
    }

    private suspend fun restore() {
        val userId = sessionStore.current()?.userId ?: return
        val active = sessions.findActive(userId) ?: return
        sessionId = active.id
        val messages = sessions.messages(active.id)
        fun content(role: String) = messages.firstOrNull { it.role == role }?.content.orEmpty()
        _state.value =
            State(
                step = active.currentStep.coerceIn(0, LAST_INPUT_STEP),
                thought = content(ROLE_THOUGHT),
                evidence = content(ROLE_EVIDENCE),
                alternative = content(ROLE_ALTERNATIVE),
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

    companion object {
        /** Steps 0, 1 and 2 collect text; step 3 is the closing card. */
        const val LAST_INPUT_STEP = 2
        const val MAX_MINUTES = 15
        const val ROLE_THOUGHT = "thought"
        const val ROLE_EVIDENCE = "evidence"
        const val ROLE_ALTERNATIVE = "alternative"
    }
}
