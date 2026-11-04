package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel

/**
 * Layer one of the safety ladder (proposal Sec. 9.4): patterns in English and Indonesian that run
 * offline, before anything else, on every piece of text the user writes.
 *
 * Two tiers, because the proposal treats them differently. A direct statement of intent reaches
 * [RiskLevel.L3], hopelessness or a third-person disclosure reaches [RiskLevel.L2]. The previous
 * version matched literal substrings and stopped at L2, which missed inflections ("wants to die"),
 * reordered Indonesian ("lebih baik aku mati") and every paraphrase in the red-team suite.
 *
 * This is still a floor, not a classifier: a miss proves nothing, and the Worker may raise the
 * level further. It never lowers it.
 */
object LocalRiskFloor {
    data class Match(val patternId: String, val level: RiskLevel)

    private class Pattern(val id: String, val level: RiskLevel, val regexes: List<Regex>)

    private val patterns: List<Pattern> =
        listOf(
            Pattern(
                id = "en-intent",
                level = RiskLevel.L3,
                regexes =
                phrases(
                    "\\bkill(ing)? myself\\b",
                    "\\bend(ing)? my life\\b",
                    "\\bcommit suicide\\b",
                    "\\btake my own life\\b",
                ),
            ),
            Pattern(
                id = "en-hopeless",
                level = RiskLevel.L2,
                regexes =
                phrases(
                    "\\bwant(s|ed)? to die\\b",
                    "\\bbetter off (if .{0,24}?(was|were|is) )?dead\\b",
                    "\\bno reason to live\\b",
                    "\\bnothing to live for\\b",
                    "\\bhurt(ing)? myself\\b",
                    "\\bpoint of waking up\\b",
                    "\\bempty and meaningless\\b",
                    "\\bdon'?t want to (be here|live)\\b",
                ),
            ),
            Pattern(
                id = "id-intent",
                level = RiskLevel.L3,
                regexes =
                phrases(
                    "\\bbunuh diri\\b",
                    "\\bmengakhiri hidup\\b",
                    "\\blebih baik (aku )?mati\\b",
                    "\\bmati saja\\b",
                ),
            ),
            Pattern(
                id = "id-hopeless",
                level = RiskLevel.L2,
                regexes =
                phrases(
                    "\\bingin mati\\b",
                    "\\bmau mati\\b",
                    "\\bmenyakiti diri\\b",
                    "\\btidak (ada alasan|ingin) (untuk )?hidup\\b",
                    "\\bjadi beban\\b",
                    "\\bputus asa\\b",
                ),
            ),
        )

    /** The strongest hit in the text, or null when nothing matched. */
    fun match(text: String): Match? = patterns
        .asSequence()
        .filter { pattern -> pattern.regexes.any { it.containsMatchIn(text) } }
        .maxByOrNull { it.level }
        ?.let { Match(patternId = it.id, level = it.level) }

    fun level(text: String): RiskLevel = match(text)?.level ?: RiskLevel.L0

    private fun phrases(vararg expressions: String): List<Regex> =
        expressions.map { Regex(it, RegexOption.IGNORE_CASE) }
}
