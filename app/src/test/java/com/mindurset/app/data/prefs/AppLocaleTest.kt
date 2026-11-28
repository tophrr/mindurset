package com.mindurset.app.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLocaleTest {
    @Test
    fun `resolveLocaleTag maps system, English, and Indonesian values`() {
        assertNull(AppLocale.resolveLocaleTag("system"))
        assertNull(AppLocale.resolveLocaleTag(" SYSTEM "))
        assertEquals("en", AppLocale.resolveLocaleTag("en"))
        assertEquals("en", AppLocale.resolveLocaleTag(" EN "))
        assertEquals("id", AppLocale.resolveLocaleTag("id"))
        assertEquals("id", AppLocale.resolveLocaleTag("in"))
        assertEquals("id", AppLocale.resolveLocaleTag(" ID "))
    }

    @Test
    fun `resolveLocaleTag falls back to system locale for unsupported or blank tags`() {
        assertNull(AppLocale.resolveLocaleTag("fr"))
        assertNull(AppLocale.resolveLocaleTag("es"))
        assertNull(AppLocale.resolveLocaleTag("ja"))
        assertNull(AppLocale.resolveLocaleTag("en-GB"))
        assertNull(AppLocale.resolveLocaleTag("unknown"))
        assertNull(AppLocale.resolveLocaleTag(""))
        assertNull(AppLocale.resolveLocaleTag("   "))
        assertNull(AppLocale.resolveLocaleTag(null))
    }

    @Test
    fun `normalizeStoredLanguage normalizes supported tags and falls back to system`() {
        assertEquals("system", AppLocale.normalizeStoredLanguage("system"))
        assertEquals("en", AppLocale.normalizeStoredLanguage("en"))
        assertEquals("id", AppLocale.normalizeStoredLanguage("id"))
        assertEquals("id", AppLocale.normalizeStoredLanguage("in"))
        assertEquals("system", AppLocale.normalizeStoredLanguage("de"))
        assertEquals("system", AppLocale.normalizeStoredLanguage(""))
        assertEquals("system", AppLocale.normalizeStoredLanguage(null))
    }

    @Test
    fun `supported tags match packaged locales`() {
        assertEquals(setOf("en", "id"), AppLocale.SUPPORTED_TAGS)
    }
}
