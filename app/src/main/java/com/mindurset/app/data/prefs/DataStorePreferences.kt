package com.mindurset.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mindurset_prefs")

/**
 * Session and preference storage. The session is a local pointer to the signed-in
 * account row; no password or long-lived token is kept here.
 */
class DataStorePreferences(context: Context) : SessionStore, PreferencesStore {

    private val store = context.applicationContext.dataStore

    private object Keys {
        val sessionUserId = stringPreferencesKey("session_user_id")
        val sessionEmail = stringPreferencesKey("session_email")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val language = stringPreferencesKey("language")
        val theme = stringPreferencesKey("theme")
    }

    override val session: Flow<SessionInfo?> = store.data.map { prefs ->
        val userId = prefs[Keys.sessionUserId]
        val email = prefs[Keys.sessionEmail]
        if (userId.isNullOrBlank()) null else SessionInfo(userId = userId, email = email.orEmpty())
    }

    override suspend fun current(): SessionInfo? = session.first()

    override suspend fun signIn(session: SessionInfo) {
        store.edit { prefs ->
            prefs[Keys.sessionUserId] = session.userId
            prefs[Keys.sessionEmail] = session.email
        }
    }

    override suspend fun signOut() {
        store.edit { prefs ->
            prefs.remove(Keys.sessionUserId)
            prefs.remove(Keys.sessionEmail)
        }
    }

    override val reminderEnabled: Flow<Boolean> = store.data.map { it[Keys.reminderEnabled] ?: false }

    override val reminderHour: Flow<Int> = store.data.map { it[Keys.reminderHour] ?: 19 }

    override val language: Flow<String> = store.data.map { it[Keys.language] ?: "en" }

    override val theme: Flow<String> = store.data.map { it[Keys.theme] ?: "system" }

    override suspend fun setReminderEnabled(enabled: Boolean) {
        store.edit { it[Keys.reminderEnabled] = enabled }
    }

    override suspend fun setReminderHour(hour: Int) {
        store.edit { it[Keys.reminderHour] = hour.coerceIn(0, 23) }
    }

    override suspend fun setLanguage(language: String) {
        store.edit { it[Keys.language] = language }
    }

    override suspend fun setTheme(theme: String) {
        store.edit { it[Keys.theme] = theme }
    }
}
