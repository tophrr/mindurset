package com.mindurset.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.usecase.DashboardSummary
import com.mindurset.app.domain.usecase.SummariseDashboard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** The four visible dashboard states the project asks for. */
sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data object Empty : DashboardUiState
    data class Content(val summary: DashboardSummary) : DashboardUiState
    data class Error(val message: String? = null) : DashboardUiState
}

/**
 * Reads everything the dashboard shows from Room [kotlinx.coroutines.flow.Flow]s and
 * exposes one StateFlow. Subscribes only while the UI is watching, so it keeps
 * updating live after any check-in or reflection is written and never blocks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val journalRepository: JournalRepository,
    private val checkInRepository: CheckInRepository,
    private val profileRepository: ProfileRepository,
    private val sessionStore: SessionStore,
    private val summariseDashboard: SummariseDashboard = SummariseDashboard(),
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = sessionStore.session
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf<DashboardUiState>(DashboardUiState.Empty)
            } else {
                dashboardStream(userId)
            }
        }
        .catch { emit(DashboardUiState.Error(it.message)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardUiState.Loading,
        )

    private fun dashboardStream(userId: String): Flow<DashboardUiState> {
        val sinceStartOfDay = summariseDashboard.startOfToday(now())
        return combine(
            profileRepository.observe(userId),
            journalRepository.count(userId),
            checkInRepository.count(userId),
            journalRepository.observeAll(userId),
            checkInRepository.observeRecent(userId, sinceStartOfDay),
        ) { profile: UserProfile?,
            journalCount: Int,
            checkInCount: Int,
            entries: List<JournalEntry>,
            recentCheckIns: List<CheckIn> ->
            summariseDashboard.summarise(
                profile = profile,
                journalCount = journalCount,
                checkInCount = checkInCount,
                journalEntries = entries,
                recentCheckIns = recentCheckIns,
                nowEpochMillis = now(),
            )
        }.map { summary ->
            if (summary.hasAnyData) DashboardUiState.Content(summary) else DashboardUiState.Empty
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
