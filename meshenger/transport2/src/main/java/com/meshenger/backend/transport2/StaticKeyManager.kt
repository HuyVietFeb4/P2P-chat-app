package com.meshenger.backend.transport2

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

// To do: to init the static public and private's key
object StaticKeyManager {
    // temporary attribute, will be deleted
    private var agreementKeyPair: Pair<ByteArray, ByteArray>? = null
    private val PROVIDER = "AndroidKeyStore"
    private val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
    private const val STATIC_IDENTITY_ALIAS = "static_identity_ed25519"
    private const val ED25519_ALGO = "Ed25519"

    private const val MASTER_KEY_ALIAS = "database_master_wrapper_key"
    private const val AES_MODE = "AES/GCM/NoPadding"

//    private const val STATIC_AGREEMENT_ALIAS = "static_agreement_x25519"
//    private const val X25519_ALGO = "X25519"


    fun getOrCreateIdentityKey(): KeyPair {
        if (keyStore.containsAlias(STATIC_IDENTITY_ALIAS)) {
            val privateKey = keyStore.getKey(STATIC_IDENTITY_ALIAS, null) as PrivateKey
            val publicKey = keyStore.getCertificate(STATIC_IDENTITY_ALIAS).publicKey
            return KeyPair(publicKey, privateKey)
        }
        val kpg = KeyPairGenerator.getInstance(ED25519_ALGO, PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            STATIC_IDENTITY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

//    fun getOrCreateAgreementKey(): KeyPair {
//        if (keyStore.containsAlias(STATIC_AGREEMENT_ALIAS)) {
//            val privateKey = keyStore.getKey(STATIC_AGREEMENT_ALIAS, null) as PrivateKey
//            val publicKey = keyStore.getCertificate(STATIC_AGREEMENT_ALIAS).publicKey
//            return KeyPair(publicKey, privateKey)
//        }
//
//        val kpg = KeyPairGenerator.getInstance(X25519_ALGO, PROVIDER)
//        val spec = KeyGenParameterSpec.Builder(
//            STATIC_AGREEMENT_ALIAS,
//            KeyProperties.PURPOSE_AGREE_KEY
//        ).build()
//
//        kpg.initialize(spec)
//        return kpg.generateKeyPair()
//    }

    /**
     * Create a pair of X25519 keys
     * @return Pair of (public key, private key) to be stored in SQLite.
     */
    fun generateX25519KeyPair(): Pair<ByteArray, ByteArray> {
        val existingKey = agreementKeyPair
        if (existingKey != null) return existingKey
        val generator = X25519KeyPairGenerator()
        val random = SecureRandom() // Use a strong random source
        val params = X25519KeyGenerationParameters(random)
        generator.init(params)
        val keyPair = generator.generateKeyPair()
        val publicKey = (keyPair.public as X25519PublicKeyParameters).getEncoded() // 32 bytes
        val privateKey = (keyPair.private as X25519PrivateKeyParameters).getEncoded() // 32 bytes
        agreementKeyPair = Pair(publicKey, privateKey)
        val newPair = Pair(publicKey, privateKey)
        return newPair
    }

    fun generateX25519RandomKeyPair(): Pair<ByteArray, ByteArray> {
        val generator = X25519KeyPairGenerator()
        val random = SecureRandom() // Use a strong random source
        val params = X25519KeyGenerationParameters(random)
        generator.init(params)
        val keyPair = generator.generateKeyPair()
        val publicKey = (keyPair.public as X25519PublicKeyParameters).getEncoded() // 32 bytes
        val privateKey = (keyPair.private as X25519PrivateKeyParameters).getEncoded() // 32 bytes
        return Pair(publicKey, privateKey)
    }

    /**
     * Encrypts raw private key bytes.
     * @return Pair of (Ciphertext, Initialization Vector) to be stored in SQLite.
     */
    fun wrapKey(rawKeyBytes: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(AES_MODE)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())

        val ciphertext = cipher.doFinal(rawKeyBytes)
        return Pair(ciphertext, cipher.iv)
    }

    /**
     * Decrypts ciphertext from SQLite back into raw private key bytes.
     */
    fun unwrapKey(ciphertext: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_MODE)
        val spec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), spec)
        return cipher.doFinal(ciphertext)
    }
    /**
     * Get or create the key to encrypt/decrypt the X25519 software-created key
     */
    private fun getOrCreateMasterKey(): SecretKey {
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)

            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                // Optional: require biometric or device lock
                // .setUserAuthenticationRequired(true)
                .build()

            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }

        val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    fun decodeRawIdentityPublicKey(rawKey: ByteArray): PublicKey {
        val x509Header = byteArrayOf(
            0x30, 0x2a,                   // SEQUENCE
            0x30, 0x05,                   // SEQUENCE
            0x06, 0x03, 0x2b, 0x65, 0x70, // OID 1.3.101.112 (Ed25519)
            0x03, 0x21, 0x00              // BIT STRING
        )

        val fullEncodedKey = x509Header + rawKey
        val spec = X509EncodedKeySpec(fullEncodedKey)
        val kf = KeyFactory.getInstance(ED25519_ALGO)
        return kf.generatePublic(spec)
    }

//    fun decodeRawAgreementPublicKey(rawKey: ByteArray): PublicKey {
//        val x509Header = byteArrayOf(
//            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
//        )
//        val spec = X509EncodedKeySpec(x509Header + rawKey)
//        return KeyFactory.getInstance(X25519_ALGO).generatePublic(spec)
//    }

    fun getRawPublicKey(publicKey: PublicKey): ByteArray {
        val encoded = publicKey.encoded // This returns the X.509 format

        // For Ed25519/X25519, the raw key is the last 32 bytes.
        return if (encoded.size >= 32) {
            encoded.takeLast(32).toByteArray()
        } else {
            throw IllegalArgumentException("Key encoding is too short")
        }
    }

    fun signData(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val s = Signature.getInstance("Ed25519")
        s.initSign(privateKey)
        s.update(data)
        return s.sign()
    }

    fun verifyData(data: ByteArray, signature:ByteArray, publicKey: PublicKey): Boolean {
        return try {
            val s = Signature.getInstance("Ed25519")
            s.initVerify(publicKey)
            s.update(data)
            s.verify(signature)
        } catch (e: Exception) {
            Log.e("StaticKeyManagement", "Error verify data with signature: ${e.message}")
            false
        }
    }
}