package com.meshenger.backend.network

interface TwoPartyMessageListener {
    fun onDirectMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong, signature: ByteArray, signedData: ByteArray)
    fun onReceiveMessageHandShake(senderID: ULong, message: ByteArray)
}