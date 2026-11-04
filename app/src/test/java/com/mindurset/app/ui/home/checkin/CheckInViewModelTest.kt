package com.mindurset.app.ui.home.checkin

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.SessionStore
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

    private fun viewModel(): CheckInViewModel = CheckInViewModel(
        checkInRepository = RecordingCheckInRepository(saved),
        sessionStore = FixedSessionStore(SessionInfo(userId = "u1", email = "guest@local")),
        clock = { 1_700_000_000_000 },
        newId = { "checkin-1" },
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )
}

private class RecordingCheckInRepository(private val rows: MutableList<CheckIn>) : CheckInRepository {
    override fun observeAll(userId: String): Flow<List<CheckIn>> = flowOf(rows)

    override fun observeRecent(userId: String, sinceEpochMillis: Long): Flow<List<CheckIn>> = flowOf(rows)

    override fun count(userId: String): Flow<Int> = flowOf(rows.size)

    override suspend fun insert(checkIn: CheckIn) {
        rows += checkIn
    }
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
