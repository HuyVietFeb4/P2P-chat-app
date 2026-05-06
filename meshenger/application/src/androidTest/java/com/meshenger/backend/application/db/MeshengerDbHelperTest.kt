package com.meshenger.backend.application.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meshenger.backend.application.messaging.Message
import com.meshenger.backend.application.messaging.MessageStatus
import com.meshenger.backend.application.user.UserProfile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeshengerDbHelperTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dbHelper: MeshengerDbHelper

    @Before
    fun setUp() {
        context.deleteDatabase("meshenger.db")
        dbHelper = MeshengerDbHelper(context)
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun createsAllSchemaTables() {
        val db = dbHelper.readableDatabase
        val expectedTables = setOf(
            "users",
            "chats",
            "sessions",
            "messages",
            "user_participation",
            "message_delivery"
        )

        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'",
            null
        )

        val actualTables = mutableSetOf<String>()
        cursor.use {
            while (it.moveToNext()) {
                actualTables.add(it.getString(0))
            }
        }

        assertTrue("Missing tables. Found: $actualTables", actualTables.containsAll(expectedTables))
    }

    @Test
    fun fullMessageFlowTest() {
        // 1. Insert Users
        val userA = UserProfile("user-a", "hash-a", "Alice")
        val userB = UserProfile("user-b", "hash-b", "Bob")
        dbHelper.upsertUserProfile(userA)
        dbHelper.upsertUserProfile(userB)

        // 2. Insert Chat
        val chatId = "chat-1"
        dbHelper.insertChat(chatId, "Alice & Bob", "DIRECT", System.currentTimeMillis())

        // 3. Insert Session
        val sessionId = "session-1"
        dbHelper.insertSession(sessionId, chatId, "secret-key")

        // 4. Insert Message
        val message = Message(
            id = "msg-1",
            sessionId = sessionId,
            senderId = "user-a",
            timestamp = System.currentTimeMillis(),
            nonce = "random-nonce",
            status = MessageStatus.PENDING,
            encryptedPayload = "encrypted-hello"
        )
        dbHelper.insertMessage(message, listOf("user-b"))

        // 5. Verify Conversation
        val conversation = dbHelper.getConversation(chatId)
        assertEquals(1, conversation.size)
        val retrievedMsg = conversation[0]
        assertEquals("msg-1", retrievedMsg.id)
        assertEquals("user-a", retrievedMsg.senderId)
        assertEquals("encrypted-hello", retrievedMsg.encryptedPayload)
        assertEquals(MessageStatus.PENDING, retrievedMsg.status)

        // 6. Test Update Status
        dbHelper.updateMessageStatus("msg-1", MessageStatus.SENT.name)
        val updatedConv = dbHelper.getConversation(chatId)
        assertEquals(MessageStatus.SENT, updatedConv[0].status)

        // 7. Verify Delivery Table
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT receiverId FROM message_delivery WHERE messageId = ?",
            arrayOf("msg-1")
        )
        cursor.use {
            assertTrue(it.moveToFirst())
            assertEquals("user-b", it.getString(0))
        }
    }

    @Test
    fun deleteDirectPeerGraph_cleansCrossChatReferencesAndDeletesUser() {
        val me = UserProfile("me", "hash-me", "Me")
        val peer = UserProfile("peer-1", "hash-peer", "Peer")
        dbHelper.upsertUserProfile(me)
        dbHelper.upsertUserProfile(peer)

        // Create peer direct graph.
        dbHelper.ensureDirectChatForPeer(peer.id, peer.userName)

        // Create a global chat/session where peer is also referenced.
        val globalChatId = "global-chat"
        val globalSessionId = "global-session"
        dbHelper.insertChat(globalChatId, "Global Chat", "GLOBAL", System.currentTimeMillis())
        dbHelper.insertSession(globalSessionId, globalChatId, "global-key")

        // Add participation rows for peer outside direct chat.
        val participationValues = android.content.ContentValues().apply {
            put("chatId", globalChatId)
            put("userId", peer.id)
            put("role", "MEMBER")
            put("joinAt", System.currentTimeMillis())
            put("isLeft", 0)
        }
        dbHelper.writableDatabase.insert("user_participation", null, participationValues)

        // Insert a global message authored by peer to validate sender FK cleanup.
        val globalMessage = Message(
            id = "global-msg-1",
            sessionId = globalSessionId,
            senderId = peer.id,
            timestamp = System.currentTimeMillis(),
            nonce = "global-nonce",
            status = MessageStatus.SENT,
            encryptedPayload = "cipher-global",
        )
        dbHelper.insertMessage(globalMessage, listOf(me.id))

        // Add peer-scoped key material that is not FK protected.
        dbHelper.upsertPeerRemoteKey(
            peerUserId = peer.id,
            keyType = "identity",
            keystoreAlias = "alias-peer",
            ciphertextBlob = "ct",
            ivBlob = "iv",
        )

        dbHelper.deleteDirectPeerGraph(peer.id)

        assertNull(dbHelper.getUserProfile(peer.id))
        assertEquals(0, dbHelper.countMessagesForDirectPeer(peer.id))
        assertNull(dbHelper.getPeerRemoteKey(peer.id, "identity"))

        dbHelper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM messages WHERE senderId = ?",
            arrayOf(peer.id),
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }

        dbHelper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM user_participation WHERE userId = ?",
            arrayOf(peer.id),
        ).use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(0, c.getInt(0))
        }
    }
}
