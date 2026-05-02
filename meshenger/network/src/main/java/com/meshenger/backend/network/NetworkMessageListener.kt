package com.meshenger.backend.network
// To go up to session
interface NetworkMessageListener {
    // Basic Chat
    fun onGlobalMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {}
    fun onBootStrapReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {}
    // Direct Messaging
    fun onDirectMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {}
    fun onReceiveMessageHandShake(senderMPAddress: ULong, message: ByteArray) {}
    // Group Management
    fun onGroupActionReceived(groupID: ULong, actionType: UInt, payload: ByteArray) {}

    // System/Reliability
    fun onAckReceived(packetSignature: ByteArray) {}
    
    // Error Handling
    fun onError(errorCode: Int, message: String) {}
}