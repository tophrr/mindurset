package com.mindurset.app.ui.action

interface ActionRepository {
    suspend fun loadActions(): List<ActionItem>

    suspend fun addAction(title: String, minutes: Int): List<ActionItem>

    suspend fun editAction(id: String, title: String, minutes: Int): List<ActionItem>

    suspend fun completeAction(id: String): List<ActionItem>

    suspend fun snoozeAction(id: String): List<ActionItem>
}
