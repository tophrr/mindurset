package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two rules of layer two: a classifier may only raise the floor, and its absence raises the level
 * by one tier. ADR 0005 records why the local-only paths do not escalate.
 */
class RiskScreeningTest {
    @Test
    fun `a local path never escalates`() {
        val screening = RiskScreening.screenLocal("A busy day, mostly deadlines")

        assertEquals(RiskLevel.L0, screening.level)
        assertEquals(ScreeningSource.FLOOR_ONLY, screening.source)
        assertFalse(screening.escalated)
        assertFalse(screening.halts)
    }

    @Test
    fun `a local path still halts on a red flag`() {
        val screening = RiskScreening.screenLocal("aku mau bunuh diri malam ini")

        assertEquals(RiskLevel.L3, screening.level)
        assertTrue(screening.halts)
        assertEquals("id-intent", screening.patternId)
    }

    @Test
    fun `an unreachable classifier raises a clean line to L1`() {
        val screening = RiskScreening.screenWith("A busy day, mostly deadlines", classifier = null)

        assertEquals(RiskLevel.L1, screening.level)
        assertEquals(ScreeningSource.CLASSIFIER_UNAVAILABLE, screening.source)
        assertTrue(screening.escalated)
        assertFalse("L1 offers resources, it does not halt", screening.halts)
    }

    @Test
    fun `an unreachable classifier raises a red flag to L3`() {
        val screening = RiskScreening.screenWith("there is no reason to live", classifier = null)

        assertEquals(RiskLevel.L3, screening.level)
        assertTrue(screening.escalated)
        assertTrue(screening.halts)
    }

    @Test
    fun `an unreachable classifier cannot raise L3 further`() {
        val screening = RiskScreening.screenWith("aku mau bunuh diri", classifier = null)

        assertEquals(RiskLevel.L3, screening.level)
        assertFalse(screening.escalated)
        assertTrue(screening.halts)
    }

    @Test
    fun `the classifier may raise the floor`() {
        val screening = RiskScreening.screenWith("deadlines everywhere", classifier = RiskLevel.L2)

        assertEquals(RiskLevel.L2, screening.level)
        assertEquals(ScreeningSource.FLOOR_AND_CLASSIFIER, screening.source)
        assertTrue(screening.escalated)
        assertTrue(screening.halts)
    }

    @Test
    fun `the classifier never lowers the floor`() {
        val screening = RiskScreening.screenWith("aku mau bunuh diri", classifier = RiskLevel.L0)

        assertEquals(RiskLevel.L3, screening.level)
        assertEquals(ScreeningSource.FLOOR_AND_CLASSIFIER, screening.source)
        assertFalse(screening.escalated)
    }

    @Test
    fun `an ordinary line with an ordinary classifier does not halt`() {
        val screening = RiskScreening.screenWith("A busy day", classifier = RiskLevel.L1)

        assertEquals(RiskLevel.L1, screening.level)
        assertFalse(screening.halts)
    }
}
