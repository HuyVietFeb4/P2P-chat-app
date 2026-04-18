package com.meshenger.backend.network
// To go up to session
interface NetworkMessageListener {
    fun onUserMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong)
}