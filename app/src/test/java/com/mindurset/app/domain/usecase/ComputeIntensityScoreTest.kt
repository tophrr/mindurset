package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.Emotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ComputeIntensityScoreTest {
    @Test
    fun `accepts the boundaries of the slider`() {
        assertEquals(1, ComputeIntensityScore.validate(1))
        assertEquals(10, ComputeIntensityScore.validate(10))
    }

    @Test
    fun `rejects values outside the slider`() {
        assertThrows(IllegalArgumentException::class.java) { ComputeIntensityScore.validate(0) }
        assertThrows(IllegalArgumentException::class.java) { ComputeIntensityScore.validate(11) }
    }

    @Test
    fun `bands a negative emotion by intensity`() {
        assertEquals(ComputeIntensityScore.Band.LOW, ComputeIntensityScore.band(Emotion.STRESSED, 3))
        assertEquals(ComputeIntensityScore.Band.MODERATE, ComputeIntensityScore.band(Emotion.STRESSED, 4))
        assertEquals(ComputeIntensityScore.Band.MODERATE, ComputeIntensityScore.band(Emotion.STRESSED, 6))
        assertEquals(ComputeIntensityScore.Band.HIGH, ComputeIntensityScore.band(Emotion.STRESSED, 7))
    }

    @Test
    fun `a positive emotion is never a high band`() {
        assertEquals(ComputeIntensityScore.Band.LOW, ComputeIntensityScore.band(Emotion.HAPPY, 10))
        assertEquals(ComputeIntensityScore.Band.LOW, ComputeIntensityScore.band(Emotion.CALM, 8))
    }
}
