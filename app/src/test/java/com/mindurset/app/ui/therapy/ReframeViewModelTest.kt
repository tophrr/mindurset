package com.mindurset.app.ui.therapy

import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.data.remote.GenerationTransport
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.SessionMessage
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryScheduler
import com.mindurset.app.safety.AsqFlow
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ReframeViewModelTest {
    private val sessions = FakeSessionRepository()
    private val actions = FakeActionRepository()
    private val scheduler = RecordingSummaryScheduler()
    private val safety = RecordingSafetyRepository()

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
        assertEquals(1, scheduler.calls.size)
        assertEquals(
            listOf(
                "I will fail",
                "one exam is not the degree",
                "This exam is hard; I have passed harder things",
                "Open the outline for ten minutes",
            ),
            scheduler.calls.single().transcript,
        )
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

    @Test
    fun `L3 at step 0 halts at crisis without persisting or calling client`() {
        val transport = NeverCalledTransport()
        val viewModel = viewModel(transport)

        viewModel.setThought("I want to kill myself tonight")
        viewModel.next()

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertNull(viewModel.asq.value)
        assertEquals(0, viewModel.state.value.step)
        assertTrue(sessions.messages.isEmpty())
        assertNull(sessions.session)
        assertEquals(0, sessions.saveCalls)
        assertEquals(0, transport.calls)
        val event = safety.events.single()
        assertEquals(RiskLevel.L3, event.level)
        assertEquals("reframe_thought", event.trigger)
        assertEquals("en-intent", event.patternId)

        viewModel.crisisAcknowledged()
        assertNull(viewModel.crisisLevel.value)
    }

    @Test
    fun `L2 at step 0 opens ASQ without persisting or calling client`() {
        val transport = NeverCalledTransport()
        val viewModel = viewModel(transport)

        viewModel.setThought("aku ingin mati")
        viewModel.next()

        assertEquals(AsqFlow.State(), viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertEquals(0, viewModel.state.value.step)
        assertTrue(sessions.messages.isEmpty())
        assertNull(sessions.session)
        assertEquals(0, sessions.saveCalls)
        assertEquals(0, transport.calls)
        val event = safety.events.single()
        assertEquals(RiskLevel.L2, event.level)
        assertEquals("reframe_thought", event.trigger)
        assertEquals("id-hopeless", event.patternId)
    }

    @Test
    fun `L2 then four noes persists the held thought once and runs requestGeneration`() {
        val transport = FakeTransport(GENERATED)
        val viewModel = viewModel(transport)

        viewModel.setThought("there is no reason to live")
        viewModel.next()
        repeat(4) { viewModel.answerAsq(yes = false) }

        assertNull(viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertEquals(1, viewModel.state.value.step)
        assertEquals(1, sessions.saveCalls)
        val thoughtMessages = sessions.messages.filter { it.role == ReframeViewModel.ROLE_THOUGHT }
        assertEquals(1, thoughtMessages.size)
        assertEquals("there is no reason to live", thoughtMessages.single().content)
        assertEquals(listOf("/v1/reframe"), transport.paths)
        assertEquals("there is no reason to live", JSONObject(transport.bodies.single()).getString("thought"))
        assertTrue(viewModel.state.value.generated)
    }

    @Test
    fun `L2 then one yes records asq_positive and halts without persisting or calling client`() {
        val transport = NeverCalledTransport()
        val viewModel = viewModel(transport)

        viewModel.setThought("there is no reason to live")
        viewModel.next()
        viewModel.answerAsq(yes = true)
        repeat(3) { viewModel.answerAsq(yes = false) }
        val pending = requireNotNull(viewModel.asq.value)
        assertEquals(AsqFlow.ID_ACUITY, AsqFlow.nextQuestion(pending)?.id)
        assertNull(viewModel.crisisLevel.value)
        viewModel.answerAsq(yes = false)

        assertNull(viewModel.asq.value)
        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertEquals(0, viewModel.state.value.step)
        assertTrue(sessions.messages.isEmpty())
        assertNull(sessions.session)
        assertEquals(0, sessions.saveCalls)
        assertEquals(0, transport.calls)
        assertEquals(2, safety.events.size)
        val positive = safety.events.last()
        assertEquals("asq_positive", positive.trigger)
        assertEquals(RiskLevel.L3, positive.level)
        assertNull(positive.patternId)
    }

    @Test
    fun `model returning L3 on an L0 thought opens crisis without persisting generation or showing readings`() {
        val l3Reply = JSONObject(GENERATED)
            .put("risk_level", JSONObject().put("level", "L3").put("confidence", 0.95))
            .toString()
        val viewModel = viewModel(FakeTransport(l3Reply))

        viewModel.setThought("I will fail no matter what")
        viewModel.next()

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertNull(viewModel.asq.value)
        assertFalse(viewModel.state.value.generating)
        assertFalse(viewModel.state.value.generated)
        assertEquals("", viewModel.state.value.distortion)
        assertTrue(viewModel.state.value.perspectives.isEmpty())
        assertTrue(viewModel.state.value.sources.isEmpty())
        assertEquals(listOf("thought"), sessions.messages.map { it.role })
        val event = safety.events.single()
        assertEquals(RiskLevel.L3, event.level)
        assertEquals("reframe_classifier", event.trigger)
    }

    @Test
    fun `model returning L2 on an L0 thought opens ASQ before showing readings`() {
        val l2Reply = JSONObject(GENERATED)
            .put("risk_level", JSONObject().put("level", "L2").put("confidence", 0.8))
            .toString()
        val viewModel = viewModel(FakeTransport(l2Reply))

        viewModel.setThought("I will fail no matter what")
        viewModel.next()

        assertEquals(AsqFlow.State(), viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertFalse(viewModel.state.value.generated)
        assertTrue(viewModel.state.value.perspectives.isEmpty())
        assertEquals(listOf("thought"), sessions.messages.map { it.role })

        repeat(4) { viewModel.answerAsq(yes = false) }

        assertNull(viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertTrue(viewModel.state.value.generated)
        assertEquals(2, viewModel.state.value.perspectives.size)
        assertEquals(listOf("thought", ReframeViewModel.ROLE_GENERATION), sessions.messages.map { it.role })
    }

    private fun viewModel(
        transport: GenerationTransport? = null,
        summaryScheduler: SessionSummaryScheduler = scheduler,
    ): ReframeViewModel = ReframeViewModel(
        sessions = sessions,
        actions = actions,
        sessionStore = FixedSessionStore(SessionInfo("u1", "guest@local")),
        generation = transport?.let { GenerationClient("http://worker.test", it) },
        summaryScheduler = summaryScheduler,
        safetyRepository = safety,
        now = { 1_700_000_002_000 },
        newId = { "id-${sessions.messages.size}" },
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )
}

private class RecordingSafetyRepository : SafetyRepository {
    val events = mutableListOf<SafetyEvent>()

    override suspend fun record(event: SafetyEvent) {
        events += event
    }

    override suspend fun countSince(userId: String, since: Long): Int =
        events.count { it.userId == userId && it.createdAt >= since }
}

private class RecordingSummaryScheduler : SessionSummaryScheduler {
    data class Call(val userId: String, val sessionId: String, val transcript: List<String>)

    val calls = mutableListOf<Call>()

    override fun enqueue(userId: String, sessionId: String, transcript: List<String>) {
        calls += Call(userId, sessionId, transcript)
    }
}

private class FakeSessionRepository : SessionRepository {
    var session: ReframeSession? = null
    var saveCalls = 0
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
        saveCalls += 1
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

private class NeverCalledTransport : GenerationTransport {
    var calls = 0
        private set

    override suspend fun post(path: String, body: String): String {
        calls += 1
        throw AssertionError("the client must not be called: $path")
    }
}
