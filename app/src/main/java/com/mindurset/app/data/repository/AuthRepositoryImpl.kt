package com.mindurset.app.data.repository

import com.mindurset.app.data.local.AccountDao
import com.mindurset.app.data.local.CheckInDao
import com.mindurset.app.data.local.JournalDao
import com.mindurset.app.data.local.ProfileDao
import com.mindurset.app.data.local.UserAccountEntity
import com.mindurset.app.data.local.UserProfileEntity
import com.mindurset.app.domain.auth.PasswordHasher
import com.mindurset.app.domain.model.UserAccount
import com.mindurset.app.domain.repository.AuthRepository
import java.util.UUID

class AuthRepositoryImpl(
    private val accountDao: AccountDao,
    private val profileDao: ProfileDao,
    private val journalDao: JournalDao,
    private val checkInDao: CheckInDao,
    private val now: () -> Long = System::currentTimeMillis,
) : AuthRepository {
    override suspend fun findAccount(email: String): UserAccount? =
        accountDao.findByEmail(email.trim().lowercase())?.let {
            UserAccount(id = it.id, email = it.email, createdAt = it.createdAt)
        }

    override suspend fun register(email: String, password: String): UserAccount {
        val normalised = email.trim().lowercase()
        require(accountDao.findByEmail(normalised) == null) { "email already registered" }
        val hashed = PasswordHasher.hash(password)
        val account =
            UserAccountEntity(
                id = UUID.randomUUID().toString(),
                email = normalised,
                passwordHash = hashed.hash,
                passwordSalt = hashed.salt,
                iterations = hashed.iterations,
                createdAt = now(),
            )
        accountDao.insert(account)
        ensureProfile(account.id, account.email)
        return UserAccount(id = account.id, email = account.email, createdAt = account.createdAt)
    }

    override suspend fun signIn(email: String, password: String): UserAccount? {
        val stored = accountDao.findByEmail(email.trim().lowercase()) ?: return null
        val ok = PasswordHasher.verify(password, stored.passwordHash, stored.passwordSalt, stored.iterations)
        if (!ok) return null
        ensureProfile(stored.id, stored.email)
        return UserAccount(id = stored.id, email = stored.email, createdAt = stored.createdAt)
    }

    override suspend fun deleteAccount(userId: String) {
        journalDao.deleteForUser(userId)
        checkInDao.deleteForUser(userId)
        profileDao.delete(userId)
        accountDao.delete(userId)
    }

    private suspend fun ensureProfile(userId: String, email: String) {
        if (profileDao.find(userId) == null) {
            profileDao.upsert(
                UserProfileEntity(
                    userId = userId,
                    displayName = email.substringBefore('@'),
                    email = email,
                    joinedAt = now(),
                    reminderEnabled = false,
                    reminderHour = 19,
                    language = "en",
                ),
            )
        }
    }
}
