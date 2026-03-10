package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import com.meshenger.backend.security_native.NativeCredentials

@RunWith(AndroidJUnit4::class)
class PacketFactoryTest {

    @Test
    fun testCreateBroadcastPackets_BasicValidation() {
        val senderId = 123456789L.toULong()
        val payload = "Hello World".toByteArray()
        val type = MessageType.USER_MESSAGE_ALL.value

        // 1. Execute
        val packets = PacketFactory.createBroadcastPackets(
            type = type,
            senderID = senderId,
            payload = payload
        )

        // 2. Assertions
        assertNotNull(packets)
        assertTrue(packets.isNotEmpty())

        val firstPacket = packets[0]
        assertEquals(type, firstPacket.header.type)
        assertEquals(senderId, firstPacket.header.senderID)
        assertEquals(SpecialRecipients.BROADCAST, firstPacket.header.recieverID)

        // Verify Signature length (HMAC-SHA512 is 64 bytes)
        assertEquals(64, firstPacket.signature.size)
    }

    @Test
    fun testSignatureIntegrity() {
        val payload = "Secret Message".toByteArray()
        val senderId = 999uL
        val type = MessageType.USER_MESSAGE_ALL.value

        val packets = PacketFactory.createBroadcastPackets(
            type = type,
            senderID = senderId,
            payload = payload
        )

        val packet = packets[0]

        // Manually recreate the HMAC to verify the factory did it correctly
        val secretKey = NativeCredentials.getAppSecretKey()
        val hmac = Mac.getInstance("HmacSHA512")
        hmac.init(SecretKeySpec(secretKey.toByteArray(), "HmacSHA512"))

        // Note: You must match the exact buffer layout used in createBroadcastPackets
        val buffer = java.nio.ByteBuffer.allocate(38 + payload.size).order(java.nio.ByteOrder.BIG_ENDIAN)
        buffer.putShort(1u.toShort()) // version
        buffer.putShort(packet.header.flags.toShort())
        buffer.putInt(type.toInt())
        buffer.putShort(packet.header.TTL.toShort())
        buffer.putShort(packet.header.totalFragments.toShort())
        buffer.putShort(packet.header.fragmentID.toShort())
        buffer.putLong(packet.header.timeStamp.toLong())
        buffer.putLong(senderId.toLong())
        buffer.put(payload)

        val expectedSignature = hmac.doFinal(buffer.array())

        assertArrayEquals("The HMAC signature in the packet is invalid!", expectedSignature, packet.signature)
    }

    @Test
    fun testFragmentation() {
        // Create a payload larger than MAX_PAYLOAD_LENGTH (e.g., 500 bytes)
        val largePayload = ByteArray(500) { it.toByte() }

        val packets = PacketFactory.createBroadcastPackets(
            type = MessageType.USER_MESSAGE_ALL.value,
            senderID = 1uL,
            payload = largePayload
        )

        // Verify multiple packets were created
        assertTrue("Should have more than 1 fragment", packets.size > 1)
        assertEquals(packets.size.toUShort(), packets[0].header.totalFragments)
        assertEquals(0.toUShort(), packets[0].header.fragmentID)
        assertEquals(1.toUShort(), packets[1].header.fragmentID)
    }
}