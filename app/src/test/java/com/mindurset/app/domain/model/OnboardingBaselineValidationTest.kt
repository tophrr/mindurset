package com.mindurset.app.domain.model

import com.mindurset.app.domain.usecase.EntryDestination
import com.mindurset.app.domain.usecase.ResolveEntryDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class OnboardingBaselineValidationTest {
    @Test
    fun `every option set maps to valid enums`() {
        for (mood in Emotion.entries) {
            for (sleep in SleepQuality.entries) {
                for (support in SupportLevel.entries) {
                    for (focus in OnboardingFocus.entries) {
                        assertTrue(ValidateOnboardingBaseline.isComplete(mood, sleep, support, focus))
                        val baseline =
                            ValidateOnboardingBaseline.fromNames(
                                id = "ob-1",
                                userId = "u-1",
                                recentMoodName = mood.name,
                                sleepQualityName = sleep.name,
                                supportLevelName = support.name,
                                primaryFocusName = focus.name,
                                createdAt = 1_700_000_000_000L,
                            )
                        assertNotNull(baseline)
                        assertEquals(mood, baseline?.recentMood)
                        assertEquals(sleep, baseline?.sleepQuality)
                        assertEquals(support, baseline?.supportLevel)
                        assertEquals(focus, baseline?.primaryFocus)
                    }
                }
            }
        }
    }

    @Test
    fun `missing or unknown option names fail validation`() {
        assertFalse(
            ValidateOnboardingBaseline.isComplete(
                null,
                SleepQuality.RESTFUL,
                SupportLevel.SOME,
                OnboardingFocus.DAILY_RHYTHM,
            ),
        )
        assertFalse(
            ValidateOnboardingBaseline.isComplete(Emotion.CALM, null, SupportLevel.SOME, OnboardingFocus.DAILY_RHYTHM),
        )
        assertFalse(
            ValidateOnboardingBaseline.isComplete(
                Emotion.CALM,
                SleepQuality.RESTFUL,
                null,
                OnboardingFocus.DAILY_RHYTHM,
            ),
        )
        assertFalse(ValidateOnboardingBaseline.isComplete(Emotion.CALM, SleepQuality.RESTFUL, SupportLevel.SOME, null))

        assertNull(
            ValidateOnboardingBaseline.fromNames(
                id = "ob-1",
                userId = "u-1",
                recentMoodName = "UNKNOWN",
                sleepQualityName = SleepQuality.RESTFUL.name,
                supportLevelName = SupportLevel.SOME.name,
                primaryFocusName = OnboardingFocus.DAILY_RHYTHM.name,
                createdAt = 1_700_000_000_000L,
            ),
        )
        assertNull(
            ValidateOnboardingBaseline.fromNames(
                id = "ob-1",
                userId = "u-1",
                recentMoodName = Emotion.CALM.name,
                sleepQualityName = "BAD_SLEEP",
                supportLevelName = SupportLevel.SOME.name,
                primaryFocusName = OnboardingFocus.DAILY_RHYTHM.name,
                createdAt = 1_700_000_000_000L,
            ),
        )
        assertNull(
            ValidateOnboardingBaseline.fromNames(
                id = "ob-1",
                userId = "u-1",
                recentMoodName = Emotion.CALM.name,
                sleepQualityName = SleepQuality.RESTFUL.name,
                supportLevelName = null,
                primaryFocusName = OnboardingFocus.DAILY_RHYTHM.name,
                createdAt = 1_700_000_000_000L,
            ),
        )
        assertNull(
            ValidateOnboardingBaseline.fromNames(
                id = "ob-1",
                userId = "u-1",
                recentMoodName = Emotion.CALM.name,
                sleepQualityName = SleepQuality.RESTFUL.name,
                supportLevelName = SupportLevel.SOME.name,
                primaryFocusName = "",
                createdAt = 1_700_000_000_000L,
            ),
        )
    }

    @Test
    fun `summary key follows each emotion uniquely`() {
        val mapped = Emotion.entries.map { OnboardingSummaryKey.fromEmotion(it) }
        assertEquals(Emotion.entries.size, mapped.toSet().size)
    }

    @Test
    fun `returning user with onboardingCompleted flag set never sees onboarding`() {
        assertEquals(
            EntryDestination.DASHBOARD,
            ResolveEntryDestination.resolve(hasSession = true, hasConsent = true, onboardingCompleted = true),
        )
        assertEquals(
            EntryDestination.ONBOARDING,
            ResolveEntryDestination.resolve(hasSession = true, hasConsent = true, onboardingCompleted = false),
        )
        assertEquals(
            EntryDestination.CONSENT,
            ResolveEntryDestination.resolve(hasSession = true, hasConsent = false, onboardingCompleted = false),
        )
        assertEquals(
            EntryDestination.CONSENT,
            ResolveEntryDestination.resolve(hasSession = true, hasConsent = false, onboardingCompleted = true),
        )
        assertEquals(
            EntryDestination.LOGIN,
            ResolveEntryDestination.resolve(hasSession = false, hasConsent = true, onboardingCompleted = true),
        )
    }

    @Test
    fun `onboarding copy exists in both locales without scores or clinical claims`() {
        val resDir = listOf(File("src/main/res"), File("app/src/main/res")).first { it.isDirectory }
        val en = parseStrings(File(resDir, "values/strings_onboarding.xml"))
        val id = parseStrings(File(resDir, "values-in/strings_onboarding.xml"))

        assertEquals(en.keys, id.keys)
        en.forEach { (key, enValue) ->
            val idValue = id[key].orEmpty()
            assertTrue("blank English string for $key", enValue.isNotBlank())
            assertTrue("blank Indonesian string for $key", idValue.isNotBlank())
            assertNotEquals("Indonesian string for $key must be translated", enValue, idValue)
            assertFalse("unexpected score wording in $key", enValue.contains("score", ignoreCase = true))
            assertFalse("unexpected score wording in $key", idValue.contains("skor", ignoreCase = true))
        }

        val summaryKeys = listOf(
            "onboarding_done_summary_happy",
            "onboarding_done_summary_calm",
            "onboarding_done_summary_low",
            "onboarding_done_summary_anxious",
            "onboarding_done_summary_stressed",
        )
        assertEquals(5, summaryKeys.map { en.getValue(it) }.toSet().size)
        assertEquals(5, summaryKeys.map { id.getValue(it) }.toSet().size)
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
