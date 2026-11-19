package com.mindurset.app.ui.reflection

import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.safety.AsqFlow
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
            onSavedCalls = 0
            val viewModel = viewModel()
            viewModel.onChallengeChange(text)

            viewModel.save { onSavedCalls++ }

            assertTrue("$text was written to Room before the halt", stored.isEmpty())
            assertEquals("$text recorded no safety event", 1, safety.events.size)
            assertEquals("journal_entry", safety.events.single().trigger)
            if (level == RiskLevel.L3) {
                assertEquals("$text should reach the crisis card", level, viewModel.crisisLevel)
            } else {
                assertNull("$text must not show the crisis card before the ASQ step", viewModel.crisisLevel)
                assertEquals("$text should open the ASQ step", AsqFlow.State(), viewModel.asq)
            }
            assertEquals("$text left the screen", 0, onSavedCalls)
        }
    }

    @Test
    fun `a positive ASQ screen halts an L2 entry without storing it`() {
        val viewModel = viewModel()
        viewModel.onChallengeChange("there is no reason to live")

        viewModel.save { onSavedCalls++ }

        assertTrue("the L2 entry was written before the screen", stored.isEmpty())
        assertEquals(1, safety.events.size)
        assertEquals("journal_entry", safety.events.single().trigger)

        // Q1 yes, Q2-Q4 no, then the acuity probe answers no: still a positive screen at L3.
        viewModel.answerAsq(true)
        repeat(3) { viewModel.answerAsq(false) }
        assertTrue("the entry was written while the screen was open", stored.isEmpty())
        assertNull("the crisis card must wait for the acuity probe", viewModel.crisisLevel)

        viewModel.answerAsq(false)

        assertTrue("the halted entry reached the repository", stored.isEmpty())
        assertEquals(2, safety.events.size)
        val positive = safety.events.last()
        assertEquals("asq_positive", positive.trigger)
        assertEquals(RiskLevel.L3, positive.level)
        assertNull(positive.patternId)
        assertEquals(RiskLevel.L3, viewModel.crisisLevel)
        assertNull("the ASQ step should be closed", viewModel.asq)
        assertEquals(0, onSavedCalls)
    }

    @Test
    fun `a negative ASQ screen saves the L2 entry that was held back`() {
        val viewModel = viewModel()
        viewModel.onChallengeChange("there is no reason to live")

        viewModel.save { onSavedCalls++ }

        assertTrue("the L2 entry was written before the screen", stored.isEmpty())
        assertEquals("journal_entry", safety.events.single().trigger)
        assertTrue("the ASQ step should be open", viewModel.asq != null)

        repeat(4) { viewModel.answerAsq(false) }

        assertNull("a negative screen must not show the crisis card", viewModel.crisisLevel)
        assertNull("the ASQ step should be closed", viewModel.asq)
        assertEquals("there is no reason to live", stored.single().challenge)
        assertEquals(1, safety.events.size)
        assertEquals("journal_entry", safety.events.single().trigger)
        assertEquals(1, onSavedCalls)
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
