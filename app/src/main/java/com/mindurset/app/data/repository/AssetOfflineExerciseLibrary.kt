package com.mindurset.app.data.repository

import android.content.res.AssetManager
import com.mindurset.app.domain.model.OfflineExercise
import com.mindurset.app.domain.model.OfflineExerciseTopics
import com.mindurset.app.domain.repository.OfflineExerciseLibrary
import com.mindurset.app.domain.usecase.OfflineExerciseParser
import java.util.concurrent.ConcurrentHashMap

class AssetOfflineExerciseLibrary(private val readAsset: (String) -> String?) : OfflineExerciseLibrary {
    constructor(assets: AssetManager) : this({ path ->
        runCatching {
            assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrNull()
    })

    private val cache = ConcurrentHashMap<String, List<OfflineExercise>>()

    override fun loadAll(language: String): List<OfflineExercise> {
        val normalized = normalizeLanguage(language)
        return cache.getOrPut(normalized) {
            OfflineExerciseTopics.ALL.mapNotNull { topic -> loadTopic(topic, normalized) }
        }
    }

    private fun loadTopic(topic: String, language: String): OfflineExercise? {
        if (language == INDONESIAN) {
            val localized = readAsset("exercises/id/$topic.md")
            if (localized != null) {
                return OfflineExerciseParser.parse(topic = topic, markdown = localized, language = INDONESIAN)
            }
        }
        val english = readAsset("exercises/$topic.md") ?: return null
        return OfflineExerciseParser.parse(topic = topic, markdown = english, language = ENGLISH)
    }

    companion object {
        private const val ENGLISH = "en"
        private const val INDONESIAN = "id"

        fun normalizeLanguage(language: String): String =
            if (language.lowercase() in setOf("id", "in")) INDONESIAN else ENGLISH
    }
}
