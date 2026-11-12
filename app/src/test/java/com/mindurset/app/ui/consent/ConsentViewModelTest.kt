package com.mindurset.app.ui.consent

import com.mindurset.app.domain.model.ConsentRecord
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.ConsentRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentViewModelTest {
    private val repository = FakeConsentRepository()

    @Test
    fun `an empty table is not a grant`() {
        val viewModel = viewModel()

        assertEquals(false, viewModel.granted.value)
    }

    @Test
    fun `granting stores one row and reports it`() {
        val viewModel = viewModel()
        var done = false

        viewModel.grant { done = true }

        val row = repository.row
        assertNotNull(row)
        assertEquals("u1", row?.userId)
        assertEquals("local_processing", row?.kind)
        assertNull(row?.revokedAt)
        assertEquals(true, viewModel.granted.value)
        assertTrue(done)
    }

    @Test
    fun `granting twice keeps one row`() {
        val viewModel = viewModel()
        viewModel.grant {}
        viewModel.grant {}

        assertEquals(1, repository.rows.size)
    }

    @Test
    fun `withdrawing stamps the row and blocks again`() {
        val viewModel = viewModel()
        viewModel.grant {}
        var done = false

        viewModel.revoke { done = true }

        assertNotNull(repository.row?.revokedAt)
        assertEquals(false, viewModel.granted.value)
        assertTrue(done)
        assertEquals(1, repository.rows.size)
    }

    @Test
    fun `an old policy version asks again`() {
        repository.row =
            ConsentRecord(
                id = "c1",
                userId = "u1",
                kind = "local_processing",
                policyVersion = "0.9",
                grantedAt = 1_700_000_000_000,
                revokedAt = null,
            )

        val viewModel = viewModel()

        assertFalse(viewModel.granted.value ?: true)
    }

    private fun viewModel(): ConsentViewModel = ConsentViewModel(
        consentRepository = repository,
        sessionStore = FixedSessionStore(SessionInfo("u1", "guest@local")),
        clock = { 1_700_000_002_000 },
        newId = { "consent-1" },
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )
}

private class FakeConsentRepository : ConsentRepository {
    val rows = mutableListOf<ConsentRecord>()
    var row: ConsentRecord?
        get() = rows.firstOrNull()
        set(value) {
            rows.clear()
            if (value != null) rows += value
        }

    override fun observeAll(userId: String): Flow<List<ConsentRecord>> = flowOf(rows.toList())

    override suspend fun find(userId: String, kind: String): ConsentRecord? =
        rows.firstOrNull { it.userId == userId && it.kind == kind }

    override suspend fun grant(record: ConsentRecord) {
        rows.removeAll { it.userId == record.userId && it.kind == record.kind }
        rows += record
    }

    override suspend fun revoke(id: String, at: Long) {
        val index = rows.indexOfFirst { it.id == id }
        if (index >= 0) rows[index] = rows[index].copy(revokedAt = at)
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
