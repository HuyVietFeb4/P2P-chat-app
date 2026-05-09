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
    XK; // -> e, es | <- e, ee | -> s, se

    companion object {
        // 1-byte wire tag prefixed to every NOISE_HANDSHAKE payload so the responder
        // can know which Noise pattern the initiator picked locally (avoids silent
        // mismatches when only one side has the peer's persisted static key).
        fun fromTag(tag: Byte): NoisePattern? = when (tag) {
            0x01.toByte() -> XX
            0x02.toByte() -> KK
            0x03.toByte() -> XK
            else -> null
        }
    }

    fun toTag(): Byte = when (this) {
        XX -> 0x01
        KK -> 0x02
        XK -> 0x03
    }
}
class TwoPartySession(
    private val isInitiator: Boolean,
    private val prologue: ByteArray,
    private val staticKey: Pair<ByteArray, ByteArray>,
    private val peerId: ULong,
    private val userName: String,
    private val receiverPublicKey: ByteArray? = null,
    val chosenPattern: NoisePattern = NoisePattern.XX,
    private var remotePublicIdentityKey: PublicKey? = null,
    /**
     * When true (default), the initiator sends Noise msg 1 immediately from `init`. Set to
     * false when the caller wants to defer the send until the mesh transport is ready
     * (e.g. just after an app restart, when BLE has not yet rediscovered any peers and the
     * outbound map is empty — sending now would silently lose the packet).
     */
    private val autoStartHandshake: Boolean = true,
) : Session(), TwoPartyMessageListener, AutoCloseable {

    private companion object {
        // Filter Logcat: `MeshengerOTO` — luồng 1-1 từ device scan đến message.
        const val OTO_TAG = "MeshengerOTO"
    }

    private var handshakeState: HandshakeState? = HandshakeState(
        isInitiator,
        prologue,
        staticKey,
        chosenPattern,
        receiverPublicKey
    )
    private var sendingState: CipherState? = null
    private var receivingState: CipherState? = null

    /** True once the Noise handshake has finished (transport keys are live). */
    val isHandshakeFinished: Boolean get() = handshakeState == null && sendingState != null

    // A queue for messages that the user tries to send while the handshake is still running
    private val pendingMessages = mutableListOf<String>()

    /**
     * Fired exactly once when the Noise handshake completes successfully. The byte array
     * is the peer's static X25519 public key learned during the handshake (XX/XK), or
     * the value passed in for KK. Caller (Application layer) can persist this to enable
     * KK on subsequent reconnects without redoing XX.
     */
    @Volatile
    var onHandshakeCompleted: ((remoteStatic: ByteArray?) -> Unit)? = null

    /**
     * Fired when the application layer must replace this session with a fresh responder session
     * and feed the inbound [message] into it. Two cases:
     *
     * 1) Pattern tag differs from [chosenPattern] (e.g. stale XX in memory, peer sends KK).
     * 2) [chosenPattern] matches but transport was already established and the peer sent a new
     * handshake anyway (e.g. they restarted the app — KK msg 1 again while we still have KK
     * cipher states). EpidemicFlooding delivers to this listener, so the handshake fallback
     * never runs unless we delegate here.
     *
     * NOTE: invoked synchronously from the network thread; keep the handler non-blocking.
     */
    @Volatile
    var onPatternMismatch: ((senderId: ULong, message: ByteArray, requestedPattern: NoisePattern) -> Unit)? = null

    /**
     * Fired when we (an initiator) receive a NOISE_HANDSHAKE that we can't process: the wire
     * pattern tag matched our [chosenPattern], but the bytes don't decrypt at the step we're
     * at. This almost always means the peer also opened the chat at the same time and is
     * also acting as initiator — so we both wrote msg 0 and now both received what we expect
     * to be msg 1. The application layer should apply an MP tie-break: typically the side
     * with the larger MP backs down, recreates as responder, and feeds the bytes back in.
     */
    @Volatile
    var onSimultaneousInitiate: ((senderId: ULong, message: ByteArray, pattern: NoisePattern) -> Unit)? = null

    init {
        peers.add(Peer(userName, peerId))
        ListenerRegistry.registerTwoPartyListener(peerId, this)
        Log.d(
            OTO_TAG,
            "TwoPartySession init userName=$userName peerMp=$peerId pattern=$chosenPattern " +
                "isInitiator=$isInitiator autoStartHandshake=$autoStartHandshake",
        )
        if (isInitiator && autoStartHandshake) {
            startHandshake()
        }
    }

    /**
     * Triggers the initiator's first Noise message. Public so the application layer can
     * defer it until the mesh transport is actually ready to forward the packet.
     * Idempotent for non-initiators / already-finished sessions: returns early without effect.
     */
    fun startHandshakeIfNeeded() {
        if (!isInitiator) return
        val hs = handshakeState ?: return
        if (hs.step != 0) return
        startHandshake()
    }
    override fun close() {
        cancelMessageBusEmitter()
        // Unregister from the singleton to prevent memory leaks
        ListenerRegistry.unregisterTwoPartyListener(peerId)
        Log.d(OTO_TAG, "TwoPartySession close userName=$userName peerMp=$peerId pattern=$chosenPattern")
    }

    private fun startHandshake() {
        val firstMessage = handshakeState?.writeMessage(ByteArray(0)) ?: return
        Log.d(
            OTO_TAG,
            "HS write step=1 (initiator first msg) pattern=$chosenPattern bytes=${firstMessage.size} peerMp=$peerId",
        )
        sendMessageHandShake(message = firstMessage)
    }

    private fun sendMessageHandShake(receiverMPAddress: ULong = peers[0].MPAddress, message: ByteArray) {
        val timeStamp = System.currentTimeMillis()
        val msgType = MessageType.NOISE_HANDSHAKE
        // Wire format: [1 byte pattern tag][noise message bytes]. Receiver strips the tag
        // before feeding the bytes back into HandshakeState.readMessage().
        val tagged = ByteArray(message.size + 1)
        tagged[0] = chosenPattern.toTag()
        System.arraycopy(message, 0, tagged, 1, message.size)
        Log.d(
            OTO_TAG,
            "HS send peerMp=$receiverMPAddress pattern=$chosenPattern wireBytes=${tagged.size} (incl. 1B tag)",
        )
        EpidemicFlooding.onTwoPartyMessageSend(tagged, timeStamp, receiverMPAddress, msgType)
    }

    private fun completeHandshake() {
        val hs = handshakeState ?: return
        // Capture before split() — split clears internal state in some implementations.
        val remoteStatic = hs.getRemoteStaticKey()
        val (send, receive) = hs.split()

        // Setup Transport states
        if (isInitiator) {
            this.sendingState = send
            this.receivingState = receive
        } else {
            this.sendingState = receive
            this.receivingState = send
        }

        this.handshakeState = null

        Log.i(
            OTO_TAG,
            "HS complete userName=$userName peerMp=$peerId pattern=$chosenPattern " +
                "role=${if (isInitiator) "initiator" else "responder"} " +
                "queuedMessages=${pendingMessages.size}",
        )

        // Empty the queue: Send any messages the user typed while we were handshaking
        pendingMessages.forEach { sendMessageStr(peerId, it) }
        pendingMessages.clear()

        try {
            onHandshakeCompleted?.invoke(remoteStatic)
        } catch (e: Exception) {
            Log.w(OTO_TAG, "onHandshakeCompleted listener threw: ${e.message}")
        }
    }
    fun initRemoteIdentityKey(key: PublicKey) {
        remotePublicIdentityKey = key
    }
    override fun sendMessageStr(receiverMPAddress: ULong, message: String) {
        val cipher = sendingState
        if (cipher == null) {
            // Handshake not ready; save it for later
            Log.d(
                OTO_TAG,
                "MSG send peerMp=$receiverMPAddress QUEUED (handshake not ready, pattern=$chosenPattern, " +
                    "queueSize=${pendingMessages.size + 1})",
            )
            pendingMessages.add(message)
            return
        }
        val currentNonce = cipher.getCurrentNonce().toLong()
        val ciphertext = cipher.encryptWithAd(ByteArray(0), message.toByteArray())
        val base64Payload = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val timeStamp = System.currentTimeMillis()
        Log.d(
            OTO_TAG,
            "MSG send peerMp=$receiverMPAddress encryptedBytes=${ciphertext.size} nonce=$currentNonce " +
                "pattern=$chosenPattern",
        )
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
        val cipher = receivingState
        if (cipher == null) {
            Log.w(
                OTO_TAG,
                "MSG recv peerMp=$senderMPAddress DROP (cipher not ready, pattern=$chosenPattern, " +
                    "encryptedBytes=${encryptedData.size})",
            )
            return // Drop messages if not secure yet
        }
        try {
            val currentNonce = cipher.getCurrentNonce().toLong()
            val plaintext = cipher.decryptWithAd(ByteArray(0), encryptedData)
            Log.d(
                OTO_TAG,
                "MSG recv peerMp=$senderMPAddress encryptedBytes=${encryptedData.size} " +
                    "plaintextBytes=${plaintext.size} nonce=$currentNonce pattern=$chosenPattern",
            )
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
            Log.e(
                OTO_TAG,
                "MSG recv peerMp=$senderMPAddress DECRYPT FAILED (tampered or replay) pattern=$chosenPattern: ${e.message}",
            )
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
     * Handles incoming handshake packets. Strips the 1-byte pattern tag prepended by
     * [sendMessageHandShake] before feeding the bytes into the underlying HandshakeState.
     * Drops the packet if the remote pattern does not match our [chosenPattern] (caller
     * is expected to recreate the session with the right pattern).
     */
    override fun onReceiveMessageHandShake(senderID: ULong, message: ByteArray) {
        if(senderID != peers[0].MPAddress) {
            Log.w(
                OTO_TAG,
                "HS recv DROP wrong sender expected=${peers[0].MPAddress} got=$senderID",
            )
            return
        }
        if (message.isEmpty()) {
            Log.w(OTO_TAG, "HS recv DROP empty payload from $senderID")
            return
        }
        val incomingPattern = NoisePattern.fromTag(message[0])
        if (incomingPattern == null) {
            Log.w(OTO_TAG, "HS recv DROP unknown pattern tag=${message[0]} from $senderID")
            return
        }
        if (incomingPattern != chosenPattern) {
            Log.w(
                OTO_TAG,
                "HS recv pattern mismatch session=$chosenPattern incoming=$incomingPattern peerMp=$senderID " +
                    "-> request session recreation",
            )
            try {
                onPatternMismatch?.invoke(senderID, message, incomingPattern)
            } catch (e: Exception) {
                Log.e(OTO_TAG, "onPatternMismatch listener threw: ${e.message}", e)
            }
            return
        }
        val payload = message.copyOfRange(1, message.size)
        val hs = handshakeState
        if (hs == null) {
            // Transport keys are live but the peer may have started a *new* KK handshake (app
            // restart with persisted statics). Only treat **KK** this way: if we also allowed
            // same-pattern XX here, late / duplicate XX packets from the mesh could tear down a
            // healthy XX tunnel and break the 1st KK reconnect ("lần 2").
            //
            // EpidemicFlooding still delivers to us because we're registered as the
            // TwoPartyListener — the handshake fallback is skipped when a listener exists.
            if (isHandshakeFinished && incomingPattern == NoisePattern.KK) {
                Log.w(
                    OTO_TAG,
                    "HS recv new KK handshake while transport active (peer likely restarted) " +
                        "peerMp=$senderID -> recreate as responder",
                )
                try {
                    onPatternMismatch?.invoke(senderID, message, incomingPattern)
                } catch (e: Exception) {
                    Log.e(OTO_TAG, "onPatternMismatch (post-handshake KK restart) threw: ${e.message}", e)
                }
            } else if (isHandshakeFinished) {
                Log.w(
                    OTO_TAG,
                    "HS recv DROP handshake after transport finished " +
                        "(chosen=$chosenPattern incoming=$incomingPattern; only KK restart auto-handled) peerMp=$senderID",
                )
            } else {
                Log.w(
                    OTO_TAG,
                    "HS recv DROP no handshake state (session not in transport) pattern=$chosenPattern peerMp=$senderID",
                )
            }
            return
        }
        Log.d(
            OTO_TAG,
            "HS recv peerMp=$senderID pattern=$chosenPattern step=${hs.step} payloadBytes=${payload.size}",
        )
        try {
            hs.readMessage(payload)
            if (!hs.isFinished && ((isInitiator && hs.step % 2 == 0) || (!isInitiator && hs.step % 2 != 0))) {
                val response = hs.writeMessage(ByteArray(0))
                Log.d(
                    OTO_TAG,
                    "HS write step=${hs.step} (response) pattern=$chosenPattern bytes=${response.size}",
                )
                sendMessageHandShake(senderID, response)
            }
            if (hs.isFinished) completeHandshake()
        } catch (e: Exception) {
            Log.e(
                OTO_TAG,
                "HS recv ERROR pattern=$chosenPattern peerMp=$senderID step=${hs.step}: ${e.message}",
            )
            // Decrypt failure on an initiator that just wrote msg 1 is the canonical signature
            // of a simultaneous-initiate conflict (both sides opened the chat and sent msg 0
            // around the same time). Hand off to the application layer to apply the MP
            // tie-break — we don't want to silently drop and leave both sides stuck.
            if (isInitiator && hs.step == 1) {
                try {
                    onSimultaneousInitiate?.invoke(senderID, message, chosenPattern)
                } catch (ce: Exception) {
                    Log.e(OTO_TAG, "onSimultaneousInitiate listener threw: ${ce.message}", ce)
                }
            }
        }
    }

    fun getSendingKey(): ByteArray? {
        return sendingState?.getKey()
    }

    fun getRecievingKey(): ByteArray? {
        return receivingState?.getKey()
    }
}

