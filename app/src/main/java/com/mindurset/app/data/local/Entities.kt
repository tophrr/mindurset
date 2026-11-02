package com.mindurset.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user_account", indices = [Index(value = ["email"], unique = true)])
data class UserAccountEntity(
    @PrimaryKey val id: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val iterations: Int,
    val createdAt: Long,
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val displayName: String,
    val email: String,
    val joinedAt: Long,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val language: String,
)

@Entity(tableName = "check_in_log", indices = [Index("userId"), Index("createdAt")])
data class CheckInLogEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val emotion: String,
    val intensity: Int,
    val note: String?,
    val createdAt: Long,
)

@Entity(tableName = "journal_entry", indices = [Index("userId"), Index("createdAt")])
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val challenge: String,
    val reframe: String,
    val gratitude: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/** One reframe attempt. Steps run 0..2, and 3 means the session completed. */
@Entity(tableName = "reframe_session", indices = [Index("userId"), Index("createdAt")])
data class ReframeSessionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val checkInId: String?,
    val currentStep: Int,
    val status: String,
    val promptVersion: String?,
    val modelVersion: String?,
    val startedAt: Long,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "session_message", indices = [Index("sessionId"), Index("createdAt")])
data class SessionMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String,
    val content: String,
    val sequence: Int,
    val createdAt: Long,
)

/** Contextual memory: at most three of these survive, each with its own retention date. */
@Entity(tableName = "session_summary", indices = [Index("userId"), Index("createdAt")])
data class SessionSummaryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sessionId: String,
    val summary: String,
    val retentionUntil: Long,
    val createdAt: Long,
)

@Entity(tableName = "micro_action", indices = [Index("userId"), Index("createdAt")])
data class MicroActionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val sessionId: String?,
    val title: String,
    val minutes: Int,
    val done: Boolean,
    val snoozedUntil: Long?,
    val createdAt: Long,
    val completedAt: Long?,
)

/** A record that the safety ladder fired, without the text that triggered it. */
@Entity(tableName = "safety_event", indices = [Index("userId"), Index("createdAt")])
data class SafetyEventEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val level: String,
    val trigger: String,
    val patternId: String?,
    val createdAt: Long,
    val retentionUntil: Long,
)

@Entity(tableName = "trusted_contact", indices = [Index("userId")])
data class TrustedContactEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val relationship: String?,
    val createdAt: Long,
)

@Entity(tableName = "consent_record", indices = [Index("userId"), Index("kind")])
data class ConsentRecordEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val kind: String,
    val policyVersion: String,
    val grantedAt: Long,
    val revokedAt: Long?,
)
