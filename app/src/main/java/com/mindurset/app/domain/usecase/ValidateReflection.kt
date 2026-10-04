package com.mindurset.app.domain.usecase

/**
 * Pure-JVM validation for the Daily Reflection form. Returns string resource
 * keys, never user-visible text, so the UI layer stays localisable and this
 * object stays unit-testable without Android.
 */
object ValidateReflection {

    enum class Field { CHALLENGE, REFRAME, GRATITUDE }

    data class Result(val errors: Map<Field, String>) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    const val CHALLENGE_MIN = 3
    const val CHALLENGE_MAX = 500
    const val OPTIONAL_MAX = 1_000

    const val ERROR_CHALLENGE_REQUIRED = "reflection_error_challenge_required"
    const val ERROR_CHALLENGE_SHORT = "reflection_error_challenge_short"
    const val ERROR_CHALLENGE_LONG = "reflection_error_challenge_long"
    const val ERROR_REFRAME_LONG = "reflection_error_reframe_long"
    const val ERROR_GRATITUDE_LONG = "reflection_error_gratitude_long"

    /**
     * The challenge is required, must not be whitespace only, and must be
     * between [CHALLENGE_MIN] and [CHALLENGE_MAX] characters. Reframe and
     * gratitude are optional but capped at [OPTIONAL_MAX] characters.
     */
    fun validate(challenge: String, reframe: String, gratitude: String): Result {
        val errors = LinkedHashMap<Field, String>()
        val trimmedChallenge = challenge.trim()
        when {
            trimmedChallenge.isEmpty() -> errors[Field.CHALLENGE] = ERROR_CHALLENGE_REQUIRED
            trimmedChallenge.length < CHALLENGE_MIN -> errors[Field.CHALLENGE] = ERROR_CHALLENGE_SHORT
            challenge.length > CHALLENGE_MAX -> errors[Field.CHALLENGE] = ERROR_CHALLENGE_LONG
        }
        if (reframe.length > OPTIONAL_MAX) errors[Field.REFRAME] = ERROR_REFRAME_LONG
        if (gratitude.length > OPTIONAL_MAX) errors[Field.GRATITUDE] = ERROR_GRATITUDE_LONG
        return Result(errors)
    }
}
