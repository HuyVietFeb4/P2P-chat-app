package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class PacketFactoryParameterizedTest {

    @Parameter(0)
    @JvmField
    var description: String = ""

    // Changed to standard Int for JUnit reflection compatibility
    @Parameter(1)
    @JvmField
    var type: Int = 0

    @Parameter(2)
    @JvmField
    var payload: ByteArray = ByteArray(0)

    // Changed to standard Long for JUnit reflection compatibility
    @Parameter(3)
    @JvmField
    var receiverID: Long = 0L

    @Parameter(4)
    @JvmField
    var expectedMinFragments: Int = 0

    // Changed to standard Long for JUnit reflection compatibility
    @Parameter(5)
    @JvmField
    var expectedReceiverID: Long = 0L
    companion object {
        @JvmStatic
        @Parameters(name = "{index}: {0}")
        fun generateData(): Collection<Array<Any>> {
            val standardPayload = "Hello World".encodeToByteArray()
            val largePayload = ByteArray(500) { it.toByte() }

            // Convert all ULong/UInt fields to standard Long/Int inside the array matrix via .toLong() / .toInt()
            val broadcastLong = SpecialRecipients.BROADCAST.toLong()

            return listOf(
                arrayOf("Global Broadcast Message", MessageType.USER_MESSAGE_ALL.value.toInt(), standardPayload, broadcastLong, 1, broadcastLong),
                arrayOf("Protocol Bootstrap Packet", MessageType.BOOTSTRAP.value.toInt(), standardPayload, broadcastLong, 1, broadcastLong),
                arrayOf("Direct 1-to-1 Chat Packet", MessageType.USER_MESSAGE_ONE_TO_ONE.value.toInt(), standardPayload, 555555L, 1, 555555L),
                arrayOf("Noise Handshake Protocol", MessageType.NOISE_HANDSHAKE.value.toInt(), standardPayload, 999999L, 1, 999999L),
                arrayOf("Large Payload Fragmentation Verification", MessageType.USER_MESSAGE_ALL.value.toInt(), largePayload, broadcastLong, 2, broadcastLong)
            )
        }
    }

    @Test
    fun verifyPacketCreationPipeline() {
        // 1. Execute Factory Pipeline (convert primitives back to Unsigned values seamlessly)
        val packets = PacketFactory.createPackets(
            type = type.toUInt(),
            payload = payload,
            receiverID = receiverID.toULong()
        )

        // 2. Data-Driven Validations
        if (expectedMinFragments == 0) {
            assertTrue("Expected empty packet collection for unhandled type: $description", packets.isEmpty())
        } else {
            assertNotNull("Packet list should not be null: $description", packets)
            assertTrue("Expected at least $expectedMinFragments fragment(s) for: $description", packets.size >= expectedMinFragments)

            val firstPacket = packets[0]
            assertEquals("Message type routing mismatched for: $description", type.toUInt(), firstPacket.header.type)
            assertEquals("Receiver addressing routing mismatched for: $description", expectedReceiverID.toULong(), firstPacket.header.receiverID)
            assertEquals("Total fragment tracking value mismatch", packets.size.toUShort(), firstPacket.header.totalFragments)

            assertNotNull("Signature block missing for: $description", firstPacket.signature)
            assertEquals("Signature size specification corrupted", 64, firstPacket.signature.size)
        }
    }
}