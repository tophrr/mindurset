package com.mindurset.app

import com.mindurset.app.domain.auth.ValidateCredentials
import com.mindurset.app.domain.auth.ValidateCredentials.Field
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateCredentialsTest {

    @Test
    fun `empty login form reports both fields`() {
        val result = ValidateCredentials.forLogin("", "")
        assertFalse(result.isValid)
        assertEquals(ValidateCredentials.ERROR_EMAIL_EMPTY, result.errors[Field.EMAIL])
        assertEquals(ValidateCredentials.ERROR_PASSWORD_EMPTY, result.errors[Field.PASSWORD])
    }

    @Test
    fun `malformed email is rejected`() {
        val result = ValidateCredentials.forLogin("bukan-email", "password123")
        assertEquals(ValidateCredentials.ERROR_EMAIL_FORMAT, result.errors[Field.EMAIL])
    }

    @Test
    fun `valid login passes`() {
        assertTrue(ValidateCredentials.forLogin("chris@student.uph.edu", "password123").isValid)
    }

    @Test
    fun `register requires eight characters and matching confirmation`() {
        val short = ValidateCredentials.forRegister("a@b.co", "1234", "1234")
        assertEquals(ValidateCredentials.ERROR_PASSWORD_SHORT, short.errors[Field.PASSWORD])

        val mismatch = ValidateCredentials.forRegister("a@b.co", "password123", "password124")
        assertEquals(ValidateCredentials.ERROR_PASSWORD_MISMATCH, mismatch.errors[Field.CONFIRM])

        assertTrue(ValidateCredentials.forRegister("a@b.co", "password123", "password123").isValid)
    }
}
