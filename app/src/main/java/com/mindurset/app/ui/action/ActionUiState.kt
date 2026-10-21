package com.mindurset.app.ui.action

import java.time.LocalDate

data class ActionItem(
    val id: String,
    val title: String,
    val minutes: Int,
    val scheduledDate: LocalDate,
    val isDone: Boolean = false,
)

sealed interface ActionUiState {
    data object Loading : ActionUiState

    data object Empty : ActionUiState

    data class Populated(
        val today: List<ActionItem>,
        val doneToday: List<ActionItem>,
        val tomorrow: List<ActionItem>,
    ) : ActionUiState

    data object Error : ActionUiState
}
