package com.mindurset.app.domain.model

data class CheckIn(
    val id: String,
    val userId: String,
    val emotion: Emotion,
    val intensity: Int,
    val note: String?,
    val createdAt: Long,
)

data class JournalEntry(
    val id: String,
    val userId: String,
    val challenge: String,
    val reframe: String,
    val gratitude: String,
    val createdAt: Long,
    val updatedAt: Long,
)

data class UserAccount(val id: String, val email: String, val createdAt: Long)

data class UserProfile(
    val userId: String,
    val displayName: String,
    val joinedAt: Long,
    val reminderHour: Int,
    val reminderEnabled: Boolean,
    val language: String,
)

data class SessionInfo(val userId: String, val email: String)
