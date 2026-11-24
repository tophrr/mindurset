package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.OfflineExercise
import com.mindurset.app.domain.model.OfflineExerciseSection

/**
 * Parses a bundled corpus markdown module into an [OfflineExercise], preserving `#` title,
 * `<!-- keywords: ... -->` metadata, and `##` section headings.
 */
object OfflineExerciseParser {
    private val keywordsPattern = Regex("<!--\\s*keywords:\\s*(.*?)\\s*-->", RegexOption.IGNORE_CASE)
    private val commentPattern = Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL))

    fun parse(topic: String, markdown: String, language: String = "en"): OfflineExercise {
        val keywords = parseKeywords(markdown)
        val body = commentPattern.replace(markdown, "")
        val lines = body.split(Regex("\\r?\\n"))

        var title: String? = null
        var currentHeading: String? = null
        val buffer = mutableListOf<String>()
        val sections = mutableListOf<OfflineExerciseSection>()

        fun flushSection() {
            val heading = currentHeading ?: return
            val prose = normalizeParagraphs(buffer)
            if (prose.isNotEmpty()) {
                sections += OfflineExerciseSection(heading = heading, body = prose)
            }
            buffer.clear()
        }

        for (rawLine in lines) {
            val line = rawLine.trimEnd()
            when {
                line.startsWith("# ") && title == null -> {
                    title = line.removePrefix("# ").trim()
                }

                line.startsWith("## ") -> {
                    flushSection()
                    currentHeading = line.removePrefix("## ").trim()
                }

                currentHeading != null -> {
                    buffer += line
                }
            }
        }
        flushSection()

        val resolvedTitle = title?.takeIf { it.isNotEmpty() } ?: topic
        return OfflineExercise(
            topic = topic,
            title = resolvedTitle,
            keywords = keywords,
            sections = sections,
            language = language,
        )
    }

    private fun parseKeywords(markdown: String): List<String> {
        val match = keywordsPattern.find(markdown) ?: return emptyList()
        return match.groupValues[1]
            .split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
    }

    // Corpus markdown is hard-wrapped at ~100 columns; unwrap single line breaks so Compose
    // wraps cleanly across narrow phone screens while keeping blank-line paragraph breaks.
    private fun normalizeParagraphs(lines: List<String>): String {
        val paragraphs = mutableListOf<String>()
        val current = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (current.isNotEmpty()) {
                    paragraphs += current.joinToString(" ")
                    current.clear()
                }
            } else {
                current += trimmed
            }
        }
        if (current.isNotEmpty()) {
            paragraphs += current.joinToString(" ")
        }
        return paragraphs.joinToString("\n\n")
    }
}
