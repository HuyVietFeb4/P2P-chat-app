package com.meshenger.backend.application.messaging

import com.meshenger.backend.application.db.MeshengerDbHelper
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Messaging store for chat messages per peer.
 *
 * In prototype stage, it is persisted into SQLite via MeshengerDbHelper.
 */
object MessagingStore {
    private val conversations = mutableMapOf<String, MutableList<Message>>()
    private val lock = Any()
    @Volatile
    private var db: MeshengerDbHelper? = null

    fun init(dbHelper: MeshengerDbHelper) {
        db = dbHelper
    }

    fun sendMessage(peerId: String, text: String, fromMe: Boolean = true): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            peerId = peerId,
            text = text,
            fromMe = fromMe
        )
        val dbRef = db
        if (dbRef != null) {
            synchronized(lock) {
                dbRef.insertMessage(message)
            }
        } else {
            synchronized(lock) {
                conversations.getOrPut(peerId) { CopyOnWriteArrayList() }.add(message)
            }
        }
        return message
    }

    fun getConversation(peerId: String): List<Message> {
        val dbRef = db
        if (dbRef != null) {
            synchronized(lock) {
                return dbRef.getConversation(peerId)
            }
        }
        return synchronized(lock) {
            conversations[peerId]?.toList() ?: emptyList()
        }
    }

    /** For testing or when Session layer pushes incoming message. */
    fun addIncomingMessage(peerId: String, text: String): Message {
        return sendMessage(peerId, text, fromMe = false)
    }
}
