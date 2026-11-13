package com.mindurset.app.data.repository

import com.mindurset.app.data.local.TrustedContactDao
import com.mindurset.app.data.local.TrustedContactEntity
import com.mindurset.app.domain.model.TrustedContact
import com.mindurset.app.domain.repository.TrustedContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * A contact is stored locally only. It is deliberately kept out of the JSON export (ADR 0006): the
 * number is the third party's data, and exporting it would copy it to wherever the person puts the
 * file.
 */
class TrustedContactRepositoryImpl(private val dao: TrustedContactDao) : TrustedContactRepository {
    override fun observe(userId: String): Flow<List<TrustedContact>> =
        dao.observe(userId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun upsert(contact: TrustedContact) {
        dao.upsert(contact.toEntity())
    }

    override suspend fun delete(id: String) = dao.delete(id)

    private fun TrustedContactEntity.toDomain() = TrustedContact(
        id = id,
        userId = userId,
        name = name,
        phone = phone,
        relationship = relationship,
        createdAt = createdAt,
    )

    private fun TrustedContact.toEntity() = TrustedContactEntity(
        id = id,
        userId = userId,
        name = name,
        phone = phone,
        relationship = relationship,
        createdAt = createdAt,
    )
}
