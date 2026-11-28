package com.mindurset.app.data.repository

import com.mindurset.app.data.local.OnboardingBaselineDao
import com.mindurset.app.data.local.OnboardingBaselineEntity
import com.mindurset.app.domain.model.OnboardingBaseline
import com.mindurset.app.domain.model.ValidateOnboardingBaseline
import com.mindurset.app.domain.repository.OnboardingBaselineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OnboardingBaselineRepositoryImpl(private val dao: OnboardingBaselineDao) : OnboardingBaselineRepository {
    override suspend fun findForUser(userId: String): OnboardingBaseline? = dao.findForUser(userId)?.toModel()

    override fun observeForUser(userId: String): Flow<OnboardingBaseline?> =
        dao.observeForUser(userId).map { it?.toModel() }

    override suspend fun save(baseline: OnboardingBaseline) {
        dao.upsert(baseline.toEntity())
    }
}

private fun OnboardingBaseline.toEntity() = OnboardingBaselineEntity(
    id = id,
    userId = userId,
    recentMood = recentMood.name,
    sleepQuality = sleepQuality.name,
    supportLevel = supportLevel.name,
    primaryFocus = primaryFocus.name,
    createdAt = createdAt,
)

private fun OnboardingBaselineEntity.toModel(): OnboardingBaseline? = ValidateOnboardingBaseline.fromNames(
    id = id,
    userId = userId,
    recentMoodName = recentMood,
    sleepQualityName = sleepQuality,
    supportLevelName = supportLevel,
    primaryFocusName = primaryFocus,
    createdAt = createdAt,
)
