package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.UserProfile
import java.time.Instant
import java.time.ZoneId

/**
 * One immutable snapshot the dashboard renders. It is derived purely from Room
 * stream emissions, so it is always fresh and never requires a manual refresh.
 */
data class DashboardSummary(
    val displayName: String,
    val journalCount: Int,
    val checkInCount: Int,
    val latestReflections: List<JournalEntry>,
    val todayCheckIn: CheckIn?,
) {
    /** No check-ins and no reflections recorded for this user yet. */
    val hasAnyData: Boolean get() = journalCount > 0 || checkInCount > 0
}

/**
 * Collapses the separate profile / journal / check-in Room streams into a single
 * dashboard snapshot. Deliberately free of Android and Compose types so it stays a
 * pure, unit-testable mapping that can run off the main thread.
 */
class SummariseDashboard {

    fun summarise(
        profile: UserProfile?,
        journalCount: Int,
        checkInCount: Int,
        journalEntries: List<JournalEntry>,
        recentCheckIns: List<CheckIn>,
        nowEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DashboardSummary {
        val startOfDay = startOfToday(nowEpochMillis, zoneId)
        val todayCheckIn = recentCheckIns
            .filter { it.createdAt in startOfDay..nowEpochMillis }
            .maxByOrNull { it.createdAt }

        return DashboardSummary(
            displayName = profile?.displayName.orEmpty(),
            journalCount = journalCount,
            checkInCount = checkInCount,
            latestReflections = journalEntries
                .sortedByDescending { it.createdAt }
                .take(MAX_REFLECTIONS),
            todayCheckIn = todayCheckIn,
        )
    }

    /** First millisecond of the local day that contains [nowEpochMillis]. */
    fun startOfToday(nowEpochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(nowEpochMillis)
            .atZone(zoneId)
            .toLocalDate()
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

    companion object {
        const val MAX_REFLECTIONS = 3
    }
}
