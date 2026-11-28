package com.mindurset.app.ui.companion

import com.mindurset.app.data.remote.GenerationClient
import com.mindurset.app.data.remote.GenerationTransport
import com.mindurset.app.data.repository.AssetOfflineExerciseLibrary
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.MemoryContextProvider
import com.mindurset.app.domain.repository.OfflineExerciseLibrary
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryScheduler
import com.mindurset.app.safety.AsqFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CompanionViewModelTest {
    private val fixture =
        requireNotNull(javaClass.getResource("/fixtures/generation-ok.json"))
            .readText()

    @Test
    fun sendsTheSelectedModeAndRendersGroundedReply() {
        val transport = QueuedTransport(fixture)
        val (viewModel, scope) = viewModelWith(transport)

        assertEquals(CompanionReplyState.Idle, viewModel.uiState.value.replyState)
        viewModel.selectMode(CompanionSupportMode.SOLVE)
        viewModel.send("A busy day")

        assertEquals("/v1/chat", transport.lastPath)
        assertEquals("solve", JSONObject(requireNotNull(transport.lastBody)).getString("mode"))
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
        assertNull(viewModel.asq.value)
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
    fun anL2LineOpensTheAsqPanelWithoutCallingTheWorker() {
        val transport = NeverCalledTransport()
        val (viewModel, safety, scope) = viewModelWithSafety(transport)

        viewModel.send("aku ingin mati")

        assertEquals(AsqFlow.State(), viewModel.asq.value)
        assertNull("L2 opens the ASQ panel instead of jumping to the card", viewModel.crisisLevel.value)
        assertTrue(viewModel.uiState.value.lines.isEmpty())
        assertEquals(0, transport.calls)
        assertEquals(1, safety.events.size)
        val event = safety.events.single()
        assertEquals(RiskLevel.L2, event.level)
        assertEquals("companion_message", event.trigger)
        assertEquals("id-hopeless", event.patternId)
        scope.cancel()
    }

    @Test
    fun aPositiveAsqScreenAfterAnL2LineHaltsAtL3WithoutCallingTheWorker() {
        val transport = NeverCalledTransport()
        val (viewModel, safety, scope) = viewModelWithSafety(transport)

        viewModel.send("there is no reason to live")

        // Q1 yes, Q2-Q4 no, then acuity probe comes last.
        viewModel.answerAsq(yes = true)
        repeat(3) { viewModel.answerAsq(yes = false) }
        val pending = requireNotNull(viewModel.asq.value)
        assertEquals("acuity comes last", AsqFlow.ID_ACUITY, AsqFlow.nextQuestion(pending)?.id)
        assertNull("the crisis card must wait for the acuity probe", viewModel.crisisLevel.value)
        assertEquals(0, transport.calls)

        viewModel.answerAsq(yes = false)

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertNull(viewModel.asq.value)
        assertTrue(viewModel.uiState.value.lines.isEmpty())
        assertEquals(0, transport.calls)
        assertEquals(2, safety.events.size)
        val positive = safety.events.last()
        assertEquals("asq_positive", positive.trigger)
        assertEquals(RiskLevel.L3, positive.level)
        assertNull(positive.patternId)
        scope.cancel()
    }

    @Test
    fun fourNoAnswersOfferOfflineSupportWithoutCallingTheWorkerOrLockingOut() {
        val transport = QueuedTransport(fixture)
        val (viewModel, safety, scope) = viewModelWithSafety(transport)

        viewModel.send("aku ingin mati")
        repeat(4) { viewModel.answerAsq(yes = false) }

        assertNull(viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertNull("L2 text must never be sent to the Worker", transport.lastPath)
        assertEquals(1, viewModel.uiState.value.lines.size)
        assertTrue(viewModel.uiState.value.lines.single().offline)
        assertTrue(viewModel.uiState.value.asqSupportOffered)
        assertEquals(CompanionReplyState.Idle, viewModel.uiState.value.replyState)
        assertEquals(1, safety.events.size)
        assertEquals("companion_message", safety.events.single().trigger)

        // Chat remains open after a negative screen, and a new ordinary message clears the banner.
        viewModel.send("A busy day, mostly deadlines")
        assertEquals("/v1/chat", transport.lastPath)
        assertFalse(viewModel.uiState.value.asqSupportOffered)
        assertTrue(viewModel.uiState.value.replyState is CompanionReplyState.Reply)
        scope.cancel()
    }

    @Test
    fun sendsAndModeChangesAreIgnoredWhileAsqIsOpen() {
        val transport = NeverCalledTransport()
        val (viewModel, _, scope) = viewModelWithSafety(transport)

        viewModel.send("aku ingin mati")
        viewModel.selectMode(CompanionSupportMode.SOLVE)
        viewModel.send("A busy day")

        assertEquals(CompanionSupportMode.LISTEN, viewModel.uiState.value.supportMode)
        assertEquals(AsqFlow.State(), viewModel.asq.value)
        assertTrue(viewModel.uiState.value.lines.isEmpty())
        assertEquals(0, transport.calls)
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
    fun aClassifierThatRaisesToL2OpensAsqAndReleasesReplyOnNegativeScreen() {
        val raised = JSONObject(fixture)
            .put("risk_level", JSONObject().put("level", "L2").put("confidence", 0.8))
            .toString()
        val (viewModel, safety, scope) = viewModelWithSafety(QueuedTransport(raised))

        viewModel.send("deadlines everywhere, nothing else")

        assertEquals(AsqFlow.State(), viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertEquals("reply is held until ASQ finishes", 1, viewModel.uiState.value.lines.size)
        assertEquals(1, safety.events.size)
        assertEquals("companion_classifier", safety.events.single().trigger)
        assertEquals(RiskLevel.L2, safety.events.single().level)

        repeat(4) { viewModel.answerAsq(yes = false) }

        assertNull(viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertTrue(viewModel.uiState.value.replyState is CompanionReplyState.Reply)
        assertTrue(viewModel.uiState.value.asqSupportOffered)
        assertEquals(2, viewModel.uiState.value.lines.size)
        scope.cancel()
    }

    @Test
    fun aClassifierThatRaisesToL2HaltsAtL3WhenAsqIsPositive() {
        val raised = JSONObject(fixture)
            .put("risk_level", JSONObject().put("level", "L2").put("confidence", 0.8))
            .toString()
        val (viewModel, safety, scope) = viewModelWithSafety(QueuedTransport(raised))

        viewModel.send("deadlines everywhere, nothing else")
        viewModel.answerAsq(yes = true)
        repeat(4) { viewModel.answerAsq(yes = false) }

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertNull(viewModel.asq.value)
        assertEquals("held reply must not be added on a positive screen", 1, viewModel.uiState.value.lines.size)
        assertEquals(2, safety.events.size)
        assertEquals("companion_classifier", safety.events[0].trigger)
        assertEquals("asq_positive", safety.events[1].trigger)
        scope.cancel()
    }

    @Test
    fun aClassifierThatRaisesToL3HaltsImmediately() {
        val raised = JSONObject(fixture)
            .put("risk_level", JSONObject().put("level", "L3").put("confidence", 0.95))
            .toString()
        val (viewModel, safety, scope) = viewModelWithSafety(QueuedTransport(raised))

        viewModel.send("deadlines everywhere, nothing else")

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertNull(viewModel.asq.value)
        assertEquals(1, safety.events.size)
        assertEquals("companion_classifier", safety.events.single().trigger)
        assertEquals(RiskLevel.L3, safety.events.single().level)
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

    @Test
    fun injectsContextIntoChatRequestWhenMemoryIsEnabled() {
        val transport = QueuedTransport(fixture)
        val contextProvider = FixedMemoryContextProvider("State: last_check_in=anxious(6/10, 1h ago)")
        val (viewModel, _, scope) = viewModelWithSafety(
            transport = transport,
            memoryContextProvider = contextProvider,
        )

        viewModel.send("A busy day")

        val body = JSONObject(requireNotNull(transport.lastBody))
        assertEquals("State: last_check_in=anxious(6/10, 1h ago)", body.getString("context"))
        scope.cancel()
    }

    @Test
    fun omitsContextWhenMemoryIsDisabled() {
        val transport = QueuedTransport(fixture)
        val contextProvider = FixedMemoryContextProvider(null)
        val (viewModel, _, scope) = viewModelWithSafety(
            transport = transport,
            memoryContextProvider = contextProvider,
        )

        viewModel.send("A busy day")

        val body = JSONObject(requireNotNull(transport.lastBody))
        assertFalse(body.has("context"))
        scope.cancel()
    }

    @Test
    fun safetyFloorStillHaltsWithoutCallingWorkerWhenMemoryIsEnabled() {
        val transport = NeverCalledTransport()
        val contextProvider = FixedMemoryContextProvider("State: last_check_in=stressed(8/10, 2h ago)")
        val (viewModel, safety, scope) = viewModelWithSafety(
            transport = transport,
            memoryContextProvider = contextProvider,
        )

        viewModel.send("aku ingin mati")

        assertEquals(AsqFlow.State(), viewModel.asq.value)
        assertEquals(0, transport.calls)
        assertEquals(1, safety.events.size)
        scope.cancel()
    }

    @Test
    fun endSessionEnqueuesSummaryForGroundedExchangeAndSkipsOfflineOnly() {
        val scheduler = RecordingCompanionSummaryScheduler()
        val (offlineVm, _, offlineScope) = viewModelWithSafety(
            transport = OfflineTransport(),
            summaryScheduler = scheduler,
        )
        offlineVm.send("A busy day")
        offlineVm.endSession()
        assertTrue("offline-only round must not enqueue a summary", scheduler.calls.isEmpty())
        offlineScope.cancel()

        val (groundedVm, _, groundedScope) = viewModelWithSafety(
            transport = QueuedTransport(fixture),
            summaryScheduler = scheduler,
        )
        groundedVm.send("A busy day")
        groundedVm.endSession()

        assertEquals(1, scheduler.calls.size)
        assertEquals("u1", scheduler.calls.single().userId)
        assertEquals(2, scheduler.calls.single().transcript.size)
        assertTrue(groundedVm.uiState.value.lines.isEmpty())
        assertEquals(CompanionReplyState.Idle, groundedVm.uiState.value.replyState)
        groundedScope.cancel()
    }

    @Test
    fun offlineFallbackAttachesMessageMatchedExerciseCard() {
        val (viewModel, scope) = viewModelWithLibrary(OfflineTransport())

        viewModel.send("I keep scrolling at night and cannot sleep")

        val reply = viewModel.uiState.value.lines.last()
        assertTrue(reply.offline)
        assertNotNull(reply.exercise)
        assertEquals("sleep", reply.exercise?.topic)
        assertEquals("Sleep", reply.exercise?.title)
        scope.cancel()
    }

    @Test
    fun offlineFallbackUsesLatestCheckInEmotionWhenMessageIsGeneric() {
        val checkIns = listOf(
            CheckIn(id = "c1", userId = "u1", emotion = Emotion.ANXIOUS, intensity = 3, note = null, createdAt = 100L),
            CheckIn(id = "c2", userId = "u1", emotion = Emotion.STRESSED, intensity = 4, note = null, createdAt = 200L),
        )
        val (viewModel, scope) = viewModelWithLibrary(OfflineTransport(), checkIns = checkIns)

        viewModel.send("hello")

        val reply = viewModel.uiState.value.lines.last()
        assertEquals("stress", reply.exercise?.topic)
        scope.cancel()
    }

    @Test
    fun offlineFallbackServesIndonesianTranslationWhenAvailableAndFallsBackToEnglish() {
        val (viewModel, scope) = viewModelWithLibrary(OfflineTransport(), language = "id")

        viewModel.send("Aku begadang dan capek")
        assertEquals("Tidur", viewModel.uiState.value.lines.last().exercise?.title)
        assertEquals("id", viewModel.uiState.value.lines.last().exercise?.language)

        viewModel.send("My thoughts are stuck in a loop")
        assertEquals("Rumination", viewModel.uiState.value.lines.last().exercise?.title)
        assertEquals("en", viewModel.uiState.value.lines.last().exercise?.language)
        scope.cancel()
    }

    private fun viewModelWithLibrary(
        transport: GenerationTransport,
        checkIns: List<CheckIn> = emptyList(),
        language: String = "en",
    ): Pair<CompanionViewModel, CoroutineScope> {
        val assetsDir = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
        val library: OfflineExerciseLibrary = AssetOfflineExerciseLibrary { path ->
            val file = File(assetsDir, path)
            if (file.isFile) file.readText(Charsets.UTF_8) else null
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = CompanionViewModel(
            generationClient = GenerationClient("http://localhost:8787", transport),
            safetyRepository = RecordingSafetyRepository(),
            sessionStore = FixedSessionStore(SessionInfo(userId = "u1", email = "guest@local")),
            checkInRepository = FixedCheckInRepository(checkIns),
            preferencesStore = FixedPreferencesStore(language),
            exerciseLibrary = library,
            deviceLanguage = { language },
            now = { 1_700_000_000_000L },
            newId = { "event-1" },
            externalScope = scope,
        )
        return viewModel to scope
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
        memoryContextProvider: MemoryContextProvider? = null,
        summaryScheduler: SessionSummaryScheduler? = null,
    ): Triple<CompanionViewModel, RecordingSafetyRepository, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val safety = RecordingSafetyRepository()
        val viewModel = CompanionViewModel(
            generationClient = GenerationClient(baseUrl, transport),
            safetyRepository = safety,
            sessionStore = FixedSessionStore(SessionInfo(userId = "u1", email = "guest@local")),
            memoryContextProvider = memoryContextProvider,
            summaryScheduler = summaryScheduler,
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

    private class FixedMemoryContextProvider(private val context: String?) : MemoryContextProvider {
        override suspend fun contextFor(userId: String): String? = context
    }

    private class RecordingCompanionSummaryScheduler : SessionSummaryScheduler {
        data class Call(val userId: String, val sessionId: String, val transcript: List<String>)

        val calls = mutableListOf<Call>()

        override fun enqueue(userId: String, sessionId: String, transcript: List<String>) {
            calls += Call(userId, sessionId, transcript)
        }
    }
    private class FixedCheckInRepository(private val items: List<CheckIn>) : CheckInRepository {
        override fun observeAll(userId: String): Flow<List<CheckIn>> = flowOf(items.filter { it.userId == userId })

        override fun observeRecent(userId: String, sinceEpochMillis: Long): Flow<List<CheckIn>> = flowOf(emptyList())

        override fun count(userId: String): Flow<Int> = flowOf(items.size)

        override suspend fun insert(checkIn: CheckIn) = Unit
    }

    private class FixedPreferencesStore(initialLanguage: String) : PreferencesStore {
        private val langState = MutableStateFlow(initialLanguage)
        override val reminderEnabled: Flow<Boolean> = flowOf(false)
        override val reminderHour: Flow<Int> = flowOf(20)
        override val language: Flow<String> = langState.asStateFlow()
        override val theme: Flow<String> = flowOf("light")
        override val memoryEnabled: Flow<Boolean> = flowOf(false)
        override val onboardingCompleted: Flow<Boolean> = flowOf(false)

        override fun memoryNote(userId: String): Flow<String> = flowOf("")

        override suspend fun setReminderEnabled(enabled: Boolean) = Unit

        override suspend fun setReminderHour(hour: Int) = Unit

        override suspend fun setLanguage(language: String) {
            langState.value = language
        }

        override suspend fun setTheme(theme: String) = Unit

        override suspend fun setMemoryEnabled(enabled: Boolean) = Unit

        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit

        override suspend fun setMemoryNote(userId: String, note: String) = Unit
    }
}
