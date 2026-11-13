package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel

/**
 * The ASQ step of the safety ladder (proposal Sec. 9.4, test plan Sec. 3.7): when the floor finds
 * ambiguous hopelessness (L2), the flow asks the four NIMH screening items before it decides, and the
 * acuity probe when any of them is answered yes.
 *
 * The answers are not stored anywhere: the flow only produces a level. That keeps the item answers out
 * of Room, out of the export, and out of any log.
 *
 * The English wording is the published ASQ instrument. The Indonesian wording is a translation made for
 * this project and has not been validated clinically; ADR 0007 records that gap.
 */
object AsqFlow {
    const val ID_Q1 = "q1"
    const val ID_Q2 = "q2"
    const val ID_Q3 = "q3"
    const val ID_Q4 = "q4"
    const val ID_ACUITY = "acuity"

    /** A question and the resource key the UI resolves for it. */
    data class Question(val id: String, val stringKey: String)

    private val screening =
        listOf(
            Question(ID_Q1, "asq_q1"),
            Question(ID_Q2, "asq_q2"),
            Question(ID_Q3, "asq_q3"),
            Question(ID_Q4, "asq_q4"),
        )

    private val acuity = Question(ID_ACUITY, "asq_acuity")

    data class State(val answers: Map<String, Boolean> = emptyMap(), val asked: List<String> = emptyList()) {
        val current: Question? get() = nextQuestion(this)

        val positive: Boolean get() = answeredYes.any { it != ID_ACUITY }

        val imminent: Boolean get() = answers[ID_ACUITY] == true

        internal val answeredYes: List<String> get() = answers.filterValues { it }.keys.toList()
    }

    /**
     * The question to show next, or null when the flow has nothing left to ask. The acuity probe only
     * appears after a yes, which is what the instrument asks for.
     */
    fun nextQuestion(state: State): Question? {
        screening.firstOrNull { it.id !in state.answers }?.let { return it }
        if (state.positive && ID_ACUITY !in state.answers) return acuity
        return null
    }

    /** Records one answer. The same question answered twice keeps the first answer. */
    fun answer(state: State, questionId: String, yes: Boolean): State {
        if (questionId in state.answers) return state
        val exists = screening.any { it.id == questionId } || questionId == ID_ACUITY
        if (!exists) return state
        return state.copy(
            answers = state.answers + (questionId to yes),
            asked = state.asked + questionId,
        )
    }

    /**
     * The level the answers add up to, or null while a question is still open. Any yes reaches L3: the
     * instrument is a screen, not a severity scale, and the proposal sends every positive screen to the
     * crisis card.
     */
    fun result(state: State): Result? {
        if (nextQuestion(state) != null) return null
        return if (state.positive) {
            Result(level = RiskLevel.L3, imminent = state.imminent)
        } else {
            Result(level = RiskLevel.L0, imminent = false)
        }
    }

    /** How many questions this flow will ask, for a progress line the person can trust. */
    fun total(state: State): Int = if (state.positive) screening.size + 1 else screening.size

    fun position(state: State): Int = (state.answers.size + 1).coerceAtMost(total(state))

    data class Result(val level: RiskLevel, val imminent: Boolean)
}
