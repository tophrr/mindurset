package com.mindurset.app.ui.home.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Holds the in-progress check-in. Because the selection, intensity and note live in
 * the ViewModel (not `remember`), they survive configuration changes such as
 * rotation. Saving writes one [CheckIn] to Room through the repository.
 */
class CheckInViewModel(
    private val checkInRepository: CheckInRepository,
    private val sessionStore: SessionStore,
    /** Emotion already picked on the home card, so the form opens pre-filled. */
    initialEmotion: Emotion? = null,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ViewModel() {

    private val _emotion = MutableStateFlow(initialEmotion)
    val emotion: StateFlow<Emotion?> = _emotion.asStateFlow()

    private val _intensity = MutableStateFlow(DEFAULT_INTENSITY)
    val intensity: StateFlow<Int> = _intensity.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    fun selectEmotion(emotion: Emotion) {
        _emotion.value = emotion
    }

    fun setIntensity(value: Int) {
        _intensity.value = value.coerceIn(MIN_INTENSITY, MAX_INTENSITY)
    }

    fun setNote(value: String) {
        _note.value = value
    }

    /** Inserts the check-in for the signed-in user, then runs [onSaved] on the main thread. */
    fun save(onSaved: () -> Unit) {
        val selected = _emotion.value ?: return
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            val userId = sessionStore.current()?.userId
            if (userId != null) {
                checkInRepository.insert(
                    CheckIn(
                        id = newId(),
                        userId = userId,
                        emotion = selected,
                        intensity = _intensity.value,
                        note = _note.value.trim().ifBlank { null },
                        createdAt = clock(),
                    ),
                )
            }
            _saving.value = false
            onSaved()
        }
    }

    companion object {
        const val MIN_INTENSITY = 1
        const val MAX_INTENSITY = 10
        private const val DEFAULT_INTENSITY = 5
    }
}
