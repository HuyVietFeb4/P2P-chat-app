// Noise__25519_ChaChaPoly_SHA256
package com.meshenger.backend.session

import android.util.Log
import com.google.crypto.tink.subtle.Hkdf
import com.meshenger.backend.network.EpidemicFlooding
import com.meshenger.backend.network.MessageType
import com.meshenger.backend.network.NetworkMessageListener
import com.meshenger.backend.transport2.StaticKeyManager
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.KeyPair
import java.security.PublicKey
import android.util.Base64

enum class NoisePattern {
    XX, // -> e | <- e, ee, s, es | -> s, se
    KK, // -> e, es, ss | <- e, ee, se
    XK  // -> e, es | <- e, ee | -> s, se
}
class TwoPartySession(
    private val isInitiator: Boolean,
    private val prologue: ByteArray,
    private val staticKey: Pair<ByteArray, ByteArray>,
    private val peerId: ULong,
    private val userName: String,
    private val receiverPublicKey: ByteArray? = null,
    private val chosenPattern: NoisePattern = NoisePattern.XX
) : Session(), NetworkMessageListener {

    private var handshakeState: HandshakeState? = HandshakeState(
        isInitiator,
        prologue,
        staticKey,
        chosenPattern,
        receiverPublicKey
    )
    private var sendingState: CipherState? = null
    private var receivingState: CipherState? = null

    // A queue for messages that the user tries to send while the handshake is still running
    private val pendingMessages = mutableListOf<String>()

    init {
        peers.add(Peer(userName, peerId))
        EpidemicFlooding.setListener(this)
        if (isInitiator) {
            startHandshake()
        }
    }

    private fun startHandshake() {
        val firstMessage = handshakeState?.writeMessage(ByteArray(0)) ?: return
        sendMessageHandShake(message = firstMessage)
    }

    private fun sendMessageHandShake(receiverMPAddress: ULong = peers[0].MPAddress, message: ByteArray) {
        val timeStamp = System.currentTimeMillis()
        val msgType = MessageType.NOISE_HANDSHAKE
        EpidemicFlooding.onTwoPartyMessageSend(message, timeStamp, receiverMPAddress,msgType)
    }

    private fun completeHandshake() {
        val (send, receive) = handshakeState!!.split()

        // Setup Transport states
        if (isInitiator) {
            this.sendingState = send
            this.receivingState = receive
        } else {
            this.sendingState = receive
            this.receivingState = send
        }

        this.handshakeState = null

        // Empty the queue: Send any messages the user typed while we were handshaking
        pendingMessages.forEach { sendMessageStr(peerId, it) }
        pendingMessages.clear()

        Log.d("TwoPartySession", "Secure tunnel established with $userName")
    }

    override fun sendMessageStr(receiverMPAddress: ULong, message: String) {
        val cipher = sendingState
        if (cipher == null) {
            // Handshake not ready; save it for later
            pendingMessages.add(message)
            return
        }
        val currentNonce = cipher.getCurrentNonce().toLong()
        val ciphertext = cipher.encryptWithAd(ByteArray(0), message.toByteArray())
        val base64Payload = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val timeStamp = System.currentTimeMillis()
        val jsonResult = buildJsonObject {
            put("PeerID", receiverMPAddress.toLong())
            put("Payload", base64Payload)
            put("Plaintext", message)
            put("Nonce", currentNonce)
            put("SessionType", "TwoPartyChat")
            put("Action", "Send")
        }
        _messageBus.tryEmit(jsonResult)
        EpidemicFlooding.onTwoPartyMessageSend(ciphertext, timeStamp, receiverMPAddress, MessageType.USER_MESSAGE_ONE_TO_ONE)
    }

    override fun receiveMessageStr(senderMPAddress: ULong, encryptedData: ByteArray, nonceTimeStamp: ULong) {
        val cipher = receivingState ?: return // Drop messages if not secure yet
        try {
            val currentNonce = cipher.getCurrentNonce().toLong()
            val plaintext = cipher.decryptWithAd(ByteArray(0), encryptedData)
            val base64Payload = Base64.encodeToString(encryptedData, Base64.NO_WRAP)
            val jsonResult = buildJsonObject {
                put("PeerID", senderMPAddress.toLong())
                put("Payload", base64Payload)
                put("Plaintext", String(plaintext, Charsets.UTF_8))
                put("Nonce", currentNonce)
                put("SessionType", "TwoPartyChat")
                put("Action", "Receive")
            }
            _messageBus.tryEmit(jsonResult)
        } catch (e: Exception) {
            Log.e("TwoPartySession", "Decryption failed (Tampered or Replay): ${e.message}")
        }
    }

    override fun onDirectMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {
        this.receiveMessageStr(senderID, payload, timeStamp)
    }

    /**
     * Handles incoming handshake packets
     */
    override fun onReceiveMessageHandShake(senderMPAddress: ULong, message: ByteArray) {
        val hs = handshakeState ?: return
        try {
            hs.readMessage(message)
            if (!hs.isFinished && ((isInitiator && hs.step % 2 == 0) || (!isInitiator && hs.step % 2 != 0))) {
                val response = hs.writeMessage(ByteArray(0))
                sendMessageHandShake(senderMPAddress, response)
            }
            if (hs.isFinished) completeHandshake()
        } catch (e: Exception) {
            Log.e("TwoPartySession", "Handshake Security Error: ${e.message}")
        }
    }

    fun getSendingKey(): ByteArray? {
        return sendingState?.getKey()
    }

    fun getRecievingKey(): ByteArray? {
        return receivingState?.getKey()
    }
}

