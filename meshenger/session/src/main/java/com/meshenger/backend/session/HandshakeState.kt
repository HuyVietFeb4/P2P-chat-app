package com.meshenger.backend.session


import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom

class HandshakeState(
    private val isInitiator: Boolean,
    private val prologue: ByteArray,
    private val s: Pair<ByteArray, ByteArray>,
    private val pattern: NoisePattern,
    private val rs: ByteArray? = null // Remote Static Public Key (required for KK/XK)
) {
    private val symmetricState = SymmetricState()
    private var e: Pair<ByteArray, ByteArray>? = null
    private var re: ByteArray? = null
    private var remoteStatic: ByteArray? = rs
    private var messageIndex = 0

    init {
        val protocolName = "Noise_${pattern.name}_25519_ChaChaPoly_SHA256"
        symmetricState.initializeSymmetric(protocolName)
        symmetricState.mixHash(prologue)

        // For KK and XK, the remote static key is known beforehand
        if (pattern == NoisePattern.XK) {
            if (isInitiator) {
                // Initiator knows Responder's key
                symmetricState.mixHash(rs!!)
            } else {
                // Responder knows their own key is pre-known by the other side
                symmetricState.mixHash(getRawPublicKey(s))
            }
        }

        if (pattern == NoisePattern.KK) {
            // ALWAYS mix Initiator's static key first
            val initiatorStatic = if (isInitiator) getRawPublicKey(s) else rs!!
            symmetricState.mixHash(initiatorStatic)

            // ALWAYS mix Responder's static key second
            val responderStatic = if (isInitiator) rs!! else getRawPublicKey(s)
            symmetricState.mixHash(responderStatic)
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
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), re!!)) // ee
                output.write(symmetricState.encryptAndHash(getRawPublicKey(s))) // s
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), re!!)) // es
            }
            2 -> { // -> s, se
                output.write(symmetricState.encryptAndHash(getRawPublicKey(s))) // s
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), re!!)) // se
            }
        }
    }

    private fun writeKK(output: ByteArrayOutputStream) {
        if (messageIndex == 0) { // -> e, es, ss
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), remoteStatic!!)) // es
            symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), remoteStatic!!)) // ss
        } else if (messageIndex == 1) { // <- e, ee, se
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), re!!)) // ee
            symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), remoteStatic!!)) // se
        }
    }

    private fun writeXK(output: ByteArrayOutputStream) {
        if (messageIndex == 0) { // -> e, es
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), remoteStatic!!)) // es
        } else if (messageIndex == 1) { // <- e, ee
            e = generateEphemeralKeyPair()
            val pubE = getRawPublicKey(e!!)
            symmetricState.mixHash(pubE)
            output.write(pubE)
            symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), re!!)) // ee
        } else if (messageIndex == 2) { // -> s, se
            output.write(symmetricState.encryptAndHash(getRawPublicKey(s))) // s
            symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), re!!)) // se
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
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), re!!)) // ee

                val encryptedS = ByteArray(32 + 16)
                input.get(encryptedS)
                remoteStatic = symmetricState.decryptAndHash(encryptedS) // rs
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), remoteStatic!!)) // es

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            2 -> { // Expecting -> s, se
                val encryptedS = ByteArray(32 + 16)
                input.get(encryptedS)
                remoteStatic = symmetricState.decryptAndHash(encryptedS) // rs
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), remoteStatic!!)) // se

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
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), re!!)) // es
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), remoteStatic!!)) // ss

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            1 -> { // Expecting <- e, ee, se
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), re!!)) // ee
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), re!!)) // se

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
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(s), re!!)) // es

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            1 -> { // Expecting <- e, ee
                val tempRe = ByteArray(32)
                input.get(tempRe)
                re = tempRe
                symmetricState.mixHash(re!!)
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), re!!)) // ee

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            2 -> { // Expecting -> s, se
                val encryptedS = ByteArray(32 + 16)
                input.get(encryptedS)
                remoteStatic = symmetricState.decryptAndHash(encryptedS)
                symmetricState.mixKey(diffieHellman(getRawPrivateKey(e!!), remoteStatic!!)) // se

                val payloadCipher = ByteArray(input.remaining())
                input.get(payloadCipher)
                symmetricState.decryptAndHash(payloadCipher)
            }
            else -> ByteArray(0)
        }
    }

    // Helper to get raw bytes from s.public
    private fun getRawPublicKey(keyPair: Pair<ByteArray, ByteArray>): ByteArray = keyPair.first
    private fun getRawPrivateKey(keyPair: Pair<ByteArray, ByteArray>): ByteArray = keyPair.second
    // Helper to perform X25519 DH

    private fun diffieHellman(localPrivate: ByteArray, remotePublic: ByteArray): ByteArray {
        val myPrivateKey = X25519PrivateKeyParameters(localPrivate, 0)
        val theirPublicKey = X25519PublicKeyParameters(remotePublic, 0)

        val agreement = X25519Agreement()
        agreement.init(myPrivateKey)

        // 1. Pre-allocate a 32-byte array for the secret
        val sharedSecret = ByteArray(agreement.agreementSize)

        // 2. Calculate the agreement.
        // This writes into 'sharedSecret' starting at offset 0.
        agreement.calculateAgreement(theirPublicKey, sharedSecret, 0)

        // 3. Return the byte array shared secret key
        return sharedSecret
    }

    private fun generateEphemeralKeyPair(): Pair<ByteArray, ByteArray> {
        val generator = X25519KeyPairGenerator()
        val random = SecureRandom() // Use a strong random source
        val params = X25519KeyGenerationParameters(random)
        generator.init(params)
        val keyPair = generator.generateKeyPair()
        val publicKey = (keyPair.public as X25519PublicKeyParameters).getEncoded() // 32 bytes
        val privateKey = (keyPair.private as X25519PrivateKeyParameters).getEncoded() // 32 bytes
        return Pair(publicKey, privateKey)
    }

    val isFinished: Boolean get() = when(pattern) {
        NoisePattern.XX, NoisePattern.XK -> messageIndex >= 3
        NoisePattern.KK -> messageIndex >= 2
    }
    fun split(): Pair<CipherState, CipherState> {
        return symmetricState.split()
    }
    val step: Int get() = messageIndex

    fun getRemoteStaticKey(): ByteArray? {
        return this.remoteStatic
    }
}