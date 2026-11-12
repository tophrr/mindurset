package com.mindurset.app.ui.action

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Reads micro actions from Room and groups them for the screen. The repository is the one the rest
 * of the app uses, so an action written here is the same row Home and the dashboard read back.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActionViewModel(
    private val actions: MicroActionRepository,
    private val sessionStore: SessionStore,
    private val today: () -> LocalDate = LocalDate::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val now: () -> Long = System::currentTimeMillis,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val attempt = MutableStateFlow(0)
    private val _editorError = MutableStateFlow<ActionEditorError?>(null)
    val editorError: StateFlow<ActionEditorError?> = _editorError.asStateFlow()

    private var userId: String? = null
    private var items: List<MicroAction> = emptyList()

    val uiState: StateFlow<ActionUiState> =
        attempt
            .flatMapLatest {
                sessionStore.session
                    .distinctUntilChanged()
                    .flatMapLatest { session ->
                        userId = session?.userId
                        if (session == null) {
                            flowOf(ActionUiState.Empty)
                        } else {
                            actions
                                .observeAll(session.userId)
                                .map(::group)
                                .catch { emit(ActionUiState.Error) }
                        }
                    }
            }
            .stateIn(scope, SharingStarted.Eagerly, ActionUiState.Loading)

    fun retry() {
        attempt.value += 1
    }

    fun clearEditorError() {
        _editorError.value = null
    }

    /** @return false when the input was rejected, after setting [editorError]. */
    fun add(title: String, minutes: Int): Boolean {
        val clean = title.trim()
        if (!accept(clean, minutes)) return false
        val owner = userId ?: return false
        val createdAt = now()
        val action =
            MicroAction(
                id = UUID.randomUUID().toString(),
                userId = owner,
                sessionId = null,
                title = clean,
                minutes = minutes,
                done = false,
                snoozedUntil = null,
                createdAt = createdAt,
                completedAt = null,
            )
        scope.launch { actions.add(action) }
        return true
    }

    /** @return false when the input was rejected, after setting [editorError]. */
    fun edit(id: String, title: String, minutes: Int): Boolean {
        val clean = title.trim()
        if (!accept(clean, minutes)) return false
        val current = items.firstOrNull { it.id == id } ?: return false
        scope.launch { actions.add(current.copy(title = clean, minutes = minutes)) }
        return true
    }

    fun complete(id: String) {
        val at = now()
        scope.launch { actions.markDone(id, at) }
    }

    fun snooze(id: String) {
        val until = startOfTomorrow()
        scope.launch { actions.snooze(id, until) }
    }

    private fun accept(title: String, minutes: Int): Boolean {
        val error =
            when {
                title.isBlank() -> ActionEditorError.TitleRequired
                minutes !in MIN_MINUTES..MAX_MINUTES -> ActionEditorError.MinutesOutOfRange
                else -> null
            }
        _editorError.value = error
        return error == null
    }

    private fun startOfTomorrow(): Long = today().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun group(rows: List<MicroAction>): ActionUiState {
        items = rows
        val dayStart = today().atStartOfDay(zone).toInstant().toEpochMilli()
        val tomorrowStart = startOfTomorrow()

        val doneToday = rows.filter { it.done && (it.completedAt ?: Long.MIN_VALUE) >= dayStart }
        val tomorrow =
            rows.filter { !it.done && (it.snoozedUntil ?: Long.MIN_VALUE) >= tomorrowStart }
        val dueToday = rows.filter { !it.done && (it.snoozedUntil ?: Long.MIN_VALUE) < tomorrowStart }

        return if (dueToday.isEmpty() && doneToday.isEmpty() && tomorrow.isEmpty()) {
            ActionUiState.Empty
        } else {
            ActionUiState.Populated(today = dueToday, doneToday = doneToday, tomorrow = tomorrow)
        }
    }

    companion object {
        /** The contract caps a micro action at fifteen minutes. */
        const val MIN_MINUTES = 1
        const val MAX_MINUTES = 15
    }
}
