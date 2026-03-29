package com.meshenger.backend.application.messaging

<<<<<<< HEAD
import com.meshenger.backend.application.db.MeshengerDbHelper
=======
>>>>>>> origin/UI
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
<<<<<<< HEAD
 * Messaging store for chat messages per peer.
 *
 * In prototype stage, it is persisted into SQLite via MeshengerDbHelper.
=======
 * In-memory store for chat messages per peer.
 * Used until Session layer provides real send/receive API; then can delegate to session.
>>>>>>> origin/UI
 */
object MessagingStore {
    private val conversations = mutableMapOf<String, MutableList<Message>>()
    private val lock = Any()
<<<<<<< HEAD
    @Volatile
    private var db: MeshengerDbHelper? = null

    fun init(dbHelper: MeshengerDbHelper) {
        db = dbHelper
    }
=======
>>>>>>> origin/UI

    fun sendMessage(peerId: String, text: String, fromMe: Boolean = true): Message {
        val message = Message(
            id = UUID.randomUUID().toString(),
            peerId = peerId,
            text = text,
            fromMe = fromMe
        )
<<<<<<< HEAD
        val dbRef = db
        if (dbRef != null) {
            synchronized(lock) {
                dbRef.insertMessage(message)
            }
        } else {
            synchronized(lock) {
                conversations.getOrPut(peerId) { CopyOnWriteArrayList() }.add(message)
            }
=======
        synchronized(lock) {
            conversations.getOrPut(peerId) { CopyOnWriteArrayList() }.add(message)
>>>>>>> origin/UI
        }
        return message
    }

    fun getConversation(peerId: String): List<Message> {
<<<<<<< HEAD
        val dbRef = db
        if (dbRef != null) {
            synchronized(lock) {
                return dbRef.getConversation(peerId)
            }
        }
=======
>>>>>>> origin/UI
        return synchronized(lock) {
            conversations[peerId]?.toList() ?: emptyList()
        }
    }

    /** For testing or when Session layer pushes incoming message. */
    fun addIncomingMessage(peerId: String, text: String): Message {
        return sendMessage(peerId, text, fromMe = false)
    }
}
