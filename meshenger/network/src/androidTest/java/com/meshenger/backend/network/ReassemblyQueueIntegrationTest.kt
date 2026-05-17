package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReassemblyQueueIntegrationTest {

    @Test
    fun fragmentedGlobalMessage_reassemblesInOrder() {
        val original = ByteArray(500) { (it % 251).toByte() }
        val senderId = 42424242uL
        val timestamp = 1_700_000_000_000uL
        val packets = PacketFactory.createPackets(
            type = MessageType.USER_MESSAGE_ALL.value,
            senderID = senderId,
            payload = original,
            inputTimeStamp = timestamp,
        )
        assertTrue(packets.size > 1)

        var assembled: ByteArray? = null
        for (packet in packets) {
            val key = ReassemblyQueue.getKeyFragment(packet)
            val result = ReassemblyQueue.addToQueue(
                key,
                packet.payload,
                packet.header.totalFragments.toInt(),
                packet.header.fragmentID.toInt(),
            )
            if (result != null) {
                assembled = result
            }
        }

        assertArrayEquals(original, assembled)
    }

    @Test
    fun fragmentedGlobalMessage_reassemblesOutOfOrder() {
        val original = ByteArray(500) { (it % 251).toByte() }
        val packets = PacketFactory.createPackets(
            type = MessageType.USER_MESSAGE_ALL.value,
            senderID = 99uL,
            payload = original,
            inputTimeStamp = 1_700_000_000_001uL,
        )
        require(packets.size >= 2)

        var assembled: ByteArray? = null
        for (packet in packets.asReversed()) {
            val key = ReassemblyQueue.getKeyFragment(packet)
            assembled = ReassemblyQueue.addToQueue(
                key,
                packet.payload,
                packet.header.totalFragments.toInt(),
                packet.header.fragmentID.toInt(),
            ) ?: assembled
        }

        assertArrayEquals(original, assembled)
    }

    @Test
    fun partialFragments_returnNullUntilComplete() {
        val payload = ByteArray(PacketLimitConfig.MAX_PAYLOAD_LENGTH + 50) { 7 }
        val packets = PacketFactory.createPackets(
            type = MessageType.USER_MESSAGE_ALL.value,
            senderID = 7uL,
            payload = payload,
            inputTimeStamp = 1_700_000_000_002uL,
        )

        val first = packets.first()
        val key = ReassemblyQueue.getKeyFragment(first)
        val partial = ReassemblyQueue.addToQueue(
            key,
            first.payload,
            first.header.totalFragments.toInt(),
            first.header.fragmentID.toInt(),
        )

        assertNull(partial)
    }
}
