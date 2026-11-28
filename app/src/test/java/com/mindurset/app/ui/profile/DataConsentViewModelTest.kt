package com.mindurset.app.ui.profile

import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.ConsentRepository
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.LocalDataWiper
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryRepository
import com.mindurset.app.domain.repository.TrustedContactRepository
import com.mindurset.app.domain.repository.WipeResult
import com.mindurset.app.domain.usecase.ExportLocalData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DataConsentViewModelTest {
    @Test
    fun `export writes json through writer and updates status`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = DataConsentViewModel(
            exporter = emptyExporter(),
            dataWiper = FakeWiper(),
            ioDispatcher = Dispatchers.Unconfined,
            externalScope = scope,
        )
        var captured = ""
        vm.export { json ->
            captured = json
            true
        }

        assertFalse(vm.uiState.value.exporting)
        assertEquals(ExportStatus.Saved, vm.uiState.value.exportStatus)
        assertTrue(captured.contains("\"format\": \"mindurset-export-v1\""))

        vm.export { false }
        assertEquals(ExportStatus.Failed, vm.uiState.value.exportStatus)
        scope.cancel()
    }

    @Test
    fun `revokeAndWipe wipes all data and invokes onWithdrawn`() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val wiper = FakeWiper()
        val vm = DataConsentViewModel(
            exporter = emptyExporter(),
            dataWiper = wiper,
            ioDispatcher = Dispatchers.Unconfined,
            externalScope = scope,
        )
        var withdrawn = false

        vm.revokeAndWipe { withdrawn = true }

        assertEquals(1, wiper.calls)
        assertFalse(vm.uiState.value.wiping)
        assertTrue(withdrawn)
        scope.cancel()
    }

    private fun emptyExporter() = ExportLocalData(
        sessionStore = object : SessionStore {
            private val state = MutableStateFlow<SessionInfo?>(null)
            override val session: StateFlow<SessionInfo?> = state.asStateFlow()
            override suspend fun current(): SessionInfo? = null
            override suspend fun signIn(session: SessionInfo) = Unit
            override suspend fun signOut() = Unit
        },
        profileRepository = object : ProfileRepository {
            override fun observe(userId: String) = flowOf(null)
            override suspend fun upsert(profile: com.mindurset.app.domain.model.UserProfile) = Unit
            override suspend fun ensureProfile(userId: String, email: String) = Unit
        },
        preferencesStore = object : PreferencesStore {
            override val reminderEnabled: Flow<Boolean> = flowOf(false)
            override val reminderHour: Flow<Int> = flowOf(19)
            override val language: Flow<String> = flowOf("en")
            override val theme: Flow<String> = flowOf("system")
            override val memoryEnabled: Flow<Boolean> = flowOf(true)
            override fun memoryNote(userId: String): Flow<String> = flowOf("")
            override suspend fun setReminderEnabled(enabled: Boolean) = Unit
            override suspend fun setReminderHour(hour: Int) = Unit
            override suspend fun setLanguage(language: String) = Unit
            override suspend fun setTheme(theme: String) = Unit
            override suspend fun setMemoryEnabled(enabled: Boolean) = Unit
            override suspend fun setMemoryNote(userId: String, note: String) = Unit
        },
        checkInRepository = object : CheckInRepository {
            override fun observeAll(userId: String) = flowOf(emptyList<com.mindurset.app.domain.model.CheckIn>())
            override fun observeRecent(userId: String, sinceEpochMillis: Long) =
                flowOf(emptyList<com.mindurset.app.domain.model.CheckIn>())
            override fun count(userId: String) = flowOf(0)
            override suspend fun insert(checkIn: com.mindurset.app.domain.model.CheckIn) = Unit
        },
        sessionRepository = object : SessionRepository {
            override suspend fun start(userId: String, checkInId: String?, now: Long) = error("unused")
            override suspend fun findActive(userId: String) = null
            override suspend fun findById(id: String) = null
            override suspend fun save(session: com.mindurset.app.domain.model.ReframeSession) = Unit
            override suspend fun appendMessage(message: com.mindurset.app.domain.model.SessionMessage) = Unit
            override suspend fun messages(sessionId: String) =
                emptyList<com.mindurset.app.domain.model.SessionMessage>()
        },
        journalRepository = object : JournalRepository {
            override fun observeAll(userId: String) = flowOf(emptyList<com.mindurset.app.domain.model.JournalEntry>())
            override fun observeById(id: String) = flowOf(null)
            override fun count(userId: String) = flowOf(0)
            override suspend fun upsert(entry: com.mindurset.app.domain.model.JournalEntry) = Unit
            override suspend fun delete(id: String) = Unit
        },
        microActionRepository = object : MicroActionRepository {
            override fun observeOpen(userId: String) = flowOf(emptyList<com.mindurset.app.domain.model.MicroAction>())
            override fun observeAll(userId: String) = flowOf(emptyList<com.mindurset.app.domain.model.MicroAction>())
            override suspend fun add(action: com.mindurset.app.domain.model.MicroAction) = Unit
            override suspend fun markDone(id: String, at: Long) = Unit
            override suspend fun snooze(id: String, until: Long) = Unit
        },
        sessionSummaryRepository = object : SessionSummaryRepository {
            override fun observe(userId: String) = flowOf(emptyList<com.mindurset.app.domain.model.SessionSummary>())
            override suspend fun latest(userId: String, now: Long) =
                emptyList<com.mindurset.app.domain.model.SessionSummary>()
            override suspend fun saveAndTrim(summary: com.mindurset.app.domain.model.SessionSummary, now: Long) = Unit
            override suspend fun updateText(id: String, summary: String) = Unit
            override suspend fun delete(id: String) = Unit
            override suspend fun deleteAll(userId: String) = Unit
            override suspend fun purgeExpired(now: Long) = Unit
        },
        trustedContactRepository = object : TrustedContactRepository {
            override fun observe(userId: String) = flowOf(emptyList<com.mindurset.app.domain.model.TrustedContact>())
            override suspend fun upsert(contact: com.mindurset.app.domain.model.TrustedContact) = Unit
            override suspend fun delete(id: String) = Unit
        },
        safetyRepository = object : SafetyRepository {
            override suspend fun record(event: com.mindurset.app.domain.model.SafetyEvent) = Unit
            override suspend fun countSince(userId: String, since: Long) = 0
        },
        consentRepository = object : ConsentRepository {
            override fun observeAll(userId: String) = flowOf(emptyList<com.mindurset.app.domain.model.ConsentRecord>())
            override suspend fun find(userId: String, kind: String) = null
            override suspend fun grant(record: com.mindurset.app.domain.model.ConsentRecord) = Unit
            override suspend fun revoke(id: String, at: Long) = Unit
        },
        appVersionName = "0.1.0",
        clock = { 1_764_316_800_000L },
    )

    private class FakeWiper : LocalDataWiper {
        var calls = 0
        override suspend fun wipeAll(): WipeResult {
            calls += 1
            return WipeResult(completed = true, tableCount = 11)
        }
    }
}
