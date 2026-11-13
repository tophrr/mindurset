package com.mindurset.app.ui.safety

import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.TrustedContact
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.TrustedContactRepository
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

class CrisisContactViewModelTest {
    private val stored = mutableListOf<TrustedContact>()

    @Test
    fun anEmptyContactListStartsInTheForm() {
        val viewModel = viewModel()

        assertNull(viewModel.state.value.contact)
        assertFalse("nothing to change yet", viewModel.state.value.editing)
        assertEquals("", viewModel.state.value.name)
    }

    @Test
    fun anExistingContactLoadsIntoTheSheet() {
        val viewModel = viewModel(listOf(contact(name = "Rani", phone = "081234567890")))

        assertEquals("Rani", viewModel.state.value.contact?.name)
        assertEquals("Rani", viewModel.state.value.name)
    }

    @Test
    fun savingStoresTheContactUnderTheSignedInPerson() {
        val viewModel = viewModel()
        viewModel.onNameChange("  Rani  ")
        viewModel.onPhoneChange(" 0812-3456-7890 ")

        viewModel.save()

        val saved = stored.single()
        assertEquals("Rani", saved.name)
        assertEquals("0812-3456-7890", saved.phone)
        assertEquals("u1", saved.userId)
        assertFalse(viewModel.state.value.invalid)
    }

    @Test
    fun aContactWithoutADialableNumberIsRefused() {
        listOf("Rani" to "", "Rani" to "12", "" to "081234567890").forEach { (name, phone) ->
            stored.clear()
            val viewModel = viewModel()
            viewModel.onNameChange(name)
            viewModel.onPhoneChange(phone)

            viewModel.save()

            assertTrue("$name/$phone should be refused", viewModel.state.value.invalid)
            assertTrue("$name/$phone was stored", stored.isEmpty())
        }
    }

    @Test
    fun editingPrefillsFromTheSavedContactAndCancelKeepsIt() {
        val viewModel = viewModel(listOf(contact(name = "Rani", phone = "081234567890")))

        viewModel.startEditing()
        assertTrue(viewModel.state.value.editing)
        assertEquals("Rani", viewModel.state.value.name)
        viewModel.onNameChange("Rani P")
        viewModel.cancelEditing()

        assertFalse(viewModel.state.value.editing)
        assertEquals("Rani", viewModel.state.value.contact?.name)
        assertEquals("081234567890", viewModel.state.value.phone)
    }

    private fun contact(name: String, phone: String) = TrustedContact(
        id = "contact-1",
        userId = "u1",
        name = name,
        phone = phone,
        relationship = null,
        createdAt = 1_700_000_000_000,
    )

    private fun viewModel(initial: List<TrustedContact> = emptyList()): CrisisContactViewModel = CrisisContactViewModel(
        repository = RecordingContactRepository(initial, stored),
        sessionStore = FixedSessionStore(SessionInfo(userId = "u1", email = "guest@local")),
        now = { 1_700_000_000_000 },
        newId = { "contact-new" },
        externalScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
    )
}

private class RecordingContactRepository(
    initial: List<TrustedContact>,
    private val saved: MutableList<TrustedContact>,
) : TrustedContactRepository {
    private val rows = MutableStateFlow(initial)

    override fun observe(userId: String): Flow<List<TrustedContact>> = rows

    override suspend fun upsert(contact: TrustedContact) {
        saved += contact
        rows.value = rows.value.filterNot { it.id == contact.id } + contact
    }

    override suspend fun delete(id: String) {
        rows.value = rows.value.filterNot { it.id == id }
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
