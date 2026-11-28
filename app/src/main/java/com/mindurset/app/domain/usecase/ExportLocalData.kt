package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.ConsentRecord
import com.mindurset.app.domain.model.JournalEntry
import com.mindurset.app.domain.model.MicroAction
import com.mindurset.app.domain.model.ReframeSession
import com.mindurset.app.domain.model.SafetyEvent
import com.mindurset.app.domain.model.SessionMessage
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.model.TrustedContact
import com.mindurset.app.domain.model.UserProfile
import com.mindurset.app.domain.repository.CheckInRepository
import com.mindurset.app.domain.repository.ConsentRepository
import com.mindurset.app.domain.repository.JournalRepository
import com.mindurset.app.domain.repository.MicroActionRepository
import com.mindurset.app.domain.repository.PreferencesStore
import com.mindurset.app.domain.repository.ProfileRepository
import com.mindurset.app.domain.repository.SafetyRepository
import com.mindurset.app.domain.repository.SessionRepository
import com.mindurset.app.domain.repository.SessionStore
import com.mindurset.app.domain.repository.SessionSummaryRepository
import com.mindurset.app.domain.repository.TrustedContactRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.format.DateTimeFormatter

data class ExportProfile(
    val displayName: String,
    val email: String?,
    val joinedAt: Long,
    val language: String,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val memoryEnabled: Boolean,
    val memoryNote: String,
)

data class ExportReframeSession(val session: ReframeSession, val messages: List<SessionMessage>)

data class ExportSnapshot(
    val profile: ExportProfile?,
    val checkIns: List<CheckIn>,
    val reframeSessions: List<ExportReframeSession>,
    val journalEntries: List<JournalEntry>,
    val microActions: List<MicroAction>,
    val sessionSummaries: List<SessionSummary>,
    val trustedContacts: List<TrustedContact>,
    val safetyEvents: List<SafetyEvent>,
    val consentRecords: List<ConsentRecord>,
)

/**
 * Collects every local row owned by the active user and formats a deterministic
 * `mindurset-export-v1` JSON document.
 */
