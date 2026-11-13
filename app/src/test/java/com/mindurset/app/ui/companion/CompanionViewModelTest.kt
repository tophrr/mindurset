package com.mindurset.app.ui.companion

import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.data.remote.GenerationTransport
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionViewModelTest {
    private val fixture =
        requireNotNull(javaClass.getResource("/fixtures/generation-ok.json"))
            .readText()

    @Test
    fun sendsTheSelectedModeAndRendersGroundedReply() {
        val transport = QueuedTransport(fixture)
        val (viewModel, scope) = viewModelWith(transport)

        assertEquals(CompanionReplyState.Idle, viewModel.uiState.value.replyState)
        viewModel.selectMode(CompanionSupportMode.NEGATIVE_THOUGHTS)
        viewModel.send("A busy day")

        assertEquals("/v1/chat", transport.lastPath)
        assertEquals("reflect", JSONObject(requireNotNull(transport.lastBody)).getString("mode"))
        assertTrue(viewModel.uiState.value.replyState is CompanionReplyState.Reply)
        assertEquals(
            listOf("who-stress-01#p3", "cbt-thought-record#s2"),
            viewModel.uiState.value.lines.last().sourceIds,
        )
        scope.cancel()
    }

    @Test
    fun ungroundedWorkerReplyUsesOfflineFallback() {
        val ungrounded = JSONObject(fixture).put("source_ids", JSONArray()).toString()
        val (viewModel, scope) = viewModelWith(QueuedTransport(ungrounded))

        viewModel.send("A busy day")

        assertEquals(CompanionReplyState.OfflineFallback, viewModel.uiState.value.replyState)
        assertTrue(viewModel.uiState.value.lines.last().offline)
        scope.cancel()
    }

    @Test
    fun offlineLabelledReplyIsNotShownAsGrounded() {
        // The Worker labels a reply it produced without reaching the model. Even when such a body
        // carries source ids, the client must not present it as a grounded answer.
        val offline = JSONObject(fixture).put("model_version", "offline-fallback").toString()
        val (viewModel, scope) = viewModelWith(QueuedTransport(offline))

        viewModel.send("A busy day")

        assertEquals(CompanionReplyState.OfflineFallback, viewModel.uiState.value.replyState)
        assertTrue(viewModel.uiState.value.lines.last().offline)
        scope.cancel()
    }

    @Test
    fun invalidContractReplyUsesOfflineCopyAndRetryCanSucceed() {
        val invalid = JSONObject(fixture)
            .put("micro_action", JSONObject(fixture).getJSONObject("micro_action").put("minutes", 16))
            .toString()
        val transport = QueuedTransport(invalid, fixture)
        val (viewModel, scope) = viewModelWith(transport)

        viewModel.send("A busy day")
        assertEquals(CompanionReplyState.Error, viewModel.uiState.value.replyState)
        assertTrue(viewModel.uiState.value.lines.last().offline)

        viewModel.retry()

        assertTrue(viewModel.uiState.value.replyState is CompanionReplyState.Reply)
        scope.cancel()
    }

    @Test
    fun aBuildWithoutAnEndpointGoesStraightToTheOfflineCopy() {
        val transport = NeverCalledTransport()
        val (viewModel, scope) = viewModelWith(transport, baseUrl = "")

        viewModel.send("A busy day")

        assertEquals(CompanionReplyState.OfflineFallback, viewModel.uiState.value.replyState)
        assertEquals(0, transport.calls)
        scope.cancel()
    }

    @Test
    fun anL3LineNeverJoinsTheTranscriptAndRecordsOneEvent() {
        val transport = NeverCalledTransport()
        val (viewModel, safety, scope) = viewModelWithSafety(transport)

        viewModel.send("aku mau bunuh diri")

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertTrue(viewModel.uiState.value.lines.isEmpty())
        assertEquals(0, transport.calls)
        assertEquals(1, safety.events.size)
        val event = safety.events.single()
        assertEquals(RiskLevel.L3, event.level)
        assertEquals("companion_message", event.trigger)
        assertEquals("u1", event.userId)
        scope.cancel()
    }

    @Test
    fun anL2LineIsScreenedTheSameWay() {
        val (viewModel, safety, scope) = viewModelWithSafety(NeverCalledTransport())

        viewModel.send("aku ingin mati")

        assertEquals(RiskLevel.L2, viewModel.crisisLevel.value)
        assertTrue(viewModel.uiState.value.lines.isEmpty())
        assertEquals(1, safety.events.size)
        scope.cancel()
    }

    @Test
    fun acknowledgingTheCardClearsTheFlag() {
        val (viewModel, _, scope) = viewModelWithSafety(NeverCalledTransport())

        viewModel.send("aku mau bunuh diri")
        viewModel.crisisAcknowledged()

        assertNull(viewModel.crisisLevel.value)
        scope.cancel()
    }

    @Test
    fun anOrdinaryLineRecordsNoSafetyEvent() {
        val (viewModel, safety, scope) = viewModelWithSafety(QueuedTransport(fixture))

        viewModel.send("A busy day, mostly deadlines")

        assertNull(viewModel.crisisLevel.value)
        assertEquals(0, safety.events.size)
        scope.cancel()
    }

    @Test
    fun anUnreachableWorkerOffersResourcesInsteadOfStoppingSilently() {
        val (viewModel, safety, scope) = viewModelWithSafety(OfflineTransport())

        viewModel.send("A busy day, mostly deadlines")

        assertEquals(CompanionReplyState.OfflineFallback, viewModel.uiState.value.replyState)
        assertTrue("L0 plus an outage is L1", viewModel.uiState.value.resourcesOffered)
        assertNull(viewModel.crisisLevel.value)
        assertEquals(0, safety.events.size)
        scope.cancel()
    }

    @Test
    fun aClassifierThatRaisesTheLevelHalts() {
        val raised = JSONObject(fixture)
            .put("risk_level", JSONObject().put("level", "L2").put("confidence", 0.8))
            .toString()
        val (viewModel, safety, scope) = viewModelWithSafety(QueuedTransport(raised))

        viewModel.send("deadlines everywhere, nothing else")

        assertEquals(RiskLevel.L2, viewModel.crisisLevel.value)
        assertEquals(1, safety.events.size)
        assertEquals("companion_classifier", safety.events.single().trigger)
        scope.cancel()
    }

    @Test
    fun theOfferedResourcesClearWhenAReplyArrives() {
        val (viewModel, _, scope) = viewModelWithSafety(FailOnceTransport(fixture))

        viewModel.send("A busy day, mostly deadlines")
        assertTrue("the outage raised the level to L1", viewModel.uiState.value.resourcesOffered)

        viewModel.retry()

        assertFalse(viewModel.uiState.value.resourcesOffered)
        assertTrue(viewModel.uiState.value.replyState is CompanionReplyState.Reply)
        scope.cancel()
    }

    private fun viewModelWith(
        transport: GenerationTransport,
        baseUrl: String = "http://localhost:8787",
    ): Pair<CompanionViewModel, CoroutineScope> {
        val (viewModel, _, scope) = viewModelWithSafety(transport, baseUrl)
        return viewModel to scope
    }

    private fun viewModelWithSafety(
        transport: GenerationTransport,
        baseUrl: String = "http://localhost:8787",
    ): Triple<CompanionViewModel, RecordingSafetyRepository, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val safety = RecordingSafetyRepository()
        val viewModel = CompanionViewModel(
            generationClient = GenerationClient(baseUrl, transport),
            safetyRepository = safety,
            sessionStore = FixedSessionStore(SessionInfo(userId = "u1", email = "guest@local")),
            now = { 1_700_000_000_000L },
            newId = { "event-1" },
            externalScope = scope,
        )
        return Triple(viewModel, safety, scope)
    }

    private class QueuedTransport(vararg responses: String) : GenerationTransport {
        private val queued = responses.toMutableList()
        var lastPath: String? = null
            private set
        var lastBody: String? = null
            private set

        override suspend fun post(path: String, body: String): String {
            lastPath = path
            lastBody = body
            return queued.removeAt(0)
        }
    }

    /** Fails the first call, then serves a body: the retry path after an outage. */
    private class FailOnceTransport(private val thenSucceed: String) : GenerationTransport {
        private var failed = false

        override suspend fun post(path: String, body: String): String {
            if (!failed) {
                failed = true
                throw java.io.IOException("no route to host")
            }
            return thenSucceed
        }
    }

    /** A transport that cannot reach the Worker, which is the fail-closed case. */
    private class OfflineTransport : GenerationTransport {
        var calls = 0
            private set

        override suspend fun post(path: String, body: String): String {
            calls += 1
            throw java.io.IOException("no route to host")
        }
    }

    /** Fails the test if the client sends anything, which is the point of the screening cases. */
    private class NeverCalledTransport : GenerationTransport {
        var calls = 0
            private set

        override suspend fun post(path: String, body: String): String {
            calls += 1
            throw AssertionError("the client must not be called: $path")
        }
    }

    private class RecordingSafetyRepository : SafetyRepository {
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
}
