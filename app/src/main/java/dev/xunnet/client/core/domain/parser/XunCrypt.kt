package dev.xunnet.client.core.domain.parser

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-GCM encryption with PBKDF2 key derivation.
 * Used for xuncrypt:// links: header = 16-byte salt + 12-byte IV.
 */
object XunCrypt {
    private const val ITERATIONS = 100_000
    private const val KEY_BITS = 256
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val GCM_TAG_BITS = 128

    fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ct = cipher.doFinal(plain)
        return salt + iv + ct
    }

    fun decrypt(payload: ByteArray, password: String): ByteArray {
        require(payload.size > SALT_LEN + IV_LEN) { "payload too short" }
        val salt = payload.copyOfRange(0, SALT_LEN)
        val iv = payload.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ct = payload.copyOfRange(SALT_LEN + IV_LEN, payload.size)
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ct)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = skf.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /** Convenience for unit tests / desktop. */
    fun encryptToBase64(plain: ByteArray, password: String): String =
        Base64.encodeToString(encrypt(plain, password), Base64.NO_WRAP)

    fun decryptFromBase64(b64: String, password: String): ByteArray =
        decrypt(Base64.decode(b64, Base64.DEFAULT), password)
}
