package com.mindurset.app.domain.usecase

import com.mindurset.app.domain.usecase.ReframeStateMachine.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ReframeStateMachineTest {
    @Test
    fun `starts at the first step`() {
        val machine = ReframeStateMachine.start()
        assertEquals(0, machine.currentStep)
        assertTrue(machine.isActive)
    }

    @Test
    fun `advances through three steps and completes`() {
        val step1 = ReframeStateMachine.start().advance()
        assertEquals(1, step1.currentStep)
        val step2 = step1.advance()
        assertEquals(2, step2.currentStep)
        val done = step2.advance()
        assertEquals(ReframeStateMachine.STEP_COUNT, done.currentStep)
        assertEquals(Status.Completed, done.status)
        assertFalse(done.isActive)
    }

    @Test
    fun `a completed session cannot advance`() {
        val done = ReframeStateMachine.resume(ReframeStateMachine.STEP_COUNT, Status.Completed)
        assertThrows(IllegalStateException::class.java) { done.advance() }
    }

    @Test
    fun `a halted session cannot advance`() {
        val halted = ReframeStateMachine.start().advance().halt()
        assertEquals(Status.Halted, halted.status)
        assertThrows(IllegalStateException::class.java) { halted.advance() }
    }

    @Test
    fun `resumes from the stored step`() {
        val resumed = ReframeStateMachine.resume(2, Status.Active)
        assertEquals(2, resumed.currentStep)
        assertTrue(resumed.isActive)
    }

    @Test
    fun `refuses an impossible stored state`() {
        assertThrows(IllegalArgumentException::class.java) {
            ReframeStateMachine.resume(2, Status.Completed)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReframeStateMachine.resume(4, Status.Active)
        }
    }

    @Test
    fun `reads the persisted status name`() {
        assertEquals(1, ReframeStateMachine.fromPersisted(1, "active").currentStep)
        assertEquals(Status.Halted, ReframeStateMachine.fromPersisted(2, "halted").status)
        assertThrows(IllegalArgumentException::class.java) {
            ReframeStateMachine.fromPersisted(0, "zombie")
        }
    }
}
