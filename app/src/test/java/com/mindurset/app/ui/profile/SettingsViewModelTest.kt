package com.mindurset.app.ui.profile

import com.mindurset.app.domain.repository.PreferencesStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
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
}

private class FakeSettingsPreferencesStore : PreferencesStore {
    private val _reminderEnabled = MutableStateFlow(false)
    private val _reminderHour = MutableStateFlow(19)
    private val _language = MutableStateFlow("en")
    private val _theme = MutableStateFlow("system")
    private val _memoryEnabled = MutableStateFlow(true)

    override val reminderEnabled: Flow<Boolean> = _reminderEnabled
    override val reminderHour: Flow<Int> = _reminderHour
    override val language: Flow<String> = _language
    override val theme: Flow<String> = _theme
    override val memoryEnabled: Flow<Boolean> = _memoryEnabled

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

    override suspend fun setMemoryNote(userId: String, note: String) = Unit
}
