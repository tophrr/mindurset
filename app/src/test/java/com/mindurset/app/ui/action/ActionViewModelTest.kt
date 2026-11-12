package com.mindurset.app.ui.action

import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ActionViewModelTest {
    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 10, 21)
    private val now = today.atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
    private val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    private val session = SessionInfo(userId = "u1", email = "christopher@student.uph.edu")

    @Test
    fun `groups open, done and snoozed actions`() {
        val walk = action("walk", "Take a short walk", minutes = 10, done = false)
        val water = action("water", "Drink water", minutes = 2, done = false)
        val stretch = action("stretch", "Stretch gently", minutes = 3, done = true, completedAt = now)
        val breathe =
            action("breathe", "Breathe slowly", minutes = 1, done = false, snoozedUntil = tomorrowStart)
        val viewModel = viewModel(FakeMicroActionRepository(listOf(walk, water, stretch, breathe)))

        val state = viewModel.uiState.value as ActionUiState.Populated
        assertEquals(listOf("walk", "water"), state.today.map { it.id })
        assertEquals(listOf("stretch"), state.doneToday.map { it.id })
        assertEquals(listOf("breathe"), state.tomorrow.map { it.id })
    }

    @Test
    fun `an empty table is the empty state, not a blank populated list`() {
        val viewModel = viewModel(FakeMicroActionRepository(emptyList()))
        assertEquals(ActionUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a missing session shows nothing`() {
        val viewModel = viewModel(FakeMicroActionRepository(emptyList()), session = null)
        assertEquals(ActionUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `adding something writes one action for the signed-in user`() {
        val repository = FakeMicroActionRepository(emptyList())
        val viewModel = viewModel(repository)

        assertTrue(viewModel.add("  Pause for water  ", 2))

        val saved = repository.rows.value.single()
        assertEquals("Pause for water", saved.title)
        assertEquals(2, saved.minutes)
        assertEquals("u1", saved.userId)
        assertEquals(now, saved.createdAt)
        assertEquals(
            "Pause for water",
            (viewModel.uiState.value as ActionUiState.Populated).today.single().title,
        )
    }

    @Test
    fun `the editor refuses a blank title and a duration outside one to fifteen`() {
        val repository = FakeMicroActionRepository(emptyList())
        val viewModel = viewModel(repository)

        assertFalse(viewModel.add("   ", 5))
        assertEquals(ActionEditorError.TitleRequired, viewModel.editorError.value)

        assertFalse(viewModel.add("Walk", 0))
        assertEquals(ActionEditorError.MinutesOutOfRange, viewModel.editorError.value)

        assertFalse(viewModel.add("Walk", 16))
        assertEquals(ActionEditorError.MinutesOutOfRange, viewModel.editorError.value)

        assertTrue(viewModel.add("Walk", ActionViewModel.MAX_MINUTES))
        assertNull(viewModel.editorError.value)
        assertEquals(1, repository.rows.value.size)
    }

    @Test
    fun `completing moves the action to done today`() {
        val walk = action("walk", "Take a short walk", minutes = 10, done = false)
        val viewModel = viewModel(FakeMicroActionRepository(listOf(walk)))

        viewModel.complete("walk")

        val state = viewModel.uiState.value as ActionUiState.Populated
        assertEquals("walk", state.doneToday.single().id)
        assertTrue(state.today.isEmpty())
    }

    @Test
    fun `snoozing moves the action to tomorrow without duplicating it`() {
        val walk = action("walk", "Take a short walk", minutes = 10, done = false)
        val repository = FakeMicroActionRepository(listOf(walk))
        val viewModel = viewModel(repository)

        viewModel.snooze("walk")

        val state = viewModel.uiState.value as ActionUiState.Populated
        assertTrue(state.today.isEmpty())
        assertEquals("walk", state.tomorrow.single().id)
        assertEquals(tomorrowStart, state.tomorrow.single().snoozedUntil)
        assertEquals(1, repository.rows.value.size)
    }

    @Test
    fun `editing keeps the row and changes only what was edited`() {
        val walk = action("walk", "Take a short walk", minutes = 10, done = false)
        val repository = FakeMicroActionRepository(listOf(walk))
        val viewModel = viewModel(repository)

        assertTrue(viewModel.edit("walk", "A gentler walk", 5))

        val edited = repository.rows.value.single()
        assertEquals("A gentler walk", edited.title)
        assertEquals(5, edited.minutes)
        assertEquals(walk.createdAt, edited.createdAt)
    }

    @Test
    fun `a failing read surfaces the error state and retry reads again`() {
        val repository = FlakyMicroActionRepository()
        val viewModel = viewModel(repository)

        assertEquals(ActionUiState.Error, viewModel.uiState.value)

        repository.healthy = true
        viewModel.retry()
        assertEquals(ActionUiState.Empty, viewModel.uiState.value)
    }

    private fun viewModel(repository: MicroActionRepository, session: SessionInfo? = this.session): ActionViewModel =
        ActionViewModel(
            actions = repository,
            sessionStore = FakeSessionStore(session),
            today = { today },
            zone = zone,
            now = { now },
            externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

    private fun action(
        id: String,
        title: String,
        minutes: Int,
        done: Boolean,
        snoozedUntil: Long? = null,
        completedAt: Long? = null,
    ): MicroAction = MicroAction(
        id = id,
        userId = "u1",
        sessionId = null,
        title = title,
        minutes = minutes,
        done = done,
        snoozedUntil = snoozedUntil,
        createdAt = now,
        completedAt = completedAt,
    )
}

private class FakeMicroActionRepository(initial: List<MicroAction>) : MicroActionRepository {
    val rows = MutableStateFlow(initial)

    override fun observeOpen(userId: String): Flow<List<MicroAction>> =
        observeAll(userId).map { actions -> actions.filter { !it.done } }

    override fun observeAll(userId: String): Flow<List<MicroAction>> =
        rows.map { actions -> actions.filter { it.userId == userId } }

    override suspend fun add(action: MicroAction) {
        rows.update { actions -> actions.filterNot { it.id == action.id } + action }
    }

    override suspend fun markDone(id: String, at: Long) {
        rows.update { actions ->
            actions.map { if (it.id == id) it.copy(done = true, completedAt = at) else it }
        }
    }

    override suspend fun snooze(id: String, until: Long) {
        rows.update { actions ->
            actions.map { if (it.id == id) it.copy(snoozedUntil = until) else it }
        }
    }
}

private class FlakyMicroActionRepository : MicroActionRepository {
    var healthy = false

    override fun observeOpen(userId: String): Flow<List<MicroAction>> = observeAll(userId)

    override fun observeAll(userId: String): Flow<List<MicroAction>> = flow {
        if (!healthy) error("database is closed")
        emit(emptyList())
    }

    override suspend fun add(action: MicroAction) = Unit

    override suspend fun markDone(id: String, at: Long) = Unit

    override suspend fun snooze(id: String, until: Long) = Unit
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
