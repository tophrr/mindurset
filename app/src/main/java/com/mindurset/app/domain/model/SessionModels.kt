package com.mindurset.app.domain.model

/** Safety ladder from proposal Sec. 9.4. L0 is ordinary text, L3 is immediate crisis resources. */
enum class RiskLevel {
    L0,
    L1,
    L2,
    L3,
    ;

    /** Never lowers a level: used when a classifier is unavailable or unsure. */
    fun escalate(): RiskLevel = entries[(ordinal + 1).coerceAtMost(entries.lastIndex)]

    fun atLeast(other: RiskLevel): RiskLevel = if (ordinal >= other.ordinal) this else other

    companion object {
        fun fromName(name: String?): RiskLevel = entries.firstOrNull { it.name == name } ?: L0
    }
}

/** Steps run 0..2; step 3 with status completed means the session finished. */
data class ReframeSession(
    val id: String,
    val userId: String,
    val checkInId: String?,
    val currentStep: Int,
    val status: String,
    val promptVersion: String?,
    val modelVersion: String?,
    val startedAt: Long,
    val completedAt: Long?,
) {
    companion object {
        const val STATUS_ACTIVE = "active"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_HALTED = "halted"
    }
}

data class SessionMessage(
    val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val sequence: Int,
    val createdAt: Long,
)

data class MicroAction(
    val id: String,
    val userId: String,
    val sessionId: String?,
    val title: String,
    val minutes: Int,
    val done: Boolean,
    val snoozedUntil: Long?,
    val createdAt: Long,
    val completedAt: Long?,
)

data class SafetyEvent(
    val id: String,
    val userId: String,
    val level: RiskLevel,
    val trigger: String,
    val patternId: String?,
    val createdAt: Long,
    val retentionUntil: Long,
)

data class TrustedContact(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val relationship: String?,
    val createdAt: Long,
)

data class ConsentRecord(
    val id: String,
    val userId: String,
    val kind: String,
    val policyVersion: String,
    val grantedAt: Long,
    val revokedAt: Long?,
)
