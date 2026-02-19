package com.meshenger.backend.application.messaging

import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory store for chat messages per peer.
 * Used until Session layer provides real send/receive API; then can delegate to session.
 */
object MessagingStore {
    private val conversations = mutableMapOf<String, MutableList<Message>>()
    private val lock = Any()

    fun sendMessage(peerId: String, text: String, fromMe: Boolean = true): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            peerId = peerId,
            text = text,
            fromMe = fromMe
        )
        synchronized(lock) {
            conversations.getOrPut(peerId) { CopyOnWriteArrayList() }.add(message)
        }
        return message
    }

    fun getConversation(peerId: String): List<Message> {
        return synchronized(lock) {
            conversations[peerId]?.toList() ?: emptyList()
        }
    }

    /** For testing or when Session layer pushes incoming message. */
    fun addIncomingMessage(peerId: String, text: String): Message {
        return sendMessage(peerId, text, fromMe = false)
    }
}