class ExportLocalData(
    private val sessionStore: SessionStore,
    private val profileRepository: ProfileRepository,
    private val preferencesStore: PreferencesStore,
    private val checkInRepository: CheckInRepository,
    private val sessionRepository: SessionRepository,
    private val journalRepository: JournalRepository,
    private val microActionRepository: MicroActionRepository,
    private val sessionSummaryRepository: SessionSummaryRepository,
    private val trustedContactRepository: TrustedContactRepository,
    private val safetyRepository: SafetyRepository,
    private val consentRepository: ConsentRepository,
    private val appVersionName: String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    suspend fun exportJson(): String {
        val snapshot = collectSnapshot()
        return buildJson(snapshot, exportedAtMillis = clock())
    }

    suspend fun collectSnapshot(): ExportSnapshot {
        val session = sessionStore.current()
            ?: return ExportSnapshot(
                profile = null,
                checkIns = emptyList(),
                reframeSessions = emptyList(),
                journalEntries = emptyList(),
                microActions = emptyList(),
                sessionSummaries = emptyList(),
                trustedContacts = emptyList(),
                safetyEvents = emptyList(),
                consentRecords = emptyList(),
            )
        val userId = session.userId
        val profile = profileRepository.observe(userId).first()
        val exportProfile = profile?.toExportProfile(
            email = session.email.takeIf { it.isNotBlank() },
            language = preferencesStore.language.first(),
            reminderEnabled = preferencesStore.reminderEnabled.first(),
            reminderHour = preferencesStore.reminderHour.first(),
            memoryEnabled = preferencesStore.memoryEnabled.first(),
            memoryNote = preferencesStore.memoryNote(userId).first(),
        )
        val sessions = sessionRepository.all(userId).map { reframe ->
            ExportReframeSession(
                session = reframe,
                messages = sessionRepository.messages(reframe.id),
            )
        }
        return ExportSnapshot(
            profile = exportProfile,
            checkIns = checkInRepository.observeAll(userId).first(),
            reframeSessions = sessions,
            journalEntries = journalRepository.observeAll(userId).first(),
            microActions = microActionRepository.observeAll(userId).first(),
            sessionSummaries = sessionSummaryRepository.observe(userId).first(),
            trustedContacts = trustedContactRepository.observe(userId).first(),
            safetyEvents = safetyRepository.all(userId),
            consentRecords = consentRepository.observeAll(userId).first(),
        )
    }

    fun buildJson(snapshot: ExportSnapshot, exportedAtMillis: Long): String {
        val root = jsonObject(
            "format" to FORMAT_V1,
            "exportedAt" to formatIsoUtc(exportedAtMillis),
            "app" to jsonObject(
                "versionName" to appVersionName,
            ),
            "profile" to snapshot.profile?.let(::encodeProfile),
            "checkIns" to snapshot.checkIns
                .sortedWith(compareBy<CheckIn>({ it.createdAt }, { it.id }))
                .map(::encodeCheckIn),
            "reframeSessions" to snapshot.reframeSessions
                .sortedWith(compareBy<ExportReframeSession>({ it.session.startedAt }, { it.session.id }))
                .map(::encodeReframeSession),
            "journalEntries" to snapshot.journalEntries
                .sortedWith(compareBy<JournalEntry>({ it.createdAt }, { it.id }))
                .map(::encodeJournalEntry),
            "microActions" to snapshot.microActions
                .sortedWith(compareBy<MicroAction>({ it.createdAt }, { it.id }))
                .map(::encodeMicroAction),
            "sessionSummaries" to snapshot.sessionSummaries
                .sortedWith(compareBy<SessionSummary>({ it.createdAt }, { it.id }))
                .map(::encodeSessionSummary),
            "trustedContacts" to snapshot.trustedContacts
                .sortedWith(compareBy<TrustedContact>({ it.createdAt }, { it.id }))
                .map(::encodeTrustedContact),
            "safetyEvents" to snapshot.safetyEvents
                .sortedWith(compareBy<SafetyEvent>({ it.createdAt }, { it.id }))
                .map(::encodeSafetyEvent),
            "consentRecords" to snapshot.consentRecords
                .sortedWith(compareBy<ConsentRecord>({ it.grantedAt }, { it.id }))
                .map(::encodeConsentRecord),
        )
        return DeterministicJsonWriter.write(root)
    }

    private fun UserProfile.toExportProfile(
        email: String?,
        language: String,
        reminderEnabled: Boolean,
        reminderHour: Int,
        memoryEnabled: Boolean,
        memoryNote: String,
    ) = ExportProfile(
        displayName = displayName,
        email = email,
        joinedAt = joinedAt,
        language = language,
        reminderEnabled = reminderEnabled,
        reminderHour = reminderHour,
        memoryEnabled = memoryEnabled,
        memoryNote = memoryNote,
    )

    private fun encodeProfile(profile: ExportProfile) = jsonObject(
        "displayName" to profile.displayName,
        "email" to profile.email,
        "joinedAt" to formatIsoUtc(profile.joinedAt),
        "language" to profile.language,
        "reminderEnabled" to profile.reminderEnabled,
        "reminderHour" to profile.reminderHour,
        "memoryEnabled" to profile.memoryEnabled,
        "memoryNote" to profile.memoryNote,
    )

    private fun encodeCheckIn(checkIn: CheckIn) = jsonObject(
        "id" to checkIn.id,
        "emotion" to checkIn.emotion.name,
        "intensity" to checkIn.intensity,
        "note" to checkIn.note,
        "timestamp" to formatIsoUtc(checkIn.createdAt),
    )

    private fun encodeReframeSession(item: ExportReframeSession): JsonObject {
        val session = item.session
        val messages = item.messages
            .sortedWith(compareBy<SessionMessage>({ it.sequence }, { it.createdAt }, { it.id }))
            .map(::encodeSessionMessage)
        return jsonObject(
            "id" to session.id,
            "checkInId" to session.checkInId,
            "currentStep" to session.currentStep,
            "status" to session.status,
            "promptVersion" to session.promptVersion,
            "modelVersion" to session.modelVersion,
            "startedAt" to formatIsoUtc(session.startedAt),
            "completedAt" to session.completedAt?.let(::formatIsoUtc),
            "messages" to messages,
        )
    }

    private fun encodeSessionMessage(message: SessionMessage) = jsonObject(
        "id" to message.id,
        "role" to message.role,
        "content" to message.content,
        "sequence" to message.sequence,
        "timestamp" to formatIsoUtc(message.createdAt),
        "promptVersion" to message.promptVersion,
        "modelVersion" to message.modelVersion,
    )

    private fun encodeJournalEntry(entry: JournalEntry) = jsonObject(
        "id" to entry.id,
        "challenge" to entry.challenge,
        "reframe" to entry.reframe,
        "gratitude" to entry.gratitude,
        "createdAt" to formatIsoUtc(entry.createdAt),
        "updatedAt" to formatIsoUtc(entry.updatedAt),
    )

    private fun encodeMicroAction(action: MicroAction) = jsonObject(
        "id" to action.id,
        "sessionId" to action.sessionId,
        "title" to action.title,
        "minutes" to action.minutes,
        "done" to action.done,
        "snoozedUntil" to action.snoozedUntil?.let(::formatIsoUtc),
        "createdAt" to formatIsoUtc(action.createdAt),
        "completedAt" to action.completedAt?.let(::formatIsoUtc),
    )

    private fun encodeSessionSummary(summary: SessionSummary) = jsonObject(
        "id" to summary.id,
        "sessionId" to summary.sessionId,
        "summary" to summary.summary,
        "createdAt" to formatIsoUtc(summary.createdAt),
        "retentionUntil" to formatIsoUtc(summary.retentionUntil),
    )

    private fun encodeTrustedContact(contact: TrustedContact) = jsonObject(
        "id" to contact.id,
        "name" to contact.name,
        "phone" to contact.phone,
        "relationship" to contact.relationship,
        "createdAt" to formatIsoUtc(contact.createdAt),
    )

    private fun encodeSafetyEvent(event: SafetyEvent) = jsonObject(
        "level" to event.level.name,
        "trigger" to event.trigger,
        "patternId" to event.patternId,
        "timestamp" to formatIsoUtc(event.createdAt),
    )

    private fun encodeConsentRecord(record: ConsentRecord) = jsonObject(
        "id" to record.id,
        "kind" to record.kind,
        "policyVersion" to record.policyVersion,
        "grantedAt" to formatIsoUtc(record.grantedAt),
        "revokedAt" to record.revokedAt?.let(::formatIsoUtc),
    )

    companion object {
        const val FORMAT_V1 = "mindurset-export-v1"

        fun formatIsoUtc(epochMillis: Long): String =
            DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(epochMillis))
    }
}

