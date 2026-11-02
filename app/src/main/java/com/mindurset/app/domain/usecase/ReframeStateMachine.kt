package com.mindurset.app.domain.usecase

/**
 * The three steps of a reframe, as an immutable value.
 *
 * Steps run 0, 1, 2, and stepping past the last one completes the session at step 3. The machine is
 * resumable from what the database stored, and a halted session can never advance again.
 */
class ReframeStateMachine private constructor(val currentStep: Int, val status: Status) {
    enum class Status { Active, Completed, Halted }

    val isActive: Boolean get() = status == Status.Active

    /** @throws IllegalStateException when the session is completed or halted. */
    fun advance(): ReframeStateMachine {
        check(isActive) { "cannot advance a session that is $status" }
        return if (currentStep + 1 >= STEP_COUNT) {
            ReframeStateMachine(STEP_COUNT, Status.Completed)
        } else {
            ReframeStateMachine(currentStep + 1, Status.Active)
        }
    }

    fun halt(): ReframeStateMachine = ReframeStateMachine(currentStep, Status.Halted)

    companion object {
        /** Steps 0..2 are the three screens; step 3 means done. */
        const val STEP_COUNT = 3

        fun start(): ReframeStateMachine = ReframeStateMachine(0, Status.Active)

        fun resume(step: Int, status: Status): ReframeStateMachine {
            require(step in 0..STEP_COUNT) { "step must be between 0 and $STEP_COUNT, was $step" }
            require(status != Status.Completed || step == STEP_COUNT) {
                "a completed session must sit at step $STEP_COUNT, was $step"
            }
            return ReframeStateMachine(step, status)
        }

        /** Reads back what the repository stored, without trusting it blindly. */
        fun fromPersisted(step: Int, statusName: String): ReframeStateMachine {
            val status = Status.entries.firstOrNull { it.name.equals(statusName, ignoreCase = true) }
            requireNotNull(status) { "unknown session status: $statusName" }
            return resume(step, status)
        }
    }
}
