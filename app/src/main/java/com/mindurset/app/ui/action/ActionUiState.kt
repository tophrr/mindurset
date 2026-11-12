package com.mindurset.app.ui.action

import com.mindurset.app.domain.model.MicroAction

/**
 * What the Today's Action screen shows. Groups are derived from the one stored [MicroAction] row:
 * done items belong to today when they were completed today, and an open item belongs to tomorrow
 * when it carries a future snooze.
 */
sealed interface ActionUiState {
    data object Loading : ActionUiState

    data object Empty : ActionUiState

    data class Populated(
        val today: List<MicroAction>,
        val doneToday: List<MicroAction>,
        val tomorrow: List<MicroAction>,
    ) : ActionUiState

    data object Error : ActionUiState
}

/** Why the editor refused to save, so the dialog can say it in the user's language. */
enum class ActionEditorError {
    TitleRequired,
    MinutesOutOfRange,
}
