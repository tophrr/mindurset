package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.Emotion

/**
 * Validates the intensity slider and turns the pair of face and slider into a band.
 *
 * The slider is 1..10 (proposal Sec. 6.1, F-02). A positive emotion is never a high band: the band
 * exists to decide whether the app should offer more help, and a good day does not need it.
 */
object ComputeIntensityScore {
    const val MIN = 1
    const val MAX = 10

    enum class Band { LOW, MODERATE, HIGH }

    fun validate(intensity: Int): Int {
        require(intensity in MIN..MAX) { "intensity must be between $MIN and $MAX, was $intensity" }
        return intensity
    }

    fun band(emotion: Emotion, intensity: Int): Band {
        validate(intensity)
        if (!emotion.isNegative) return Band.LOW
        return when {
            intensity <= 3 -> Band.LOW
            intensity <= 6 -> Band.MODERATE
            else -> Band.HIGH
        }
    }
}
