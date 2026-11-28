package com.mindurset.app

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import com.mindurset.app.data.prefs.AppLocale
import com.mindurset.app.di.LocalAppContainer
import com.mindurset.app.domain.model.ConsentPolicy
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.domain.usecase.EntryDestination
import com.mindurset.app.domain.usecase.ResolveEntryDestination
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.navigation.MindUrSetNavigation
import com.mindurset.app.ui.navigation.Routes
import com.mindurset.app.ui.theme.MindUrSetTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

sealed interface SessionState {
    data object Loading : SessionState

    data class Ready(val session: SessionInfo?) : SessionState
}

private const val LOCALE_READ_TIMEOUT_MS = 150L

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            super.attachBaseContext(newBase)
            return
        }
        // This runs before the first frame, so it must not wait on disk without a bound: a slow
        // DataStore read here would be an ANR, not a slow screen. The read is given 150 ms; if the
        // store is not ready by then the app starts in the system locale and the language choice is
        // applied on the next recreation, which the settings screen triggers when it changes.
        val storedLanguage = (newBase.applicationContext as? MindUrSetApp)?.let { app ->
            runBlocking {
                withTimeoutOrNull(LOCALE_READ_TIMEOUT_MS) {
                    app.container.preferencesStore.language.first()
                }
            }
        }
        super.attachBaseContext(AppLocale.wrapContext(newBase, storedLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is light-only, so the system bars always need dark icons. The default follows the
        // system theme, which leaves white icons over the pale background when the phone is in dark
        // mode and makes the clock and battery unreadable.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val container = (application as MindUrSetApp).container
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lifecycleScope.launch {
                val synced = AppLocale.readPlatformLanguage(this@MainActivity)
                if (container.preferencesStore.language.first() != synced) {
                    container.preferencesStore.setLanguage(synced)
                }
            }
        }
        setContent {
            MindUrSetTheme {
                CompositionLocalProvider(LocalAppContainer provides container) {
                    val sessionState by produceState<SessionState>(SessionState.Loading) {
                        container.sessionStore.session.collect { value = SessionState.Ready(it) }
                    }
                    when (val state = sessionState) {
                        is SessionState.Loading -> {
                            LoadingState()
                        }

                        is SessionState.Ready -> {
                            val userId = state.session?.userId
                            val consent by produceState<Boolean?>(null, userId) {
                                value =
                                    if (userId == null) {
                                        null
                                    } else {
                                        container.consentRepository
                                            .find(userId, ConsentPolicy.KIND)
                                            ?.let { it.revokedAt == null } ?: false
                                    }
                            }
                            val onboardingCompleted by produceState<Boolean?>(null, userId) {
                                value =
                                    if (userId == null) {
                                        null
                                    } else {
                                        container.preferencesStore.onboardingCompleted.first()
                                    }
                            }
                            val start =
                                remember(state.session, consent, onboardingCompleted) {
                                    when (
                                        ResolveEntryDestination.resolve(
                                            hasSession = state.session != null,
                                            hasConsent = consent == true,
                                            onboardingCompleted = onboardingCompleted == true,
                                        )
                                    ) {
                                        EntryDestination.LOGIN -> Routes.LOGIN
                                        EntryDestination.CONSENT -> Routes.CONSENT
                                        EntryDestination.ONBOARDING -> Routes.ONBOARDING
                                        EntryDestination.DASHBOARD -> Routes.DASHBOARD
                                    }
                                }
                            if (state.session != null && (consent == null || onboardingCompleted == null)) {
                                LoadingState()
                            } else {
                                MindUrSetNavigation(startDestination = start, container = container)
                            }
                        }
                    }
                }
            }
        }
    }
}
