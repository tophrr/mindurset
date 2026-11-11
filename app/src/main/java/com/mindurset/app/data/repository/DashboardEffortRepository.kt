package com.mindurset.app.data.repository

import com.mindurset.app.data.local.MicroActionDao
import com.mindurset.app.data.local.ReframeSessionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Streams completion timestamps for the dashboard without exposing session content. */
class DashboardEffortRepository(private val reframeDao: ReframeSessionDao, private val actionDao: MicroActionDao) {
    fun observeCompletionTimestamps(userId: String, since: Long): Flow<Pair<List<Long>, List<Long>>> = combine(
        reframeDao.observeCompletedSince(userId, since),
        actionDao.observeCompletedSince(userId, since),
    ) { reframes, actions ->
        reframes.mapNotNull { it.completedAt } to actions.mapNotNull { it.completedAt }
    }
}
