package com.mindurset.app.ui.home.checkin

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.ConsentRecord
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.ConsentRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckInViewModelTest {
    private val saved = mutableListOf<CheckIn>()
    private val safety = RecordingSafetyRepository()
    private var onDoneCalls = 0

    @Test
    fun `a heavy negative check-in offers the reframe instead of leaving`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.ANXIOUS)
        viewModel.setIntensity(7)

        viewModel.save { onDoneCalls++ }

        assertTrue(viewModel.offerReframe.value)
        assertEquals(0, onDoneCalls)
        assertEquals(Emotion.ANXIOUS, saved.single().emotion)
        assertEquals(7, saved.single().intensity)
    }

    @Test
    fun `a check-in at the threshold does not offer the reframe`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.ANXIOUS)
        viewModel.setIntensity(5)

        viewModel.save { onDoneCalls++ }

        assertFalse(viewModel.offerReframe.value)
        assertEquals(1, onDoneCalls)
    }

    @Test
    fun `a positive emotion never offers the reframe, however intense`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.HAPPY)
        viewModel.setIntensity(9)

        viewModel.save { onDoneCalls++ }

        assertFalse(viewModel.offerReframe.value)
        assertEquals(1, onDoneCalls)
    }

    @Test
    fun `declining the offer leaves the screen and clears it`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.STRESSED)
        viewModel.setIntensity(8)
        viewModel.save { onDoneCalls++ }
        assertTrue(viewModel.offerReframe.value)

        viewModel.skipOffer { onDoneCalls++ }

        assertFalse(viewModel.offerReframe.value)
        assertEquals(1, onDoneCalls)
    }

    @Test
    fun `a check-in without an emotion saves nothing`() {
        val viewModel = viewModel()

        viewModel.save { onDoneCalls++ }

        assertTrue(saved.isEmpty())
        assertEquals(0, onDoneCalls)
        assertNull(viewModel.emotion.value)
    }

    @Test
    fun `an L2 note opens the screening step instead of the card`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.ANXIOUS)
        viewModel.setIntensity(8)
        viewModel.setNote("aku ingin mati")

        viewModel.save { onDoneCalls++ }

        assertTrue("the ASQ step should be open", viewModel.asq.value != null)
        assertNull("L2 does not jump to the card any more", viewModel.crisisLevel.value)
        assertTrue("nothing may be written before the answers", saved.isEmpty())
        assertEquals(0, onDoneCalls)
        val event = safety.events.single()
        assertEquals("checkin_note", event.trigger)
        assertEquals(RiskLevel.L2, event.level)
    }

    @Test
    fun `an L3 note goes straight to the card and is never stored`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.LOW)
        viewModel.setIntensity(6)
        viewModel.setNote("I want to kill myself tonight")

        viewModel.save { onDoneCalls++ }

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertNull(viewModel.asq.value)
        assertTrue("a halted note was written", saved.isEmpty())
        assertEquals(0, onDoneCalls)
    }

    @Test
    fun `a positive screen halts without saving`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.LOW)
        viewModel.setIntensity(4)
        viewModel.setNote("aku ingin mati")
        viewModel.save { onDoneCalls++ }

        viewModel.answerAsq(yes = true)
        repeat(3) { viewModel.answerAsq(yes = false) }
        val pending = requireNotNull(viewModel.asq.value)
        assertEquals("acuity comes last", AsqFlow.ID_ACUITY, AsqFlow.nextQuestion(pending)?.id)
        viewModel.answerAsq(yes = false)

        assertEquals(RiskLevel.L3, viewModel.crisisLevel.value)
        assertTrue("a positive screen must not store the note", saved.isEmpty())
        assertEquals(0, onDoneCalls)
        assertEquals("asq_positive", safety.events.last().trigger)
        assertEquals(RiskLevel.L3, safety.events.last().level)
    }

    @Test
    fun `four no answers save the check-in that was held back`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.LOW)
        viewModel.setIntensity(4)
        viewModel.setNote("aku ingin mati")
        viewModel.save { onDoneCalls++ }

        repeat(4) { viewModel.answerAsq(yes = false) }

        assertNull(viewModel.asq.value)
        assertNull(viewModel.crisisLevel.value)
        assertEquals(1, saved.size)
        assertEquals("aku ingin mati", saved.single().note)
        assertEquals(1, onDoneCalls)
    }

    @Test
    fun `a benign note records no safety event`() {
        val viewModel = viewModel()
        viewModel.selectEmotion(Emotion.LOW)
        viewModel.setIntensity(4)
        viewModel.setNote("lelah tapi masih bisa bekerja")

        viewModel.save { onDoneCalls++ }

        assertNull(viewModel.crisisLevel.value)
        assertTrue(safety.events.isEmpty())
        assertEquals(1, onDoneCalls)
    }

    private fun viewModel(consent: Boolean = true): CheckInViewModel = CheckInViewModel(
        checkInRepository = RecordingCheckInRepository(saved),
        sessionStore = FixedSessionStore(SessionInfo(userId = "u1", email = "guest@local")),
        safetyRepository = safety,
        consentRepository = FakeConsentRepository(granted = consent),
        clock = { 1_700_000_000_000 },
        newId = { "checkin-1" },
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )

    @Test
    fun `without consent a check-in is refused and nothing is stored`() {
        val vm = viewModel(consent = false)
        vm.selectEmotion(Emotion.ANXIOUS)
        vm.setIntensity(7)

        var callbackRan = false
        vm.save { callbackRan = true }

        assertTrue(vm.needsConsent.value)
        assertFalse(callbackRan)
    }
}

private class RecordingCheckInRepository(private val rows: MutableList<CheckIn>) : CheckInRepository {
    override fun observeAll(userId: String): Flow<List<CheckIn>> = flowOf(rows)

    override fun observeRecent(userId: String, sinceEpochMillis: Long): Flow<List<CheckIn>> = flowOf(rows)

    override fun count(userId: String): Flow<Int> = flowOf(rows.size)

    override suspend fun insert(checkIn: CheckIn) {
        rows += checkIn
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

private class FakeConsentRepository(private val granted: Boolean) : ConsentRepository {
    override fun observeAll(userId: String): Flow<List<ConsentRecord>> = flowOf(emptyList())

    override suspend fun find(userId: String, kind: String): ConsentRecord? = if (!granted) {
        null
    } else {
        ConsentRecord(
            id = "c1",
            userId = userId,
            kind = kind,
            policyVersion = "1.0",
            grantedAt = 1_700_000_000_000,
            revokedAt = null,
        )
    }

    override suspend fun grant(record: ConsentRecord) = Unit

    override suspend fun revoke(id: String, at: Long) = Unit
}
