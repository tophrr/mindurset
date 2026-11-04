package com.mindurset.app.ui.therapy

import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.SessionMessage
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReframeViewModelTest {
    private val sessions = FakeSessionRepository()
    private val actions = FakeActionRepository()

    @Test
    fun `each answer is stored before the next step`() {
        val viewModel = viewModel()

        viewModel.setThought("I will fail no matter what")
        viewModel.next()
        assertEquals(1, viewModel.state.value.step)
        assertEquals("thought", sessions.messages.single().role)
        assertEquals(1, sessions.session?.currentStep)

        viewModel.setEvidence("I passed two exams after worse weeks")
        viewModel.next()
        assertEquals(2, viewModel.state.value.step)
        assertEquals(listOf("thought", "evidence"), sessions.messages.map { it.role })
    }

    @Test
    fun `a blank answer does not move the step`() {
        val viewModel = viewModel()

        viewModel.next()

        assertEquals(0, viewModel.state.value.step)
        assertEquals(ReframeViewModel.Error.ThoughtRequired, viewModel.state.value.error)
        assertTrue(sessions.messages.isEmpty())
    }

    @Test
    fun `the third answer completes the session`() {
        val viewModel = viewModel()
        viewModel.setThought("I will fail")
        viewModel.next()
        viewModel.setEvidence("one exam is not the degree")
        viewModel.next()
        viewModel.setAlternative("This exam is hard; I have passed harder things")
        viewModel.next()

        assertEquals(3, viewModel.state.value.step)
        assertEquals("completed", sessions.session?.status)
        assertNotNull(sessions.session?.completedAt)
    }

    @Test
    fun `finishing writes one micro action`() {
        val viewModel = viewModel()
        viewModel.setThought("I will fail")
        viewModel.next()
        viewModel.setEvidence("one exam is not the degree")
        viewModel.next()
        viewModel.setAlternative("This exam is hard; I have passed harder things")
        viewModel.next()

        viewModel.setAction("Open the outline for ten minutes")
        viewModel.setMinutes(10)
        viewModel.finish()

        val action = actions.rows.value.single()
        assertEquals("Open the outline for ten minutes", action.title)
        assertEquals(10, action.minutes)
        assertEquals(sessions.session?.id, action.sessionId)
        assertTrue(viewModel.state.value.finished)
    }

    @Test
    fun `finishing without a step is refused`() {
        val viewModel = viewModel()
        viewModel.setThought("I will fail")
        viewModel.next()
        viewModel.setEvidence("one exam is not the degree")
        viewModel.next()
        viewModel.setAlternative("A fairer version")
        viewModel.next()

        viewModel.finish()

        assertEquals(ReframeViewModel.Error.ActionRequired, viewModel.state.value.error)
        assertTrue(actions.rows.value.isEmpty())
    }

    @Test
    fun `an interrupted session resumes at the step it reached`() {
        val active =
            ReframeSession(
                id = "s1",
                userId = "u1",
                checkInId = null,
                currentStep = 2,
                status = "active",
                promptVersion = null,
                modelVersion = null,
                startedAt = 1_700_000_000_000,
                completedAt = null,
            )
        sessions.session = active
        sessions.messages +=
            SessionMessage("m1", "s1", "thought", "I will fail", 0, 1_700_000_000_000)
        sessions.messages +=
            SessionMessage("m2", "s1", "evidence", "I passed before", 1, 1_700_000_001_000)

        val viewModel = viewModel()

        assertEquals(2, viewModel.state.value.step)
        assertEquals("I will fail", viewModel.state.value.thought)
        assertEquals("I passed before", viewModel.state.value.evidence)
    }

    private fun viewModel(): ReframeViewModel = ReframeViewModel(
        sessions = sessions,
        actions = actions,
        sessionStore = FixedSessionStore(SessionInfo("u1", "guest@local")),
        now = { 1_700_000_002_000 },
        newId = { "id-${sessions.messages.size}" },
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )
}

private class FakeSessionRepository : SessionRepository {
    var session: ReframeSession? = null
    val messages = mutableListOf<SessionMessage>()

    override suspend fun start(userId: String, checkInId: String?, now: Long): ReframeSession {
        val started =
            ReframeSession(
                id = "session-1",
                userId = userId,
                checkInId = checkInId,
                currentStep = 0,
                status = "active",
                promptVersion = null,
                modelVersion = null,
                startedAt = now,
                completedAt = null,
            )
        session = started
        return started
    }

    override suspend fun findActive(userId: String): ReframeSession? =
        session?.takeIf { it.userId == userId && it.status == "active" }

    override suspend fun findById(id: String): ReframeSession? = session?.takeIf { it.id == id }

    override suspend fun save(session: ReframeSession) {
        this.session = session
    }

    override suspend fun appendMessage(message: SessionMessage) {
        messages += message
    }

    override suspend fun messages(sessionId: String): List<SessionMessage> =
        messages.filter { it.sessionId == sessionId }
}

private class FakeActionRepository : MicroActionRepository {
    val rows = MutableStateFlow<List<MicroAction>>(emptyList())

    override fun observeOpen(userId: String): Flow<List<MicroAction>> = rows.map { list -> list.filter { !it.done } }

    override fun observeAll(userId: String): Flow<List<MicroAction>> = rows

    override suspend fun add(action: MicroAction) {
        rows.update { it + action }
    }

    override suspend fun markDone(id: String, at: Long) = Unit

    override suspend fun snooze(id: String, until: Long) = Unit
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
