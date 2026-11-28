package com.mindurset.app.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mindurset.app.domain.model.Emotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CheckInDraftStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `full draft round-trips through serialised form`() {
        val original = CheckInDraft(
            emotion = Emotion.ANXIOUS,
            intensity = 8,
            note = "Tight chest before \"presentation\"\nNeed air",
            updatedAtMillis = 1_700_000_123_456L,
        )

        val restored = CheckInDraft.fromJson(original.toJson())

        assertEquals(original, restored)
    }

    @Test
    fun `empty draft round-trips through serialised form`() {
        val empty = CheckInDraft()

        val restored = CheckInDraft.fromJson(empty.toJson())

        assertEquals(empty, restored)
        assertNull(restored?.emotion)
        assertEquals(CheckInDraft.DEFAULT_INTENSITY, restored?.intensity)
        assertEquals("", restored?.note)
    }

    @Test
    fun `emotion-only draft round-trips without inventing note or default LOW`() {
        val emotionOnly = CheckInDraft(emotion = Emotion.HAPPY)

        val restored = CheckInDraft.fromJson(emotionOnly.toJson())

        assertEquals(emotionOnly, restored)
        assertEquals(Emotion.HAPPY, restored?.emotion)
        assertEquals("", restored?.note)
    }

    @Test
    fun `blank or malformed serialised input returns null without crashing`() {
        assertNull(CheckInDraft.fromJson(""))
        assertNull(CheckInDraft.fromJson("   "))
        assertNull(CheckInDraft.fromJson("{not-json"))
    }

    @Test
    fun `unknown emotion name deserialises as null instead of falling back to LOW`() {
        val raw = JSONObject()
            .put("emotion", "UNKNOWN_MOOD")
            .put("intensity", 4)
            .put("note", "hello")
            .put("updated_at", 1_700_000_000_000L)
            .toString()

        val restored = CheckInDraft.fromJson(raw)

        assertNull(restored?.emotion)
        assertEquals(4, restored?.intensity)
        assertEquals("hello", restored?.note)
    }

    @Test
    fun `out-of-range intensity is clamped on read rather than crashing`() {
        val tooLow = JSONObject()
            .put("intensity", -4)
            .put("updated_at", 1_700_000_000_000L)
            .toString()
        val tooHigh = JSONObject()
            .put("intensity", 99)
            .put("updated_at", 1_700_000_000_000L)
            .toString()
        val nonNumeric = JSONObject()
            .put("intensity", "high")
            .put("updated_at", 1_700_000_000_000L)
            .toString()

        assertEquals(CheckInDraft.MIN_INTENSITY, CheckInDraft.fromJson(tooLow)?.intensity)
        assertEquals(CheckInDraft.MAX_INTENSITY, CheckInDraft.fromJson(tooHigh)?.intensity)
        assertEquals(CheckInDraft.DEFAULT_INTENSITY, CheckInDraft.fromJson(nonNumeric)?.intensity)
    }

    @Test
    fun `7-day expiry rule keeps fresh draft and drops draft older than 7 days`() {
        val savedAt = 1_700_000_000_000L
        val sevenDays = CheckInDraft.MAX_AGE_MILLIS

        assertFalse("6 days old stays fresh", isCheckInDraftExpired(savedAt, savedAt + 6L * 24 * 60 * 60 * 1000))
        assertFalse(
            "boundary: exact 7 days is not older than 7 days",
            isCheckInDraftExpired(
                savedAt,
                savedAt + sevenDays,
            ),
        )
        assertTrue(
            "boundary: 7 days plus 1 ms is expired",
            isCheckInDraftExpired(savedAt, savedAt + sevenDays + 1L),
        )
        assertTrue("8 days old is expired", isCheckInDraftExpired(savedAt, savedAt + 8L * 24 * 60 * 60 * 1000))
    }

    @Test
    fun `datastore store persists loads and clears draft`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { tempFolder.newFile("draft.preferences_pb") },
            )
            val store = DataStoreCheckInDraftStore(ds)
            val draft = CheckInDraft(
                emotion = Emotion.STRESSED,
                intensity = 9,
                note = "long day",
                updatedAtMillis = 1_700_000_000_000L,
            )

            store.save(draft)
            assertEquals(draft, store.load(1_700_000_001_000L))

            store.clear()
            assertNull(store.load(1_700_000_001_000L))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `datastore store ignores and clears expired draft on read`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { tempFolder.newFile("expired_draft.preferences_pb") },
            )
            val store = DataStoreCheckInDraftStore(ds)
            val savedAt = 1_700_000_000_000L
            store.save(
                CheckInDraft(
                    emotion = Emotion.LOW,
                    intensity = 6,
                    note = "old note",
                    updatedAtMillis = savedAt,
                ),
            )

            val expiredNow = savedAt + CheckInDraft.MAX_AGE_MILLIS + 1L
            assertNull(store.load(expiredNow))
            assertFalse(ds.data.first().contains(stringPreferencesKey("checkin_draft")))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `datastore store clears corrupt payload on read`() = runTest {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val ds = PreferenceDataStoreFactory.create(
                scope = scope,
                produceFile = { tempFolder.newFile("corrupt_draft.preferences_pb") },
            )
            val key = stringPreferencesKey("checkin_draft")
            ds.edit { it[key] = "{corrupt" }
            val store = DataStoreCheckInDraftStore(ds)

            assertNull(store.load(1_700_000_000_000L))
            assertFalse(ds.data.first().contains(key))
        } finally {
            scope.cancel()
        }
    }
}
