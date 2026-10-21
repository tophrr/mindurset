package com.mindurset.app.data.repository

import com.mindurset.app.data.local.JournalDao
import com.mindurset.app.data.local.JournalEntryEntity
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.repository.JournalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class JournalRepositoryImpl(private val dao: JournalDao) : JournalRepository {
    override fun observeAll(userId: String): Flow<List<JournalEntry>> =
        dao.observeAll(userId).map { list -> list.map { it.toDomain() } }

    override fun observeById(id: String): Flow<JournalEntry?> = dao.observeById(id).map { it?.toDomain() }

    override fun count(userId: String): Flow<Int> = dao.count(userId)

    override suspend fun upsert(entry: JournalEntry) {
        dao.upsert(
            JournalEntryEntity(
                id = entry.id,
                userId = entry.userId,
                challenge = entry.challenge,
                reframe = entry.reframe,
                gratitude = entry.gratitude,
                createdAt = entry.createdAt,
                updatedAt = entry.updatedAt,
            ),
        )
    }

    override suspend fun delete(id: String) = dao.delete(id)

    private fun JournalEntryEntity.toDomain() = JournalEntry(
        id = id,
        userId = userId,
        challenge = challenge,
        reframe = reframe,
        gratitude = gratitude,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}
