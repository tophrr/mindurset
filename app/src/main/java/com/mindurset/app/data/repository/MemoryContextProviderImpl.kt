package com.mindurset.app.data.repository

import com.mindurset.app.domain.model.MemorySnapshot
import com.mindurset.app.domain.model.StructuredFacts
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.MemoryContextProvider
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionSummaryRepository
import com.mindurset.app.domain.usecase.BuildMemoryContext
import kotlinx.coroutines.flow.first

/**
 * Collects the three local memory layers (structured facts, up to three non-expired summaries,
 * and the user's own memory note) and formats them via [BuildMemoryContext].
 */
class MemoryContextProviderImpl(
    private val preferencesStore: PreferencesStore,
    private val checkInRepository: CheckInRepository,
    private val sessionRepository: SessionRepository,
    private val microActionRepository: MicroActionRepository,
    private val summaryRepository: SessionSummaryRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : MemoryContextProvider {
    override suspend fun contextFor(userId: String): String? {
        if (!preferencesStore.memoryEnabled.first()) return null
        val at = now()
        val latestCheckIn = checkInRepository.observeAll(userId).first().firstOrNull()
        val completedActionsCount = microActionRepository.observeAll(userId).first().count { it.done }
        val chosenPerspective = sessionRepository.latestCompleted(userId)?.let { session ->
            sessionRepository.messages(session.id)
                .lastOrNull { it.role == ROLE_ALTERNATIVE }
                ?.content
        }
        val summaries = summaryRepository.latest(userId, at)
        val userNote = preferencesStore.memoryNote(userId).first()
        return BuildMemoryContext.build(
            snapshot = MemorySnapshot(
                enabled = true,
                facts = StructuredFacts(
                    latestCheckIn = latestCheckIn,
                    chosenPerspective = chosenPerspective,
                    completedActionsCount = completedActionsCount,
                ),
                summaries = summaries,
                userNote = userNote,
            ),
            now = at,
        )
    }

    private companion object {
        const val ROLE_ALTERNATIVE = "alternative"
    }
}
