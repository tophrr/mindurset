package com.mindurset.app.di

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import com.mindurset.app.data.local.MindUrSetDatabase
import com.mindurset.app.data.prefs.DataStorePreferences
import com.mindurset.app.data.repository.AuthRepositoryImpl
import com.mindurset.app.data.repository.CheckInRepositoryImpl
import com.mindurset.app.data.repository.JournalRepositoryImpl
import com.mindurset.app.data.repository.ProfileRepositoryImpl
import com.mindurset.app.domain.repository.AuthRepository
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SessionStore

/**
 * Manual dependency container: one object graph for the whole app, no annotation
 * processor. Screens receive what they need through their ViewModel.
 */
class AppContainer(context: Context) {

    private val database = MindUrSetDatabase.build(context)

    val preferences: DataStorePreferences = DataStorePreferences(context)

    val sessionStore: SessionStore = preferences

    val preferencesStore: PreferencesStore = preferences

    val authRepository: AuthRepository = AuthRepositoryImpl(
        accountDao = database.accountDao(),
        profileDao = database.profileDao(),
        journalDao = database.journalDao(),
        checkInDao = database.checkInDao(),
    )

    val journalRepository: JournalRepository = JournalRepositoryImpl(database.journalDao())

    val checkInRepository: CheckInRepository = CheckInRepositoryImpl(database.checkInDao())

    val profileRepository: ProfileRepository = ProfileRepositoryImpl(database.profileDao())
}

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer is not provided")
}
