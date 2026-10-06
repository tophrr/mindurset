package com.mindurset.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.repository.AuthRepository
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Everything the profile screen shows, derived from the live session, Room and check-ins. */
data class ProfileUiState(
    val loading: Boolean = true,
    val signedOut: Boolean = false,
    val displayName: String = "",
    val email: String = "",
    val joinedAt: Long? = null,
    val year: Int = 0,
    val monthValue: Int = 0,
    /** Day-of-month -> dominant emotion recorded that day, for the current month only. */
    val dayEmotions: Map<Int, Emotion> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val preferencesStore: PreferencesStore,
    private val checkInRepository: CheckInRepository,
) : ViewModel() {

    /** Flips to true once sign-out has actually completed, so the screen can navigate. */
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    /** The month the calendar renders: the current local month, fixed for the VM's lifetime. */
    private val month: YearMonth = YearMonth.now()
    private val monthStartMillis: Long = month
        .atDay(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    val uiState: StateFlow<ProfileUiState> = sessionStore.session
        .flatMapLatest { session ->
            if (session == null) {
                flowOf(ProfileUiState(loading = false, signedOut = true))
            } else {
                combine(
                    profileRepository.observe(session.userId),
                    checkInRepository.observeRecent(session.userId, monthStartMillis),
                ) { profile, checkIns ->
                    ProfileUiState(
                        loading = false,
                        signedOut = false,
                        displayName = profile?.displayName?.takeIf { it.isNotBlank() }
                            ?: session.email.substringBefore('@'),
                        email = session.email,
                        joinedAt = profile?.joinedAt,
                        year = month.year,
                        monthValue = month.monthValue,
                        dayEmotions = dominantPerDay(checkIns, month),
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(loading = true))

    init {
        // Guarantees a Room profile row for the signed-in account (e.g. after "skip"),
        // so the header never has to fall back to invented data.
        viewModelScope.launch {
            sessionStore.session.collect { session ->
                if (session != null) {
                    profileRepository.ensureProfile(session.userId, session.email)
                }
            }
        }
    }

    /**
     * Signs out for real. When [deleteLocalData] is true the account row and all of
     * its journal/check-in/profile data are deleted first; otherwise the data stays.
     */
    fun logout(deleteLocalData: Boolean) {
        viewModelScope.launch {
            val session = sessionStore.current()
            if (deleteLocalData && session != null) {
                authRepository.deleteAccount(session.userId)
            }
            sessionStore.signOut()
            _loggedOut.value = true
        }
    }

    /** Picks the most frequent emotion per calendar day within [month]. */
    private fun dominantPerDay(checkIns: List<CheckIn>, month: YearMonth): Map<Int, Emotion> {
        val zone = ZoneId.systemDefault()
        return checkIns
            .mapNotNull { checkIn ->
                val date = Instant.ofEpochMilli(checkIn.createdAt).atZone(zone).toLocalDate()
                if (YearMonth.from(date) == month) date.dayOfMonth to checkIn.emotion else null
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, emotions) ->
                emotions.groupingBy { it }.eachCount().maxByOrNull { it.value }!!.key
            }
    }
}
