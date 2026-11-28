package com.mindurset.app.data.remote

import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SummaryWorkerTest {
    private val fixture =
        requireNotNull(javaClass.getResource("/fixtures/generation-ok.json"))
            .readText()

    @Test
    fun `summariseNow calls summarise endpoint and sets retentionUntil`() = runTest {
        val now = 1_700_000_000_000L
        val summaryReply = JSONObject(fixture)
            .put("message", "Worked through exam worry and picked a five-minute outline step.")
            .toString()
        val transport = RecordingTransport(summaryReply)
        val repo = InMemorySummaryRepository()
        val prefs = InMemoryPreferencesStore(memoryEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var counter = 0
        val worker = SummaryWorker(
            generationClient = GenerationClient("http://localhost:8787", transport),
            summaryRepository = repo,
            preferencesStore = prefs,
            scope = scope,
            now = { now },
            newId = { "sum-${++counter}" },
        )

        val saved = worker.summariseNow("u1", "sess-1", listOf("I feel behind", "Pick one small outline step"))

        assertNotNull(saved)
        assertEquals("/v1/summarise", transport.lastPath)
        val requestJson = JSONObject(requireNotNull(transport.lastBody))
        assertEquals(2, requestJson.getJSONArray("transcript").length())
        assertEquals(now + MemoryPolicy.RETENTION_MILLIS, saved!!.retentionUntil)
        assertEquals(1, repo.rows.value.size)
        scope.cancel()
    }

    @Test
    fun `saving a fourth summary deletes the oldest so at most three remain`() = runTest {
        var now = 1_700_000_000_000L
        val replies = (1..4).map { idx ->
            JSONObject(fixture).put("message", "Summary $idx").toString()
        }
        val transport = RecordingTransport(*replies.toTypedArray())
        val repo = InMemorySummaryRepository()
        val prefs = InMemoryPreferencesStore(memoryEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var counter = 0
        val worker = SummaryWorker(
            generationClient = GenerationClient("http://localhost:8787", transport),
            summaryRepository = repo,
            preferencesStore = prefs,
            scope = scope,
            now = { now },
            newId = { "sum-${++counter}" },
        )

        repeat(4) { idx ->
            now += 1_000L
            worker.summariseNow("u1", "sess-${idx + 1}", listOf("Line ${idx + 1}"))
        }

        val stored = repo.rows.value
        assertEquals(3, stored.size)
        assertEquals(listOf("Summary 4", "Summary 3", "Summary 2"), stored.map { it.summary })
        scope.cancel()
    }

    @Test
    fun `when memory is disabled summariseNow does not call worker or store anything`() = runTest {
        val transport = RecordingTransport(fixture)
        val repo = InMemorySummaryRepository()
        val prefs = InMemoryPreferencesStore(memoryEnabled = false)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val worker = SummaryWorker(
            generationClient = GenerationClient("http://localhost:8787", transport),
            summaryRepository = repo,
            preferencesStore = prefs,
            scope = scope,
        )

        val result = worker.summariseNow("u1", "sess-1", listOf("First line", "Second line"))

        assertNull(result)
        assertEquals(0, transport.calls)
        assertTrue(repo.rows.value.isEmpty())
        scope.cancel()
    }

    @Test
    fun `offline, ungrounded, and L3 replies are never persisted as summaries`() = runTest {
        val offlineFallback = JSONObject(fixture).put("model_version", OFFLINE_MODEL_VERSION).toString()
        val ungrounded = JSONObject(fixture).put("source_ids", JSONArray()).toString()
        val crisisReply = JSONObject(fixture)
            .put("risk_level", JSONObject().put("level", "L3").put("confidence", 0.95))
            .toString()
        val repo = InMemorySummaryRepository()
        val prefs = InMemoryPreferencesStore(memoryEnabled = true)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        listOf(
            RecordingTransport(offlineFallback),
            RecordingTransport(ungrounded),
            RecordingTransport(crisisReply),
            FailingTransport(),
        ).forEach { transport ->
            val worker = SummaryWorker(
                generationClient = GenerationClient("http://localhost:8787", transport),
                summaryRepository = repo,
                preferencesStore = prefs,
                scope = scope,
            )
            assertNull(worker.summariseNow("u1", "sess-x", listOf("line")))
        }

        assertTrue(repo.rows.value.isEmpty())
        scope.cancel()
    }

    @Test
    fun `GenerationClient includes context only when non-blank`() = runTest {
        val transport = RecordingTransport(fixture, fixture, fixture, fixture)
        val client = GenerationClient("http://localhost:8787", transport)

        client.reply("Hello", "listen", "State: last_check_in=calm(3/10, 1h ago)")
        assertEquals(
            "State: last_check_in=calm(3/10, 1h ago)",
            JSONObject(requireNotNull(transport.lastBody)).getString("context"),
        )

        client.reply("Hello", "listen", "   ")
        assertFalse(JSONObject(requireNotNull(transport.lastBody)).has("context"))

        client.reframe("I will fail", "User note: Keep steps small")
        assertEquals("/v1/reframe", transport.lastPath)
        assertEquals(
            "User note: Keep steps small",
            JSONObject(requireNotNull(transport.lastBody)).getString("context"),
        )

        client.reframe("I will fail", null)
        assertFalse(JSONObject(requireNotNull(transport.lastBody)).has("context"))
    }

    @Test
    fun `cancelPending or cleared session prevents summary write after wipe`() = runTest {
        val summaryReply = JSONObject(fixture).put("message", "Should not be stored").toString()
        val repo = InMemorySummaryRepository()
        val prefs = InMemoryPreferencesStore(memoryEnabled = true)
        val sessionState = MutableStateFlow<SessionInfo?>(SessionInfo("u1", "guest@local"))
        val sessionStore = object : SessionStore {
            override val session = sessionState
            override suspend fun current() = sessionState.value
            override suspend fun signIn(session: SessionInfo) {
                sessionState.value = session
            }
            override suspend fun signOut() {
                sessionState.value = null
            }
        }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        lateinit var worker: SummaryWorker
        val cancellingTransport = object : GenerationTransport {
            override suspend fun post(path: String, body: String): String {
                worker.cancelPending()
                sessionStore.signOut()
                return summaryReply
            }
        }
        worker = SummaryWorker(
            generationClient = GenerationClient("http://localhost:8787", cancellingTransport),
            summaryRepository = repo,
            preferencesStore = prefs,
            scope = scope,
            sessionStore = sessionStore,
        )

        assertNull(worker.summariseNow("u1", "sess-1", listOf("First line")))
        assertNull(worker.summariseNow("u1", "sess-2", listOf("Post-wipe line")))
        assertTrue(repo.rows.value.isEmpty())
        scope.cancel()
    }
}

private class RecordingTransport(vararg responses: String) : GenerationTransport {
    private val queue = responses.toMutableList()
    var calls = 0
        private set
    var lastPath: String? = null
        private set
    var lastBody: String? = null
        private set

    override suspend fun post(path: String, body: String): String {
        calls += 1
        lastPath = path
        lastBody = body
        return queue.removeAt(0)
    }
}

private class FailingTransport : GenerationTransport {
    override suspend fun post(path: String, body: String): String = throw IOException("offline")
}

private class InMemorySummaryRepository : SessionSummaryRepository {
    val rows = MutableStateFlow<List<SessionSummary>>(emptyList())

    override fun observe(userId: String): Flow<List<SessionSummary>> =
        rows.map { list -> list.filter { it.userId == userId }.sortedByDescending { it.createdAt } }

    override suspend fun latest(userId: String, now: Long): List<SessionSummary> {
        purgeExpired(now)
        return rows.value
            .filter { it.userId == userId && it.retentionUntil > now }
            .sortedByDescending { it.createdAt }
            .take(MemoryPolicy.MAX_SUMMARIES)
    }

    override suspend fun saveAndTrim(summary: SessionSummary, now: Long) {
        purgeExpired(now)
        rows.update { current ->
            val others = current.filterNot { it.userId == summary.userId }
            val userRows = (current.filter { it.userId == summary.userId && it.id != summary.id } + summary)
                .sortedWith(compareByDescending<SessionSummary> { it.createdAt }.thenByDescending { it.id })
                .take(MemoryPolicy.MAX_SUMMARIES)
            others + userRows
        }
    }

    override suspend fun updateText(id: String, summary: String) {
        rows.update { list -> list.map { if (it.id == id) it.copy(summary = summary) else it } }
    }

    override suspend fun delete(id: String) {
        rows.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun deleteAll(userId: String) {
        rows.update { list -> list.filterNot { it.userId == userId } }
    }

    override suspend fun purgeExpired(now: Long) {
        rows.update { list -> list.filter { it.retentionUntil > now } }
    }
}

private class InMemoryPreferencesStore(memoryEnabled: Boolean) : PreferencesStore {
    private val _memoryEnabled = MutableStateFlow(memoryEnabled)
    private val notes = MutableStateFlow<Map<String, String>>(emptyMap())

    override val reminderEnabled: Flow<Boolean> = MutableStateFlow(false)
    override val reminderHour: Flow<Int> = MutableStateFlow(19)
    override val language: Flow<String> = MutableStateFlow("en")
    override val theme: Flow<String> = MutableStateFlow("system")
    override val memoryEnabled: Flow<Boolean> = _memoryEnabled

    override fun memoryNote(userId: String): Flow<String> = notes.map { it[userId].orEmpty() }

    override suspend fun setReminderEnabled(enabled: Boolean) = Unit

    override suspend fun setReminderHour(hour: Int) = Unit

    override suspend fun setLanguage(language: String) = Unit

    override suspend fun setTheme(theme: String) = Unit

    override suspend fun setMemoryEnabled(enabled: Boolean) {
        _memoryEnabled.value = enabled
    }

    override suspend fun setMemoryNote(userId: String, note: String) {
        notes.update { current -> if (note.isBlank()) current - userId else current + (userId to note) }
    }
}
