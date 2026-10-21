package com.mindurset.app

import com.mindurset.app.domain.auth.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun `hash is verifiable and never stores plaintext`() {
        val password = "rahasia-kuat-123"
        val hashed = PasswordHasher.hash(password, iterations = 1_000)
        assertNotEquals(password, hashed.hash)
        assertTrue(PasswordHasher.verify(password, hashed.hash, hashed.salt, hashed.iterations))
    }

    @Test
    fun `wrong password fails verification`() {
        val hashed = PasswordHasher.hash("correct-horse", iterations = 1_000)
        assertFalse(PasswordHasher.verify("wrong-horse", hashed.hash, hashed.salt, hashed.iterations))
    }

    @Test
    fun `same password produces different hashes`() {
        val a = PasswordHasher.hash("same-password", iterations = 1_000)
        val b = PasswordHasher.hash("same-password", iterations = 1_000)
        assertNotEquals(a.hash, b.hash)
        assertNotEquals(a.salt, b.salt)
    }
}
