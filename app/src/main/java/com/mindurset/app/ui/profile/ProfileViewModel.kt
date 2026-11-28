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
import com.mindurset.app.domain.usecase.BuildMoodInsights
import com.mindurset.app.domain.usecase.MoodInsights
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
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

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
    val weeklyTrend: MoodInsights = MoodInsights.empty(BuildMoodInsights.WEEKLY_WINDOW_DAYS),
    val breakdownWindowDays: Int = BuildMoodInsights.WEEKLY_WINDOW_DAYS,
    val breakdown: MoodInsights = MoodInsights.empty(BuildMoodInsights.WEEKLY_WINDOW_DAYS),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val sessionStore: SessionStore,
    private val preferencesStore: PreferencesStore,
    private val checkInRepository: CheckInRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    /** Flips to true once sign-out has actually completed, so the screen can navigate. */
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private val selectedBreakdownWindow = MutableStateFlow(BuildMoodInsights.WEEKLY_WINDOW_DAYS)

    private val today: LocalDate = Instant.ofEpochMilli(clock.millis()).atZone(clock.zone).toLocalDate()

    /** The month the calendar renders: the current local month, fixed for the VM's lifetime. */
    private val month: YearMonth = YearMonth.from(today)
    private val monthStartMillis: Long =
        month
            .atDay(1)
            .atStartOfDay(clock.zone)
            .toInstant()
            .toEpochMilli()
    private val thirtyDayStartMillis: Long =
        today
            .minusDays(BuildMoodInsights.MONTHLY_WINDOW_DAYS - 1L)
            .atStartOfDay(clock.zone)
            .toInstant()
            .toEpochMilli()
    private val observeSinceMillis: Long = minOf(monthStartMillis, thirtyDayStartMillis)

    val uiState: StateFlow<ProfileUiState> =
        sessionStore.session
            .flatMapLatest { session ->
                if (session == null) {
                    flowOf(ProfileUiState(loading = false, signedOut = true))
                } else {
                    combine(
                        profileRepository.observe(session.userId),
                        checkInRepository.observeRecent(session.userId, observeSinceMillis),
                        selectedBreakdownWindow,
                    ) { profile, checkIns, windowDays ->
                        ProfileUiState(
                            loading = false,
                            signedOut = false,
                            displayName =
                            profile?.displayName?.takeIf { it.isNotBlank() }
                                ?: session.email.substringBefore('@'),
                            email = session.email,
                            joinedAt = profile?.joinedAt,
                            year = month.year,
                            monthValue = month.monthValue,
                            dayEmotions = dominantPerDay(checkIns, month),
                            weeklyTrend =
                            BuildMoodInsights.build(
                                checkIns = checkIns,
                                windowDays = BuildMoodInsights.WEEKLY_WINDOW_DAYS,
                                clock = clock,
                            ),
                            breakdownWindowDays = windowDays,
                            breakdown =
                            BuildMoodInsights.build(
                                checkIns = checkIns,
                                windowDays = windowDays,
                                clock = clock,
                            ),
                        )
                    }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(loading = true))

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

    /** Switches the breakdown window between 7 and 30 days. */
    fun selectBreakdownWindow(days: Int) {
        if (days == BuildMoodInsights.WEEKLY_WINDOW_DAYS || days == BuildMoodInsights.MONTHLY_WINDOW_DAYS) {
            selectedBreakdownWindow.value = days
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
                preferencesStore.setMemoryNote(session.userId, "")
            }
            sessionStore.signOut()
            _loggedOut.value = true
        }
    }

    /** Picks the most frequent emotion per calendar day within [month]. */
    private fun dominantPerDay(checkIns: List<CheckIn>, month: YearMonth): Map<Int, Emotion> {
        val zone = clock.zone
        return checkIns
            .mapNotNull { checkIn ->
                val date = Instant.ofEpochMilli(checkIn.createdAt).atZone(zone).toLocalDate()
                if (YearMonth.from(date) == month) date.dayOfMonth to checkIn.emotion else null
            }.groupBy({ it.first }, { it.second })
            .mapValues { (_, emotions) ->
                emotions
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }!!
                    .key
            }
    }
}
