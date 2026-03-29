package com.meshenger.backend.application.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import com.meshenger.backend.application.messaging.Message
import com.meshenger.backend.application.user.UserProfile

/**
 * Small SQLite helper to persist messages and local user profile.
 *
 * This is intentionally minimal (no Room) to match the "sqlhelper" request.
 */
class MeshengerDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile (
              id TEXT PRIMARY KEY,
              display_name TEXT NOT NULL,
              avatar_url TEXT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS messages (
              id TEXT PRIMARY KEY,
              peer_id TEXT NOT NULL,
              text TEXT NOT NULL,
              from_me INTEGER NOT NULL,
              timestamp INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_peer_id ON messages(peer_id);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp);")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // For this prototype, keep simple. Add migrations when schema evolves.
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS messages;")
            db.execSQL("DROP TABLE IF EXISTS user_profile;")
            onCreate(db)
        }
    }

    fun upsertUserProfile(profile: UserProfile) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_USER_ID, profile.id)
            put(COL_USER_DISPLAY_NAME, profile.displayName)
            put(COL_USER_AVATAR_URL, profile.avatarUrl)
        }
        db.insertWithOnConflict(
            TABLE_USER_PROFILE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getUserProfile(userId: String): UserProfile? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USER_PROFILE,
            arrayOf(COL_USER_ID, COL_USER_DISPLAY_NAME, COL_USER_AVATAR_URL),
            "${COL_USER_ID} = ?",
            arrayOf(userId),
            null,
            null,
            null
        )

        cursor.use {
            if (!it.moveToFirst()) return null
            val id = it.getString(it.getColumnIndexOrThrow(COL_USER_ID))
            val displayName = it.getString(it.getColumnIndexOrThrow(COL_USER_DISPLAY_NAME))
            val avatarUrl = it.getString(it.getColumnIndexOrThrow(COL_USER_AVATAR_URL))
            return UserProfile(id = id, displayName = displayName, avatarUrl = avatarUrl)
        }
    }

    fun getAllUserProfiles(): List<UserProfile> {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_USER_PROFILE,
            arrayOf(COL_USER_ID, COL_USER_DISPLAY_NAME, COL_USER_AVATAR_URL),
            null,
            null,
            null,
            null,
            null
        )

        cursor.use {
            val profiles = mutableListOf<UserProfile>()
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndexOrThrow(COL_USER_ID))
                val displayName = it.getString(it.getColumnIndexOrThrow(COL_USER_DISPLAY_NAME))
                val avatarUrl = it.getString(it.getColumnIndexOrThrow(COL_USER_AVATAR_URL))
                profiles.add(UserProfile(id = id, displayName = displayName, avatarUrl = avatarUrl))
            }
            return profiles
        }
    }

    fun insertMessage(message: Message) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_MSG_ID, message.id)
            put(COL_MSG_PEER_ID, message.peerId)
            put(COL_MSG_TEXT, message.text)
            put(COL_MSG_FROM_ME, if (message.fromMe) 1 else 0)
            put(COL_MSG_TIMESTAMP, message.timestamp)
        }
        db.insertWithOnConflict(
            TABLE_MESSAGES,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun getConversation(peerId: String): List<Message> {
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_MESSAGES,
            arrayOf(COL_MSG_ID, COL_MSG_PEER_ID, COL_MSG_TEXT, COL_MSG_FROM_ME, COL_MSG_TIMESTAMP),
            "${COL_MSG_PEER_ID} = ?",
            arrayOf(peerId),
            null,
            null,
            "${COL_MSG_TIMESTAMP} ASC"
        )

        cursor.use {
            val out = ArrayList<Message>()
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndexOrThrow(COL_MSG_ID))
                val pId = it.getString(it.getColumnIndexOrThrow(COL_MSG_PEER_ID))
                val text = it.getString(it.getColumnIndexOrThrow(COL_MSG_TEXT))
                val fromMeInt = it.getInt(it.getColumnIndexOrThrow(COL_MSG_FROM_ME))
                val timestamp = it.getLong(it.getColumnIndexOrThrow(COL_MSG_TIMESTAMP))
                out.add(
                    Message(
                        id = id,
                        peerId = pId,
                        text = text,
                        fromMe = fromMeInt != 0,
                        timestamp = timestamp
                    )
                )
            }
            return out
        }
    }

    companion object {
        private const val DATABASE_NAME = "meshenger.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_USER_PROFILE = "user_profile"
        private const val COL_USER_ID = "id"
        private const val COL_USER_DISPLAY_NAME = "display_name"
        private const val COL_USER_AVATAR_URL = "avatar_url"

        private const val TABLE_MESSAGES = "messages"
        private const val COL_MSG_ID = "id"
        private const val COL_MSG_PEER_ID = "peer_id"
        private const val COL_MSG_TEXT = "text"
        private const val COL_MSG_FROM_ME = "from_me"
        private const val COL_MSG_TIMESTAMP = "timestamp"
    }
}
