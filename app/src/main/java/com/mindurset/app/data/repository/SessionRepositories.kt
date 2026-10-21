package com.mindurset.app.data.repository

import com.mindurset.app.data.local.ConsentRecordDao
import com.mindurset.app.data.local.ConsentRecordEntity
import com.mindurset.app.data.local.MicroActionDao
import com.mindurset.app.data.local.MicroActionEntity
import com.mindurset.app.data.local.ReframeSessionDao
import com.mindurset.app.data.local.ReframeSessionEntity
import com.mindurset.app.data.local.SafetyEventDao
import com.mindurset.app.data.local.SafetyEventEntity
import com.mindurset.app.data.local.SessionMessageDao
import com.mindurset.app.data.local.SessionMessageEntity
import com.mindurset.app.domain.model.ConsentRecord
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionMessage
import com.mindurset.app.domain.repository.ConsentRepository
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SessionRepositoryImpl(private val sessionDao: ReframeSessionDao, private val messageDao: SessionMessageDao) :
    SessionRepository {
    override suspend fun start(userId: String, checkInId: String?, now: Long): ReframeSession {
        val session =
            ReframeSession(
                id = UUID.randomUUID().toString(),
                userId = userId,
                checkInId = checkInId,
                currentStep = 0,
                status = ReframeSession.STATUS_ACTIVE,
                promptVersion = null,
                modelVersion = null,
                startedAt = now,
                completedAt = null,
            )
        sessionDao.upsert(session.toEntity(now))
        return session
    }

    override suspend fun findActive(userId: String): ReframeSession? = sessionDao.findActive(userId)?.toModel()

    override suspend fun findById(id: String): ReframeSession? = sessionDao.findById(id)?.toModel()

    override suspend fun save(session: ReframeSession) {
        sessionDao.upsert(session.toEntity(session.startedAt))
    }

    override suspend fun appendMessage(message: SessionMessage) {
        messageDao.insert(message.toEntity())
    }

    override suspend fun messages(sessionId: String): List<SessionMessage> =
        messageDao.bySession(sessionId).map { it.toModel() }
}

class MicroActionRepositoryImpl(private val dao: MicroActionDao) : MicroActionRepository {
    override fun observeOpen(userId: String): Flow<List<MicroAction>> =
        dao.observeOpen(userId).map { rows -> rows.map { it.toModel() } }

    override suspend fun add(action: MicroAction) {
        dao.upsert(action.toEntity())
    }

    override suspend fun markDone(id: String, at: Long) {
        dao.markDone(id, at)
    }

    override suspend fun snooze(id: String, until: Long) {
        dao.snooze(id, until)
    }
}

class SafetyRepositoryImpl(private val dao: SafetyEventDao) : SafetyRepository {
    override suspend fun record(event: SafetyEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun countSince(userId: String, since: Long): Int = dao.countSince(userId, since)
}

class ConsentRepositoryImpl(private val dao: ConsentRecordDao) : ConsentRepository {
    override fun observeAll(userId: String): Flow<List<ConsentRecord>> =
        dao.observeAll(userId).map { rows -> rows.map { it.toModel() } }

    override suspend fun find(userId: String, kind: String): ConsentRecord? = dao.find(userId, kind)?.toModel()

    override suspend fun grant(record: ConsentRecord) {
        dao.upsert(record.toEntity())
    }

    override suspend fun revoke(id: String, at: Long) {
        dao.revoke(id, at)
    }
}

private fun ReframeSession.toEntity(updatedAt: Long) = ReframeSessionEntity(
    id = id,
    userId = userId,
    checkInId = checkInId,
    currentStep = currentStep,
    status = status,
    promptVersion = promptVersion,
    modelVersion = modelVersion,
    startedAt = startedAt,
    completedAt = completedAt,
    createdAt = startedAt,
    updatedAt = updatedAt,
)

private fun ReframeSessionEntity.toModel() = ReframeSession(
    id = id,
    userId = userId,
    checkInId = checkInId,
    currentStep = currentStep,
    status = status,
    promptVersion = promptVersion,
    modelVersion = modelVersion,
    startedAt = startedAt,
    completedAt = completedAt,
)

private fun SessionMessage.toEntity() = SessionMessageEntity(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    sequence = sequence,
    createdAt = createdAt,
)

private fun SessionMessageEntity.toModel() = SessionMessage(
    id = id,
    sessionId = sessionId,
    role = role,
    content = content,
    sequence = sequence,
    createdAt = createdAt,
)

private fun MicroAction.toEntity() = MicroActionEntity(
    id = id,
    userId = userId,
    sessionId = sessionId,
    title = title,
    minutes = minutes,
    done = done,
    snoozedUntil = snoozedUntil,
    createdAt = createdAt,
    completedAt = completedAt,
)

private fun MicroActionEntity.toModel() = MicroAction(
    id = id,
    userId = userId,
    sessionId = sessionId,
    title = title,
    minutes = minutes,
    done = done,
    snoozedUntil = snoozedUntil,
    createdAt = createdAt,
    completedAt = completedAt,
)

private fun SafetyEvent.toEntity() = SafetyEventEntity(
    id = id,
    userId = userId,
    level = level.name,
    trigger = trigger,
    patternId = patternId,
    createdAt = createdAt,
    retentionUntil = retentionUntil,
)

private fun ConsentRecord.toEntity() = ConsentRecordEntity(
    id = id,
    userId = userId,
    kind = kind,
    policyVersion = policyVersion,
    grantedAt = grantedAt,
    revokedAt = revokedAt,
)

private fun ConsentRecordEntity.toModel() = ConsentRecord(
    id = id,
    userId = userId,
    kind = kind,
    policyVersion = policyVersion,
    grantedAt = grantedAt,
    revokedAt = revokedAt,
)
