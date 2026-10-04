package com.mindurset.app.data.repository

import com.mindurset.app.data.local.ProfileDao
import com.mindurset.app.data.local.UserProfileEntity
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val dao: ProfileDao,
    private val now: () -> Long = System::currentTimeMillis,
) : ProfileRepository {

    override fun observe(userId: String): Flow<UserProfile?> =
        dao.observe(userId).map { it?.toDomain() }

    override suspend fun upsert(profile: UserProfile) {
        dao.upsert(
            UserProfileEntity(
                userId = profile.userId,
                displayName = profile.displayName,
                email = dao.find(profile.userId)?.email.orEmpty(),
                joinedAt = profile.joinedAt,
                reminderEnabled = profile.reminderEnabled,
                reminderHour = profile.reminderHour,
                language = profile.language,
            ),
        )
    }

    override suspend fun ensureProfile(userId: String, email: String) {
        if (dao.find(userId) == null) {
            dao.upsert(
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

    private fun UserProfileEntity.toDomain() = UserProfile(
        userId = userId,
        displayName = displayName,
        joinedAt = joinedAt,
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        language = language,
    )
}
