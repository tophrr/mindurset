package com.mindurset.app.ui.companion

import com.mindurset.app.data.remote.GenerationResponse
import com.mindurset.app.domain.model.OfflineExercise

/**
 * The three support modes from use case UC-09. `workerMode` is the exact string the Worker's
 * allowlist accepts, so this enum and `worker/src/prompts.ts` move together: `GenerationClient`
 * rejects anything outside its copy of the same set before a request leaves the device.
 */
enum class CompanionSupportMode(
    val workerMode: String,
    val labelRes: Int,
    val descriptionRes: Int,
    val starterRes: Int,
    val fallbackTopic: String,
) {
    LISTEN(
        "listen",
        com.mindurset.app.R.string.companion_chip_listen,
        com.mindurset.app.R.string.companion_mode_listen_description,
        com.mindurset.app.R.string.companion_starter_listen,
        "self-compassion",
    ),
    SOLVE(
        "solve",
        com.mindurset.app.R.string.companion_chip_solve,
        com.mindurset.app.R.string.companion_mode_solve_description,
        com.mindurset.app.R.string.companion_starter_solve,
        "all-or-nothing",
    ),
    DISTRACT(
        "distract",
        com.mindurset.app.R.string.companion_chip_distract,
        com.mindurset.app.R.string.companion_mode_distract_description,
        com.mindurset.app.R.string.companion_starter_distract,
        "stress",
    ),
}

data class CompanionLine(
    val text: String?,
    val fromUser: Boolean,
    val sourceIds: List<String> = emptyList(),
    val offline: Boolean = false,
    val exercise: OfflineExercise? = null,
)

sealed interface CompanionReplyState {
    data object Idle : CompanionReplyState

    data object Sending : CompanionReplyState

    data class Reply(val response: GenerationResponse) : CompanionReplyState

    data object OfflineFallback : CompanionReplyState

    data object Error : CompanionReplyState
}

data class CompanionUiState(
    val supportMode: CompanionSupportMode = CompanionSupportMode.LISTEN,
    val lines: List<CompanionLine> = emptyList(),
    val replyState: CompanionReplyState = CompanionReplyState.Idle,
    /** Set when the Worker could not be reached and the level was raised a tier (L1). */
    val resourcesOffered: Boolean = false,
    /** Set after a negative ASQ screen so the helpline stays one tap away without a lockout. */
    val asqSupportOffered: Boolean = false,
)
