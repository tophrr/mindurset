package com.mindurset.app.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStorePreferencesTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `language defaults to system when unset`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = tempFolder.newFile("default_lang.preferences_pb")
            val ds = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val prefs = DataStorePreferences(ds)

            assertEquals("system", prefs.language.first())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `chosen language survives being written and read back across store reopen`() = runTest {
        val file = tempFolder.newFile("persisted_lang.preferences_pb")

        val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds1 = PreferenceDataStoreFactory.create(scope = scope1, produceFile = { file })
            val prefs1 = DataStorePreferences(ds1)
            prefs1.setLanguage("id")
            assertEquals("id", prefs1.language.first())
        } finally {
            scope1.cancel()
        }

        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds2 = PreferenceDataStoreFactory.create(scope = scope2, produceFile = { file })
            val prefs2 = DataStorePreferences(ds2)
            assertEquals("id", prefs2.language.first())

            prefs2.setLanguage("en")
            assertEquals("en", prefs2.language.first())
        } finally {
            scope2.cancel()
        }

        val scope3 = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds3 = PreferenceDataStoreFactory.create(scope = scope3, produceFile = { file })
            val prefs3 = DataStorePreferences(ds3)
            assertEquals("en", prefs3.language.first())

            prefs3.setLanguage("system")
            assertEquals("system", prefs3.language.first())
        } finally {
            scope3.cancel()
        }
    }

    @Test
    fun `unsupported language tag falls back to system on write and read`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val file = tempFolder.newFile("unsupported_lang.preferences_pb")
            val ds = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            val prefs = DataStorePreferences(ds)

            prefs.setLanguage("fr")
            assertEquals("system", prefs.language.first())

            ds.edit { it[stringPreferencesKey("language")] = "xx-YY" }
            assertEquals("system", prefs.language.first())
        } finally {
            scope.cancel()
        }
    }
}
