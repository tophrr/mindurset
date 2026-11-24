package com.mindurset.app.ui.therapy

import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.data.remote.GenerationTransport
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
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

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

    @Test
    fun `a grounded answer offers two readings and records their versions`() {
        val transport = FakeTransport(GENERATED)
        val viewModel = viewModel(transport)

        viewModel.setThought("I will fail no matter what")
        viewModel.next()

        assertEquals("/v1/reframe", transport.paths.single())
        assertEquals("I will fail no matter what", JSONObject(transport.bodies.single()).getString("thought"))
        assertTrue(viewModel.state.value.generated)
        assertEquals("all-or-nothing thinking", viewModel.state.value.distortion)
        assertEquals(2, viewModel.state.value.perspectives.size)
        assertEquals(listOf("who-stress-01#p3"), viewModel.state.value.sources)

        val stored = sessions.messages.single { it.role == ReframeViewModel.ROLE_GENERATION }
        assertEquals("v1.1.0", stored.promptVersion)
        assertEquals("test-model", stored.modelVersion)
    }

    @Test
    fun `choosing a reading fills the fairer version`() {
        val viewModel = viewModel(FakeTransport(GENERATED))
        viewModel.setThought("I will fail no matter what")
        viewModel.next()

        viewModel.choosePerspective(1)

        assertEquals(viewModel.state.value.perspectives[1], viewModel.state.value.alternative)
    }

    @Test
    fun `the suggested action prefills the last step`() {
        val viewModel = viewModel(FakeTransport(GENERATED))
        viewModel.setThought("I will fail no matter what")
        viewModel.next()

        assertEquals("Open the exam outline for ten minutes", viewModel.state.value.actionTitle)
        assertEquals(10, viewModel.state.value.minutes)
    }

    @Test
    fun `a failed answer leaves the session on the offline path`() {
        val viewModel = viewModel(FailingTransport())

        viewModel.setThought("I will fail no matter what")
        viewModel.next()

        assertEquals(1, viewModel.state.value.step)
        assertFalse(viewModel.state.value.generated)
        assertTrue(viewModel.state.value.perspectives.isEmpty())
        assertEquals(listOf("thought"), sessions.messages.map { it.role })
    }

    private fun viewModel(transport: GenerationTransport? = null): ReframeViewModel = ReframeViewModel(
        sessions = sessions,
        actions = actions,
        sessionStore = FixedSessionStore(SessionInfo("u1", "guest@local")),
        generation = transport?.let { GenerationClient("http://worker.test", it) },
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

private const val GENERATED = """
{
  "message": "You wrote that you will fail no matter what. That reads like all-or-nothing thinking.",
  "distortion": "all-or-nothing thinking",
  "perspectives": [
    "You passed other exams after bad weeks.",
    "One paper is not the degree."
  ],
  "micro_action": { "title": "Open the exam outline for ten minutes", "minutes": 10 },
  "source_ids": ["who-stress-01#p3"],
  "risk_level": { "level": "L0", "confidence": 0.9 },
  "prompt_version": "v1.1.0",
  "model_version": "test-model"
}
"""

private class FakeTransport(private val response: String) : GenerationTransport {
    val paths = mutableListOf<String>()
    val bodies = mutableListOf<String>()

    override suspend fun post(path: String, body: String): String {
        paths += path
        bodies += body
        return response
    }
}

private class FailingTransport : GenerationTransport {
    override suspend fun post(path: String, body: String): String = throw IOException("worker unreachable")
}
