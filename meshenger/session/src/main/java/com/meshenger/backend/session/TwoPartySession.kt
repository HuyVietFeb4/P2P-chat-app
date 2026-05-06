// Noise_{XX,KK,XK}_25519_ChaChaPoly_SHA256
package com.meshenger.backend.session

import android.util.Log
import com.meshenger.backend.network.EpidemicFlooding
import com.meshenger.backend.network.MessageType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import android.util.Base64
import com.meshenger.backend.network.ListenerRegistry
import com.meshenger.backend.network.PacketSigner
import com.meshenger.backend.network.TwoPartyMessageListener
import java.security.PublicKey

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
    private val chosenPattern: NoisePattern = NoisePattern.XX,
    private var remotePublicIdentityKey: PublicKey? = null
) : Session(), TwoPartyMessageListener, AutoCloseable {

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
        ListenerRegistry.registerTwoPartyListener(peerId, this)
        if (isInitiator) {
            startHandshake()
        }
    }
    override fun close() {
        cancelMessageBusEmitter()
        // Unregister from the singleton to prevent memory leaks
        ListenerRegistry.unregisterTwoPartyListener(peerId)
        Log.d("TwoPartySession", "Session with $userName closed and unregistered.")
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
    fun initRemoteIdentityKey(key: PublicKey) {
        remotePublicIdentityKey = key
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
        offerMessageBus(jsonResult)
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
            offerMessageBus(jsonResult)
        } catch (e: Exception) {
            Log.e("TwoPartySession", "Decryption failed (Tampered or Replay): ${e.message}")
        }
    }

    override fun onDirectMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong, signature: ByteArray, signedData: ByteArray) {
        val currentKey = remotePublicIdentityKey
        if (currentKey != null) {
            if (!PacketSigner.verifyTwoPartySession(signedData, signature, currentKey)) return
        }
        // When no Ed25519 peer binding yet, authenticity is enforced by PacketSigner.verifyDirectProtocolKey()
        // in EpidemicFlooding before this callback (same shared key as handshake).
        receiveMessageStr(senderID, payload, timeStamp)
    }

    /**
     * Handles incoming handshake packets
     */
    override fun onReceiveMessageHandShake(senderID: ULong, message: ByteArray) {
        if(senderID != peers[0].MPAddress) return
        val hs = handshakeState ?: return
        try {
            hs.readMessage(message)
            if (!hs.isFinished && ((isInitiator && hs.step % 2 == 0) || (!isInitiator && hs.step % 2 != 0))) {
                val response = hs.writeMessage(ByteArray(0))
                sendMessageHandShake(senderID, response)
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

