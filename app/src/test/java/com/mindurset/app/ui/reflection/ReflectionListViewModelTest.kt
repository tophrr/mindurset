package com.mindurset.app.ui.reflection

import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReflectionListViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val session = SessionInfo(userId = "u1", email = "christopher@student.uph.edu")

    @Test
    fun `entries from the journal become the ready state`() = runTest {
        val newest = entry("j2", "A calmer evening")
        val older = entry("j1", "A hard meeting")
        val viewModel = viewModel(listOf(newest, older))

        val state =
            viewModel.state.first { it is ReflectionListViewModel.UiState.Ready }
                as ReflectionListViewModel.UiState.Ready
        advanceUntilIdle()

        assertEquals(listOf("j2", "j1"), state.entries.map { it.id })
    }

    @Test
    fun `an empty journal is the empty state, not a ready list`() = runTest {
        val viewModel = viewModel(emptyList())

        val state = viewModel.state.first { it is ReflectionListViewModel.UiState.Empty }
        advanceUntilIdle()

        assertEquals(ReflectionListViewModel.UiState.Empty, state)
    }

    @Test
    fun `a missing session asks the person to start again instead of showing an empty list`() = runTest {
        val viewModel = ReflectionListViewModel(FakeJournalRepository(emptyList()), FakeSessionStore(null))

        val state = viewModel.state.first { it is ReflectionListViewModel.UiState.Error }
        advanceUntilIdle()

        assertEquals(
            ReflectionListViewModel.UiState.Error(ReflectionErrorKeys.NO_SESSION),
            state,
        )
    }

    @Test
    fun `a failing read is reported and retry reads the journal again`() = runTest {
        val repository = FakeJournalRepository(listOf(entry("j1", "A hard meeting")))
        repository.failure = IllegalStateException("database is closed")
        val viewModel = ReflectionListViewModel(repository, FakeSessionStore(session))

        val failure = viewModel.state.first { it is ReflectionListViewModel.UiState.Error }
        advanceUntilIdle()
        assertEquals(ReflectionListViewModel.UiState.Error(ReflectionErrorKeys.LOAD), failure)

        repository.failure = null
        viewModel.retry()
        val recovered = viewModel.state.first { it is ReflectionListViewModel.UiState.Ready }
        advanceUntilIdle()

        assertEquals(listOf("j1"), (recovered as ReflectionListViewModel.UiState.Ready).entries.map { it.id })
    }

    private fun viewModel(entries: List<JournalEntry>) =
        ReflectionListViewModel(FakeJournalRepository(entries), FakeSessionStore(session))

    private fun entry(id: String, challenge: String) = JournalEntry(
        id = id,
        userId = session.userId,
        challenge = challenge,
        reframe = "A different angle",
        gratitude = "One person who listened",
        createdAt = 1_000L,
        updatedAt = 1_000L,
    )
}

private class FakeJournalRepository(private val entries: List<JournalEntry>) : JournalRepository {
    var failure: Throwable? = null

    override fun observeAll(userId: String): Flow<List<JournalEntry>> = flow {
        failure?.let { throw it }
        emit(entries)
    }

    override fun observeById(id: String): Flow<JournalEntry?> = flow {
        failure?.let { throw it }
        emit(entries.firstOrNull { it.id == id })
    }

    override fun count(userId: String): Flow<Int> = flow {
        failure?.let { throw it }
        emit(entries.size)
    }

    override suspend fun upsert(entry: JournalEntry) = Unit

    override suspend fun delete(id: String) = Unit
}

private class FakeSessionStore(initial: SessionInfo?) : SessionStore {
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
