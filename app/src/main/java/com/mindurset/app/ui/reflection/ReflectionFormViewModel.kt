package com.mindurset.app.ui.reflection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.RiskLevel
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.usecase.ValidateReflection
import com.mindurset.app.safety.AsqFlow
import com.mindurset.app.safety.RiskScreening
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

private const val SAFETY_EVENT_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000

/**
 * Holds the create/edit form state. Living in a ViewModel means the text
 * survives rotation (requirement) instead of being lost with `remember`.
 */
class ReflectionFormViewModel(
    private val entryId: String?,
    private val journalRepository: JournalRepository,
    private val sessionStore: SessionStore,
    private val safetyRepository: SafetyRepository,
    private val clock: () -> Long = System::currentTimeMillis,
    externalScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = externalScope ?: viewModelScope
    val isEdit: Boolean = entryId != null

    var crisisLevel by mutableStateOf<RiskLevel?>(null)
        private set

    var crisisThirdParty by mutableStateOf(false)
        private set

    /**
     * Set when the floor found ambiguous hopelessness (L2): the entry is held back until the four
     * screening items are answered, because a yes moves it to the crisis card and a no lets it save.
     */
    var asq by mutableStateOf<AsqFlow.State?>(null)
        private set

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
    private var pending: PendingEntry? = null

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
        scope.launch {
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
        scope.launch {
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
            // Screen before the row exists, for the same reason as the check-in: a halted entry must
            // not be stored with its text.
            val screening =
                RiskScreening.screenLocal(
                    listOf(entry.challenge, entry.reframe, entry.gratitude).joinToString(" "),
                )
            if (screening.halts) {
                val at = clock()
                safetyRepository.record(
                    SafetyEvent(
                        id = UUID.randomUUID().toString(),
                        userId = entry.userId,
                        level = screening.level,
                        trigger = TRIGGER_JOURNAL_ENTRY,
                        patternId = screening.patternId,
                        createdAt = at,
                        retentionUntil = at + SAFETY_EVENT_RETENTION_MILLIS,
                    ),
                )
                isSaving = false
                if (screening.level >= RiskLevel.L3) {
                    crisisThirdParty = screening.thirdParty
                    crisisLevel = screening.level
                } else {
                    // L2 is the ambiguous rung: the ASQ items decide, not the phrase table alone.
                    // The entry, new or edited, is held in memory and never reaches Room here.
                    pending = PendingEntry(entry = entry, thirdParty = screening.thirdParty, onSaved = onSaved)
                    asq = AsqFlow.State()
                }
                return@launch
            }
            journalRepository.upsert(entry)
            isSaving = false
            onSaved()
        }
    }

    /**
     * Answers the question on screen. A negative screen saves the entry that was held back; a
     * positive one records an L3 safety event and hands over to the crisis card.
     */
    fun answerAsq(yes: Boolean) {
        val state = asq ?: return
        val question = AsqFlow.nextQuestion(state) ?: return
        val next = AsqFlow.answer(state, question.id, yes)
        val result = AsqFlow.result(next)
        if (result == null) {
            asq = next
            return
        }
        val held = pending
        pending = null
        asq = null
        isSaving = true
        scope.launch {
            if (held == null) {
                isSaving = false
                return@launch
            }
            if (result.level >= RiskLevel.L3) {
                val at = clock()
                safetyRepository.record(
                    SafetyEvent(
                        id = UUID.randomUUID().toString(),
                        userId = held.entry.userId,
                        level = RiskLevel.L3,
                        trigger = TRIGGER_ASQ_POSITIVE,
                        patternId = null,
                        createdAt = at,
                        retentionUntil = at + SAFETY_EVENT_RETENTION_MILLIS,
                    ),
                )
                isSaving = false
                crisisThirdParty = held.thirdParty
                crisisLevel = RiskLevel.L3
                return@launch
            }
            journalRepository.upsert(held.entry)
            isSaving = false
            held.onSaved()
        }
    }

    /** Clears the flag after the crisis card was shown, so it does not repeat. */
    fun acknowledgeCrisis() {
        crisisLevel = null
        crisisThirdParty = false
    }

    /** The entry and the callback that the ASQ step holds while it asks its questions. */
    private data class PendingEntry(val entry: JournalEntry, val thirdParty: Boolean = false, val onSaved: () -> Unit)

    private companion object {
        const val TRIGGER_JOURNAL_ENTRY = "journal_entry"
        const val TRIGGER_ASQ_POSITIVE = "asq_positive"
    }
}
