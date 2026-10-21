package com.mindurset.app.domain.usecase

import com.mindurset.app.safety.CrisisResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAndCrisisCopyTest {
    @Test
    fun `the reminder copy is non-punitive with and without a name`() {
        listOf(
            DraftNotificationText.draft(null),
            DraftNotificationText.draft("  "),
            DraftNotificationText.draft("Ana"),
        ).forEach { text ->
            assertTrue("punitive copy: $text", DraftNotificationText.isNonPunitive(text))
            assertTrue("does not invite a check-in: $text", text.contains("Check in"))
        }
    }

    @Test
    fun `the reminder greets the person by name when there is one`() {
        assertTrue(DraftNotificationText.draft("Ana").startsWith("Ana,"))
    }

    @Test
    fun `the detector recognises loss framing`() {
        assertFalse(DraftNotificationText.isNonPunitive("You are on a 3 day streak"))
        assertFalse(DraftNotificationText.isNonPunitive("You missed 4 days"))
        assertFalse(DraftNotificationText.isNonPunitive("don't break the chain"))
        assertFalse(DraftNotificationText.isNonPunitive("kamu kehilangan streak"))
    }

    @Test
    fun `crisis resources carry the national line`() {
        assertEquals("119", CrisisResources.PHONE)
        assertEquals("8", CrisisResources.EXTENSION)
        assertEquals("119 ext. 8", CrisisResources.display())
        assertEquals(CrisisResources.display(), CrisisResources.LABEL)
    }
}
