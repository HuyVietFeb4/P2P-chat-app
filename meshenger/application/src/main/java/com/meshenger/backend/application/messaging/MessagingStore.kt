package com.meshenger.backend.application.messaging

import com.meshenger.backend.application.db.MeshengerDbHelper
import java.util.UUID

/**
 * Persists chat rows using [Message] (ciphertext in [Message.encryptedPayload] only).
 */
object MessagingStore {
    private const val LOCAL_SENDER_ID = "local-device"

    private var db: MeshengerDbHelper? = null

    var onStatusChanged: ((id: String, status: MessageStatus) -> Unit)? = null

    fun init(dbHelper: MeshengerDbHelper) {
        db = dbHelper
    }

    /**
     * Outgoing message from this device. [encryptedPayload] is opaque ciphertext (e.g. Base64).
     */
    fun sendMessage(
        peerId: String,
        encryptedPayload: String,
        nonce: String,
        bodyText: String? = null,
    ): Message {
        val helper = db ?: throw IllegalStateException("MessagingStore not initialized")
        if (peerId.isBlank() || encryptedPayload.isBlank()) {
            throw IllegalArgumentException("peerId and encryptedPayload must be non-blank")
        }
        val preferredPeerName = helper.getUserProfile(peerId)?.userName ?: peerId
        helper.ensureDirectChatForPeer(peerId, peerUserName = preferredPeerName)
        val sessionId = helper.directSessionId(peerId)
        val message = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            senderId = LOCAL_SENDER_ID,
            timestamp = System.currentTimeMillis(),
            nonce = nonce,
            status = MessageStatus.PENDING,
            encryptedPayload = encryptedPayload,
            bodyText = bodyText,
        )
        helper.insertMessage(message, receiverIds = listOf(peerId))

        Thread {
            try {
                Thread.sleep(2000)
                updateMessageStatus(message.id, MessageStatus.SENT)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

        return message
    }

    /**
     * Incoming message from [senderId] in the conversation keyed by [peerId] (chat counterparty).
     */
    fun addIncomingMessage(
        peerId: String,
        senderId: String,
        encryptedPayload: String,
        nonce: String,
        bodyText: String? = null,
    ): Message {
        val helper = db ?: throw IllegalStateException("MessagingStore not initialized")
        if (peerId.isBlank() || senderId.isBlank() || encryptedPayload.isBlank()) {
            throw IllegalArgumentException("peerId, senderId and encryptedPayload must be non-blank")
        }
        val preferredPeerName = helper.getUserProfile(peerId)?.userName ?: senderId
        helper.ensureDirectChatForPeer(peerId, peerUserName = preferredPeerName)
        val sessionId = helper.directSessionId(peerId)
        val message = Message(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            senderId = senderId,
            timestamp = System.currentTimeMillis(),
            nonce = nonce,
            status = MessageStatus.SENT,
            encryptedPayload = encryptedPayload,
            bodyText = bodyText,
        )
        helper.insertMessage(message, receiverIds = listOf(LOCAL_SENDER_ID))
        return message
    }

    fun getConversation(peerId: String): List<Message> {
        val helper = db ?: return emptyList()
        return helper.getConversation(helper.directChatId(peerId))
    }

    private fun updateMessageStatus(id: String, status: MessageStatus) {
        db?.updateMessageStatus(id, status.name)
        onStatusChanged?.invoke(id, status)
    }
}
