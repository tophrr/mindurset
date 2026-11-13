package com.mindurset.app.ui.reflection

import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionFormViewModelTest {
    private val stored = mutableListOf<JournalEntry>()
    private val safety = RecordingSafety()
    private var onSavedCalls = 0

    @Test
    fun `an entry that halts the flow is never stored`() {
        listOf(
            "aku mau bunuh diri" to RiskLevel.L3,
            "there is no reason to live" to RiskLevel.L2,
        ).forEach { (text, level) ->
            stored.clear()
            safety.events.clear()
            val viewModel = viewModel()
            viewModel.onChallengeChange(text)

            viewModel.save { onSavedCalls++ }

            assertEquals("$text should reach the crisis card", level, viewModel.crisisLevel)
            assertTrue("$text was written to Room before the halt", stored.isEmpty())
            assertEquals("$text recorded no safety event", 1, safety.events.size)
            assertEquals("journal_entry", safety.events.single().trigger)
        }
    }

    @Test
    fun `a benign entry is stored and leaves the screen`() {
        val viewModel = viewModel()
        viewModel.onChallengeChange("deadline minggu ini menumpuk")
        viewModel.onReframeChange("aku bisa memecahnya jadi tiga bagian")

        viewModel.save { onSavedCalls++ }

        assertNull(viewModel.crisisLevel)
        assertTrue(safety.events.isEmpty())
        assertEquals("deadline minggu ini menumpuk", stored.single().challenge)
        assertEquals(1, onSavedCalls)
    }

    @Test
    fun `an empty challenge stores nothing`() {
        val viewModel = viewModel()

        viewModel.save { onSavedCalls++ }

        assertTrue(stored.isEmpty())
        assertEquals(0, onSavedCalls)
    }

    private fun viewModel(): ReflectionFormViewModel = ReflectionFormViewModel(
        entryId = null,
        journalRepository = RecordingJournal(stored),
        sessionStore = FixedSessionStore(SessionInfo(userId = "u1", email = "guest@local")),
        safetyRepository = safety,
        clock = { 1_700_000_000_000 },
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )
}

private class RecordingJournal(private val rows: MutableList<JournalEntry>) : JournalRepository {
    override fun observeAll(userId: String): Flow<List<JournalEntry>> = flowOf(rows)

    override fun observeById(id: String): Flow<JournalEntry?> = flowOf(rows.firstOrNull { it.id == id })

    override fun count(userId: String): Flow<Int> = flowOf(rows.size)

    override suspend fun upsert(entry: JournalEntry) {
        rows += entry
    }

    override suspend fun delete(id: String) = Unit
}

private class RecordingSafety : SafetyRepository {
    val events = mutableListOf<SafetyEvent>()

    override suspend fun record(event: SafetyEvent) {
        events += event
    }

    override suspend fun countSince(userId: String, since: Long): Int =
        events.count { it.userId == userId && it.createdAt >= since }
}

private class FixedSessionStore(initial: SessionInfo?) : SessionStore {
    private val state = MutableStateFlow(initial)
    override val session: StateFlow<SessionInfo?> = state.asStateFlow()

    override suspend fun current(): SessionInfo? = state.value

    override suspend fun signIn(session: SessionInfo) {
        state.value = session
    }

    override suspend fun signOut() {
        state.value = null
    }
}
