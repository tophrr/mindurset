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
