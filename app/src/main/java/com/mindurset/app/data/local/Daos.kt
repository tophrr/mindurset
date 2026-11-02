package com.mindurset.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM user_account WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserAccountEntity?

    @Query("SELECT * FROM user_account WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: UserAccountEntity)

    @Query("DELETE FROM user_account WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE userId = :userId LIMIT 1")
    fun observe(userId: String): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE userId = :userId LIMIT 1")
    suspend fun find(userId: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile WHERE userId = :userId")
    suspend fun delete(userId: String)
}

@Dao
interface CheckInDao {
    @Query("SELECT * FROM check_in_log WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<CheckInLogEntity>>

    @Query("SELECT * FROM check_in_log WHERE userId = :userId AND createdAt >= :since ORDER BY createdAt DESC")
    fun observeRecent(userId: String, since: Long): Flow<List<CheckInLogEntity>>

    @Query("SELECT COUNT(*) FROM check_in_log WHERE userId = :userId")
    fun count(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(checkIn: CheckInLogEntity)

    @Query("DELETE FROM check_in_log WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entry WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entry WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<JournalEntryEntity?>

    @Query("SELECT * FROM journal_entry WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): JournalEntryEntity?

    @Query("SELECT COUNT(*) FROM journal_entry WHERE userId = :userId")
    fun count(userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entry WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM journal_entry WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface ReframeSessionDao {
    @Query("SELECT * FROM reframe_session WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): ReframeSessionEntity?

    @Query("SELECT * FROM reframe_session WHERE userId = :userId AND status = 'active' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun findActive(userId: String): ReframeSessionEntity?

    @Query("SELECT * FROM reframe_session WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(userId: String, limit: Int): List<ReframeSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ReframeSessionEntity)

    @Query("DELETE FROM reframe_session WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface SessionMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: SessionMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<SessionMessageEntity>)

    @Query("SELECT * FROM session_message WHERE sessionId = :sessionId ORDER BY sequence ASC")
    suspend fun bySession(sessionId: String): List<SessionMessageEntity>

    @Query("SELECT COUNT(*) FROM session_message WHERE sessionId = :sessionId")
    suspend fun count(sessionId: String): Int
}

@Dao
interface SessionSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: SessionSummaryEntity)

    @Query("SELECT * FROM session_summary WHERE userId = :userId ORDER BY createdAt DESC LIMIT 3")
    suspend fun lastThree(userId: String): List<SessionSummaryEntity>

    @Query("DELETE FROM session_summary WHERE retentionUntil < :now")
    suspend fun deleteExpired(now: Long)
}

@Dao
interface MicroActionDao {
    @Query("SELECT * FROM micro_action WHERE userId = :userId AND done = 0 ORDER BY createdAt DESC")
    fun observeOpen(userId: String): Flow<List<MicroActionEntity>>

    @Query("SELECT * FROM micro_action WHERE userId = :userId ORDER BY createdAt DESC")
    fun observeAll(userId: String): Flow<List<MicroActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(action: MicroActionEntity)

    @Query("UPDATE micro_action SET done = 1, completedAt = :at WHERE id = :id")
    suspend fun markDone(id: String, at: Long)

    @Query("UPDATE micro_action SET snoozedUntil = :until WHERE id = :id")
    suspend fun snooze(id: String, until: Long)

    @Query("DELETE FROM micro_action WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM micro_action WHERE userId = :userId")
    suspend fun deleteForUser(userId: String)
}

@Dao
interface SafetyEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SafetyEventEntity)

    @Query("SELECT COUNT(*) FROM safety_event WHERE userId = :userId AND createdAt >= :since")
    suspend fun countSince(userId: String, since: Long): Int

    @Query("SELECT * FROM safety_event WHERE userId = :userId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun recent(userId: String, limit: Int): List<SafetyEventEntity>

    @Query("DELETE FROM safety_event WHERE retentionUntil < :now")
    suspend fun deleteExpired(now: Long)
}

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contact WHERE userId = :userId ORDER BY createdAt ASC")
    fun observe(userId: String): Flow<List<TrustedContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(contact: TrustedContactEntity)

    @Query("DELETE FROM trusted_contact WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ConsentRecordDao {
    @Query("SELECT * FROM consent_record WHERE userId = :userId AND kind = :kind LIMIT 1")
    suspend fun find(userId: String, kind: String): ConsentRecordEntity?

    @Query("SELECT * FROM consent_record WHERE userId = :userId")
    fun observeAll(userId: String): Flow<List<ConsentRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: ConsentRecordEntity)

    @Query("UPDATE consent_record SET revokedAt = :at WHERE id = :id")
    suspend fun revoke(id: String, at: Long)
}
