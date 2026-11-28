package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.ConsentRecord
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.SessionMessage
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.model.TrustedContact
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.ConsentRepository
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryRepository
import com.mindurset.app.domain.repository.TrustedContactRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportLocalDataTest {
    private val fixedClockMillis = 1_764_316_800_000L // 2025-11-28T08:00:00Z

    @Test
    fun `populated export includes every section, parses as valid JSON, and omits safety trigger text`() = runTest {
        val harness = FakeExportRepositories(
            sessionInfo = SessionInfo("u1", "user@example.com"),
            profile = UserProfile(
                userId = "u1",
                displayName = "Alex \"Calm\"\nLine",
                joinedAt = fixedClockMillis - 86_400_000L,
                reminderHour = 20,
                reminderEnabled = true,
                language = "id",
            ),
            memoryNote = "Prefer short breathing steps",
            checkIns = listOf(
                CheckIn("ci-2", "u1", Emotion.CALM, 3, "Evening walk", 200L),
                CheckIn("ci-1", "u1", Emotion.ANXIOUS, 7, "Busy \"morning\"", 100L),
            ),
            sessions = listOf(
                ReframeSession("s-1", "u1", "ci-1", 3, "completed", "v1.0.0", "model-a", 300L, 400L),
            ),
            messages = mapOf(
                "s-1" to listOf(
                    SessionMessage("m-2", "s-1", "evidence", "I passed before", 1, 320L),
                    SessionMessage("m-1", "s-1", "thought", "I will fail", 0, 310L),
                ),
            ),
            journals = listOf(
                JournalEntry("j-1", "u1", "Hard day", "One step at a time", "Warm tea", 500L, 550L),
            ),
            actions = listOf(
                MicroAction("a-1", "u1", "s-1", "Stretch", 3, true, null, 600L, 650L),
            ),
            summaries = listOf(
                SessionSummary("sum-1", "s-1", "u1", "Focused on small steps", 999_000L, 700L),
            ),
            contacts = listOf(
                TrustedContact("tc-1", "u1", "Sam", "+628123456789", "Sister", 800L),
            ),
            safetyEvents = listOf(
                SafetyEvent("se-2", "u1", RiskLevel.L3, "asq_positive", null, 950L, 999_950L),
                SafetyEvent("se-1", "u1", RiskLevel.L2, "checkin_note", "id-hopeless-01", 900L, 999_900L),
            ),
            consents = listOf(
                ConsentRecord("c-1", "u1", "local_processing", "1.0", 50L, null),
            ),
        )

        val exporter = harness.exporter(appVersionName = "0.1.0", clock = { fixedClockMillis })
        val json = exporter.exportJson()
        val parsed = JSONObject(json)

        assertEquals("mindurset-export-v1", parsed.getString("format"))
        assertEquals("2025-11-28T08:00:00Z", parsed.getString("exportedAt"))
        assertEquals("0.1.0", parsed.getJSONObject("app").getString("versionName"))

        val profile = parsed.getJSONObject("profile")
        assertEquals("Alex \"Calm\"\nLine", profile.getString("displayName"))
        assertEquals("user@example.com", profile.getString("email"))
        assertEquals("id", profile.getString("language"))
        assertEquals("Prefer short breathing steps", profile.getString("memoryNote"))

        val checkIns = parsed.getJSONArray("checkIns")
        assertEquals(2, checkIns.length())
        assertEquals("ci-1", checkIns.getJSONObject(0).getString("id"))
        assertEquals("ci-2", checkIns.getJSONObject(1).getString("id"))

        val sessions = parsed.getJSONArray("reframeSessions")
        assertEquals(1, sessions.length())
        val messages = sessions.getJSONObject(0).getJSONArray("messages")
        assertEquals(
            listOf("m-1", "m-2"),
            listOf(messages.getJSONObject(0).getString("id"), messages.getJSONObject(1).getString("id")),
        )

        assertEquals(1, parsed.getJSONArray("journalEntries").length())
        assertEquals(1, parsed.getJSONArray("microActions").length())
        assertEquals(1, parsed.getJSONArray("sessionSummaries").length())
        assertEquals(1, parsed.getJSONArray("trustedContacts").length())
        assertEquals(1, parsed.getJSONArray("consentRecords").length())

        val events = parsed.getJSONArray("safetyEvents")
        assertEquals(2, events.length())
        val firstEvent = events.getJSONObject(0)
        assertEquals("L2", firstEvent.getString("level"))
        assertEquals("checkin_note", firstEvent.getString("trigger"))
        assertEquals("id-hopeless-01", firstEvent.getString("patternId"))
        assertEquals("1970-01-01T00:00:00.900Z", firstEvent.getString("timestamp"))
        assertEquals(setOf("level", "trigger", "patternId", "timestamp"), firstEvent.keys().asSequence().toSet())
        assertFalse(firstEvent.has("text"))
        assertFalse(firstEvent.has("note"))
        assertFalse(firstEvent.has("content"))
    }

    @Test
    fun `two exports of unchanged data are byte-identical regardless of repository order`() = runTest {
        val checkInsA = listOf(
            CheckIn("b", "u1", Emotion.LOW, 6, null, 1_000L),
            CheckIn("a", "u1", Emotion.HAPPY, 2, "fine", 1_000L),
            CheckIn("c", "u1", Emotion.CALM, 4, null, 500L),
        )
        val checkInsB = checkInsA.reversed()

        val json1 = FakeExportRepositories(
            sessionInfo = SessionInfo("u1", "guest@local"),
            checkIns = checkInsA,
        ).exporter("0.1.0") { fixedClockMillis }.exportJson()

        val json2 = FakeExportRepositories(
            sessionInfo = SessionInfo("u1", "guest@local"),
            checkIns = checkInsB,
        ).exporter("0.1.0") { fixedClockMillis }.exportJson()

        assertEquals(json1, json2)

        val ids = JSONObject(json1).getJSONArray("checkIns").let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
        }
        assertEquals(listOf("c", "a", "b"), ids)
    }

    @Test
    fun `empty data produces valid document with all section keys and empty arrays`() = runTest {
        val exporter = FakeExportRepositories(sessionInfo = null).exporter("0.1.0") { fixedClockMillis }
        val json = exporter.exportJson()
        val parsed = JSONObject(json)

        assertEquals("mindurset-export-v1", parsed.getString("format"))
        assertEquals("2025-11-28T08:00:00Z", parsed.getString("exportedAt"))
        assertTrue(parsed.isNull("profile"))

        val arraySections = listOf(
            "checkIns",
            "reframeSessions",
            "journalEntries",
            "microActions",
            "sessionSummaries",
            "trustedContacts",
            "safetyEvents",
            "consentRecords",
        )
        arraySections.forEach { key ->
            assertTrue("missing key $key", parsed.has(key))
            assertEquals("non-empty $key", 0, parsed.getJSONArray(key).length())
        }
    }
}

