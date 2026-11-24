package com.mindurset.app.domain.model

/** Retention and size caps for the three-layer contextual memory (proposal Sec. 9.3). */
object MemoryPolicy {
    const val MAX_SUMMARIES = 3
    const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000L
    const val MAX_USER_NOTE_LENGTH = 400
    const val MAX_SUMMARY_LENGTH = 600
    const val MAX_CONTEXT_LENGTH = 1400
}

/**
 * Layer 2 of contextual memory: a short user-visible summary of a completed session,
 * bounded to three rows and tagged with an explicit retention expiry.
 */
data class SessionSummary(
    val id: String,
    val userId: String,
    val sessionId: String,
    val summary: String,
    val retentionUntil: Long,
    val createdAt: Long,
)

/**
 * Layer 1 of contextual memory: structured facts only (emotion, intensity, time since check-in,
 * chosen perspective, and completed micro-actions). Never includes raw check-in notes or journal text.
 */
data class StructuredFacts(
    val latestCheckIn: CheckIn? = null,
    val chosenPerspective: String? = null,
    val completedActionsCount: Int = 0,
)

/** Full local state assembled before building the prompt context line. */
data class MemorySnapshot(
    val enabled: Boolean,
    val facts: StructuredFacts,
    val summaries: List<SessionSummary>,
    val userNote: String,
)
