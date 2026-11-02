package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel

/**
 * Layer one of the safety ladder (proposal Sec. 9.4): a fixed list of explicit phrases in English
 * and Indonesian that always runs, offline, before anything else.
 *
 * This is a floor, not a classifier. A hit raises the level to at least L2; a miss proves nothing.
 * The list is deliberately short and literal: matching paraphrases is the classifier's job.
 */
object LocalRiskFloor {
    data class Match(val patternId: String, val level: RiskLevel)

    private data class Pattern(val id: String, val phrases: List<String>)

    private val patterns =
        listOf(
            Pattern(
                "en-self-harm",
                listOf("kill myself", "want to die", "end my life", "hurt myself", "better off dead"),
            ),
            Pattern(
                "en-hopeless",
                listOf("no reason to live", "nothing to live for"),
            ),
            Pattern(
                "id-self-harm",
                listOf(
                    "ingin mati",
                    "mau bunuh diri",
                    "mengakhiri hidup",
                    "menyakiti diri",
                    "lebih baik mati",
                ),
            ),
            Pattern(
                "id-hopeless",
                listOf("tidak ingin hidup", "tidak ada alasan untuk hidup"),
            ),
        )

    fun match(text: String): Match? {
        val haystack = text.lowercase()
        for (pattern in patterns) {
            if (pattern.phrases.any { haystack.contains(it) }) {
                return Match(patternId = pattern.id, level = RiskLevel.L2)
            }
        }
        return null
    }

    fun level(text: String): RiskLevel = match(text)?.level ?: RiskLevel.L0
}
