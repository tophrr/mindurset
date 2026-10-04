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
