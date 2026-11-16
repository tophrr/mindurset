package com.mindurset.app.ui.profile

import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ProfileEditViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val session = SessionInfo(userId = "u1", email = "christopher@student.uph.edu")

    @Test
    fun `the stored display name is loaded into the form`() = runTest {
        val profile = profile(displayName = "Christopher")
        val viewModel = ProfileEditViewModel(FakeProfileRepository(profile), FakeSessionStore(session))
        runCurrent()

        assertFalse(viewModel.uiState.value.loading)
        assertEquals("Christopher", viewModel.uiState.value.displayName)
    }

    @Test
    fun `a blank name is refused with the required error and nothing is written`() = runTest {
        val repository = FakeProfileRepository(profile(displayName = "Christopher"))
        val viewModel = ProfileEditViewModel(repository, FakeSessionStore(session))

        viewModel.onDisplayNameChange("   ")
        viewModel.save()
        runCurrent()

        assertEquals(NameError.REQUIRED, viewModel.uiState.value.nameError)
        assertTrue(repository.upserts.isEmpty())
    }

    @Test
    fun `a name past the length limit is refused instead of being saved`() = runTest {
        val repository = FakeProfileRepository(profile(displayName = "Christopher"))
        val viewModel = ProfileEditViewModel(repository, FakeSessionStore(session))

        viewModel.onDisplayNameChange("c".repeat(ProfileEditViewModel.MAX_NAME_LENGTH + 1))
        viewModel.save()
        runCurrent()

        assertEquals(NameError.LENGTH, viewModel.uiState.value.nameError)
        assertTrue(repository.upserts.isEmpty())
    }

    @Test
    fun `saving trims the name and keeps the reminder the person already chose`() = runTest {
        val existing =
            profile(
                displayName = "Christopher",
                joinedAt = 777L,
                reminderHour = 20,
                reminderEnabled = true,
                language = "in",
            )
        val repository = FakeProfileRepository(existing)
        val viewModel = ProfileEditViewModel(repository, FakeSessionStore(session))

        viewModel.onDisplayNameChange("  Chris G  ")
        viewModel.save()
        runCurrent()

        val saved = repository.upserts.single()
        assertEquals("Chris G", saved.displayName)
        assertEquals(777L, saved.joinedAt)
        assertEquals(20, saved.reminderHour)
        assertTrue(saved.reminderEnabled)
        assertEquals("in", saved.language)
        assertTrue(viewModel.uiState.value.saved)
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `without a session the form stays empty and saves nothing`() = runTest {
        val repository = FakeProfileRepository(profile(displayName = "Christopher"))
        val viewModel = ProfileEditViewModel(repository, FakeSessionStore(null))

        viewModel.onDisplayNameChange("Chris G")
        viewModel.save()
        runCurrent()

        assertTrue(repository.upserts.isEmpty())
        assertFalse(viewModel.uiState.value.saving)
    }

    private fun profile(
        displayName: String,
        joinedAt: Long = 1_000L,
        reminderHour: Int = 19,
        reminderEnabled: Boolean = false,
        language: String = "en",
    ) = UserProfile(
        userId = session.userId,
        displayName = displayName,
        joinedAt = joinedAt,
        reminderHour = reminderHour,
        reminderEnabled = reminderEnabled,
        language = language,
    )
}

private class FakeProfileRepository(initial: UserProfile?) : ProfileRepository {
    private val state = MutableStateFlow(initial)
    val upserts = mutableListOf<UserProfile>()

    override fun observe(userId: String): Flow<UserProfile?> = state

    override suspend fun upsert(profile: UserProfile) {
        upserts += profile
        state.value = profile
    }

    override suspend fun ensureProfile(userId: String, email: String) {
        if (state.value == null) {
            state.value =
                UserProfile(
                    userId = userId,
                    displayName = email.substringBefore('@'),
                    joinedAt = 1_000L,
                    reminderHour = 19,
                    reminderEnabled = false,
                    language = "en",
                )
        }
    }
}

private class FakeSessionStore(initial: SessionInfo?) : SessionStore {
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
