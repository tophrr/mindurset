package com.mindurset.app.domain.usecase

import com.mindurset.app.data.repository.AssetOfflineExerciseLibrary
import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.OfflineExerciseTopics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OfflineExerciseLibraryTest {
    private val assetsDir: File by lazy {
        listOf(
            File("src/main/assets"),
            File("app/src/main/assets"),
        ).firstOrNull { it.isDirectory }
            ?: error("Could not locate app/src/main/assets from ${File("").absolutePath}")
    }

    private val library = AssetOfflineExerciseLibrary { path ->
        val file = File(assetsDir, path)
        if (file.isFile) file.readText(Charsets.UTF_8) else null
    }

    @Test
    fun loadsAllSevenEnglishModulesWithSectionHeadings() {
        val exercises = library.loadAll("en")

        assertEquals(OfflineExerciseTopics.ALL, exercises.map { it.topic })
        exercises.forEach { exercise ->
            assertEquals("en", exercise.language)
            assertTrue("${exercise.topic} should have a title", exercise.title.isNotBlank())
            assertTrue("${exercise.topic} should have keywords", exercise.keywords.isNotEmpty())
            assertEquals("${exercise.topic} should keep all 3 sections", 3, exercise.sections.size)
            exercise.sections.forEach { section ->
                assertTrue(section.heading.isNotBlank())
                assertTrue(section.body.isNotBlank())
            }
        }
    }

    @Test
    fun indonesianLoadsTranslatedModulesAndFallsBackToEnglish() {
        val exercises = library.loadAll("in").associateBy { it.topic }

        assertEquals("id", exercises.getValue("stress").language)
        assertEquals("Stres", exercises.getValue("stress").title)
        assertEquals("id", exercises.getValue("sleep").language)
        assertEquals("Tidur", exercises.getValue("sleep").title)
        assertEquals("id", exercises.getValue("self-compassion").language)
        assertEquals("Welas asih diri", exercises.getValue("self-compassion").title)

        listOf("action", "all-or-nothing", "rumination", "safety-plan").forEach { topic ->
            assertEquals("$topic should fall back to English", "en", exercises.getValue(topic).language)
        }
    }

    @Test
    fun selectsDeterministicallyFromMessageKeywordsAcrossLanguages() {
        val exercises = library.loadAll("en")

        assertEquals("stress", SelectOfflineExercise.select(exercises, "I am overwhelmed by this exam deadline")?.topic)
        assertEquals("sleep", SelectOfflineExercise.select(exercises, "Aku begadang lagi dan susah tidur")?.topic)
        assertEquals(
            "all-or-nothing",
            SelectOfflineExercise.select(exercises, "I always fail and it is never enough")?.topic,
        )
        assertEquals("rumination", SelectOfflineExercise.select(exercises, "My mind keeps replaying in a loop")?.topic)
        assertEquals("action", SelectOfflineExercise.select(exercises, "I keep procrastinating and feel stuck")?.topic)
        assertEquals(
            "self-compassion",
            SelectOfflineExercise.select(exercises, "I feel full of shame right now")?.topic,
        )
        assertEquals(
            "safety-plan",
            SelectOfflineExercise.select(exercises, "I feel alone and need emergency support")?.topic,
        )
    }

    @Test
    fun fallsBackToCheckInEmotionAndThenModeWhenMessageHasNoKeywords() {
        val exercises = library.loadAll("en")

        assertEquals("stress", SelectOfflineExercise.select(exercises, "hello", Emotion.STRESSED)?.topic)
        assertEquals("rumination", SelectOfflineExercise.select(exercises, "hello", Emotion.ANXIOUS)?.topic)
        assertEquals("self-compassion", SelectOfflineExercise.select(exercises, "hello", Emotion.LOW)?.topic)
        assertEquals("action", SelectOfflineExercise.select(exercises, "hello", Emotion.CALM)?.topic)
        assertEquals(
            "all-or-nothing",
            SelectOfflineExercise.select(exercises, "hello", emotion = null, fallbackTopic = "all-or-nothing")?.topic,
        )
    }

    @Test
    fun selectionIsDeterministicAcrossRepeatedCalls() {
        val exercises = library.loadAll("id")
        val first = SelectOfflineExercise.select(exercises, "beban ujian berat", Emotion.LOW, "stress")
        val second = SelectOfflineExercise.select(exercises, "beban ujian berat", Emotion.LOW, "stress")

        assertEquals(first, second)
        assertEquals("stress", first?.topic)
    }
}
