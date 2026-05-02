package com.meshenger.backend.network
interface GlobalMessageListener {
    fun onGlobalMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong)
    fun onBootStrapReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong)
}