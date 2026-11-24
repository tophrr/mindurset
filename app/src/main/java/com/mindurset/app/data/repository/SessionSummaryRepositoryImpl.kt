package com.mindurset.app.data.repository

import com.mindurset.app.data.local.SessionSummaryDao
import com.mindurset.app.data.local.SessionSummaryEntity
import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.repository.SessionSummaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Room-backed store for Layer 2 session summaries with 3-row rolling cap and expiry purge. */
class SessionSummaryRepositoryImpl(private val dao: SessionSummaryDao) : SessionSummaryRepository {
    override fun observe(userId: String): Flow<List<SessionSummary>> =
        dao.observeForUser(userId).map { rows -> rows.map { it.toModel() } }

    override suspend fun latest(userId: String, now: Long): List<SessionSummary> {
        dao.deleteExpired(now)
        return dao.activeLastThree(userId, now).map { it.toModel() }
    }

    override suspend fun saveAndTrim(summary: SessionSummary, now: Long) {
        dao.deleteExpired(now)
        dao.insert(summary.toEntity())
        dao.trimToLatest(summary.userId, MemoryPolicy.MAX_SUMMARIES)
    }

    override suspend fun updateText(id: String, summary: String) {
        val clean = summary.trim().take(MemoryPolicy.MAX_SUMMARY_LENGTH)
        if (clean.isNotEmpty()) {
            dao.updateSummary(id, clean)
        }
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    override suspend fun deleteAll(userId: String) {
        dao.deleteForUser(userId)
    }

    override suspend fun purgeExpired(now: Long) {
        dao.deleteExpired(now)
    }
}

private fun SessionSummary.toEntity() = SessionSummaryEntity(
    id = id,
    userId = userId,
    sessionId = sessionId,
    summary = summary,
    retentionUntil = retentionUntil,
    createdAt = createdAt,
)

private fun SessionSummaryEntity.toModel() = SessionSummary(
    id = id,
    userId = userId,
    sessionId = sessionId,
    summary = summary,
    retentionUntil = retentionUntil,
    createdAt = createdAt,
)
