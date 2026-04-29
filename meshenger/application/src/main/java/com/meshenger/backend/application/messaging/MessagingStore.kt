package com.meshenger.backend.application.messaging

import com.meshenger.backend.application.db.MeshengerDbHelper
import java.util.UUID

/**
 * Messaging store for chat messages per peer.
 */
object MessagingStore {
    private var db: MeshengerDbHelper? = null

    var onStatusChanged: ((id: String, status: MessageStatus) -> Unit)? = null

    fun init(dbHelper: MeshengerDbHelper) {
        db = dbHelper
    }

    fun sendMessage(peerId: String, text: String, fromMe: Boolean = true): Message {
        val status = if (fromMe) MessageStatus.PENDING else MessageStatus.SENT

        val message = Message(
            id = UUID.randomUUID().toString(),
            peerId = peerId,
            text = text,
            fromMe = fromMe,
            status = status
        )

        db?.insertMessage(message)

        // Simulate Queue
        if (fromMe) {
            Thread {
                try {
                    Thread.sleep(2000)
                    updateMessageStatus(message.id, MessageStatus.SENT)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }

        return message
    }

    fun getConversation(peerId: String): List<Message> {
        return db?.getConversation(peerId) ?: emptyList()
    }

    fun addIncomingMessage(peerId: String, text: String): Message {
        return sendMessage(peerId, text, fromMe = false)
    }

    private fun updateMessageStatus(id: String, status: MessageStatus) {
        db?.updateMessageStatus(id, status.name)

        //CALL INVOKE
        onStatusChanged?.invoke(id, status)
    }
}
