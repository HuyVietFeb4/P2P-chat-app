package com.meshenger.backend.application

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.messaging.MessageStatus
import com.meshenger.backend.application.messaging.MessagingStore
import com.meshenger.backend.application.user.UserProfile
import com.meshenger.backend.network.MessageType
import com.meshenger.backend.network.PacketFactory
import com.meshenger.backend.network.PacketSigner
import com.meshenger.backend.session.HandshakeState
import com.meshenger.backend.session.NoisePattern
import com.meshenger.backend.transport2.StaticKeyManager
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cross-module path: Noise session → encrypted payload → signed mesh packet → local DB.
 */
@RunWith(AndroidJUnit4::class)
class SecureDirectMessagePipelineIntegrationTest {
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
    fun noiseEncryptedPayload_signedPacket_persistsCiphertext() {
        val prologue = "TwoPartySessionMeshengerV1".toByteArray()
        val phoneAStatic = StaticKeyManager.generateX25519RandomKeyPair()
        val phoneBStatic = StaticKeyManager.generateX25519RandomKeyPair()

        val initiator = HandshakeState(
            isInitiator = true,
            prologue = prologue,
            s = phoneAStatic,
            rs = phoneBStatic.first,
            pattern = NoisePattern.XK,
        )
        val responder = HandshakeState(
            isInitiator = false,
            prologue = prologue,
            s = phoneBStatic,
            pattern = NoisePattern.XK,
        )

        responder.readMessage(initiator.writeMessage(ByteArray(0)))
        initiator.readMessage(responder.writeMessage(ByteArray(0)))

        val (sendCipher, _) = initiator.split()
        val plaintext = "Hello over Noise + mesh".toByteArray(Charsets.UTF_8)
        val ciphertext = sendCipher.encryptWithAd(ByteArray(0), plaintext)
        val cipherB64 = android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)

        val peerId = "peer-noise-1"
        val receiverId = 0x00_00_00_00_00_00_CA_FEuL
        val packet = PacketFactory.createPackets(
            type = MessageType.USER_MESSAGE_ONE_TO_ONE.value,
            senderID = 1001uL,
            receiverID = receiverId,
            payload = ciphertext,
            inputTimeStamp = 1_700_000_400_000uL,
        ).single()

        assertTrue(PacketSigner.verifyDirectProtocolKey(packet))
        assertArrayEquals(ciphertext, packet.payload)

        val stored = MessagingStore.sendMessage(peerId, cipherB64, "nonce-1")
        assertEquals(cipherB64, stored.encryptedPayload)
        assertEquals(MessageStatus.PENDING, stored.status)

        val thread = MessagingStore.getConversation(peerId)
        assertEquals(1, thread.size)
        assertEquals(cipherB64, thread[0].encryptedPayload)
    }
}
