package com.meshenger.backend.session

import com.meshenger.backend.transport2.StaticKeyManager
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.KeyPair
import java.security.KeyPairGenerator
import javax.crypto.KeyAgreement

class HandshakeState(
    private val isInitiator: Boolean,
    private val prologue: ByteArray,
    private val s: KeyPair,
    private val pattern: NoisePattern,
    private val rs: ByteArray? = null // Remote Static Public Key (required for KK/XK)
) {
    private val symmetricState = SymmetricState()
    private var e: KeyPair? = null
    private var re: ByteArray? = null
    private var remoteStatic: ByteArray? = rs
    private var messageIndex = 0

    init {
        val protocolName = "Noise_${pattern.name}_25519_ChaChaPoly_SHA256"
        symmetricState.initializeSymmetric(protocolName)
        symmetricState.mixHash(prologue)

        // For KK and XK, the remote static key is known beforehand
        if ((pattern == NoisePattern.KK || pattern == NoisePattern.XK) && remoteStatic != null) {
            symmetricState.mixHash(remoteStatic!!)
        }

        // For KK, the initiator's static key is also known by the responder
        if (pattern == NoisePattern.KK) {
            symmetricState.mixHash(StaticKeyManager.getRawPublicKey(s.public))
        }
    }

    fun writeMessage(payload: ByteArray): ByteArray {
        val output = ByteArrayOutputStream()
        when (pattern) {
            NoisePattern.XX -> writeXX(output)
            NoisePattern.KK -> writeKK(output)
            NoisePattern.XK -> writeXK(output)
        }
        output.write(symmetricState.encryptAndHash(payload))
        messageIndex++
        return output.toByteArray()
    }

    fun readMessage(message: ByteArray): ByteArray {
        val input = ByteBuffer.wrap(message)
        val payload = when (pattern) {
            NoisePattern.XX -> readXX(input)
            NoisePattern.KK -> readKK(input)
            NoisePattern.XK -> readXK(input)
            else -> ByteArray(0)
        }
        messageIndex++
        return payload
    }

    // --- Pattern Specific Logic (Writing) ---

    private fun writeXX(output: ByteArrayOutputStream) {
        when (messageIndex) {
            0 -> { // -> e
                e = generateEphemeralKeyPair()
                val pubE = getRawPublicKey(e!!)
                symmetricState.mixHash(pubE)
                output.write(pubE)
            }
            1 -> { // <- e, ee, s, es
                e = generateEphemeralKeyPair()
                val pubE = getRawPublicKey(e!!)
                symmetricState.mixHash(pubE)
                output.write(pubE)
                symmetricState.mixKey(diffieHellman(e!!, re!!)) // ee
                output.write(symmetricState.encryptAndHash(getRawPublicKey(s))) // s
                symmetricState.mixKey(diffieHellman(s, re!!)) // es
            }
            2 -> { // -> s, se
                output.write(symmetricState.encryptAndHash(getRawPublicKey(s))) // s
                symmetricState.mixKey(diffieHellman(e!!, remoteStatic!!)) // se
            }
        }
    }

    private fun writeKK(output: ByteArrayOutputStream) {
        if (messageIndex == 0) { // -> e, es, ss
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(e!!, remoteStatic!!)) // es
            symmetricState.mixKey(diffieHellman(s, remoteStatic!!)) // ss
        } else if (messageIndex == 1) { // <- e, ee, se
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(e!!, re!!)) // ee
            symmetricState.mixKey(diffieHellman(e!!, remoteStatic!!)) // se
        }
    }

    private fun writeXK(output: ByteArrayOutputStream) {
        if (messageIndex == 0) { // -> e, es
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(e!!, remoteStatic!!)) // es
        } else if (messageIndex == 1) { // <- e, ee
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(e!!, re!!)) // ee
        } else if (messageIndex == 2) { // -> s, se
            output.write(symmetricState.encryptAndHash(getRawPublicKey(s))) // s
            symmetricState.mixKey(diffieHellman(e!!, re!!)) // se
        }
    }

    // --- Pattern Specific Logic (Reading) ---
    private fun readXX(input: ByteBuffer): ByteArray {
        return when (messageIndex) {
            0 -> { // Expecting -> e
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            1 -> { // Expecting <- e, ee, s, es
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)
                symmetricState.mixKey(diffieHellman(e!!, re!!)) // ee

                val encryptedS = ByteArray(32 + 16)
                input.get(encryptedS)
                remoteStatic = symmetricState.decryptAndHash(encryptedS) // rs
                symmetricState.mixKey(diffieHellman(e!!, remoteStatic!!)) // es

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            2 -> { // Expecting -> s, se
                val encryptedS = ByteArray(32 + 16)
                input.get(encryptedS)
                remoteStatic = symmetricState.decryptAndHash(encryptedS) // rs
                symmetricState.mixKey(diffieHellman(e!!, remoteStatic!!)) // se

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            else -> ByteArray(0)
        }
    }

    private fun readKK(input: ByteBuffer): ByteArray {
        return when (messageIndex) {
            0 -> { // Expecting -> e, es, ss
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)
                symmetricState.mixKey(diffieHellman(s, re!!)) // es
                symmetricState.mixKey(diffieHellman(s, remoteStatic!!)) // ss

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            1 -> { // Expecting <- e, ee, se
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)
                symmetricState.mixKey(diffieHellman(e!!, re!!)) // ee
                symmetricState.mixKey(diffieHellman(s, re!!)) // se

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            else -> ByteArray(0)
        }
    }

    private fun readXK(input: ByteBuffer): ByteArray {
        return when (messageIndex) {
            0 -> { // Expecting -> e, es
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)
                symmetricState.mixKey(diffieHellman(s, re!!)) // es

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            1 -> { // Expecting <- e, ee
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)
                symmetricState.mixKey(diffieHellman(e!!, re!!)) // ee

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            2 -> { // Expecting -> s, se
                val encryptedS = ByteArray(32 + 16)
                input.get(encryptedS)
                remoteStatic = symmetricState.decryptAndHash(encryptedS)
                symmetricState.mixKey(diffieHellman(e!!, remoteStatic!!)) // se

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            else -> ByteArray(0)
        }
    }

    // Helper to get raw bytes from s.public
    private fun getRawPublicKey(pair: KeyPair): ByteArray = StaticKeyManager.getRawPublicKey(pair.public)
    // Helper to perform X25519 DH

    private fun diffieHellman(local: KeyPair, remotePublic: ByteArray): ByteArray {
        val agreement = KeyAgreement.getInstance("X25519")
        agreement.init(local.private)
        val remotePublicKey = StaticKeyManager.decodeRawAgreementPublicKey(remotePublic)
        agreement.doPhase(remotePublicKey, true)
        return agreement.generateSecret()
    }

    private fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("X25519")
        val keyPair = kpg.generateKeyPair()
        return keyPair
    }

    val isFinished: Boolean get() = when(pattern) {
        NoisePattern.XX, NoisePattern.XK -> messageIndex >= 3
        NoisePattern.KK -> messageIndex >= 2
    }
    fun split(): Pair<CipherState, CipherState> {
        return symmetricState.split()
    }
    val step: Int get() = messageIndex
}