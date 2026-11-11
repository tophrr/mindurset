package com.mindurset.app.ui.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.data.remote.GenerationClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class CompanionViewModel(private val generationClient: GenerationClient, externalScope: CoroutineScope? = null) :
    ViewModel() {
    private val scope = externalScope ?: viewModelScope
    private val _uiState = MutableStateFlow(CompanionUiState())
    val uiState: StateFlow<CompanionUiState> = _uiState.asStateFlow()
    private var lastRequest: PendingRequest? = null

    fun selectMode(mode: CompanionSupportMode) {
        if (_uiState.value.replyState != CompanionReplyState.Sending) {
            _uiState.update { it.copy(supportMode = mode) }
        }
    }

    fun send(message: String) {
        val clean = message.trim()
        if (clean.isBlank() || _uiState.value.replyState == CompanionReplyState.Sending) return
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

    fun retry() {
        val pending = lastRequest ?: return
        if (_uiState.value.replyState == CompanionReplyState.Sending) return
        request(pending)
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
