package com.mindurset.app.ui.action

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ActionViewModelTest {
    @Test
    fun `load complete snooze edit and add update screen groups`() {
        val today = LocalDate.of(2026, 10, 21)
        val walk = ActionItem("walk", "Take a short walk", 10, today)
        val water = ActionItem("water", "Drink water", 2, today)
        val repository = InMemoryActionRepository(listOf(walk, water))
        val viewModel = ActionViewModel(
            repository = repository,
            today = { today },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )

        val loaded = viewModel.uiState.value as ActionUiState.Populated
        assertEquals(listOf(walk, water), loaded.today)

        viewModel.completeAction("walk")
        val completed = viewModel.uiState.value as ActionUiState.Populated
        assertEquals(listOf(walk.copy(isDone = true)), completed.doneToday)
        assertEquals(listOf(water), completed.today)

        viewModel.snoozeAction("water")
        val snoozed = viewModel.uiState.value as ActionUiState.Populated
        assertEquals(listOf(water.copy(scheduledDate = today.plusDays(1))), snoozed.tomorrow)

        viewModel.editAction("walk", "A gentler walk", 5)
        val edited = viewModel.uiState.value as ActionUiState.Populated
        assertEquals("A gentler walk", edited.doneToday.single().title)
        assertEquals(5, edited.doneToday.single().minutes)

        viewModel.addAction("Pause for water", 2)
        val added = viewModel.uiState.value as ActionUiState.Populated
        assertEquals("Pause for water", added.today.single().title)

        val emptyRepository = InMemoryActionRepository(emptyList())
        val emptyStateViewModel = ActionViewModel(
            repository = emptyRepository,
            today = { today },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined),
        )
        assertEquals(ActionUiState.Empty, emptyStateViewModel.uiState.value)
        emptyStateViewModel.addAction("Stretch gently", 3)
        val populated = emptyStateViewModel.uiState.value as ActionUiState.Populated
        assertEquals("Stretch gently", populated.today.single().title)
    }
}

private class InMemoryActionRepository(initialActions: List<ActionItem>) : ActionRepository {
    private val today = LocalDate.of(2026, 10, 21)
    private var actions = initialActions.toMutableList()

    override suspend fun loadActions(): List<ActionItem> = actions.toList()

    override suspend fun addAction(title: String, minutes: Int): List<ActionItem> {
        actions += ActionItem("added", title, minutes, today)
        return actions.toList()
    }

    override suspend fun editAction(id: String, title: String, minutes: Int): List<ActionItem> = update(id) {
        it.copy(title = title, minutes = minutes)
    }

    override suspend fun completeAction(id: String): List<ActionItem> = update(id) {
        it.copy(scheduledDate = today, isDone = true)
    }

    override suspend fun snoozeAction(id: String): List<ActionItem> = update(id) {
        it.copy(scheduledDate = today.plusDays(1))
    }

    private fun update(id: String, transform: (ActionItem) -> ActionItem): List<ActionItem> {
        actions = actions.map { if (it.id == id) transform(it) else it }.toMutableList()
        return actions.toList()
    }
}
