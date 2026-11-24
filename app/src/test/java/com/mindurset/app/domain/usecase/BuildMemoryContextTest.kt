package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.CheckIn
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.MemorySnapshot
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.model.StructuredFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildMemoryContextTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `when memory is disabled context returns null even when populated`() {
        val snapshot = MemorySnapshot(
            enabled = false,
            facts = StructuredFacts(
                latestCheckIn = CheckIn("c1", "u1", Emotion.ANXIOUS, 7, "private note", now - 3_600_000L),
                chosenPerspective = "One exam is not the whole degree",
                completedActionsCount = 2,
            ),
            summaries = listOf(
                SessionSummary("s1", "u1", "sess-1", "Summary one", now + 10_000L, now - 5_000L),
            ),
            userNote = "Keep suggestions under five minutes.",
        )

        assertNull(BuildMemoryContext.build(snapshot, now))
    }

    @Test
    fun `empty snapshot with memory enabled returns null`() {
        val snapshot = MemorySnapshot(
            enabled = true,
            facts = StructuredFacts(),
            summaries = emptyList(),
            userNote = "   ",
        )

        assertNull(BuildMemoryContext.build(snapshot, now))
    }

    @Test
    fun `layer one formats structured facts and never leaks raw check-in note`() {
        val secretNote = "RAW_PERSONAL_CHECKIN_NOTE_DO_NOT_INJECT"
        val snapshot = MemorySnapshot(
            enabled = true,
            facts = StructuredFacts(
                latestCheckIn = CheckIn("c1", "u1", Emotion.STRESSED, 8, secretNote, now - 2 * 3_600_000L),
                chosenPerspective = "A tough week is data, not a verdict.",
                completedActionsCount = 3,
            ),
            summaries = emptyList(),
            userNote = "",
        )

        val built = requireNotNull(BuildMemoryContext.build(snapshot, now))

        assertTrue(built.contains("State: last_check_in=stressed(8/10, 2h ago)"))
        assertTrue(built.contains("completed_actions=3"))
        assertTrue(built.contains("last_perspective=\"A tough week is data, not a verdict.\""))
        assertFalse(built.contains(secretNote))
    }

    @Test
    fun `layer two caps at three newest non-expired summaries and excludes expired rows`() {
        val summaries = listOf(
            SessionSummary("expired", "u1", "s0", "Expired summary", now - 1L, now - 10_000L),
            SessionSummary("boundary", "u1", "s0b", "Boundary expired", now, now - 9_000L),
            SessionSummary("s1", "u1", "sess-1", "Oldest active", now + 10_000L, now - 4_000L),
            SessionSummary("s2", "u1", "sess-2", "Second active", now + 10_000L, now - 3_000L),
            SessionSummary("s3", "u1", "sess-3", "Third active", now + 10_000L, now - 2_000L),
            SessionSummary("s4", "u1", "sess-4", "Newest active", now + 10_000L, now - 1_000L),
        )
        val snapshot = MemorySnapshot(
            enabled = true,
            facts = StructuredFacts(),
            summaries = summaries,
            userNote = "",
        )

        val built = requireNotNull(BuildMemoryContext.build(snapshot, now))

        assertFalse(built.contains("Expired summary"))
        assertFalse(built.contains("Boundary expired"))
        assertFalse(built.contains("Oldest active"))
        assertTrue(built.contains("- Newest active"))
        assertTrue(built.contains("- Third active"))
        assertTrue(built.contains("- Second active"))
        assertEquals(3, built.lines().count { it.startsWith("- ") })
    }

    @Test
    fun `layer three includes user note and output is clamped to context budget`() {
        val longSummary = "word ".repeat(200)
        val snapshot = MemorySnapshot(
            enabled = true,
            facts = StructuredFacts(
                latestCheckIn = CheckIn("c1", "u1", Emotion.CALM, 4, null, now - 3 * 24 * 3_600_000L),
            ),
            summaries = listOf(
                SessionSummary("s1", "u1", "sess-1", longSummary, now + 10_000L, now - 1_000L),
                SessionSummary("s2", "u1", "sess-2", longSummary, now + 10_000L, now - 500L),
            ),
            userNote = "Prefer short breathing steps.",
        )

        val built = BuildMemoryContext.build(snapshot, now)

        assertNotNull(built)
        assertTrue(built!!.length <= MemoryPolicy.MAX_CONTEXT_LENGTH)
        assertTrue(built.contains("last_check_in=calm(4/10, 3d ago)"))
    }
}
