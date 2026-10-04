package com.mindurset.app.ui.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * Reactive list state for the Daily Reflection screen. Entries come straight
 * from Room (newest first) so the list updates itself after a create or delete.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReflectionListViewModel(
    private val journalRepository: JournalRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data class Ready(val entries: List<JournalEntry>) : UiState
        data class Error(val messageKey: String) : UiState
    }

    private val refresh = MutableStateFlow(0)

    val state = refresh
        .flatMapLatest { load() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun load(): Flow<UiState> =
        sessionStore.session.flatMapLatest { session ->
            if (session == null) {
                flowOf<UiState>(UiState.Error(ReflectionErrorKeys.NO_SESSION))
            } else {
                journalRepository.observeAll(session.userId)
                    .map<List<JournalEntry>, UiState> { entries ->
                        if (entries.isEmpty()) UiState.Empty else UiState.Ready(entries)
                    }
                    .onStart { emit(UiState.Loading) }
                    .catch { emit(UiState.Error(ReflectionErrorKeys.LOAD)) }
            }
        }

    fun retry() {
        refresh.value += 1
    }
}
