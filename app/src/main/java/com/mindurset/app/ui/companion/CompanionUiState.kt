package com.mindurset.app.ui.companion

import com.mindurset.app.data.remote.GenerationResponse

enum class CompanionSupportMode(val workerMode: String, val labelRes: Int) {
    PERSONAL("listen", com.mindurset.app.R.string.companion_chip_personal),
    NEGATIVE_THOUGHTS("reflect", com.mindurset.app.R.string.companion_chip_negative_thoughts),
    EMOTION_REGULATION("plan", com.mindurset.app.R.string.companion_chip_emotion_regulation),
}

data class CompanionLine(
    val text: String?,
    val fromUser: Boolean,
    val sourceIds: List<String> = emptyList(),
    val offline: Boolean = false,
)

sealed interface CompanionReplyState {
    data object Idle : CompanionReplyState

    data object Sending : CompanionReplyState

    data class Reply(val response: GenerationResponse) : CompanionReplyState

    data object OfflineFallback : CompanionReplyState

    data object Error : CompanionReplyState
}

data class CompanionUiState(
    val supportMode: CompanionSupportMode = CompanionSupportMode.PERSONAL,
    val lines: List<CompanionLine> = emptyList(),
    val replyState: CompanionReplyState = CompanionReplyState.Idle,
    /** Set when the Worker could not be reached and the level was raised a tier (L1). */
    val resourcesOffered: Boolean = false,
)