private class JsonObject(val fields: List<Pair<String, Any?>>)

private fun jsonObject(vararg fields: Pair<String, Any?>): JsonObject = JsonObject(fields.toList())

/**
 * Emits pretty-printed RFC 8259 JSON preserving explicit field order so repeated exports of
 * unchanged data are byte-identical across JVM and Android runtimes.
 */
private object DeterministicJsonWriter {
    fun write(root: JsonObject): String = buildString {
        appendValue(root, indent = 0)
        append('\n')
    }

    private fun StringBuilder.appendValue(value: Any?, indent: Int) {
        when (value) {
            null -> append("null")
            is Boolean -> append(value)
            is Int, is Long -> append(value.toString())
            is String -> appendEscapedString(value)
            is JsonObject -> appendObject(value, indent)
            is List<*> -> appendArray(value, indent)
            else -> error("Unsupported JSON value type: ${value::class.java.name}")
        }
    }

    private fun StringBuilder.appendObject(obj: JsonObject, indent: Int) {
        if (obj.fields.isEmpty()) {
            append("{}")
            return
        }
        append("{\n")
        obj.fields.forEachIndexed { index, (key, value) ->
            appendIndent(indent + 2)
            appendEscapedString(key)
            append(": ")
            appendValue(value, indent + 2)
            if (index < obj.fields.lastIndex) append(',')
            append('\n')
        }
        appendIndent(indent)
        append('}')
    }

    private fun StringBuilder.appendArray(items: List<*>, indent: Int) {
        if (items.isEmpty()) {
            append("[]")
            return
        }
        append("[\n")
        items.forEachIndexed { index, item ->
            appendIndent(indent + 2)
            appendValue(item, indent + 2)
            if (index < items.lastIndex) append(',')
            append('\n')
        }
        appendIndent(indent)
        append(']')
    }

    private fun StringBuilder.appendIndent(spaces: Int) {
        repeat(spaces) { append(' ') }
    }

    private fun StringBuilder.appendEscapedString(value: String) {
        append('"')
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")

                '\\' -> append("\\\\")

                '\b' -> append("\\b")

                '\u000C' -> append("\\f")

                '\n' -> append("\\n")

                '\r' -> append("\\r")

                '\t' -> append("\\t")

                else -> if (ch.code < 0x20) {
                    append("\\u")
                    append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    append(ch)
                }
            }
        }
        append('"')
    }
}
