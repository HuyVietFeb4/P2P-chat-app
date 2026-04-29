package com.meshenger.backend.session
import com.google.crypto.tink.subtle.Hkdf
import java.security.MessageDigest

class SymmetricState {
    private val HASH_LEN = 32
    private var cipherState = CipherState()
    private var ck: ByteArray = ByteArray(HASH_LEN)
    private var h: ByteArray = ByteArray(HASH_LEN)

    fun initializeSymmetric(protocolName: String) {
        val protocolBytes = protocolName.toByteArray(Charsets.UTF_8)
        if (protocolBytes.size <= HASH_LEN) {
            h = protocolBytes.copyOf(HASH_LEN)
        } else {
            val md = MessageDigest.getInstance("SHA-256")
            h = md.digest(protocolBytes)
        }
        ck = h.copyOf()
        cipherState.initializeKey(null)
    }

    //Purpose: To incorporate a new secret into the session.
    //Logic: It takes the output of a Diffie-Hellman (DH) operation and runs it through HKDF.
    //Result: It produces a new ck and a new encryption key k for the cipherState.
    fun mixKey(inputKeyMaterial: ByteArray) {
        val derivedKeys = Hkdf.computeHkdf(
            "HmacSHA256",
            inputKeyMaterial, // IKM
            ck,               // Salt
            ByteArray(0),     // Info (context)
            64                // Total bytes needed (32 for ck + 32 for k)
        )
        ck = derivedKeys.sliceArray(0 until 32)
        val tempK = derivedKeys.sliceArray(32 until 64)
        cipherState.initializeKey(tempK)
    }

    //Purpose: To update the "fingerprint" of the session.
    //Logic: h = Hash(h + data).
    //Usage: You call this whenever you send or receive cleartext data (like an ephemeral public key).
    fun mixHash(data: ByteArray) {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(h)
        md.update(data)
        h = md.digest()
    }

    //Purpose: To securely send sensitive data during the handshake.
    //Logic: It encrypts the data using h as the Associated Data, then immediately calls mixHash(ciphertext).
    //Why: This ensures the ciphertext is bound to the current handshake hash.
    fun encryptAndHash(plaintext: ByteArray): ByteArray {
        val ciphertext = cipherState.encryptWithAd(h, plaintext)
        mixHash(ciphertext)
        return ciphertext
    }
    //Purpose: To read sensitive data sent by the other party.
    //Logic: The reverse of the above. It decrypts, then hashes the ciphertext into h.
    fun decryptAndHash(ciphertext: ByteArray): ByteArray {
        val plaintext = cipherState.decryptWithAd(h, ciphertext)
        mixHash(ciphertext)
        return plaintext
    }
    //Purpose: The "Exit Strategy."
    //Logic: Once the handshake is over, it uses the final ck to derive two final CipherState objects.
    //Usage: One is used for sending transport messages and the other for receiving. This is where you move from "Handshake" to "Chatting."
    fun split(): Pair<CipherState, CipherState> {
        val derivedKeys = Hkdf.computeHkdf(
            "HmacSHA256",
            ByteArray(0), // Empty IKM for split
            ck,           // Current ck as salt
            ByteArray(0),
            64            // Two 32-byte keys
        )
        // Truncate if necessary and create two new CipherStates
        val tempK1 = derivedKeys.sliceArray(0 until 32)
        val tempK2 = derivedKeys.sliceArray(32 until 64)
        val cipherStateSend = CipherState()
        val cipherStateReceive = CipherState()
        cipherStateSend.initializeKey(tempK1)
        cipherStateReceive.initializeKey(tempK2)
        return Pair(cipherStateSend, cipherStateReceive)
    }
}