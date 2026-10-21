package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.Emotion

/**
 * Decides whether to offer the reframe, from proposal Sec. 6.1 F-03: offer it when the emotion is
 * negative and the intensity is above five. The app offers, it never navigates on its own.
 */
object EvaluateTriage {
    const val THRESHOLD = 5

    enum class Offer { None, Reframe }

    fun evaluate(emotion: Emotion, intensity: Int): Offer {
        ComputeIntensityScore.validate(intensity)
        return if (emotion.isNegative && intensity > THRESHOLD) Offer.Reframe else Offer.None
    }
}
