package com.mindurset.app.ui.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.TrustedContact
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.TrustedContactRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * State for the sheet the crisis card opens: the saved contact, and the form that appears when there
 * is none. Nothing here talks to the network — the card must work when nothing else does.
 */
data class TrustedContactState(
    val contact: TrustedContact? = null,
    val name: String = "",
    val phone: String = "",
    val editing: Boolean = false,
    val invalid: Boolean = false,
)

class CrisisContactViewModel(
    private val repository: TrustedContactRepository,
    private val sessionStore: SessionStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val _state = MutableStateFlow(TrustedContactState())
    val state: StateFlow<TrustedContactState> = _state.asStateFlow()

    init {
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            repository.observe(userId).collect { contacts ->
                val first = contacts.firstOrNull()
                _state.update {
                    it.copy(
                        contact = first,
                        name = if (it.editing) it.name else first?.name.orEmpty(),
                        phone = if (it.editing) it.phone else first?.phone.orEmpty(),
                    )
                }
            }
        }
    }

    fun startEditing() {
        _state.update {
            it.copy(editing = true, name = it.contact?.name.orEmpty(), phone = it.contact?.phone.orEmpty())
        }
    }

    fun cancelEditing() {
        _state.update { it.copy(editing = false, invalid = false) }
    }

    fun onNameChange(value: String) {
        _state.update { it.copy(name = value, invalid = false) }
    }

    fun onPhoneChange(value: String) {
        _state.update { it.copy(phone = value, invalid = false) }
    }

    /**
     * Saves the contact under the current person. A blank name or a number with fewer than four digits
     * is refused rather than stored: a contact that cannot be dialled is worse than none on this card.
     */
    fun save() {
        val current = _state.value
        val name = current.name.trim()
        val phone = current.phone.trim()
        val digits = phone.count { it.isDigit() }
        if (name.isEmpty() || digits < MIN_PHONE_DIGITS) {
            _state.update { it.copy(invalid = true) }
            return
        }
        scope.launch {
            val userId = sessionStore.current()?.userId ?: return@launch
            repository.upsert(
                TrustedContact(
                    id = current.contact?.id ?: newId(),
                    userId = userId,
                    name = name,
                    phone = phone,
                    relationship = current.contact?.relationship,
                    createdAt = current.contact?.createdAt ?: now(),
                ),
            )
            _state.update { it.copy(editing = false, invalid = false) }
        }
    }

    companion object {
        const val MIN_PHONE_DIGITS = 4
    }
}
