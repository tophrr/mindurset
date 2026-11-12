package com.mindurset.app.ui.consent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.ConsentPolicy
import com.mindurset.app.domain.model.ConsentRecord
import com.mindurset.app.domain.repository.ConsentRepository
import com.mindurset.app.domain.repository.SessionStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Reads and writes the one consent row that gates the first check-in. The record is the source of
 * truth: a revoked row keeps its id and gains a timestamp, so the history survives a withdrawal.
 */
class ConsentViewModel(
    private val consentRepository: ConsentRepository,
    private val sessionStore: SessionStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _granted = MutableStateFlow<Boolean?>(null)

    /** Null while the row is being read, then true when an active grant exists. */
    val granted: StateFlow<Boolean?> = _granted.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch { _granted.value = readGranted() }
    }

    fun grant(onDone: () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        scope.launch {
            val userId = sessionStore.current()?.userId
            if (userId != null) {
                val existing = consentRepository.find(userId, ConsentPolicy.KIND)
                consentRepository.grant(
                    ConsentRecord(
                        id = existing?.id ?: newId(),
                        userId = userId,
                        kind = ConsentPolicy.KIND,
                        policyVersion = ConsentPolicy.VERSION,
                        grantedAt = clock(),
                        revokedAt = null,
                    ),
                )
                _granted.value = true
            }
            _saving.value = false
            onDone()
        }
    }

    fun revoke(onDone: () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        scope.launch {
            val userId = sessionStore.current()?.userId
            if (userId != null) {
                val existing = consentRepository.find(userId, ConsentPolicy.KIND)
                if (existing != null && existing.revokedAt == null) {
                    consentRepository.revoke(existing.id, clock())
                }
                _granted.value = false
            }
            _saving.value = false
            onDone()
        }
    }

    private suspend fun readGranted(): Boolean {
        val userId = sessionStore.current()?.userId ?: return false
        val record = consentRepository.find(userId, ConsentPolicy.KIND) ?: return false
        return record.revokedAt == null && record.policyVersion == ConsentPolicy.VERSION
    }
}
