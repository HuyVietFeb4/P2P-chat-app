package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PacketSignerIntegrationTest {

    @Test
    fun globalChatPacket_roundTripVerification() {
        val packet = singlePacket(
            type = MessageType.USER_MESSAGE_ALL.value,
            receiverId = SpecialRecipients.BROADCAST,
        )
        assertTrue(PacketSigner.verifyGlobalChatKey(packet))
    }

    @Test
    fun bootstrapPacket_roundTripVerification() {
        val packet = singlePacket(
            type = MessageType.BOOTSTRAP.value,
            receiverId = SpecialRecipients.BROADCAST,
        )
        assertTrue(PacketSigner.verifyGlobalProtocolKey(packet))
    }

    @Test
    fun directProtocolPacket_roundTripVerification() {
        val receiverId = 0x00_00_00_00_00_00_12_34uL
        val packet = singlePacket(
            type = MessageType.USER_MESSAGE_ONE_TO_ONE.value,
            receiverId = receiverId,
        )
        assertTrue(PacketSigner.verifyDirectProtocolKey(packet))
    }

    @Test
    fun tamperedPayload_failsVerification() {
        val packet = singlePacket(
            type = MessageType.USER_MESSAGE_ALL.value,
            receiverId = SpecialRecipients.BROADCAST,
        )
        val tampered = packet.copy(payload = packet.payload + byteArrayOf(0xFF.toByte()))
        assertFalse(PacketSigner.verifyGlobalChatKey(tampered))
    }

    private fun singlePacket(type: UInt, receiverId: ULong): Packet {
        val payload = "integration-signer".encodeToByteArray()
        val packets = PacketFactory.createPackets(
            type = type,
            senderID = 555uL,
            receiverID = receiverId,
            payload = payload,
            inputTimeStamp = 1_700_000_100_000uL,
        )
        assertTrue(packets.isNotEmpty())
        return packets.single()
    }
}
