package com.meshenger.backend.transport2

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

// To do: to init the static public and private's key
object StaticKeyManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private const val STATIC_IDENTITY_ALIAS = "static_identity_ed25519"
    private const val ED25519_ALGO = "Ed25519"

    private const val STATIC_AGREEMENT_ALIAS = "static_identity_x25519"
    private const val X25519_ALGO = "X25519"

    fun getOrCreateIdentityKey(): KeyPair {
        if (keyStore.containsAlias(STATIC_IDENTITY_ALIAS)) {
            val privateKey = keyStore.getKey(STATIC_IDENTITY_ALIAS, null) as PrivateKey
            val publicKey = keyStore.getCertificate(STATIC_IDENTITY_ALIAS).publicKey
            return KeyPair(publicKey, privateKey)
        }
        val kpg = KeyPairGenerator.getInstance(ED25519_ALGO, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            STATIC_IDENTITY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    fun getOrCreateAgreementKey(): KeyPair {
        if (keyStore.containsAlias(STATIC_AGREEMENT_ALIAS)) {
            val privateKey = keyStore.getKey(STATIC_AGREEMENT_ALIAS, null) as PrivateKey
            val publicKey = keyStore.getCertificate(STATIC_AGREEMENT_ALIAS).publicKey
            return KeyPair(publicKey, privateKey)
        }

        val kpg = KeyPairGenerator.getInstance(X25519_ALGO, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            STATIC_AGREEMENT_ALIAS,
            KeyProperties.PURPOSE_AGREE_KEY
        ).build()

        kpg.initialize(spec)
        return kpg.generateKeyPair()
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

    fun decodeRawAgreementPublicKey(rawKey: ByteArray): PublicKey {
        val x509Header = byteArrayOf(
            0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00
        )
        val spec = X509EncodedKeySpec(x509Header + rawKey)
        return KeyFactory.getInstance(X25519_ALGO).generatePublic(spec)
    }

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