package com.mindurset.app.ui.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MemoryManagerUiState(
    val loading: Boolean = true,
    val memoryEnabled: Boolean = true,
    val summaries: List<SessionSummary> = emptyList(),
    val userNote: String = "",
    val noteDraft: String = "",
    val noteSaved: Boolean = false,
)

/**
 * ViewModel for S-52 Memory manager: exposes Layer 2 session summaries (max 3),
 * Layer 3 user-written note, and the global `memory_enabled` toggle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MemoryManagerViewModel(
    private val summaryRepository: SessionSummaryRepository,
    private val preferencesStore: PreferencesStore,
    private val sessionStore: SessionStore,
    private val now: () -> Long = System::currentTimeMillis,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val noteDraftOverride = MutableStateFlow<String?>(null)
    private val noteSavedFlag = MutableStateFlow(false)

    val uiState: StateFlow<MemoryManagerUiState> =
        sessionStore.session
            .flatMapLatest { session ->
                val userId = session?.userId
                if (userId == null) {
                    combine(
                        preferencesStore.memoryEnabled,
                        noteDraftOverride,
                        noteSavedFlag,
                    ) { enabled, draft, saved ->
                        MemoryManagerUiState(
                            loading = false,
                            memoryEnabled = enabled,
                            summaries = emptyList(),
                            userNote = "",
                            noteDraft = draft.orEmpty(),
                            noteSaved = saved,
                        )
                    }
                } else {
                    combine(
                        preferencesStore.memoryEnabled,
                        summaryRepository.observe(userId),
                        preferencesStore.memoryNote(userId),
                        noteDraftOverride,
                        noteSavedFlag,
                    ) { enabled, summaries, note, draft, saved ->
                        val at = now()
                        val active = summaries.filter { it.retentionUntil > at }
                        MemoryManagerUiState(
                            loading = false,
                            memoryEnabled = enabled,
                            summaries = active,
                            userNote = note,
                            noteDraft = draft ?: note,
                            noteSaved = saved,
                        )
                    }
                }
            }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), MemoryManagerUiState(loading = true))

    init {
        scope.launch {
            summaryRepository.purgeExpired(now())
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        scope.launch {
            preferencesStore.setMemoryEnabled(enabled)
        }
    }

    fun onNoteDraftChange(text: String) {
        noteDraftOverride.value = text.take(MemoryPolicy.MAX_USER_NOTE_LENGTH)
        noteSavedFlag.value = false
    }

    fun saveNote() {
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            val text = (noteDraftOverride.value ?: uiState.value.userNote)
                .trim()
                .take(MemoryPolicy.MAX_USER_NOTE_LENGTH)
            preferencesStore.setMemoryNote(userId, text)
            noteDraftOverride.value = null
            noteSavedFlag.value = text.isNotEmpty()
        }
    }

    fun clearNote() {
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            preferencesStore.setMemoryNote(userId, "")
            noteDraftOverride.value = null
            noteSavedFlag.value = false
        }
    }

    fun editSummary(id: String, newText: String) {
        val clean = newText.trim().take(MemoryPolicy.MAX_SUMMARY_LENGTH)
        if (clean.isEmpty()) return
        scope.launch {
            summaryRepository.updateText(id, clean)
        }
    }

    fun deleteSummary(id: String) {
        scope.launch {
            summaryRepository.delete(id)
        }
    }

    fun clearAllMemory() {
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            summaryRepository.deleteAll(userId)
            preferencesStore.setMemoryNote(userId, "")
            noteDraftOverride.value = null
            noteSavedFlag.value = false
        }
    }
}
