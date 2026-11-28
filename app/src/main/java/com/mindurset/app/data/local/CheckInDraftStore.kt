package com.mindurset.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mindurset.app.domain.model.Emotion
import kotlinx.coroutines.flow.first
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

private val Context.checkInDraftDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "checkin_draft",
)

/**
 * Uncommitted check-in form state persisted locally so it survives process death.
 */
data class CheckInDraft(
    val emotion: Emotion? = null,
    val intensity: Int = DEFAULT_INTENSITY,
    val note: String = "",
    val updatedAtMillis: Long = 0L,
) {
    /** Serialises this draft into a compact JSON string for DataStore persistence. */
    fun toJson(): String = JSONObject().apply {
        if (emotion != null) put(KEY_EMOTION, emotion.name)
        put(KEY_INTENSITY, intensity.coerceIn(MIN_INTENSITY, MAX_INTENSITY))
        put(KEY_NOTE, note)
        put(KEY_UPDATED_AT, updatedAtMillis)
    }.toString()

    companion object {
        const val MIN_INTENSITY = 1
        const val MAX_INTENSITY = 10
        const val DEFAULT_INTENSITY = 5
        const val MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

        private const val KEY_EMOTION = "emotion"
        private const val KEY_INTENSITY = "intensity"
        private const val KEY_NOTE = "note"
        private const val KEY_UPDATED_AT = "updated_at"

        /**
         * Parses a serialised draft, clamping out-of-range intensity values instead of throwing.
         * Returns null when the input is blank or malformed.
         */
        fun fromJson(raw: String): CheckInDraft? {
            if (raw.isBlank()) return null
            return try {
                val obj = JSONObject(raw)
                val emotion = if (obj.isNull(KEY_EMOTION)) {
                    null
                } else {
                    val name = obj.optString(KEY_EMOTION, "")
                    Emotion.entries.firstOrNull { it.name == name }
                }
                val rawIntensity = obj.optInt(KEY_INTENSITY, DEFAULT_INTENSITY)
                val intensity = rawIntensity.coerceIn(MIN_INTENSITY, MAX_INTENSITY)
                val note = if (obj.isNull(KEY_NOTE)) "" else obj.optString(KEY_NOTE, "")
                val updatedAt = obj.optLong(KEY_UPDATED_AT, 0L)
                CheckInDraft(
                    emotion = emotion,
                    intensity = intensity,
                    note = note,
                    updatedAtMillis = updatedAt,
                )
            } catch (_: JSONException) {
                null
            }
        }
    }
}

/** Returns true when a draft timestamp is older than 7 days relative to [nowMillis]. */
fun isCheckInDraftExpired(updatedAtMillis: Long, nowMillis: Long): Boolean =
    nowMillis - updatedAtMillis > CheckInDraft.MAX_AGE_MILLIS

/** Local store for an in-progress check-in draft. */
interface CheckInDraftStore {
    suspend fun load(nowMillis: Long): CheckInDraft?

    suspend fun save(draft: CheckInDraft)

    suspend fun clear()
}

/**
 * DataStore-backed [CheckInDraftStore] using a dedicated `checkin_draft` preferences file.
 * Draft content is never logged (NFR-08).
 */
class DataStoreCheckInDraftStore internal constructor(private val dataStore: DataStore<Preferences>) :
    CheckInDraftStore {
    constructor(context: Context) : this(context.applicationContext.checkInDraftDataStore)

    override suspend fun load(nowMillis: Long): CheckInDraft? {
        val raw = try {
            dataStore.data.first()[KEY_DRAFT]
        } catch (_: IOException) {
            null
        } ?: return null

        val draft = CheckInDraft.fromJson(raw)
        if (draft == null || isCheckInDraftExpired(draft.updatedAtMillis, nowMillis)) {
            clear()
            return null
        }
        return draft
    }

    override suspend fun save(draft: CheckInDraft) {
        dataStore.edit { prefs ->
            prefs[KEY_DRAFT] = draft.toJson()
        }
    }

    override suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_DRAFT)
        }
    }

    private companion object {
        val KEY_DRAFT = stringPreferencesKey("checkin_draft")
    }
}
