package com.meshenger.backend.session
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.crypto.tink.subtle.Hkdf
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.transport2.StaticKeyManager
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom

@RunWith(AndroidJUnit4::class)
class TwoPartySessionTest {
    private val SALT = "This-is-salt-value-to-generate-routing-id".encodeToByteArray()
    private val INFO_PREFIX = "Meshenger-v1-generate-routing-id".encodeToByteArray()
    private fun getRandomMPAddress(
        epochHours: Int = 2,
        length: Int = 8
    ): ByteArray {
        val generator = Ed25519KeyPairGenerator()
        val random = SecureRandom() // Use a strong random source
        val params = Ed25519KeyGenerationParameters(random)
        generator.init(params)
        val keyPair = generator.generateKeyPair()
        val publicKey = (keyPair.public as Ed25519PublicKeyParameters).getEncoded() // 32 bytes

        val epochSeconds = epochHours * 3600
        val currentTimeStamp = System.currentTimeMillis() / 1000
        val epochIndex = currentTimeStamp / epochSeconds

        val epochBytes = ByteBuffer.allocate(8).putLong(epochIndex).array()
        val stream = ByteArrayOutputStream()
        stream.write(INFO_PREFIX)
        stream.write(epochBytes)
        stream.write(NativeCredentials.getAppSecretKey().encodeToByteArray())

        val info = stream.toByteArray()
        return Hkdf.computeHkdf(
            "HmacSHA256",
            publicKey, // IKM
            SALT,           // Salt
            info,           // Info
            length          // Output Length
        )
    }

    @Test
    fun testSetUpXXSession_FullBidirectionalExchange() {
        // --- 1. Setup Identities ---
        val prologue = "TwoPartySessionMeshengerV1".toByteArray()
        val chosenPattern = NoisePattern.XX

        // Phone A Setup
        val phoneAStaticKey = StaticKeyManager.generateX25519RandomKeyPair()
        val phoneAhandshakeState = HandshakeState(
            isInitiator = true,
            prologue = prologue,
            s = phoneAStaticKey,
            pattern = chosenPattern
        )

        // Phone B Setup
        val phoneBStaticKey = StaticKeyManager.generateX25519RandomKeyPair()
        val phoneBhandshakeState = HandshakeState(
            isInitiator = false,
            prologue = prologue,
            s = phoneBStaticKey,
            pattern = chosenPattern
        )

        // --- 2. Handshake Phase (XX: 3-step exchange) ---

        // Step 1: A -> B (e)
        val msg1 = phoneAhandshakeState.writeMessage(ByteArray(0))
        phoneBhandshakeState.readMessage(msg1)

        // Step 2: B -> A (e, ee, s, es)
        val msg2 = phoneBhandshakeState.writeMessage(ByteArray(0))
        phoneAhandshakeState.readMessage(msg2)

        // Step 3: A -> B (s, se)
        val msg3 = phoneAhandshakeState.writeMessage(ByteArray(0))
        phoneBhandshakeState.readMessage(msg3)

        // --- 3. Split Phase ---
        // After 3 steps, XX is complete. Split into transport ciphers.
        val (sendA, receiveA) = phoneAhandshakeState.split()
        val (receiveB, sendB) = phoneBhandshakeState.split()

        // --- 4. Data Transport Verification ---

        // Test A sending to B
        val payloadAtoB = "Hello Phone B, this is a secure message.".toByteArray(Charsets.UTF_8)
        val cipherA = sendA.encryptWithAd(ByteArray(0), payloadAtoB)
        val decryptedB = receiveB.decryptWithAd(ByteArray(0), cipherA)

        assertArrayEquals("Phone B failed to decrypt A's message", payloadAtoB, decryptedB)
        assertEquals("Decrypted content mismatch", String(payloadAtoB), String(decryptedB))

        // Test B sending to A (Verification of Bidirectional Keys)
        val payloadBtoA = "General Kenobi! You are a bold one.".toByteArray(Charsets.UTF_8)
        val cipherB = sendB.encryptWithAd(ByteArray(0), payloadBtoA)
        val decryptedA = receiveA.decryptWithAd(ByteArray(0), cipherB)

        assertArrayEquals("Phone A failed to decrypt B's message", payloadBtoA, decryptedA)
        assertEquals("Decrypted content mismatch", String(payloadBtoA), String(decryptedA))

        // --- 5. Verify Authentication ---
        // Verify that B now knows A's static public key and vice versa
        assertArrayEquals(phoneAStaticKey.first, phoneBhandshakeState.getRemoteStaticKey())
        assertArrayEquals(phoneBStaticKey.first, phoneAhandshakeState.getRemoteStaticKey())
    }