private class FakeExportRepositories(
    private val sessionInfo: SessionInfo?,
    private val profile: UserProfile? = null,
    private val memoryNote: String = "",
    private val checkIns: List<CheckIn> = emptyList(),
    private val sessions: List<ReframeSession> = emptyList(),
    private val messages: Map<String, List<SessionMessage>> = emptyMap(),
    private val journals: List<JournalEntry> = emptyList(),
    private val actions: List<MicroAction> = emptyList(),
    private val summaries: List<SessionSummary> = emptyList(),
    private val contacts: List<TrustedContact> = emptyList(),
    private val safetyEvents: List<SafetyEvent> = emptyList(),
    private val consents: List<ConsentRecord> = emptyList(),
) {
    fun exporter(appVersionName: String, clock: () -> Long) = ExportLocalData(
        sessionStore = object : SessionStore {
            private val state = MutableStateFlow(sessionInfo)
            override val session: StateFlow<SessionInfo?> = state.asStateFlow()
            override suspend fun current(): SessionInfo? = state.value
            override suspend fun signIn(session: SessionInfo) = Unit
            override suspend fun signOut() = Unit
        },
        profileRepository = object : ProfileRepository {
            override fun observe(userId: String): Flow<UserProfile?> = flowOf(profile)
            override suspend fun upsert(profile: UserProfile) = Unit
            override suspend fun ensureProfile(userId: String, email: String) = Unit
        },
        preferencesStore = object : PreferencesStore {
            override val reminderEnabled: Flow<Boolean> = flowOf(profile?.reminderEnabled ?: false)
            override val reminderHour: Flow<Int> = flowOf(profile?.reminderHour ?: 19)
            override val language: Flow<String> = flowOf(profile?.language ?: "en")
            override val theme: Flow<String> = flowOf("system")
            override val memoryEnabled: Flow<Boolean> = flowOf(true)
            override fun memoryNote(userId: String): Flow<String> = flowOf(memoryNote)
            override suspend fun setReminderEnabled(enabled: Boolean) = Unit
            override suspend fun setReminderHour(hour: Int) = Unit
            override suspend fun setLanguage(language: String) = Unit
            override suspend fun setTheme(theme: String) = Unit
            override suspend fun setMemoryEnabled(enabled: Boolean) = Unit
            override suspend fun setMemoryNote(userId: String, note: String) = Unit
        },
        checkInRepository = object : CheckInRepository {
            override fun observeAll(userId: String): Flow<List<CheckIn>> = flowOf(checkIns)
            override fun observeRecent(userId: String, sinceEpochMillis: Long): Flow<List<CheckIn>> = flowOf(checkIns)
            override fun count(userId: String): Flow<Int> = flowOf(checkIns.size)
            override suspend fun insert(checkIn: CheckIn) = Unit
        },
        sessionRepository = object : SessionRepository {
            override suspend fun start(userId: String, checkInId: String?, now: Long) = error("unused")
            override suspend fun findActive(userId: String): ReframeSession? = null
            override suspend fun findById(id: String): ReframeSession? = null
            override suspend fun all(userId: String): List<ReframeSession> = sessions
            override suspend fun save(session: ReframeSession) = Unit
            override suspend fun appendMessage(message: SessionMessage) = Unit
            override suspend fun messages(sessionId: String): List<SessionMessage> = messages[sessionId].orEmpty()
        },
        journalRepository = object : JournalRepository {
            override fun observeAll(userId: String): Flow<List<JournalEntry>> = flowOf(journals)
            override fun observeById(id: String): Flow<JournalEntry?> = flowOf(null)
            override fun count(userId: String): Flow<Int> = flowOf(journals.size)
            override suspend fun upsert(entry: JournalEntry) = Unit
            override suspend fun delete(id: String) = Unit
        },
        microActionRepository = object : MicroActionRepository {
            override fun observeOpen(userId: String): Flow<List<MicroAction>> = flowOf(actions)
            override fun observeAll(userId: String): Flow<List<MicroAction>> = flowOf(actions)
            override suspend fun add(action: MicroAction) = Unit
            override suspend fun markDone(id: String, at: Long) = Unit
            override suspend fun snooze(id: String, until: Long) = Unit
        },
        sessionSummaryRepository = object : SessionSummaryRepository {
            override fun observe(userId: String): Flow<List<SessionSummary>> = flowOf(summaries)
            override suspend fun latest(userId: String, now: Long): List<SessionSummary> = summaries
            override suspend fun saveAndTrim(summary: SessionSummary, now: Long) = Unit
            override suspend fun updateText(id: String, summary: String) = Unit
            override suspend fun delete(id: String) = Unit
            override suspend fun deleteAll(userId: String) = Unit
            override suspend fun purgeExpired(now: Long) = Unit
        },
        trustedContactRepository = object : TrustedContactRepository {
            override fun observe(userId: String): Flow<List<TrustedContact>> = flowOf(contacts)
            override suspend fun upsert(contact: TrustedContact) = Unit
            override suspend fun delete(id: String) = Unit
        },
        safetyRepository = object : SafetyRepository {
            override suspend fun record(event: SafetyEvent) = Unit
            override suspend fun countSince(userId: String, since: Long): Int = safetyEvents.size
            override suspend fun all(userId: String): List<SafetyEvent> = safetyEvents
        },
        consentRepository = object : ConsentRepository {
            override fun observeAll(userId: String): Flow<List<ConsentRecord>> = flowOf(consents)
            override suspend fun find(userId: String, kind: String): ConsentRecord? = consents.firstOrNull()
            override suspend fun grant(record: ConsentRecord) = Unit
            override suspend fun revoke(id: String, at: Long) = Unit
        },
        appVersionName = appVersionName,
        clock = clock,
    )
}
