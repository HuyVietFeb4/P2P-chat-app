package com.meshenger.backend.network

import java.util.concurrent.ConcurrentHashMap

object ListenerRegistry {
    private val twoPartyListener = ConcurrentHashMap<ULong, TwoPartyMessageListener>()
    private var globalListener: GlobalMessageListener? = null

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
}