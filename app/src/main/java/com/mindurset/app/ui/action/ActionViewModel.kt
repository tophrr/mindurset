package com.mindurset.app.ui.action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ActionViewModel(
    private val repository: ActionRepository,
    private val today: () -> LocalDate = LocalDate::now,
    scope: CoroutineScope? = null,
) : ViewModel() {
    private val scope = scope ?: viewModelScope
    private val _uiState = MutableStateFlow<ActionUiState>(ActionUiState.Loading)
    val uiState: StateFlow<ActionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    fun addAction(title: String, minutes: Int) = mutate {
        repository.addAction(title.trim(), minutes)
    }

    fun editAction(id: String, title: String, minutes: Int) = mutate {
        repository.editAction(id, title.trim(), minutes)
    }

    fun completeAction(id: String) = mutate { repository.completeAction(id) }

    fun snoozeAction(id: String) = mutate { repository.snoozeAction(id) }

    private fun load() {
        _uiState.value = ActionUiState.Loading
        scope.launch {
            runCatching { repository.loadActions() }
                .onSuccess(::showActions)
                .onFailure { _uiState.value = ActionUiState.Error }
        }
    }

    private fun mutate(operation: suspend () -> List<ActionItem>) {
        scope.launch {
            runCatching { operation() }
                .onSuccess(::showActions)
                .onFailure { _uiState.value = ActionUiState.Error }
        }
    }

    private fun showActions(actions: List<ActionItem>) {
        val currentDate = today()
        val todayActions = actions.filter { it.scheduledDate == currentDate && !it.isDone }
        val doneActions = actions.filter { it.scheduledDate == currentDate && it.isDone }
        val tomorrowActions = actions.filter { it.scheduledDate == currentDate.plusDays(1) && !it.isDone }
        _uiState.value =
            if (todayActions.isEmpty() && doneActions.isEmpty() && tomorrowActions.isEmpty()) {
                ActionUiState.Empty
            } else {
                ActionUiState.Populated(todayActions, doneActions, tomorrowActions)
            }
    }
}
