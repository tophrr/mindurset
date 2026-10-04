package com.mindurset.app.domain.repository

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.UserAccount
import com.mindurset.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun findAccount(email: String): UserAccount?

    /** @throws IllegalArgumentException when the email is already registered. */
    suspend fun register(email: String, password: String): UserAccount

    /** Returns null when the credentials do not match any account. */
    suspend fun signIn(email: String, password: String): UserAccount?

    suspend fun deleteAccount(userId: String)
}

interface SessionStore {
    val session: Flow<SessionInfo?>
    suspend fun current(): SessionInfo?
    suspend fun signIn(session: SessionInfo)
    suspend fun signOut()
}

interface JournalRepository {
    fun observeAll(userId: String): Flow<List<JournalEntry>>
    fun observeById(id: String): Flow<JournalEntry?>
    fun count(userId: String): Flow<Int>
    suspend fun upsert(entry: JournalEntry)
    suspend fun delete(id: String)
}

interface CheckInRepository {
    fun observeAll(userId: String): Flow<List<CheckIn>>
    fun observeRecent(userId: String, sinceEpochMillis: Long): Flow<List<CheckIn>>
    fun count(userId: String): Flow<Int>
    suspend fun insert(checkIn: CheckIn)
}

interface ProfileRepository {
    fun observe(userId: String): Flow<UserProfile?>
    suspend fun upsert(profile: UserProfile)
    suspend fun ensureProfile(userId: String, email: String)
}

interface PreferencesStore {
    val reminderEnabled: Flow<Boolean>
    val reminderHour: Flow<Int>
    val language: Flow<String>
    val theme: Flow<String>
    suspend fun setReminderEnabled(enabled: Boolean)
    suspend fun setReminderHour(hour: Int)
    suspend fun setLanguage(language: String)
    suspend fun setTheme(theme: String)
}
