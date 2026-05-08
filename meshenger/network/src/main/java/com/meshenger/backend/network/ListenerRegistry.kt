package com.meshenger.backend.network

import java.util.concurrent.ConcurrentHashMap

/**
 * Fired when a NOISE_HANDSHAKE arrives for a peer that has no registered [TwoPartyMessageListener].
 * Implementations should create the responder-side `TwoPartySession` (which auto-registers itself)
 * and forward the inbound handshake bytes via `onReceiveMessageHandShake`.
 */
fun interface TwoPartyHandshakeFallback {
    fun onIncomingHandshake(senderId: ULong, message: ByteArray)
}

/** Mesh 1:1 invite / accept / reject delivered to application layer after verify + decrypt N/A — payload opaque. */
interface DirectChatNegotiationListener {
    fun onInviteReceived(senderId: ULong, payload: ByteArray, timeStamp: ULong)
    fun onInviteAccepted(senderId: ULong, payload: ByteArray, timeStamp: ULong)
    fun onInviteRejected(senderId: ULong, payload: ByteArray, timeStamp: ULong)
}

object ListenerRegistry {
    private val twoPartyListener = ConcurrentHashMap<ULong, TwoPartyMessageListener>()
    private var globalListener: GlobalMessageListener? = null
    @Volatile
    private var handshakeFallback: TwoPartyHandshakeFallback? = null
    @Volatile
    private var directChatNegotiationListener: DirectChatNegotiationListener? = null

    fun setGlobalListener(listener: GlobalMessageListener) {
        this.globalListener = listener
    }

    fun getGlobalListener(): GlobalMessageListener? { return globalListener }

    fun registerTwoPartyListener(peerId: ULong, listener: TwoPartyMessageListener) {
        twoPartyListener[peerId] = listener
    }

    fun unregisterTwoPartyListener(peerId: ULong) {
        twoPartyListener.remove(peerId)
    }

    fun getTwoPartyListener(peerId: ULong): TwoPartyMessageListener? {
        return twoPartyListener[peerId]
    }

    fun setTwoPartyHandshakeFallback(fallback: TwoPartyHandshakeFallback?) {
        this.handshakeFallback = fallback
    }

    fun getTwoPartyHandshakeFallback(): TwoPartyHandshakeFallback? = handshakeFallback

    fun setDirectChatNegotiationListener(listener: DirectChatNegotiationListener?) {
        directChatNegotiationListener = listener
    }

    fun getDirectChatNegotiationListener(): DirectChatNegotiationListener? =
        directChatNegotiationListener
}