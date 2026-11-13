package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel

/** Which layers produced the level, so a report can tell them apart. */
enum class ScreeningSource {
    /** The floor alone: this path never consults a classifier. */
    FLOOR_ONLY,

    /** The floor and the classifier's answer, whichever is higher. */
    FLOOR_AND_CLASSIFIER,

    /** The call could not be made or returned nothing usable, so the level was raised a tier. */
    CLASSIFIER_UNAVAILABLE,
}

data class Screening(
    val level: RiskLevel,
    val source: ScreeningSource,
    val patternId: String?,
    val escalated: Boolean,
) {
    /** True when the flow has to halt and show the crisis card instead of continuing. */
    val halts: Boolean get() = level >= RiskLevel.L2
}

/**
 * Layer two of the safety ladder (proposal Sec. 9.4). Two rules, both about never letting a turn
 * pass on a guess:
 *
 * - a classifier may only raise the floor, never lower it;
 * - when the classifier cannot answer, the level goes up one tier instead of staying where the floor
 *   left it.
 *
 * [screenLocal] is for the paths that never talk to a classifier at all. An outage cannot degrade a
 * decision that never leaves the device, so nothing is escalated there; ADR 0005 records that reading
 * and its limits.
 */
object RiskScreening {
    /** The floor alone. Used by check-in and journal, which make no network call. */
    fun screenLocal(text: String): Screening {
        val hit = LocalRiskFloor.match(text)
        return Screening(
            level = hit?.level ?: RiskLevel.L0,
            source = ScreeningSource.FLOOR_ONLY,
            patternId = hit?.patternId,
            escalated = false,
        )
    }

    /**
     * The floor plus a classifier answer. [classifier] is null when the call failed, timed out, or
     * returned no usable level: that is the fail-closed case, one tier up and never down.
     */
    fun screenWith(text: String, classifier: RiskLevel?): Screening {
        val hit = LocalRiskFloor.match(text)
        val floor = hit?.level ?: RiskLevel.L0
        if (classifier == null) {
            val raised = floor.escalate()
            return Screening(
                level = raised,
                source = ScreeningSource.CLASSIFIER_UNAVAILABLE,
                patternId = hit?.patternId,
                escalated = raised != floor,
            )
        }
        val level = maxOf(floor, classifier)
        return Screening(
            level = level,
            source = ScreeningSource.FLOOR_AND_CLASSIFIER,
            patternId = hit?.patternId,
            escalated = level != floor,
        )
    }
}
