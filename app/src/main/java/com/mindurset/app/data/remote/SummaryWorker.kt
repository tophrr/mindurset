package com.mindurset.app.data.remote

import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryRepository
import com.mindurset.app.domain.repository.SessionSummaryScheduler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Generates and stores a short session summary at the end of a reframe or companion session
 * when contextual memory is enabled (proposal Sec. 9.3).
 *
 * Offline, ungrounded, invalid, or L3 responses are silently skipped rather than storing canned
 * fallback text as a memory entry.
 */
class SummaryWorker(
    private val generationClient: GenerationClient,
    private val summaryRepository: SessionSummaryRepository,
    private val preferencesStore: PreferencesStore,
    private val scope: CoroutineScope,
    private val sessionStore: SessionStore? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : SessionSummaryScheduler {
    private val generation = AtomicLong(0L)

    override fun enqueue(userId: String, sessionId: String, transcript: List<String>) {
        val expectedGeneration = generation.get()
        scope.launch {
            summariseNow(
                userId = userId,
                sessionId = sessionId,
                transcript = transcript,
                expectedGeneration = expectedGeneration,
            )
        }
    }

    override fun cancelPending() {
        generation.incrementAndGet()
        scope.coroutineContext.cancelChildren()
    }

    suspend fun summariseNow(
        userId: String,
        sessionId: String,
        transcript: List<String>,
        expectedGeneration: Long = generation.get(),
    ): SessionSummary? {
        if (!canPersist(userId, expectedGeneration)) return null
        val cleanLines = transcript.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanLines.isEmpty()) return null

        val response = try {
            generationClient.summarise(cleanLines)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        } catch (_: RuntimeException) {
            return null
        }

        if (response.riskLevel.level == "L3") return null
        if (!canPersist(userId, expectedGeneration)) return null

        val cleanSummary = response.message
            .trim()
            .replace(Regex("\\s+"), " ")
            .take(MemoryPolicy.MAX_SUMMARY_LENGTH)
        if (cleanSummary.isEmpty()) return null

        val at = now()
        val entry = SessionSummary(
            id = newId(),
            userId = userId,
            sessionId = sessionId,
            summary = cleanSummary,
            retentionUntil = at + MemoryPolicy.RETENTION_MILLIS,
            createdAt = at,
        )
        summaryRepository.saveAndTrim(entry, now = at)
        return entry
    }

    private suspend fun canPersist(userId: String, expectedGeneration: Long): Boolean {
        if (generation.get() != expectedGeneration) return false
        if (sessionStore != null && sessionStore.current()?.userId != userId) return false
        return preferencesStore.memoryEnabled.first()
    }
}
