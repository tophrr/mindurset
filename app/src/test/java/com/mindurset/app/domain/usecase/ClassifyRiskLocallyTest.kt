package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class ClassifyRiskLocallyTest {
    @Test
    fun `an unavailable classifier raises the floor by one level`() {
        assertEquals(RiskLevel.L3, ClassifyRiskLocally.resolve(RiskLevel.L2, null))
        assertEquals(RiskLevel.L1, ClassifyRiskLocally.resolve(null, null))
        assertEquals(RiskLevel.L1, ClassifyRiskLocally.resolve(RiskLevel.L0, null))
    }

    @Test
    fun `the final level never drops below the floor`() {
        assertEquals(RiskLevel.L2, ClassifyRiskLocally.resolve(RiskLevel.L2, RiskLevel.L1))
        assertEquals(RiskLevel.L3, ClassifyRiskLocally.resolve(RiskLevel.L2, RiskLevel.L3))
        assertEquals(RiskLevel.L3, ClassifyRiskLocally.resolve(null, RiskLevel.L3))
    }
}
