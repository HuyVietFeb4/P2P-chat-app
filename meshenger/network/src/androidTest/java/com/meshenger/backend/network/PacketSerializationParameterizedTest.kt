package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import kotlin.Int
import kotlin.toUInt

@RunWith(Parameterized::class)
class PacketSerializationParameterizedTest {
    @Parameter(0)
    @JvmField
    val description: String = ""

    @Parameter(1)
    @JvmField
    val type: Int = 0

    @Parameter(2)
    @JvmField
    val payload: ByteArray = ByteArray(0)


    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun generateSerializationData(): Collection<Array<Any>> {
            return listOf(
                // Scenario A: Standard Packet
                arrayOf("Standard Plain Packet", MessageType.USER_MESSAGE_ALL.value.toInt(), "Normal Msg".encodeToByteArray()),

                // Scenario B: Packet requiring Acknowledgment flag
                arrayOf("Packet with NEED_ACK Flag enabled", MessageType.USER_MESSAGE_ONE_TO_ONE.value.toInt(), "Ack Required Payload".encodeToByteArray()),

                // Scenario C: Packet with Compression flag
                arrayOf("Packet with IS_COMPRESSED Flag enabled", MessageType.ANTI_ENTROPY_REQUEST.value.toInt(), "Compressed Payload Chunk Data".encodeToByteArray()),

                // Scenario D: Boundary Condition - Completely Empty Payload (Testing Byte Alignment / Padding Core Logic)
                arrayOf("Boundary Condition - Zero Length Byte Payload", MessageType.BOOTSTRAP.value.toInt(), ByteArray(0)),

                // Scenario E: Boundary Condition - Completely Empty Payload (Testing Byte Alignment / Padding Core Logic)
                arrayOf("Fragmentation Condition - >= 396 Length Byte Payload", MessageType.USER_MESSAGE_ALL.value.toInt(), ByteArray(500) { it.toByte() })
            )
        }
    }

    @Test
    fun verifyBinaryCodecRoundTrip() {
        val dummySignature = ByteArray(64) { 0x1.toByte() } // Matches PacketLimitConfig expectations

        val packetLst = PacketFactory.createPackets(
            type = type.toUInt(),
            payload = payload,
            receiverID = 111111UL
        )
        for (packet in packetLst) {
            // 1. Execute Binary Encode
            val serializedBytes = Packet.encode(packet)
            assertNotNull("Binary encoding processing failed for: $description", serializedBytes)

            // 2. Execute Binary Decode
            val deserializedPacket = Packet.decode(serializedBytes!!)
            assertNotNull("Binary decoding pipeline returned structural null object for: $description", deserializedPacket)

            // 3. Complete Equivalence Assertions Check
            assertEquals("Flags bitwise value altered post-roundtrip", packet.header.flags, deserializedPacket!!.header.flags)
            assertEquals("Type routing configuration corrupted post-roundtrip", packet.header.type, deserializedPacket.header.type)
            assertEquals("Payload byte structure length mismatch after decode padding clean", packet.payload.size, deserializedPacket.payload.size)
            assertArrayEquals("Payload data array mismatch", packet.payload, deserializedPacket.payload)
            assertArrayEquals("Signature verification arrays corrupted", packet.signature, deserializedPacket.signature)
        }
    }
}