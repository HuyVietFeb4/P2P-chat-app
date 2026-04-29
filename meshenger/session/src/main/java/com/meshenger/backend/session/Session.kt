package com.meshenger.backend.session

import com.meshenger.backend.network.MessageType
import kotlinx.serialization.json.*
import com.meshenger.backend.network.SpecialRecipients
import kotlinx.coroutines.flow.MutableSharedFlow
import java.security.MessageDigest
abstract class Session {
    protected val peers = mutableListOf<Peer>()
    protected val _messageBus = MutableSharedFlow<JsonObject>(replay = 1, extraBufferCapacity = 10)
    protected fun getFixedKey(rawKey: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawKey.encodeToByteArray())
    }
    open fun addMember(newMember: Peer) {
        if (!peers.contains(newMember)) {
            peers.add(newMember)
        }
    }
    open fun removeMember(toRemove: Peer) {
        if(peers.contains(toRemove)) {
            peers.remove(toRemove)
        }
    }
    open fun getPeerUsername(addressToFind: ULong): String? {
        return peers.find { it.MPAddress == addressToFind }?.userName
    }

    open fun getMessageBus(): MutableSharedFlow<JsonObject> {
        return _messageBus
    }
    abstract fun sendMessageStr(
        receiverMPAddress: ULong = SpecialRecipients.BROADCAST,
        message: String
    )
    abstract fun receiveMessageStr(senderMPAddress: ULong, encryptedData: ByteArray, nonceTimeStamp: ULong)
//    abstract fun sendFile(dest: Peer, file: ByteArray)
//    abstract fun receiveFile(senderMPAddress: ULong, file: ByteArray)
}

enum class SessionType(val value: UInt) {
    GlobalChat(0x0000u),
    TwoPartyChat(0x0001u),
    GroupChat(0x0002u);

    companion object {
        fun fromValue(value: UInt): MessageType? {
            return MessageType.values().find { it.value == value }
        }
    }
}