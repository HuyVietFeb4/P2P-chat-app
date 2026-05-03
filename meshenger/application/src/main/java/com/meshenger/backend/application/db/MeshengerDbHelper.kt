package com.meshenger.backend.application.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import com.meshenger.backend.application.messaging.Message
import com.meshenger.backend.application.messaging.MessageStatus
import com.meshenger.backend.application.user.UserProfile

class MeshengerDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        // Enable Foreign Keys
        db.execSQL("PRAGMA foreign_keys = ON;")

        // 1. Table User
        db.execSQL("""
            CREATE TABLE users (
                userId TEXT PRIMARY KEY,
                publicKeyHash TEXT NOT NULL,
                userName TEXT NOT NULL
            );
        """)

        // 2. Table Chat
        db.execSQL("""
            CREATE TABLE chats (
                chatId TEXT PRIMARY KEY,
                name TEXT,
                chatType TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            );
        """)

        // 3. Table Session
        db.execSQL("""
            CREATE TABLE sessions (
                sessionId TEXT PRIMARY KEY,
                chatId TEXT NOT NULL,
                chachaKey TEXT NOT NULL,
                FOREIGN KEY(chatId) REFERENCES chats(chatId) ON DELETE CASCADE
            );
        """)

        // 4. Table Message
        db.execSQL("""
            CREATE TABLE messages (
                messageId TEXT PRIMARY KEY,
                sessionId TEXT NOT NULL,
                senderId TEXT NOT NULL,
                timeStamp INTEGER NOT NULL,
                nonce TEXT NOT NULL,
                messageStatus TEXT NOT NULL,
                encryptedPayload TEXT NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES sessions(sessionId) ON DELETE CASCADE,
                FOREIGN KEY(senderId) REFERENCES users(userId)
            );
        """)

        // 5. Table User Participation
        db.execSQL("""
            CREATE TABLE user_participation (
                chatId TEXT NOT NULL,
                userId TEXT NOT NULL,
                role TEXT NOT NULL,
                joinAt INTEGER NOT NULL,
                isLeft INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY(chatId, userId),
                FOREIGN KEY(chatId) REFERENCES chats(chatId) ON DELETE CASCADE,
                FOREIGN KEY(userId) REFERENCES users(userId)
            );
        """)

        // 6. Table Message Delivery
        db.execSQL("""
            CREATE TABLE message_delivery (
                messageId TEXT NOT NULL,
                receiverId TEXT NOT NULL,
                PRIMARY KEY(messageId, receiverId),
                FOREIGN KEY(messageId) REFERENCES messages(messageId) ON DELETE CASCADE,
                FOREIGN KEY(receiverId) REFERENCES users(userId)
            );
        """)

        db.execSQL("""
            CREATE TABLE peer_remote_keys (
                peerUserId TEXT NOT NULL,
                keyType TEXT NOT NULL,
                keystoreAlias TEXT NOT NULL,
                ciphertextBlob TEXT NOT NULL,
                ivBlob TEXT NOT NULL,
                PRIMARY KEY(peerUserId, keyType)
            );
        """)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys = ON;")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS peer_remote_keys")
            db.execSQL("DROP TABLE IF EXISTS message_delivery")
            db.execSQL("DROP TABLE IF EXISTS user_participation")
            db.execSQL("DROP TABLE IF EXISTS messages")
            db.execSQL("DROP TABLE IF EXISTS sessions")
            db.execSQL("DROP TABLE IF EXISTS chats")
            db.execSQL("DROP TABLE IF EXISTS users")
            onCreate(db)
            return
        }
        if (oldVersion < 5) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS peer_remote_keys (
                    peerUserId TEXT NOT NULL,
                    keyType TEXT NOT NULL,
                    keystoreAlias TEXT NOT NULL,
                    ciphertextBlob TEXT NOT NULL,
                    ivBlob TEXT NOT NULL,
                    PRIMARY KEY(peerUserId, keyType)
                );
                """.trimIndent(),
            )
        }
    }

    // --- Helper Methods ---

    fun upsertUserProfile(user: UserProfile) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("userId", user.id)
            put("publicKeyHash", user.publicKeyHash)
            put("userName", user.userName)
        }
        db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getUserProfile(userId: String): UserProfile? {
        val db = readableDatabase
        db.rawQuery("SELECT userId, publicKeyHash, userName FROM users WHERE userId = ?", arrayOf(userId)).use { c ->
            if (!c.moveToFirst()) return null
            return UserProfile(
                id = c.getString(0),
                publicKeyHash = c.getString(1),
                userName = c.getString(2)
            )
        }
    }

    fun getAllUserProfiles(): List<UserProfile> {
        val db = readableDatabase
        val out = mutableListOf<UserProfile>()
        db.rawQuery("SELECT userId, publicKeyHash, userName FROM users ORDER BY userName", null).use { c ->
            while (c.moveToNext()) {
                out.add(
                    UserProfile(
                        id = c.getString(0),
                        publicKeyHash = c.getString(1),
                        userName = c.getString(2)
                    )
                )
            }
        }
        return out
    }

    private fun rowExists(table: String, column: String, id: String): Boolean {
        readableDatabase.rawQuery("SELECT 1 FROM $table WHERE $column = ? LIMIT 1", arrayOf(id)).use {
            return it.moveToFirst()
        }
    }

    /**
     * Ensures [peerId] exists in users and a 1:1 chat + session row exist for DB message FKs.
     */
    fun ensureDirectChatForPeer(peerId: String, peerUserName: String) {
        upsertUserProfile(UserProfile(peerId, publicKeyHash = "-", userName = peerUserName))
        val chatId = directChatId(peerId)
        val sessionId = directSessionId(peerId)
        if (!rowExists("chats", "chatId", chatId)) {
            insertChat(chatId, peerUserName, "DIRECT", System.currentTimeMillis())
        }
        if (!rowExists("sessions", "sessionId", sessionId)) {
            insertSession(sessionId, chatId, "placeholder-key")
        }
    }

    fun directChatId(peerId: String) = "direct-$peerId"

    fun directSessionId(peerId: String) = "session-$peerId"

    fun ensureGlobalChat(globalChatId: String, globalSessionId: String, keyId: String) {
        if (!rowExists("chats", "chatId", globalChatId)) {
            insertChat(globalChatId, "Global Chat", "GLOBAL", System.currentTimeMillis())
        }
        if (!rowExists("sessions", "sessionId", globalSessionId)) {
            insertSession(globalSessionId, globalChatId, keyId)
        }
    }

    fun getSessionKeyId(sessionId: String): String? {
        val db = readableDatabase
        db.rawQuery("SELECT chachaKey FROM sessions WHERE sessionId = ?", arrayOf(sessionId)).use { c ->
            if (!c.moveToFirst()) return null
            return c.getString(0)
        }
    }

    fun insertChat(chatId: String, name: String?, chatType: String, createdAt: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("chatId", chatId)
            put("name", name)
            put("chatType", chatType)
            put("createdAt", createdAt)
        }
        db.insert("chats", null, values)
    }

    fun insertSession(sessionId: String, chatId: String, chachaKey: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("sessionId", sessionId)
            put("chatId", chatId)
            put("chachaKey", chachaKey)
        }
        db.insert("sessions", null, values)
    }

    fun insertMessage(message: Message, receiverIds: List<String>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("messageId", message.id)
                put("sessionId", message.sessionId)
                put("senderId", message.senderId)
                put("timeStamp", message.timestamp)
                put("nonce", message.nonce)
                put("messageStatus", message.status.name)
                put("encryptedPayload", message.encryptedPayload)
            }
            db.insert("messages", null, values)

            for (receiverId in receiverIds) {
                val deliveryValues = ContentValues().apply {
                    put("messageId", message.id)
                    put("receiverId", receiverId)
                }
                db.insert("message_delivery", null, deliveryValues)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateMessageStatus(messageId: String, status: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("messageStatus", status)
        }
        db.update("messages", values, "messageId = ?", arrayOf(messageId))
    }

    fun getConversation(chatId: String): List<Message> {
        val db = readableDatabase
        val query = """
            SELECT m.* FROM messages m
            JOIN sessions s ON m.sessionId = s.sessionId
            WHERE s.chatId = ?
            ORDER BY m.timeStamp ASC
        """
        val cursor = db.rawQuery(query, arrayOf(chatId))
        val messages = mutableListOf<Message>()
        cursor.use {
            while (it.moveToNext()) {
                messages.add(Message(
                    id = it.getString(it.getColumnIndexOrThrow("messageId")),
                    sessionId = it.getString(it.getColumnIndexOrThrow("sessionId")),
                    senderId = it.getString(it.getColumnIndexOrThrow("senderId")),
                    timestamp = it.getLong(it.getColumnIndexOrThrow("timeStamp")),
                    nonce = it.getString(it.getColumnIndexOrThrow("nonce")),
                    status = MessageStatus.valueOf(it.getString(it.getColumnIndexOrThrow("messageStatus"))),
                    encryptedPayload = it.getString(it.getColumnIndexOrThrow("encryptedPayload"))
                ))
            }
        }
        return messages
    }

    data class PeerRemoteKeyRow(
        val peerUserId: String,
        val keyType: String,
        val keystoreAlias: String,
        val ciphertextBlob: String,
        val ivBlob: String,
    )

    fun upsertPeerRemoteKey(
        peerUserId: String,
        keyType: String,
        keystoreAlias: String,
        ciphertextBlob: String,
        ivBlob: String,
    ) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("peerUserId", peerUserId)
            put("keyType", keyType)
            put("keystoreAlias", keystoreAlias)
            put("ciphertextBlob", ciphertextBlob)
            put("ivBlob", ivBlob)
        }
        db.insertWithOnConflict("peer_remote_keys", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getPeerRemoteKey(peerUserId: String, keyType: String): PeerRemoteKeyRow? {
        val db = readableDatabase
        db.rawQuery(
            "SELECT peerUserId, keyType, keystoreAlias, ciphertextBlob, ivBlob FROM peer_remote_keys WHERE peerUserId = ? AND keyType = ?",
            arrayOf(peerUserId, keyType),
        ).use { c ->
            if (!c.moveToFirst()) return null
            return PeerRemoteKeyRow(
                peerUserId = c.getString(0),
                keyType = c.getString(1),
                keystoreAlias = c.getString(2),
                ciphertextBlob = c.getString(3),
                ivBlob = c.getString(4),
            )
        }
    }

    fun deletePeerRemoteKey(peerUserId: String, keyType: String) {
        writableDatabase.delete("peer_remote_keys", "peerUserId = ? AND keyType = ?", arrayOf(peerUserId, keyType))
    }

    companion object {
        private const val DATABASE_NAME = "meshenger.db"
        private const val DATABASE_VERSION = 5
    }
}
