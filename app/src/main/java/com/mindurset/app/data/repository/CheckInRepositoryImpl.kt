package com.mindurset.app.data.repository

import com.mindurset.app.data.local.CheckInDao
import com.mindurset.app.data.local.CheckInLogEntity
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CheckInRepositoryImpl(private val dao: CheckInDao) : CheckInRepository {
    override fun observeAll(userId: String): Flow<List<CheckIn>> =
        dao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override fun observeRecent(userId: String, sinceEpochMillis: Long): Flow<List<CheckIn>> =
        dao.observeRecent(userId, sinceEpochMillis).map { list -> list.map { it.toDomain() } }

    override fun count(userId: String): Flow<Int> = dao.count(userId)

    override suspend fun insert(checkIn: CheckIn) {
        dao.insert(
            CheckInLogEntity(
                id = checkIn.id,
                userId = checkIn.userId,
                emotion = checkIn.emotion.name,
                intensity = checkIn.intensity,
                note = checkIn.note,
                createdAt = checkIn.createdAt,
            ),
        )
    }

    private fun CheckInLogEntity.toDomain() = CheckIn(
        id = id,
        userId = userId,
        emotion = Emotion.fromName(emotion),
        intensity = intensity,
        note = note,
        createdAt = createdAt,
    )
}
