package com.mindurset.app.ui.action

import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.UUID

class FakeActionRepository(
    initialActions: List<ActionItem>,
    private val loadingDelayMillis: Long = DEFAULT_LOADING_DELAY_MILLIS,
) : ActionRepository {
    private val today = LocalDate.now()
    private var actions = initialActions.map { it.copy(scheduledDate = today) }

    override suspend fun loadActions(): List<ActionItem> {
        delay(loadingDelayMillis)
        return actions.toList()
    }

    override suspend fun addAction(title: String, minutes: Int): List<ActionItem> {
        actions = actions + ActionItem(UUID.randomUUID().toString(), title, minutes, today)
        return actions.toList()
    }

    override suspend fun editAction(id: String, title: String, minutes: Int): List<ActionItem> =
        update(id) { it.copy(title = title, minutes = minutes) }

    override suspend fun completeAction(id: String): List<ActionItem> =
        update(id) { it.copy(scheduledDate = today, isDone = true) }

    override suspend fun snoozeAction(id: String): List<ActionItem> =
        update(id) { it.copy(scheduledDate = today.plusDays(1)) }

    private fun update(id: String, transform: (ActionItem) -> ActionItem): List<ActionItem> {
        actions = actions.map { if (it.id == id) transform(it) else it }
        return actions.toList()
    }

    private companion object {
        const val DEFAULT_LOADING_DELAY_MILLIS = 450L
    }
}
