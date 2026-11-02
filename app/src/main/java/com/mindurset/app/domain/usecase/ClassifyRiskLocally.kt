package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.RiskLevel

/**
 * Fail closed (proposal Sec. 9.4). When the classifier is unavailable or unsure, the level never
 * drops below what the local floor found, and it is raised one step. Silence is not evidence of
 * safety.
 */
object ClassifyRiskLocally {
    /**
     * @param floor the level the offline phrase list found, or null when it found nothing
     * @param classifier the level the model reported, or null when it was unavailable
     */
    fun resolve(floor: RiskLevel?, classifier: RiskLevel?): RiskLevel {
        val base = floor ?: RiskLevel.L0
        return if (classifier == null) base.escalate() else base.atLeast(classifier)
    }
}
