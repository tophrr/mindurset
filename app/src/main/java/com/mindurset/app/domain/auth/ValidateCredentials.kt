package com.mindurset.app.domain.auth

/**
 * Form validation for the login and register screens. Returns string resource
 * keys, never user-visible text, so the UI layer stays localisable.
 */
object ValidateCredentials {
    enum class Field { EMAIL, PASSWORD, CONFIRM }

    data class Result(val errors: Map<Field, String>) {
        val isValid: Boolean get() = errors.isEmpty()
    }

    const val ERROR_EMAIL_EMPTY = "error_email_empty"
    const val ERROR_EMAIL_FORMAT = "error_email_format"
    const val ERROR_PASSWORD_EMPTY = "error_password_empty"
    const val ERROR_PASSWORD_SHORT = "error_password_short"
    const val ERROR_PASSWORD_MISMATCH = "error_password_mismatch"

    const val MIN_PASSWORD_LENGTH = 8

    private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

    fun forLogin(email: String, password: String): Result {
        val errors = LinkedHashMap<Field, String>()
        val trimmedEmail = email.trim()
        when {
            trimmedEmail.isEmpty() -> errors[Field.EMAIL] = ERROR_EMAIL_EMPTY
            !EMAIL_PATTERN.matches(trimmedEmail) -> errors[Field.EMAIL] = ERROR_EMAIL_FORMAT
        }
        if (password.isEmpty()) errors[Field.PASSWORD] = ERROR_PASSWORD_EMPTY
        return Result(errors)
    }

    fun forRegister(email: String, password: String, confirm: String): Result {
        val errors = LinkedHashMap<Field, String>(forLogin(email, password).errors)
        if (password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH) {
            errors[Field.PASSWORD] = ERROR_PASSWORD_SHORT
        }
        when {
            confirm.isEmpty() -> errors[Field.CONFIRM] = ERROR_PASSWORD_EMPTY
            confirm != password -> errors[Field.CONFIRM] = ERROR_PASSWORD_MISMATCH
        }
        return Result(errors)
    }
}
