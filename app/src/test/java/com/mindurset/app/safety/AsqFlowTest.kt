package com.mindurset.app.safety

import com.mindurset.app.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ASQ step: four items, an acuity probe only after a yes, and one level out of it. ADR 0007 records
 * where the wording comes from and what is not validated.
 */
class AsqFlowTest {
    @Test
    fun `the flow asks four items when every answer is no`() {
        var state = AsqFlow.State()
        val asked = mutableListOf<String>()

        repeat(4) {
            val question = AsqFlow.nextQuestion(state)
            requireNotNull(question)
            asked += question.id
            state = AsqFlow.answer(state, question.id, yes = false)
        }

        assertEquals(listOf(AsqFlow.ID_Q1, AsqFlow.ID_Q2, AsqFlow.ID_Q3, AsqFlow.ID_Q4), asked)
        assertNull("nothing left to ask", AsqFlow.nextQuestion(state))
        val result = requireNotNull(AsqFlow.result(state))
        assertEquals(RiskLevel.L0, result.level)
        assertFalse(result.imminent)
    }

    @Test
    fun `a yes adds the acuity probe after all four items`() {
        var state = AsqFlow.State()
        state = AsqFlow.answer(state, AsqFlow.ID_Q1, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_Q2, yes = true)
        assertEquals("the instrument is asked in full", AsqFlow.ID_Q3, AsqFlow.nextQuestion(state)?.id)
        state = AsqFlow.answer(state, AsqFlow.ID_Q3, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_Q4, yes = false)

        assertEquals(AsqFlow.ID_ACUITY, AsqFlow.nextQuestion(state)?.id)
        assertNull("the flow is not finished while acuity is open", AsqFlow.result(state))

        state = AsqFlow.answer(state, AsqFlow.ID_ACUITY, yes = false)
        val result = requireNotNull(AsqFlow.result(state))
        assertEquals(RiskLevel.L3, result.level)
        assertFalse("a yes on a past item is not an imminent yes", result.imminent)
    }

    @Test
    fun `an acuity yes is recorded as imminent`() {
        var state = AsqFlow.State()
        state = AsqFlow.answer(state, AsqFlow.ID_Q1, yes = true)
        state = AsqFlow.answer(state, AsqFlow.ID_Q2, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_Q3, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_Q4, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_ACUITY, yes = true)

        val result = requireNotNull(AsqFlow.result(state))
        assertEquals(RiskLevel.L3, result.level)
        assertTrue(result.imminent)
    }

    @Test
    fun `a past attempt alone still reaches the crisis card`() {
        var state = AsqFlow.State()
        state = AsqFlow.answer(state, AsqFlow.ID_Q1, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_Q2, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_Q3, yes = false)
        state = AsqFlow.answer(state, AsqFlow.ID_Q4, yes = true)
        state = AsqFlow.answer(state, AsqFlow.ID_ACUITY, yes = false)

        assertEquals(RiskLevel.L3, requireNotNull(AsqFlow.result(state)).level)
    }

    @Test
    fun `the same question cannot be answered twice`() {
        val once = AsqFlow.answer(AsqFlow.State(), AsqFlow.ID_Q1, yes = true)
        val twice = AsqFlow.answer(once, AsqFlow.ID_Q1, yes = false)

        assertEquals(once, twice)
        assertTrue(twice.answers.getValue(AsqFlow.ID_Q1))
    }

    @Test
    fun `an unknown id changes nothing`() {
        val state = AsqFlow.State()

        assertEquals(state, AsqFlow.answer(state, "q9", yes = true))
    }

    @Test
    fun `progress counts the questions this person will actually see`() {
        val negative = AsqFlow.State()
        assertEquals(4, AsqFlow.total(negative))
        assertEquals(1, AsqFlow.position(negative))

        val positive = AsqFlow.answer(negative, AsqFlow.ID_Q1, yes = true)
        assertEquals("a yes adds the acuity probe", 5, AsqFlow.total(positive))
        assertEquals(2, AsqFlow.position(positive))
    }
}
