package com.meshenger.backend.application.messaging

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.user.UserProfile
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagingStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dbHelper: MeshengerDbHelper

    @Before
    fun setUp() {
        context.deleteDatabase("meshenger.db")
        dbHelper = MeshengerDbHelper(context)
        dbHelper.upsertUserProfile(UserProfile("local-device", "-", "Me"))
        MessagingStore.init(dbHelper)
    }

    @After
    fun tearDown() {
        dbHelper.close()
    }

    @Test
    fun sendMessage_persistsCiphertextOnly() {
        val peerId = "peer-99"
        val cipher = "YmFzZTY0LWNpcGhlci1ibG9i"
        val nonce = "n-1"

        val msg = MessagingStore.sendMessage(peerId, cipher, nonce)

        assertEquals(cipher, msg.encryptedPayload)
        assertEquals("local-device", msg.senderId)

        val thread = MessagingStore.getConversation(peerId)
        assertEquals(1, thread.size)
        assertEquals(cipher, thread[0].encryptedPayload)
        assertEquals(MessageStatus.PENDING, thread[0].status)
    }

    @Test
    fun addIncomingMessage_recordsRemoteSender() {
        val peerId = "peer-100"
        val senderId = "user-remote"
        val cipher = "aW5jb21pbmc="
        val nonce = "n-2"

        val msg = MessagingStore.addIncomingMessage(peerId, senderId, cipher, nonce)

        assertEquals(senderId, msg.senderId)
        val thread = MessagingStore.getConversation(peerId)
        assertTrue(thread.any { it.encryptedPayload == cipher && it.senderId == senderId })
    }
}
