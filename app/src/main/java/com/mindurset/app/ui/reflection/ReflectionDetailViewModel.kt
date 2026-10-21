package com.mindurset.app.ui.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.repository.JournalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Single-entry state plus the delete action for the detail screen. */
class ReflectionDetailViewModel(private val entryId: String, private val journalRepository: JournalRepository) :
    ViewModel() {
    sealed interface UiState {
        data object Loading : UiState

        data object NotFound : UiState

        data class Ready(val entry: JournalEntry) : UiState

        data class Error(val messageKey: String) : UiState
    }

    val state =
        journalRepository
            .observeById(entryId)
            .map<JournalEntry?, UiState> { entry ->
                if (entry == null) UiState.NotFound else UiState.Ready(entry)
            }.onStart { emit(UiState.Loading) }
            .catch { emit(UiState.Error(ReflectionErrorKeys.LOAD)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** Only called after the confirmation dialog is accepted. */
    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            journalRepository.delete(entryId)
            onDeleted()
        }
    }
}
