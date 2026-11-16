package com.mindurset.app.ui.profile

import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.testutil.MainDispatcherRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    @Test
    fun `the stored preferences become the loaded state`() = runTest {
        val store =
            FakePreferencesStore(
                reminderEnabled = true,
                reminderHour = 20,
                language = "in",
                theme = "light",
            )
        val viewModel = SettingsViewModel(store)

        val state = viewModel.uiState.first { !it.loading }
        advanceUntilIdle()
        assertTrue("loading must clear once the store answers", !state.loading)
        assertEquals(true, state.reminderEnabled)
        assertEquals(20, state.reminderHour)
        assertEquals("in", state.language)
        assertEquals("light", state.theme)
    }

    @Test
    fun `turning the reminder on is written through to the store`() = runTest {
        val store = FakePreferencesStore()
        val viewModel = SettingsViewModel(store)

        viewModel.setReminderEnabled(true)
        val state = viewModel.uiState.first { it.reminderEnabled }
        advanceUntilIdle()

        assertEquals(true, store.writtenReminderEnabled)
        assertEquals(true, state.reminderEnabled)
    }

    @Test
    fun `a new reminder hour is written through and shown`() = runTest {
        val store = FakePreferencesStore()
        val viewModel = SettingsViewModel(store)

        viewModel.setReminderHour(7)
        val state = viewModel.uiState.first { it.reminderHour == 7 }
        advanceUntilIdle()

        assertEquals(7, store.writtenReminderHour)
        assertEquals(7, state.reminderHour)
    }

    @Test
    fun `choosing Indonesian is stored as the language and not kept only on screen`() = runTest {
        val store = FakePreferencesStore()
        val viewModel = SettingsViewModel(store)

        viewModel.setLanguage("in")
        val state = viewModel.uiState.first { it.language == "in" }
        advanceUntilIdle()

        assertEquals("in", store.writtenLanguage)
        assertEquals("in", state.language)
    }
}

private class FakePreferencesStore(
    reminderEnabled: Boolean = false,
    reminderHour: Int = 19,
    language: String = "en",
    theme: String = "system",
) : PreferencesStore {
    private val enabledState = MutableStateFlow(reminderEnabled)
    private val hourState = MutableStateFlow(reminderHour)
    private val languageState = MutableStateFlow(language)
    private val themeState = MutableStateFlow(theme)

    var writtenReminderEnabled: Boolean? = null
    var writtenReminderHour: Int? = null
    var writtenLanguage: String? = null

    override val reminderEnabled: Flow<Boolean> = enabledState
    override val reminderHour: Flow<Int> = hourState
    override val language: Flow<String> = languageState
    override val theme: Flow<String> = themeState

    override suspend fun setReminderEnabled(enabled: Boolean) {
        writtenReminderEnabled = enabled
        enabledState.value = enabled
    }

    override suspend fun setReminderHour(hour: Int) {
        writtenReminderHour = hour
        hourState.value = hour
    }

    override suspend fun setLanguage(language: String) {
        writtenLanguage = language
        languageState.value = language
    }

    override suspend fun setTheme(theme: String) {
        themeState.value = theme
    }
}
