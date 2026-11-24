package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.MemoryPolicy
import com.mindurset.app.domain.model.MemorySnapshot
import com.mindurset.app.domain.model.SessionSummary
import com.mindurset.app.domain.model.StructuredFacts

/**
 * Builds the three-layer contextual memory string from structured facts, up to three non-expired
 * session summaries, and the user's own memory note (proposal Sec. 9.3).
 *
 * Returns null whenever memory is turned off or when nothing non-expired is available, so the
 * client behaves identically to a first-time session.
 */
object BuildMemoryContext {
    private const val MAX_PERSPECTIVE_LENGTH = 140
    private const val MINUTE_MILLIS = 60_000L
    private const val HOUR_MILLIS = 60L * MINUTE_MILLIS
    private const val DAY_MILLIS = 24L * HOUR_MILLIS

    fun build(snapshot: MemorySnapshot, now: Long): String? {
        if (!snapshot.enabled) return null

        val sections = buildList {
            buildStateLine(snapshot.facts, now)?.let(::add)

            val recentSummaries = snapshot.summaries
                .asSequence()
                .filter { it.retentionUntil > now && it.summary.isNotBlank() }
                .sortedWith(compareByDescending<SessionSummary> { it.createdAt }.thenByDescending { it.id })
                .take(MemoryPolicy.MAX_SUMMARIES)
                .map { it.summary.collapseWhitespace().take(MemoryPolicy.MAX_SUMMARY_LENGTH) }
                .filter { it.isNotEmpty() }
                .toList()
            if (recentSummaries.isNotEmpty()) {
                add("Recent sessions:\n" + recentSummaries.joinToString("\n") { "- $it" })
            }

            val cleanNote = snapshot.userNote.collapseWhitespace().take(MemoryPolicy.MAX_USER_NOTE_LENGTH)
            if (cleanNote.isNotEmpty()) {
                add("User note: $cleanNote")
            }
        }

        if (sections.isEmpty()) return null
        val combined = sections.joinToString("\n")
        return if (combined.length <= MemoryPolicy.MAX_CONTEXT_LENGTH) {
            combined
        } else {
            combined.take(MemoryPolicy.MAX_CONTEXT_LENGTH)
        }
    }

    /** Formats Layer 1 structured facts without ever reading raw free-text check-in notes. */
    fun buildStateLine(facts: StructuredFacts, now: Long): String? {
        val parts = buildList {
            facts.latestCheckIn?.let { checkIn ->
                val age = formatRelativeAge(now - checkIn.createdAt)
                add("last_check_in=${checkIn.emotion.name.lowercase()}(${checkIn.intensity}/10, $age)")
            }
            if (facts.completedActionsCount > 0) {
                add("completed_actions=${facts.completedActionsCount}")
            }
            facts.chosenPerspective
                ?.collapseWhitespace()
                ?.takeIf { it.isNotEmpty() }
                ?.take(MAX_PERSPECTIVE_LENGTH)
                ?.replace("\"", "'")
                ?.let { perspective ->
                    add("last_perspective=\"$perspective\"")
                }
        }
        return if (parts.isEmpty()) null else "State: " + parts.joinToString("; ")
    }

    private fun formatRelativeAge(diffMillis: Long): String {
        if (diffMillis <= 0L) return "just now"
        val minutes = diffMillis / MINUTE_MILLIS
        if (minutes < 60L) return "<1h ago"
        val hours = diffMillis / HOUR_MILLIS
        if (hours < 48L) return "${hours}h ago"
        val days = diffMillis / DAY_MILLIS
        return "${days}d ago"
    }

    private fun String.collapseWhitespace(): String = trim().replace(Regex("\\s+"), " ")
}
