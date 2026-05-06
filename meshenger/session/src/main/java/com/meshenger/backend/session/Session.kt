package com.meshenger.backend.session

import com.meshenger.backend.network.MessageType
import com.meshenger.backend.network.SpecialRecipients
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.security.MessageDigest

abstract class Session {
    protected val peers = mutableListOf<Peer>()
    protected val _messageBus = MutableSharedFlow<JsonObject>(replay = 0, extraBufferCapacity = 64)
    private val messageBusEmitJob = SupervisorJob()
    private val messageBusEmitScope = CoroutineScope(messageBusEmitJob + Dispatchers.Default)

    /**
     * If [tryEmit] misses (subscriber not ready yet), suspend [emit] so chat lines aren't dropped.
     */
    protected fun offerMessageBus(json: JsonObject) {
        if (_messageBus.tryEmit(json)) return
        messageBusEmitScope.launch {
            _messageBus.emit(json)
        }
    }

    protected fun cancelMessageBusEmitter() {
        messageBusEmitJob.cancel()
    }
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