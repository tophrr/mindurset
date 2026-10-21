package com.mindurset.app.domain.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-HMAC-SHA256 password hashing. The plaintext password is never stored
 * or logged anywhere on the device.
 */
object PasswordHasher {
    const val ITERATIONS: Int = 100_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class Hashed(val hash: String, val salt: String, val iterations: Int)

    fun hash(password: String, iterations: Int = ITERATIONS): Hashed {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return Hashed(derive(password, salt, iterations), encode(salt), iterations)
    }

    fun verify(password: String, expectedHash: String, salt: String, iterations: Int): Boolean {
        val saltBytes =
            try {
                Base64.getDecoder().decode(salt)
            } catch (e: IllegalArgumentException) {
                return false
            }
        val candidate = derive(password, saltBytes, iterations).toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(candidate, expectedHash.toByteArray(Charsets.UTF_8))
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key =
            try {
                factory.generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        return encode(key)
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
