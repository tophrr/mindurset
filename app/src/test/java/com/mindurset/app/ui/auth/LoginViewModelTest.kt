package com.mindurset.app.ui.auth

import com.mindurset.app.domain.auth.ValidateCredentials
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.model.UserAccount
import com.mindurset.app.domain.repository.AuthRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val account = UserAccount(id = "u1", email = "christopher@student.uph.edu", createdAt = 1_000L)

    @Test
    fun `an empty form reports both fields and never reaches the repository`() = runTest {
        val repository = FakeAuthRepository()
        val sessions = FakeSessionStore(null)
        val viewModel = LoginViewModel(repository, sessions)

        viewModel.submit()
        runCurrent()

        assertEquals(ValidateCredentials.ERROR_EMAIL_EMPTY, viewModel.uiState.emailErrorKey)
        assertEquals(ValidateCredentials.ERROR_PASSWORD_EMPTY, viewModel.uiState.passwordErrorKey)
        assertEquals(0, repository.signInCalls)
        assertNull(sessions.current())
    }

    @Test
    fun `a malformed email is refused before any password check`() = runTest {
        val repository = FakeAuthRepository()
        val viewModel = LoginViewModel(repository, FakeSessionStore(null))

        viewModel.onEmailChange("not-an-email")
        viewModel.onPasswordChange("secret123")
        viewModel.submit()
        runCurrent()

        assertEquals(ValidateCredentials.ERROR_EMAIL_FORMAT, viewModel.uiState.emailErrorKey)
        assertNull(viewModel.uiState.passwordErrorKey)
        assertEquals(0, repository.signInCalls)
    }

    @Test
    fun `typing again clears the error the person already saw`() = runTest {
        val viewModel = LoginViewModel(FakeAuthRepository(), FakeSessionStore(null))

        viewModel.submit()
        runCurrent()
        viewModel.onEmailChange("christopher@student.uph.edu")

        assertNull(viewModel.uiState.emailErrorKey)
    }

    @Test
    fun `wrong credentials show one generic message and start no session`() = runTest {
        val repository = FakeAuthRepository(accounts = emptyMap())
        val sessions = FakeSessionStore(null)
        val viewModel = LoginViewModel(repository, sessions)
        val effects = mutableListOf<LoginEffect>()
        backgroundScope.launch(mainRule.dispatcher) { viewModel.effects.collect { effects += it } }

        signIn(viewModel, "christopher@student.uph.edu", "secret123")
        runCurrent()

        assertEquals(AuthErrors.INVALID_CREDENTIALS, viewModel.uiState.formErrorKey)
        assertNull(sessions.current())
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `a valid sign-in stores the session and asks the screen to leave`() = runTest {
        val repository = FakeAuthRepository(accounts = mapOf(account.email to account))
        val sessions = FakeSessionStore(null)
        val viewModel = LoginViewModel(repository, sessions)
        val effects = mutableListOf<LoginEffect>()
        backgroundScope.launch(mainRule.dispatcher) { viewModel.effects.collect { effects += it } }

        signIn(viewModel, account.email, "secret123")
        runCurrent()

        assertEquals(SessionInfo(account.id, account.email), sessions.current())
        assertEquals(listOf(LoginEffect.SignedIn), effects)
        assertEquals(false, viewModel.uiState.isLoading)
    }

    @Test
    fun `a repository that throws reports a generic error instead of crashing the screen`() = runTest {
        val repository = FakeAuthRepository(accounts = mapOf(account.email to account))
        repository.failure = IllegalStateException("keystore unavailable")
        val sessions = FakeSessionStore(null)
        val viewModel = LoginViewModel(repository, sessions)

        signIn(viewModel, account.email, "secret123")
        runCurrent()

        assertEquals(AuthErrors.GENERIC, viewModel.uiState.formErrorKey)
        assertNull(sessions.current())
    }

    @Test
    fun `the guest path creates the shared account on first use and skips the form`() = runTest {
        val repository = FakeAuthRepository()
        val sessions = FakeSessionStore(null)
        val viewModel = LoginViewModel(repository, sessions)
        val effects = mutableListOf<LoginEffect>()
        backgroundScope.launch(mainRule.dispatcher) { viewModel.effects.collect { effects += it } }

        viewModel.continueAsGuest()
        runCurrent()

        assertEquals(listOf(LoginViewModel.GUEST_EMAIL), repository.registeredEmails)
        assertEquals(LoginViewModel.GUEST_EMAIL, sessions.current()?.email)
        assertEquals(listOf(LoginEffect.Skipped), effects)
    }

    @Test
    fun `the guest path reuses an account that already exists`() = runTest {
        val guest = UserAccount(id = "guest", email = LoginViewModel.GUEST_EMAIL, createdAt = 1_000L)
        val repository = FakeAuthRepository(accounts = mapOf(guest.email to guest))
        val sessions = FakeSessionStore(null)
        val viewModel = LoginViewModel(repository, sessions)

        viewModel.continueAsGuest()
        runCurrent()

        assertTrue(repository.registeredEmails.isEmpty())
        assertEquals("guest", sessions.current()?.userId)
    }

    private fun signIn(viewModel: LoginViewModel, email: String, password: String) {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.submit()
    }
}

private class FakeAuthRepository(private val accounts: Map<String, UserAccount> = emptyMap()) : AuthRepository {
    var failure: Throwable? = null
    var signInCalls = 0
    val registeredEmails = mutableListOf<String>()

    override suspend fun findAccount(email: String): UserAccount? = accounts[email]

    override suspend fun register(email: String, password: String): UserAccount {
        registeredEmails += email
        return UserAccount(id = "new", email = email, createdAt = 1_000L)
    }

    override suspend fun signIn(email: String, password: String): UserAccount? {
        signInCalls += 1
        failure?.let { throw it }
        return accounts[email]
    }

    override suspend fun deleteAccount(userId: String) = Unit
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
