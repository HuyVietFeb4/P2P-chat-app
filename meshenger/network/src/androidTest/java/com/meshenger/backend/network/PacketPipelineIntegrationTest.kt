package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end packet path: factory → encode → decode → verify → reassemble.
 * Mirrors [EpidemicFlooding.onReceivePacket] validation without BLE transport.
 */
@RunWith(AndroidJUnit4::class)
class PacketPipelineIntegrationTest {

    @Test
    fun globalBroadcast_largePayload_survivesWireRoundTripAndReassembly() {
        val plaintext = ByteArray(900) { (it and 0xFF).toByte() }
        val senderId = 12345uL
        val timestamp = 1_700_000_200_000uL

        val fragments = PacketFactory.createPackets(
            type = MessageType.USER_MESSAGE_ALL.value,
            senderID = senderId,
            payload = plaintext,
            inputTimeStamp = timestamp,
        )

        var reassembled: ByteArray? = null
        for (fragment in fragments) {
            val wire = Packet.encode(fragment)
            assertNotNull(wire)

            val decoded = Packet.decode(wire!!)
            assertNotNull(decoded)
            assertTrue(PacketSigner.verifyGlobalChatKey(decoded!!))

            val key = ReassemblyQueue.getKeyFragment(decoded)
            reassembled = ReassemblyQueue.addToQueue(
                key,
                decoded.payload,
                decoded.header.totalFragments.toInt(),
                decoded.header.fragmentID.toInt(),
            ) ?: reassembled
        }

        assertArrayEquals(plaintext, reassembled)
    }

    @Test
    fun directUnicast_encodeDecode_verify() {
        val receiverId = 0x00_00_00_00_AB_CD_EF_01uL
        val payload = "direct-unicast-pipeline".encodeToByteArray()
        val packet = PacketFactory.createPackets(
            type = MessageType.DIRECT_CHAT_INVITE.value,
            senderID = 777uL,
            receiverID = receiverId,
            payload = payload,
            inputTimeStamp = 1_700_000_300_000uL,
        ).single()

        val decoded = Packet.decode(Packet.encode(packet)!!)!!
        assertTrue(PacketSigner.verifyDirectProtocolKey(decoded))
        assertArrayEquals(payload, decoded.payload)
    }
}
