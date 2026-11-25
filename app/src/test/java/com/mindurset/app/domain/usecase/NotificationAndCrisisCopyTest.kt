package com.mindurset.app.domain.usecase

import com.mindurset.app.safety.CrisisResources
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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

    @Test
    fun `third-party crisis copy is defined, non-empty, and translated in both locales`() {
        val resDir = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val en = parseStrings(File(resDir, "values/strings_crisis.xml"))
        val id = parseStrings(File(resDir, "values-in/strings_crisis.xml"))

        listOf(
            "crisis_third_party_title",
            "crisis_third_party_body",
            "crisis_third_party_note",
        ).forEach { key ->
            val enValue = en[key]?.trim().orEmpty()
            val idValue = id[key]?.trim().orEmpty()
            assertTrue("missing or blank English string for $key", enValue.isNotEmpty())
            assertTrue("missing or blank Indonesian string for $key", idValue.isNotEmpty())
            assertNotEquals("Indonesian string for $key must not equal English", enValue, idValue)
        }
    }

    private fun parseStrings(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val nodes = doc.getElementsByTagName("string")
        return buildMap {
            for (i in 0 until nodes.length) {
                val element = nodes.item(i) as Element
                put(element.getAttribute("name"), element.textContent)
            }
        }
    }
}
