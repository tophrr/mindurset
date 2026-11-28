package com.mindurset.app.ui.onboarding

import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.OnboardingBaseline
import com.mindurset.app.domain.model.OnboardingFocus
import com.mindurset.app.domain.model.OnboardingSummaryKey
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.SleepQuality
import com.mindurset.app.domain.model.SupportLevel
import com.mindurset.app.domain.repository.OnboardingBaselineRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.usecase.EntryDestination
import com.mindurset.app.domain.usecase.ResolveEntryDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class OnboardingViewModelTest {
    @Test
    fun `skip sets onboardingCompleted flag, writes no baseline row, and finishes`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repo = FakeOnboardingBaselineRepository()
        val prefs = FakeOnboardingPreferencesStore()
        val vm = viewModel(repo, prefs, scope)
        var finished = false

        vm.skip { finished = true }

        assertTrue(finished)
        assertTrue(prefs.onboardingCompleted.first())
        assertTrue(repo.rows.isEmpty())
        scope.cancel()
    }

    @Test
    fun `completing baseline saves single row, sets flag, and advances to Done`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repo = FakeOnboardingBaselineRepository()
        val prefs = FakeOnboardingPreferencesStore()
        val vm = viewModel(repo, prefs, scope)

        vm.continueFromIntro()
        assertEquals(OnboardingStep.BASELINE, vm.uiState.value.step)
        assertFalse(vm.uiState.value.canSubmitBaseline)

        vm.selectMood(Emotion.CALM)
        vm.selectSleep(SleepQuality.RESTFUL)
        vm.selectSupport(SupportLevel.STRONG)
        assertFalse(vm.uiState.value.canSubmitBaseline)

        vm.selectFocus(OnboardingFocus.DAILY_RHYTHM)
        assertTrue(vm.uiState.value.canSubmitBaseline)

        vm.saveBaseline()

        assertEquals(OnboardingStep.DONE, vm.uiState.value.step)
        assertTrue(prefs.onboardingCompleted.first())
        assertEquals(1, repo.rows.size)
        val saved = repo.rows.single()
        assertEquals("u1", saved.userId)
        assertEquals(Emotion.CALM, saved.recentMood)
        assertEquals(SleepQuality.RESTFUL, saved.sleepQuality)
        assertEquals(SupportLevel.STRONG, saved.supportLevel)
        assertEquals(OnboardingFocus.DAILY_RHYTHM, saved.primaryFocus)
        assertEquals(OnboardingSummaryKey.CALM, vm.uiState.value.summaryKey)
        scope.cancel()
    }

    @Test
    fun `saving incomplete baseline does nothing`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repo = FakeOnboardingBaselineRepository()
        val prefs = FakeOnboardingPreferencesStore()
        val vm = viewModel(repo, prefs, scope)

        vm.continueFromIntro()
        vm.selectMood(Emotion.LOW)
        vm.saveBaseline()

        assertEquals(OnboardingStep.BASELINE, vm.uiState.value.step)
        assertFalse(prefs.onboardingCompleted.first())
        assertTrue(repo.rows.isEmpty())
        scope.cancel()
    }

    @Test
    fun `summary text follows each chosen emotion`() {
        val resDir = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val enStrings = parseStrings(File(resDir, "values/strings_onboarding.xml"))
        val expectedKeys = mapOf(
            Emotion.HAPPY to (OnboardingSummaryKey.HAPPY to "onboarding_done_summary_happy"),
            Emotion.CALM to (OnboardingSummaryKey.CALM to "onboarding_done_summary_calm"),
            Emotion.LOW to (OnboardingSummaryKey.LOW to "onboarding_done_summary_low"),
            Emotion.ANXIOUS to (OnboardingSummaryKey.ANXIOUS to "onboarding_done_summary_anxious"),
            Emotion.STRESSED to (OnboardingSummaryKey.STRESSED to "onboarding_done_summary_stressed"),
        )

        val seenSummaries = mutableSetOf<String>()
        Emotion.entries.forEach { emotion ->
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
            val repo = FakeOnboardingBaselineRepository()
            val prefs = FakeOnboardingPreferencesStore()
            val vm = viewModel(repo, prefs, scope)

            vm.continueFromIntro()
            vm.selectMood(emotion)
            vm.selectSleep(SleepQuality.MIXED)
            vm.selectSupport(SupportLevel.SOME)
            vm.selectFocus(OnboardingFocus.CALMING_WORRY)
            vm.saveBaseline()

            val (expectedKey, xmlKey) = expectedKeys.getValue(emotion)
            assertEquals(expectedKey, vm.uiState.value.summaryKey)
            val text = enStrings[xmlKey]
            assertNotNull("missing summary string for $emotion", text)
            assertTrue(seenSummaries.add(text!!))
            scope.cancel()
        }
        assertEquals(Emotion.entries.size, seenSummaries.size)
    }

    @Test
    fun `returning user with onboardingCompleted flag set never sees onboarding`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val repo = FakeOnboardingBaselineRepository()
        val prefs = FakeOnboardingPreferencesStore()
        val vm = viewModel(repo, prefs, scope)

        vm.skip {}
        val completed = prefs.onboardingCompleted.first()
        val destination = ResolveEntryDestination.resolve(
            hasSession = true,
            hasConsent = true,
            onboardingCompleted = completed,
        )

        assertTrue(completed)
        assertEquals(EntryDestination.DASHBOARD, destination)
        scope.cancel()
    }

    private fun viewModel(
        repo: OnboardingBaselineRepository,
        prefs: PreferencesStore,
        scope: CoroutineScope,
    ): OnboardingViewModel = OnboardingViewModel(
        baselineRepository = repo,
        preferencesStore = prefs,
        sessionStore = FixedSessionStore(SessionInfo("u1", "guest@mindurset.local")),
        clock = { 1_700_000_000_000L },
        newId = { "ob-1" },
        externalScope = scope,
    )

    private fun parseStrings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return buildMap {
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as Element
                put(element.getAttribute("name"), element.textContent)
            }
        }
    }
}

private class FakeOnboardingBaselineRepository : OnboardingBaselineRepository {
    val rows = mutableListOf<OnboardingBaseline>()

    override suspend fun findForUser(userId: String): OnboardingBaseline? = rows.firstOrNull { it.userId == userId }

    override fun observeForUser(userId: String): Flow<OnboardingBaseline?> =
        flowOf(rows.firstOrNull { it.userId == userId })

    override suspend fun save(baseline: OnboardingBaseline) {
        rows.removeAll { it.userId == baseline.userId }
        rows += baseline
    }
}

private class FakeOnboardingPreferencesStore : PreferencesStore {
    private val _onboardingCompleted = MutableStateFlow(false)

    override val reminderEnabled: Flow<Boolean> = flowOf(false)
    override val reminderHour: Flow<Int> = flowOf(19)
    override val language: Flow<String> = flowOf("en")
    override val theme: Flow<String> = flowOf("system")
    override val memoryEnabled: Flow<Boolean> = flowOf(true)
    override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted.asStateFlow()

    override fun memoryNote(userId: String): Flow<String> = flowOf("")

    override suspend fun setReminderEnabled(enabled: Boolean) = Unit

    override suspend fun setReminderHour(hour: Int) = Unit

    override suspend fun setLanguage(language: String) = Unit

    override suspend fun setTheme(theme: String) = Unit

    override suspend fun setMemoryEnabled(enabled: Boolean) = Unit

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }

    override suspend fun setMemoryNote(userId: String, note: String) = Unit
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
