package com.mindurset.app.ui.memory

import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryManagerViewModelTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `purges expired summaries on load and starts empty when none active`() {
        val repo = FakeSummaryRepo(
            listOf(SessionSummary("expired", "u1", "s0", "Old expired summary", now - 10L, now - 100L)),
        )
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = MemoryManagerViewModel(
            summaryRepository = repo,
            preferencesStore = FakeMemoryPreferences(),
            sessionStore = FakeMemorySessionStore(SessionInfo("u1", "guest@local")),
            now = { now },
            externalScope = scope,
        )
        val collector = scope.launch { vm.uiState.collect {} }

        assertFalse(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.memoryEnabled)
        assertTrue(vm.uiState.value.summaries.isEmpty())
        assertTrue(repo.rows.value.isEmpty())

        collector.cancel()
        scope.cancel()
    }

    @Test
    fun `edit delete clearAll and user note changes persist`() {
        val repo = FakeSummaryRepo(
            listOf(
                SessionSummary("s1", "u1", "sess-1", "First summary", now + 10_000L, now - 2_000L),
                SessionSummary("s2", "u1", "sess-2", "Second summary", now + 10_000L, now - 1_000L),
            ),
        )
        val prefs = FakeMemoryPreferences()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = MemoryManagerViewModel(
            summaryRepository = repo,
            preferencesStore = prefs,
            sessionStore = FakeMemorySessionStore(SessionInfo("u1", "guest@local")),
            now = { now },
            externalScope = scope,
        )
        val collector = scope.launch { vm.uiState.collect {} }

        vm.setMemoryEnabled(false)
        assertFalse(vm.uiState.value.memoryEnabled)

        vm.editSummary("s1", "Updated first summary")
        assertEquals("Updated first summary", vm.uiState.value.summaries.first { it.id == "s1" }.summary)

        vm.deleteSummary("s2")
        assertEquals(listOf("s1"), vm.uiState.value.summaries.map { it.id })

        vm.onNoteDraftChange("Prefer slow grounding prompts.")
        vm.saveNote()
        assertEquals("Prefer slow grounding prompts.", vm.uiState.value.userNote)
        assertTrue(vm.uiState.value.noteSaved)

        vm.clearNote()
        assertEquals("", vm.uiState.value.userNote)
        assertEquals("", vm.uiState.value.noteDraft)

        vm.onNoteDraftChange("Keep")
        vm.saveNote()
        vm.clearAllMemory()
        assertTrue(vm.uiState.value.summaries.isEmpty())
        assertEquals("", vm.uiState.value.userNote)

        collector.cancel()
        scope.cancel()
    }
}

private class FakeSummaryRepo(initial: List<SessionSummary>) : SessionSummaryRepository {
    val rows = MutableStateFlow(initial)

    override fun observe(userId: String): Flow<List<SessionSummary>> =
        rows.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override suspend fun latest(userId: String, now: Long): List<SessionSummary> =
        rows.value.filter { it.userId == userId && it.retentionUntil > now }.take(MemoryPolicy.MAX_SUMMARIES)

    override suspend fun saveAndTrim(summary: SessionSummary, now: Long) = Unit

    override suspend fun updateText(id: String, summary: String) {
        rows.update { list -> list.map { if (it.id == id) it.copy(summary = summary) else it } }
    }

    override suspend fun delete(id: String) {
        rows.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun deleteAll(userId: String) {
        rows.update { list -> list.filterNot { it.userId == userId } }
    }

    override suspend fun purgeExpired(now: Long) {
        rows.update { list -> list.filter { it.retentionUntil > now } }
    }
}

private class FakeMemoryPreferences : PreferencesStore {
    private val _memoryEnabled = MutableStateFlow(true)
    private val notes = MutableStateFlow<Map<String, String>>(emptyMap())

    override val reminderEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val reminderHour: Flow<Int> = MutableStateFlow(19)
    override val language: Flow<String> = MutableStateFlow("en")
    override val theme: Flow<String> = MutableStateFlow("system")
    override val memoryEnabled: Flow<Boolean> = _memoryEnabled
    override val onboardingCompleted: Flow<Boolean> = MutableStateFlow(false)

    override fun memoryNote(userId: String): Flow<String> = notes.map { it[userId].orEmpty() }

    override suspend fun setReminderEnabled(enabled: Boolean) = Unit

    override suspend fun setReminderHour(hour: Int) = Unit

    override suspend fun setLanguage(language: String) = Unit

    override suspend fun setTheme(theme: String) = Unit

    override suspend fun setMemoryEnabled(enabled: Boolean) {
        _memoryEnabled.value = enabled
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

    override suspend fun setMemoryNote(userId: String, note: String) {
        notes.update { current -> if (note.isBlank()) current - userId else current + (userId to note) }
    }
}

private class FakeMemorySessionStore(initial: SessionInfo?) : SessionStore {
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
