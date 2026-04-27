package com.meshenger.backend.session
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherState {
    private val ALGORITHM = "ChaCha20/Poly1305/NoPadding"
    private val KEY_SIZE = 32
    private val NONCE_SIZE = 8
    private val REQUIRED_NONCE_SIZE = 12
    private var k: ByteArray? = null // Empty value indicates uninitialized
    private var n: ULong = 0uL
    private val cipher = Cipher.getInstance(ALGORITHM)
    private fun getNonceBytes(n: ULong): ByteArray {
        val bytes = ByteArray(12)
        for (i in 0..7) {
            bytes[11 - i] = ((n shr (i * 8)) and 0xFFu).toByte()
        }
        return bytes
    }

    fun initializeKey(key: ByteArray?) {
        k = key
        n = 0uL // Reset nonce on key initialization
    }

    fun hasKey(): Boolean = k != null

    fun encryptWithAd(ad: ByteArray, plainText: ByteArray): ByteArray {
        if (n == 0xFFFFFFFFFFFFFFFFuL) throw IllegalStateException("Nonce exhausted")
        if (!hasKey()) return plainText

        val keySpec = SecretKeySpec(k, "ChaCha20")
        val ivSpec = IvParameterSpec(getNonceBytes(n))
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        cipher.updateAAD(ad)
        val cipherText = cipher.doFinal(plainText)
        n++ // Increment after successful encryption
        return cipherText
    }

    fun decryptWithAd(ad: ByteArray, cipherText: ByteArray): ByteArray {
        if (n == 0xFFFFFFFFFFFFFFFFuL) throw IllegalStateException("Nonce exhausted")
        if (!hasKey()) return cipherText

        val keySpec = SecretKeySpec(k, "ChaCha20")
        val ivSpec = IvParameterSpec(getNonceBytes(n))
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        cipher.updateAAD(ad)

        val plainText = cipher.doFinal(cipherText)
        n++

        return plainText
    }
    fun getCurrentNonce(): ULong {
        return n
    }
    fun getKey(): ByteArray? {
        return k
    }
}