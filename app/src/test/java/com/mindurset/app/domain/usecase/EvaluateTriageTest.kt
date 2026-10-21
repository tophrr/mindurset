package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class EvaluateTriageTest {
    @Test
    fun `offers the reframe only above five and only for a negative emotion`() {
        val expected =
            mapOf(
                (Emotion.HAPPY to 4) to EvaluateTriage.Offer.None,
                (Emotion.HAPPY to 7) to EvaluateTriage.Offer.None,
                (Emotion.CALM to 7) to EvaluateTriage.Offer.None,
                (Emotion.LOW to 4) to EvaluateTriage.Offer.None,
                (Emotion.LOW to 5) to EvaluateTriage.Offer.None,
                (Emotion.LOW to 6) to EvaluateTriage.Offer.Reframe,
                (Emotion.LOW to 7) to EvaluateTriage.Offer.Reframe,
                (Emotion.ANXIOUS to 5) to EvaluateTriage.Offer.None,
                (Emotion.ANXIOUS to 6) to EvaluateTriage.Offer.Reframe,
                (Emotion.STRESSED to 5) to EvaluateTriage.Offer.None,
                (Emotion.STRESSED to 6) to EvaluateTriage.Offer.Reframe,
            )

        expected.forEach { (input, offer) ->
            assertEquals("${input.first} at ${input.second}", offer, EvaluateTriage.evaluate(input.first, input.second))
        }
    }

    @Test
    fun `rejects an intensity outside the slider`() {
        assertThrows(IllegalArgumentException::class.java) { EvaluateTriage.evaluate(Emotion.LOW, 0) }
    }
}
