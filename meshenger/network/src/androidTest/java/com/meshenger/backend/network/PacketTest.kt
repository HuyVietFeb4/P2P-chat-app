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
        val payload = "Hello World".encodeToByteArray()
        val type = MessageType.USER_MESSAGE_ALL.value

        // 1. Execute
        val packets = PacketFactory.createPackets(
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
        assertEquals(SpecialRecipients.BROADCAST, firstPacket.header.receiverID)

        // Verify Signature length (HMAC-SHA512 is 64 bytes)
        assertEquals(64, firstPacket.signature.size)
    }

    @Test
    fun testSignatureIntegrity() {
        val payload = "Secret Message".encodeToByteArray()
        val senderId = 999uL
        val type = MessageType.USER_MESSAGE_ALL.value
        val timeStamp = System.currentTimeMillis().toULong()
        val packets = PacketFactory.createPackets(
            type = type,
            senderID = senderId,
            payload = payload,
            inputTimeStamp = timeStamp
        )

        val packet = packets[0]

        // Manually recreate the HMAC to verify the factory did it correctly
        val secretKey = NativeCredentials.getGlobalChatKey()
        val hmac = Mac.getInstance("HmacSHA512")
        hmac.init(SecretKeySpec(secretKey.encodeToByteArray(), "HmacSHA512"))

        // Note: You must match the exact buffer layout used in createBroadcastPackets
        val buffer = java.nio.ByteBuffer.allocate(36 + payload.size).order(java.nio.ByteOrder.BIG_ENDIAN)
        buffer.putShort(1u.toShort())            // version
        buffer.putShort(packet.header.flags.toShort())
        buffer.putInt(type.toInt())             // type
        buffer.putShort(packet.payload.size.toShort()) // payload.size (MISSING IN YOUR TEST)
        buffer.putShort(0.toShort())            // fragmentID (index 0)
        buffer.putShort(1.toShort())            // totalFragments
        buffer.putLong(timeStamp.toLong())      // timeStamp
        buffer.putLong(senderId.toLong())       // senderID
        buffer.put(payload)

        val expectedSignature = hmac.doFinal(buffer.array())

        assertArrayEquals("The HMAC signature in the packet is invalid!", expectedSignature, packet.signature)
    }

    @Test
    fun testFragmentation() {
        // Create a payload larger than MAX_PAYLOAD_LENGTH (e.g., 500 bytes)
        val largePayload = ByteArray(500) { it.toByte() }

        val packets = PacketFactory.createPackets(
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

    @Test
    fun testPacketSerializationRoundTrip() {
        // 1. Create a dummy packet
        val originalHeader = Header(
            version = 1u,
            flags = Packet.NEED_ACK,
            type = MessageType.USER_MESSAGE_ALL.value,
            TTL = 20u,
            totalFragments = 1u,
            fragmentID = 0u,
            timeStamp = System.currentTimeMillis().toULong(),
            receiverID = SpecialRecipients.BROADCAST,
            senderID = 987654321uL
        )
        val dummySignature = ByteArray(64) { 0xA.toByte() }
        val dummyPayload = "Test Payload".encodeToByteArray()

        val originalPacket = Packet(originalHeader, dummySignature, dummyPayload)

        // 2. Encode to ByteArray
        val encodedBytes = Packet.encode(originalPacket)
        assertNotNull("Encoded bytes should not be null", encodedBytes)

        // 3. Decode back to Packet
        val decodedPacket = Packet.decode(encodedBytes!!)
        assertNotNull("Decoded packet should not be null", decodedPacket)

        // 4. Assertions (Compare all fields)
        assertEquals(originalPacket.header.version, decodedPacket!!.header.version)
        assertEquals(originalPacket.header.type, decodedPacket.header.type)
        assertEquals(originalPacket.header.senderID, decodedPacket.header.senderID)
        assertEquals(originalPacket.header.timeStamp, decodedPacket.header.timeStamp)

        // Verify Binary Data
        assertArrayEquals("Signature mismatch after decoding", originalPacket.signature, decodedPacket.signature)
        assertArrayEquals("Payload mismatch after decoding", originalPacket.payload, decodedPacket.payload)
    }

    @Test
    fun testDecodeWithInvalidData() {
        val garbageData = byteArrayOf(0x00, 0x01, 0x02) // Too short to be a packet
        val result = Packet.decode(garbageData)

        assertNull("Decoding garbage data should return null, not throw exception", result)
    }

    @Test
    fun testPaddingDoesNotCorruptData() {
        val shortPayload = "Short".encodeToByteArray()
        val packet = Packet(1u, 0u, 1u, 20u, 1u, 0u, 100uL, 0uL, 0uL, ByteArray(64), shortPayload)

        val encoded = Packet.encode(packet)
        val decoded = Packet.decode(encoded!!)

        assertEquals("Payload length should match original despite padding",
            shortPayload.size, decoded?.payload?.size)
        assertArrayEquals(shortPayload, decoded?.payload)
    }
}