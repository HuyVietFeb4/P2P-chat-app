package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import okio.ByteString.Companion.toByteString
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class ReassemblyQueueParameterizedTest {

    @Parameter(0)
    @JvmField
    var description: String = ""

    @Parameter(1)
    @JvmField
    var type: Int = 0

    @Parameter(2)
    @JvmField
    var payload: ByteArray = ByteArray(0)

    @Parameter(3)
    @JvmField
    var receiverID: Long = 0L

    @Parameter(4)
    @JvmField
    var processingOrder: Int = 0 // 1: Sequential, 2: Shuffled (Out-of-Order)

    @Parameter(5)
    @JvmField
    var expectedToComplete: Boolean = true

    @Before
    fun setUp() {
        ReassemblyQueue.queue.evictAll()
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{index}: {0}")
        fun generateQueueTestData(): Collection<Array<Any>> {
            val standardPayload = "Short Single-Fragment Payload".encodeToByteArray()
            val largePayload = ByteArray(2000) { it.toByte() }
            val broadcastLong = SpecialRecipients.BROADCAST.toLong()

            return listOf(
                // Scenario A: Standard single-packet data delivery
                arrayOf("Single Fragment Delivery Edge Case", MessageType.USER_MESSAGE_ALL.value.toInt(), standardPayload, broadcastLong, 1, true),

                // Scenario B: Large broadcast payload arriving sequentially
                arrayOf("Large Broadcast Payload - Sequential Arrival", MessageType.USER_MESSAGE_ALL.value.toInt(), largePayload, broadcastLong, 1, true),

                // Scenario C: Large direct payload arriving out of order
                arrayOf("Large 1-to-1 Payload - Out-of-Order Arrival", MessageType.USER_MESSAGE_ONE_TO_ONE.value.toInt(), largePayload, 999999L, 2, true),

                // Scenario D: Chaos Simulation - Packet loss where the message remains incomplete
                arrayOf("Large 1-to-1 Payload - Out-of-Order with Simulated Packet Loss", MessageType.USER_MESSAGE_ONE_TO_ONE.value.toInt(), largePayload, 999999L, 2, false)
            )
        }
    }

    @Test
    fun verifyQueueReassemblyPipeline() {
        // 1. Generate real fragments using PacketFactory
        val fragments = PacketFactory.createPackets(
            type = type.toUInt(),
            payload = payload,
            receiverID = receiverID.toULong()
        )

        assertNotNull("PacketFactory returned null for: $description", fragments)
        assertTrue("PacketFactory generated an empty fragment list for: $description", fragments.isNotEmpty())

        // 2. Sort or adjust arrival delivery layout matrix
        var orderedFragments = when (processingOrder) {
            2 -> fragments.shuffled()
            else -> fragments
        }

        // --- THE FIX FOR PACKET LOSS ---
        // If we expect it to fail completion, drop the first element from our processing stream
        if (!expectedToComplete && orderedFragments.size > 1) {
            orderedFragments = orderedFragments.drop(1)
        }

        var executionResult: ByteArray? = null
        val processingCount = orderedFragments.size

        // 3. Process the stream fragments
        if (fragments.size > 1) {
            for ((index, packet) in orderedFragments.withIndex()) {
                val key = ReassemblyQueue.getKeyFragment(packet)

                val currentResult = ReassemblyQueue.addToQueue(
                    key,
                    packet.payload,
                    packet.header.totalFragments.toInt(),
                    packet.header.fragmentID.toInt()
                )

                // The last packet processed *in our loop* is our checking boundary
                if (index == processingCount - 1) {
                    executionResult = currentResult
                } else {
                    assertNull("Intermediate tracking segment leaked premature assembly on: $description", currentResult)
                }
            }
        } else {
            // If it's a single fragment and we simulate a drop, it never arrives at all
            if (expectedToComplete) {
                executionResult = fragments[0].payload
            }
        }

        // 4. Final assertions verification
        if (expectedToComplete) {
            assertNotNull("Reassembly processing stalled across segment boundaries for: $description", executionResult)
            assertArrayEquals("Payload byte alignment corrupted post-reassembly sequence for: $description", payload, executionResult)

            val trackingKey = ReassemblyQueue.getKeyFragment(fragments[0])
            assertNull("Completed cache node failed to auto-evict upon delivery completion", ReassemblyQueue.queue.get(trackingKey.toByteString()))
        } else {
            // Passes cleanly now because the missing item ensures the internal array filter retains structural null values
            assertNull("Expected incomplete package payload structure processing block to return null reference for: $description", executionResult)

            // Explicit sanity check: Assert that the partial data structure is still hanging out waiting inside the LRU cache
            if (fragments.size > 1) {
                val trackingKey = ReassemblyQueue.getKeyFragment(fragments[0])
                assertNotNull("Incomplete packet array was prematurely dropped from tracking context", ReassemblyQueue.queue.get(trackingKey.toByteString()))
            }
        }
    }
}