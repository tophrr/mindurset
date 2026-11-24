package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.Emotion
import com.mindurset.app.domain.model.OfflineExercise
import java.text.Normalizer

/**
 * Picks one bundled CBT exercise deterministically from the user's message, falling back to their
 * latest check-in [Emotion] and finally [fallbackTopic] when the message shares no corpus tokens.
 */
object SelectOfflineExercise {
    private val diacritics = Regex("\\p{Mn}+")
    private val nonWord = Regex("[^a-z0-9]+")

    private val stopwords = setOf(
        "the", "and", "for", "are", "but", "not", "you", "your", "yours", "with", "this", "that",
        "these", "those", "from", "have", "has", "had", "was", "were", "will", "would", "can",
        "could", "should", "about", "into", "over", "under", "just", "very", "too", "all", "any",
        "how", "what", "when", "where", "why", "who", "its", "my", "me", "we", "our", "they",
        "them", "their", "been", "being", "does", "did", "than", "then", "there", "here", "out",
        "off", "yang", "dan", "atau", "untuk", "dengan", "saya", "aku", "kamu", "dia", "ini",
        "itu", "tidak", "nggak", "gak", "dari", "pada", "juga", "sudah", "masih", "akan", "bisa",
        "ada", "adalah", "karena", "kalau", "jadi", "lagi", "saja", "banget",
    )

    private val synonymGroups = listOf(
        listOf("stress", "stressed", "stres", "tekanan", "pressure", "overwhelmed", "kewalahan", "beban", "burden"),
        listOf("anxious", "anxiety", "cemas", "kecemasan", "worried", "worry", "khawatir", "takut", "panic", "panik"),
        listOf("sad", "sadness", "sedih", "down", "low", "mood", "murung"),
        listOf("fail", "failure", "failed", "gagal", "kegagalan", "not good enough", "tidak cukup"),
        listOf(
            "exam", "exams", "ujian", "test", "tests", "paper", "papers",
            "deadline", "deadlines", "tugas", "assignment",
        ),
        listOf(
            "sleep", "sleeping", "tidur", "insomnia", "sleepless", "begadang",
            "night", "malam", "tired", "lelah", "capek", "mengantuk",
        ),
        listOf(
            "think", "thinking", "thought", "thoughts", "overthinking", "rumination",
            "ruminating", "overpikir", "mikir", "loop", "replay",
        ),
        listOf(
            "action", "step", "steps", "tindakan", "langkah", "start", "starting",
            "mulai", "procrastination", "menunda", "stuck", "macet",
        ),
        listOf(
            "kind", "compassion", "self-compassion", "baik", "self-worth", "harga diri",
            "shame", "malu", "friend", "teman", "welas asih",
        ),
        listOf(
            "safe", "safety", "crisis", "krisis", "danger", "bahaya",
            "emergency", "darurat", "alone", "sendirian", "bantuan", "support",
        ),
        listOf("perfect", "perfectionism", "sempurna", "always", "never", "selalu", "tidak pernah", "all-or-nothing"),
    )

    private val synonymLookup: Map<String, List<String>> = buildMap {
        for (group in synonymGroups) {
            for (word in group) {
                put(word, group)
            }
        }
    }

    fun select(
        exercises: List<OfflineExercise>,
        message: String,
        emotion: Emotion? = null,
        fallbackTopic: String? = null,
    ): OfflineExercise? {
        if (exercises.isEmpty()) return null

        val rawQueryTokens = tokenize(message).distinct()
        val normalizedMessage = normalize(message).trim()

        if (rawQueryTokens.isNotEmpty() || normalizedMessage.length >= 4) {
            val best = exercises
                .map { exercise -> exercise to score(exercise, rawQueryTokens, normalizedMessage) }
                .filter { (_, points) -> points > 0 }
                .sortedWith(compareByDescending<Pair<OfflineExercise, Int>> { it.second }.thenBy { it.first.topic })
                .firstOrNull()
                ?.first
            if (best != null) return best
        }

        val emotionTopic = emotion?.let(::topicForEmotion)
        if (emotionTopic != null) {
            exercises.firstOrNull { it.topic == emotionTopic }?.let { return it }
        }

        if (fallbackTopic != null) {
            exercises.firstOrNull { it.topic == fallbackTopic }?.let { return it }
        }

        return exercises.firstOrNull { it.topic == "stress" } ?: exercises.first()
    }

    fun topicForEmotion(emotion: Emotion): String = when (emotion) {
        Emotion.STRESSED -> "stress"
        Emotion.ANXIOUS -> "rumination"
        Emotion.LOW -> "self-compassion"
        Emotion.CALM -> "action"
        Emotion.HAPPY -> "action"
    }

    private fun score(exercise: OfflineExercise, rawQueryTokens: List<String>, normalizedMessage: String): Int {
        val keywordTokens = expandedTokenSet(exercise.keywords.joinToString(" "))
        val headingTokens = expandedTokenSet(
            buildString {
                append(exercise.title)
                for (section in exercise.sections) {
                    append(' ').append(section.heading)
                }
            },
        )
        val bodyTokens = expandedTokenSet(exercise.sections.joinToString(" ") { it.body })

        // Match each distinct query token once per field so larger synonym groups do not outweigh smaller ones.
        var points = 0
        for (token in rawQueryTokens) {
            val variants = synonymLookup[token] ?: listOf(token)
            if (variants.any { it in keywordTokens }) points += 3
            if (variants.any { it in headingTokens }) points += 2
            if (variants.any { it in bodyTokens }) points += 1
        }

        if (normalizedMessage.length >= 4) {
            for (keyword in exercise.keywords) {
                val normalizedKeyword = normalize(keyword)
                if (normalizedKeyword.contains(' ') && normalizedKeyword in normalizedMessage) {
                    points += 4
                }
            }
        }
        return points
    }

    private fun expandedTokenSet(value: String): Set<String> = buildSet {
        for (token in tokenize(value)) {
            val variants = synonymLookup[token] ?: listOf(token)
            addAll(variants)
        }
    }

    private fun tokenize(value: String): List<String> = normalize(value)
        .split(nonWord)
        .filter { it.length > 2 && it !in stopwords }

    private fun normalize(value: String): String =
        diacritics.replace(Normalizer.normalize(value.lowercase(), Normalizer.Form.NFD), "")
}
