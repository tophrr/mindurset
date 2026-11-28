package com.mindurset.app.data.local

import android.util.Log
import com.mindurset.app.domain.repository.LocalDataWiper
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.SessionSummaryScheduler
import com.mindurset.app.domain.repository.WipeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Deletes every Room table and clears all DataStore preferences while cancelling any pending
 * background summarisation so nothing can be re-written from memory after the wipe.
 */
class LocalDataWiperImpl(
    private val database: MindUrSetDatabase,
    private val preferencesStore: PreferencesStore,
    private val summaryScheduler: SessionSummaryScheduler,
    private val tableCount: Int = DATABASE_TABLE_COUNT,
) : LocalDataWiper {
    override suspend fun wipeAll(): WipeResult {
        summaryScheduler.cancelPending()
        preferencesStore.clearAll()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
        summaryScheduler.cancelPending()
        val result = WipeResult(completed = true, tableCount = tableCount)
        Log.i(TAG, "completed=${result.completed} tables=${result.tableCount}")
        return result
    }

    companion object {
        private const val TAG = "MindUrSetDataWipe"
        const val DATABASE_TABLE_COUNT = 11
    }
}
