package com.mindurset.app.ui.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.safety.LocalRiskFloor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

/** The same retention the check-in and journal paths apply to a safety event. */
private const val SAFETY_EVENT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000

class CompanionViewModel(
    private val generationClient: GenerationClient,
    private val safetyRepository: SafetyRepository,
    private val sessionStore: SessionStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = externalScope ?: viewModelScope
    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()

    /** Set when the offline floor found risk in the line, so the screen shows the crisis card. */
    private val _crisisLevel = MutableStateFlow<RiskLevel?>(null)
    val crisisLevel: StateFlow<RiskLevel?> = _crisisLevel.asStateFlow()

    private var lastRequest: PendingRequest? = null

    fun selectMode(mode: CompanionSupportMode) {
        if (_uiState.value.replyState != CompanionReplyState.Sending) {
            _uiState.update { it.copy(supportMode = mode) }
        }
    }

    /**
     * The offline floor runs before anything else. An L2 or L3 line is recorded as one safety event
     * and shown as a crisis card; it never joins the transcript and never leaves the device.
     */
    fun send(message: String) {
        val clean = message.trim()
        if (clean.isBlank() || _uiState.value.replyState == CompanionReplyState.Sending) return

        val hit = LocalRiskFloor.match(clean)
        if (hit != null && hit.level >= RiskLevel.L2) {
            _crisisLevel.value = hit.level
            recordSafetyEvent(level = hit.level, patternId = hit.patternId)
            return
        }

        val request = PendingRequest(clean, _uiState.value.supportMode)
        lastRequest = request
        _uiState.update {
            it.copy(
                lines = it.lines + CompanionLine(text = clean, fromUser = true),
                replyState = CompanionReplyState.Sending,
            )
        }
        request(request)
    }

    /** Called once the card is on screen, before navigation, so a return does not show it twice. */
    fun crisisAcknowledged() {
        _crisisLevel.value = null
    }

    fun retry() {
        val pending = lastRequest ?: return
        if (_uiState.value.replyState == CompanionReplyState.Sending) return
        request(pending)
    }

    private fun recordSafetyEvent(level: RiskLevel, patternId: String) {
        scope.launch {
            val session = sessionStore.current() ?: return@launch
            val at = now()
            safetyRepository.record(
                SafetyEvent(
                    id = newId(),
                    userId = session.userId,
                    level = level,
                    trigger = "companion_message",
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
                _uiState.update {
                    it.copy(
                        lines = it.lines + CompanionLine(
                            text = response.message,
                            fromUser = false,
                            sourceIds = response.sourceIds,
                        ),
                        replyState = CompanionReplyState.Reply(response),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IOException) {
                addOfflineFallback(CompanionReplyState.OfflineFallback)
            } catch (_: Exception) {
                addOfflineFallback(CompanionReplyState.Error)
            }
        }
    }

    private fun addOfflineFallback(state: CompanionReplyState) {
        _uiState.update {
            it.copy(
                lines = it.lines + CompanionLine(text = null, fromUser = false, offline = true),
                replyState = state,
            )
        }
    }

    private data class PendingRequest(val message: String, val mode: CompanionSupportMode)
}
