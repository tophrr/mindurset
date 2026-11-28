package com.mindurset.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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

sealed interface SessionState {
    data object Loading : SessionState

    data class Ready(val session: SessionInfo?) : SessionState
}

class MainActivity : ComponentActivity() {
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
