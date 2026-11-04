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
import com.mindurset.app.domain.model.SessionInfo
import com.mindurset.app.ui.common.LoadingState
import com.mindurset.app.ui.navigation.MindUrSetNavigation
import com.mindurset.app.ui.navigation.Routes
import com.mindurset.app.ui.theme.MindUrSetTheme

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
                            val start =
                                remember {
                                    if (state.session != null) Routes.DASHBOARD else Routes.LOGIN
                                }
                            MindUrSetNavigation(startDestination = start, container = container)
                        }
                    }
                }
            }
        }
    }
}
