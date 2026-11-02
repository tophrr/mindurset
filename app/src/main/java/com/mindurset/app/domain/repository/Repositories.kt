package com.mindurset.app.domain.repository

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.ConsentRecord
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.SessionMessage
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

/** A reframe session and its messages. Steps run 0..2, step 3 means completed. */
interface SessionRepository {
    suspend fun start(userId: String, checkInId: String?, now: Long): ReframeSession

    suspend fun findActive(userId: String): ReframeSession?

    suspend fun findById(id: String): ReframeSession?

    suspend fun save(session: ReframeSession)

    suspend fun appendMessage(message: SessionMessage)

    suspend fun messages(sessionId: String): List<SessionMessage>
}

interface MicroActionRepository {
    fun observeOpen(userId: String): Flow<List<MicroAction>>

    suspend fun add(action: MicroAction)

    suspend fun markDone(id: String, at: Long)

    suspend fun snooze(id: String, until: Long)
}

/** Safety events never carry the text that triggered them. */
interface SafetyRepository {
    suspend fun record(event: SafetyEvent)

    suspend fun countSince(userId: String, since: Long): Int
}

interface ConsentRepository {
    fun observeAll(userId: String): Flow<List<ConsentRecord>>

    suspend fun find(userId: String, kind: String): ConsentRecord?

    suspend fun grant(record: ConsentRecord)

    suspend fun revoke(id: String, at: Long)
}
