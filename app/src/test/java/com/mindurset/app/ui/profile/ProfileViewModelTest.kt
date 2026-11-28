package com.mindurset.app.ui.profile

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.UserAccount
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.domain.repository.AuthRepository
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Asia/Bangkok")
    private val today = LocalDate.of(2026, 11, 2)
    private val fixedClock = Clock.fixed(today.atTime(10, 0).atZone(zone).toInstant(), zone)

    @Test
    fun `observes prior month check-ins and updates breakdown when toggled to thirty days`() = runTest {
        val recentMillis = today.atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val lastMonthMillis = today.minusDays(14).atTime(15, 0).atZone(zone).toInstant().toEpochMilli()
        val checkIns =
            listOf(
                CheckIn("c1", "u1", Emotion.HAPPY, 7, null, recentMillis),
                CheckIn("c2", "u1", Emotion.CALM, 5, null, lastMonthMillis),
            )

        val vm =
            ProfileViewModel(
                profileRepository = FakeProfileRepo(),
                authRepository = FakeAuthRepo(),
                sessionStore = FakeSessionStore(SessionInfo("u1", "ana@example.com")),
                preferencesStore = FakePrefs(),
                checkInRepository = FakeCheckInRepo(checkIns),
                clock = fixedClock,
            )

        val collector = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect {}
        }

        val initial = vm.uiState.value
        assertFalse(initial.loading)
        assertEquals(7, initial.weeklyTrend.series.size)
        assertEquals(1, initial.weeklyTrend.totalCheckIns)
        assertEquals(7, initial.breakdownWindowDays)
        assertEquals(1, initial.breakdown.totalCheckIns)

        vm.selectBreakdownWindow(30)

        val updated = vm.uiState.value
        assertEquals(30, updated.breakdownWindowDays)
        assertEquals(2, updated.breakdown.totalCheckIns)
        assertEquals(1, updated.breakdown.counts[Emotion.HAPPY])
        assertEquals(1, updated.breakdown.counts[Emotion.CALM])

        collector.cancel()
    }
}

private class FakeSessionStore(initial: SessionInfo?) : SessionStore {
    private val state = MutableStateFlow(initial)
    override val session = state.asStateFlow()
    override suspend fun current(): SessionInfo? = state.value
    override suspend fun signIn(session: SessionInfo) {
        state.value = session
    }
    override suspend fun signOut() {
        state.value = null
    }
}

private class FakeProfileRepo : ProfileRepository {
    override fun observe(userId: String): Flow<UserProfile?> = flowOf(
        UserProfile(
            userId = userId,
            displayName = "Ana",
            joinedAt = 1_000L,
            reminderHour = 19,
            reminderEnabled = false,
            language = "en",
        ),
    )
    override suspend fun upsert(profile: UserProfile) = Unit
    override suspend fun ensureProfile(userId: String, email: String) = Unit
}

private class FakeAuthRepo : AuthRepository {
    override suspend fun findAccount(email: String): UserAccount? = null
    override suspend fun register(email: String, password: String): UserAccount = error("unused")
    override suspend fun signIn(email: String, password: String): UserAccount? = null
    override suspend fun deleteAccount(userId: String) = Unit
}

private class FakeCheckInRepo(items: List<CheckIn>) : CheckInRepository {
    private val flow = MutableStateFlow(items)
    override fun observeAll(userId: String): Flow<List<CheckIn>> = flow
    override fun observeRecent(userId: String, sinceEpochMillis: Long): Flow<List<CheckIn>> =
        flow.map { list -> list.filter { it.userId == userId && it.createdAt >= sinceEpochMillis } }
    override fun count(userId: String): Flow<Int> = flow.map { it.size }
    override suspend fun insert(checkIn: CheckIn) = Unit
}

private class FakePrefs : PreferencesStore {
    override val reminderEnabled: Flow<Boolean> = flowOf(false)
    override val reminderHour: Flow<Int> = flowOf(19)
    override val language: Flow<String> = flowOf("en")
    override val theme: Flow<String> = flowOf("system")
    override val memoryEnabled: Flow<Boolean> = flowOf(true)

    override val onboardingCompleted: Flow<Boolean> get() = flowOf(false)

    override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    override fun memoryNote(userId: String): Flow<String> = flowOf("")
    override suspend fun setReminderEnabled(enabled: Boolean) = Unit
    override suspend fun setReminderHour(hour: Int) = Unit
    override suspend fun setLanguage(language: String) = Unit
    override suspend fun setTheme(theme: String) = Unit
    override suspend fun setMemoryEnabled(enabled: Boolean) = Unit
    override suspend fun setMemoryNote(userId: String, note: String) = Unit
}
