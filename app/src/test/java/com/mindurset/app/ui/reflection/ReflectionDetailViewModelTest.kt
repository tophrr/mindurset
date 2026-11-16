package com.mindurset.app.ui.reflection

import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReflectionDetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val entry =
        JournalEntry(
            id = "j1",
            userId = "u1",
            challenge = "A hard meeting",
            reframe = "I said what I could",
            gratitude = "A friend who listened",
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )

    @Test
    fun `the requested entry becomes the ready state`() = runTest {
        val viewModel = ReflectionDetailViewModel(entry.id, FakeEntryRepository(listOf(entry)))

        val state = viewModel.state.first { it is ReflectionDetailViewModel.UiState.Ready }
        advanceUntilIdle()

        assertEquals(ReflectionDetailViewModel.UiState.Ready(entry), state)
    }

    @Test
    fun `an id that no longer exists is not found, not an error`() = runTest {
        val viewModel = ReflectionDetailViewModel("missing", FakeEntryRepository(listOf(entry)))

        val state = viewModel.state.first { it is ReflectionDetailViewModel.UiState.NotFound }
        advanceUntilIdle()

        assertEquals(ReflectionDetailViewModel.UiState.NotFound, state)
    }

    @Test
    fun `a failing read is reported as a load error`() = runTest {
        val repository = FakeEntryRepository(listOf(entry))
        repository.failure = IllegalStateException("database is closed")
        val viewModel = ReflectionDetailViewModel(entry.id, repository)

        val state = viewModel.state.first { it is ReflectionDetailViewModel.UiState.Error }
        advanceUntilIdle()

        assertEquals(
            ReflectionDetailViewModel.UiState.Error(ReflectionErrorKeys.LOAD),
            state,
        )
    }

    @Test
    fun `delete removes the entry before it reports back`() = runTest {
        val repository = FakeEntryRepository(listOf(entry))
        val viewModel = ReflectionDetailViewModel(entry.id, repository)

        viewModel.delete { repository.events += "callback" }
        advanceUntilIdle()

        assertEquals(listOf("delete:j1", "callback"), repository.events)
    }
}

private class FakeEntryRepository(private val entries: List<JournalEntry>) : JournalRepository {
    var failure: Throwable? = null
    val events = mutableListOf<String>()

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

    override suspend fun delete(id: String) {
        events += "delete:$id"
    }
}
