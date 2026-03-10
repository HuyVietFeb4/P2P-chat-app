package com.meshenger.backend.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import android.util.Log
// To do: to init the static public and private's key
object StaticKeyManager {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private const val STATIC_IDENTITY_ALIAS = "static_identity_ed25519"
    private const val ED25519_ALGO = "Ed25519"


    fun getOrCreateIdentityKey(): KeyPair {
        if(keyStore.containsAlias(STATIC_IDENTITY_ALIAS)) {
            val privateKey = keyStore.getKey(STATIC_IDENTITY_ALIAS, null) as java.security.PrivateKey
            val publicKey = keyStore.getCertificate(STATIC_IDENTITY_ALIAS).publicKey
            return KeyPair(publicKey, privateKey)
        }
        val kpg = KeyPairGenerator.getInstance(ED25519_ALGO, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            STATIC_IDENTITY_ALIAS,
            KeyProperties.PURPOSE_AGREE_KEY
        ).apply {
            setUserAuthenticationRequired(false)
        }.build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    fun getRawIdentityPublicKey(publicKey: java.security.PublicKey): ByteArray {
        val encoded = publicKey.encoded
        return encoded.takeLast(32).toByteArray()
    }

    fun decodeRawIdentityPublicKey(rawKey: ByteArray): java.security.PublicKey {
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

    fun signData(data: ByteArray, privateKey: java.security.PrivateKey): ByteArray {
        val s = Signature.getInstance("Ed25519")
        s.initSign(privateKey)
        s.update(data)
        return s.sign()
    }

    fun verifyData(data: ByteArray, signature:ByteArray, publicKey: java.security.PublicKey): Boolean {
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