    @Test
    fun testSetUpXKSession_PreKnownResponderKey() {
        val prologue = "TwoPartySessionMeshengerV1".toByteArray()
        val chosenPattern = NoisePattern.XK

        // 1. Setup Identities
        val phoneAStaticKey = StaticKeyManager.generateX25519RandomKeyPair()
        val phoneBStaticKey = StaticKeyManager.generateX25519RandomKeyPair()

        // 2. Handshake Setup: Phone A MUST know Phone B's Public Key beforehand
        val phoneAhandshakeState = HandshakeState(
            isInitiator = true,
            prologue = prologue,
            s = phoneAStaticKey,
            rs = phoneBStaticKey.first, // A knows B's public key
            pattern = chosenPattern
        )

        val phoneBhandshakeState = HandshakeState(
            isInitiator = false,
            prologue = prologue,
            s = phoneBStaticKey,
            pattern = chosenPattern
        )

        // 3. Handshake Phase (XK: 2-step exchange)
        // Step 1: A -> B (e, es)
        val msg1 = phoneAhandshakeState.writeMessage(ByteArray(0))
        phoneBhandshakeState.readMessage(msg1)

        // Step 2: B -> A (e, ee, s, es)
        val msg2 = phoneBhandshakeState.writeMessage(ByteArray(0))
        phoneAhandshakeState.readMessage(msg2)

        // 4. Split and Verify
        val (sendA, receiveA) = phoneAhandshakeState.split()
        val (receiveB, sendB) = phoneBhandshakeState.split()

        val secret = "Secret message in XK pattern".toByteArray()
        val cipher = sendA.encryptWithAd(ByteArray(0), secret)
        val decrypted = receiveB.decryptWithAd(ByteArray(0), cipher)

        assertEquals(String(secret), String(decrypted))
    }
    @Test
    fun testSetUpKKSession_BothKeysKnown() {
        val prologue = "TwoPartySessionMeshengerV1".toByteArray()
        val chosenPattern = NoisePattern.KK

        // 1. Setup Identities
        val phoneAStaticKey = StaticKeyManager.generateX25519RandomKeyPair()
        val phoneBStaticKey = StaticKeyManager.generateX25519RandomKeyPair()

        // 2. Handshake Setup: Both must provide the remote public key
        val phoneAhandshakeState = HandshakeState(
            isInitiator = true,
            prologue = prologue,
            s = phoneAStaticKey,
            rs = phoneBStaticKey.first, // A knows B
            pattern = chosenPattern
        )

        val phoneBhandshakeState = HandshakeState(
            isInitiator = false,
            prologue = prologue,
            s = phoneBStaticKey,
            rs = phoneAStaticKey.first, // B knows A
            pattern = chosenPattern
        )

        // 3. Handshake Phase (KK: 2-step exchange)
        // Step 1: A -> B (e, es, ss)
        val msg1 = phoneAhandshakeState.writeMessage(ByteArray(0))
        phoneBhandshakeState.readMessage(msg1)

        // Step 2: B -> A (e, ee, se)
        val msg2 = phoneBhandshakeState.writeMessage(ByteArray(0))
        phoneAhandshakeState.readMessage(msg2)

        // 4. Split and Verify
        val (sendA, receiveA) = phoneAhandshakeState.split()
        val (receiveB, sendB) = phoneBhandshakeState.split()

        val secret = "Direct secure link via KK".toByteArray()
        val cipher = sendB.encryptWithAd(ByteArray(0), secret)
        val decrypted = receiveA.decryptWithAd(ByteArray(0), cipher)

        assertEquals(String(secret), String(decrypted))
    }
}