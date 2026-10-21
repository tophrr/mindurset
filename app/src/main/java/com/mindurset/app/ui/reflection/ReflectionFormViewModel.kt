package com.mindurset.app.ui.reflection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.usecase.ValidateReflection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Holds the create/edit form state. Living in a ViewModel means the text
 * survives rotation (requirement) instead of being lost with `remember`.
 */
class ReflectionFormViewModel(
    private val entryId: String?,
    private val journalRepository: JournalRepository,
    private val sessionStore: SessionStore,
) : ViewModel() {
    val isEdit: Boolean = entryId != null

    var challenge by mutableStateOf("")
        private set
    var reframe by mutableStateOf("")
        private set
    var gratitude by mutableStateOf("")
        private set

    var challengeTouched by mutableStateOf(false)
        private set
    var reframeTouched by mutableStateOf(false)
        private set
    var gratitudeTouched by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(isEdit)
        private set
    var notFound by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set

    private var original: JournalEntry? = null

    val validation: ValidateReflection.Result
        get() = ValidateReflection.validate(challenge, reframe, gratitude)

    val isValid: Boolean get() = validation.isValid

    /** Called when Save is pressed on an invalid form, so every field error becomes visible. */
    fun markAllTouched() {
        challengeTouched = true
        reframeTouched = true
        gratitudeTouched = true
    }

    init {
        if (entryId != null) load(entryId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            val entry = journalRepository.observeById(id).first()
            if (entry == null) {
                notFound = true
            } else {
                original = entry
                challenge = entry.challenge
                reframe = entry.reframe
                gratitude = entry.gratitude
            }
            isLoading = false
        }
    }

    fun onChallengeChange(value: String) {
        challenge = value
        challengeTouched = true
    }

    fun onReframeChange(value: String) {
        reframe = value
        reframeTouched = true
    }

    fun onGratitudeChange(value: String) {
        gratitude = value
        gratitudeTouched = true
    }

    /** Inserts (new UUID) or updates the row, then calls [onSaved]. */
    fun save(onSaved: () -> Unit) {
        challengeTouched = true
        reframeTouched = true
        gratitudeTouched = true
        if (!isValid || isSaving) return
        isSaving = true
        viewModelScope.launch {
            val session = sessionStore.current()
            if (session == null) {
                isSaving = false
                return@launch
            }
            val now = System.currentTimeMillis()
            val previous = original
            val entry =
                JournalEntry(
                    id = previous?.id ?: UUID.randomUUID().toString(),
                    userId = previous?.userId ?: session.userId,
                    challenge = challenge.trim(),
                    reframe = reframe.trim(),
                    gratitude = gratitude.trim(),
                    createdAt = previous?.createdAt ?: now,
                    updatedAt = now,
                )
            journalRepository.upsert(entry)
            isSaving = false
            onSaved()
        }
    }
}
