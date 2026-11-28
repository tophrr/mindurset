package com.mindurset.app.ui.profile

import com.mindurset.app.domain.repository.PreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun `memoryEnabled defaults to true and updates when toggled`() {
        val prefs = FakeSettingsPreferencesStore()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = SettingsViewModel(prefs, externalScope = scope)
        val collector = scope.launch { vm.uiState.collect {} }

        assertFalse(vm.uiState.value.loading)
        assertTrue(vm.uiState.value.memoryEnabled)

        vm.setMemoryEnabled(false)
        assertFalse(vm.uiState.value.memoryEnabled)

        vm.setMemoryEnabled(true)
        assertTrue(vm.uiState.value.memoryEnabled)

        collector.cancel()
        scope.cancel()
    }

    @Test
    fun `setLanguage persists normalized choice and invokes callback`() {
        val prefs = FakeSettingsPreferencesStore(initialLanguage = "system")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val vm = SettingsViewModel(prefs, externalScope = scope)
        val collector = scope.launch { vm.uiState.collect {} }

        assertEquals("system", vm.uiState.value.language)

        var persistedCalls = 0
        vm.setLanguage("id") { persistedCalls += 1 }
        assertEquals("id", vm.uiState.value.language)
        assertEquals(1, persistedCalls)

        vm.setLanguage("en") { persistedCalls += 1 }
        assertEquals("en", vm.uiState.value.language)
        assertEquals(2, persistedCalls)

        vm.setLanguage("fr") { persistedCalls += 1 }
        assertEquals("system", vm.uiState.value.language)
        assertEquals(3, persistedCalls)

        collector.cancel()
        scope.cancel()
    }
}

private class FakeSettingsPreferencesStore(initialLanguage: String = "en") : PreferencesStore {
    private val _reminderEnabled = MutableStateFlow(false)
    private val _reminderHour = MutableStateFlow(19)
    private val _language = MutableStateFlow(initialLanguage)
    private val _theme = MutableStateFlow("system")
    private val _memoryEnabled = MutableStateFlow(true)
    private val _onboardingCompleted = MutableStateFlow(false)

    override val reminderEnabled: Flow<Boolean> = _reminderEnabled
    override val reminderHour: Flow<Int> = _reminderHour
    override val language: Flow<String> = _language
    override val theme: Flow<String> = _theme
    override val memoryEnabled: Flow<Boolean> = _memoryEnabled
    override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted

    override fun memoryNote(userId: String): Flow<String> = MutableStateFlow("")

    override suspend fun setReminderEnabled(enabled: Boolean) {
        _reminderEnabled.value = enabled
    }

    override suspend fun setReminderHour(hour: Int) {
        _reminderHour.value = hour
    }

    override suspend fun setLanguage(language: String) {
        _language.value = language
    }

    override suspend fun setTheme(theme: String) {
        _theme.value = theme
    }

    override suspend fun setMemoryEnabled(enabled: Boolean) {
        _memoryEnabled.value = enabled
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }

    override suspend fun setMemoryNote(userId: String, note: String) = Unit
}
