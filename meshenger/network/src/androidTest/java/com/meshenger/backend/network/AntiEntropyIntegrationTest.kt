package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AntiEntropyIntegrationTest {

    @Before
    fun setup() {
        // Clear caches to ensure isolation between tests
        UserPacketCache.getAllEntries().keys.forEach { UserPacketCache.removeFromCache(it.toByteArray()) }
        ProtocolPacketCache.getAllEntries().keys.forEach { ProtocolPacketCache.removeFromCache(it.toByteArray()) }
    }

    @Test
    fun testBloomFilterSerialization() {
        // 1. Create a mock packet signature
        val mockSignature = UUID.randomUUID().toString().toByteArray()
        
        // 2. Add to bloom filter
        val originalFilter = KMCompactRefinedBloomFilter()
        originalFilter.add(mockSignature)
        
        // 3. Serialize
        val compactedBinary = originalFilter.toCompactedBinary()
        
        // 4. Deserialize into a new bloom filter
        val restoredFilter = KMCompactRefinedBloomFilter()
        restoredFilter.fromCompactedBinary(compactedBinary)
        
        // 5. Verify that the signature is present in the restored filter
        assertTrue("Restored bloom filter should contain the mock signature", restoredFilter.isAvailable(mockSignature))
        
        // Ensure a random signature is NOT present
        val randomSignature = UUID.randomUUID().toString().toByteArray()
        assertFalse("Restored bloom filter should NOT contain a random signature", restoredFilter.isAvailable(randomSignature))
    }

    @Test
    fun testAntiEntropyMissingPacketDetection() {
        // 1. Phone A creates a message packet and stores it in its cache
        val payload = "Important Message".encodeToByteArray()
        val packetA = PacketFactory.createPackets(
            type = MessageType.USER_MESSAGE_ONE_TO_ONE.value,
            senderID = 1001uL,
            receiverID = 2002uL,
            payload = payload,
            inputTimeStamp = 1700000000000uL
        ).single()
        
        // Put in Phone A's cache (Simulated by the global UserPacketCache)
        UserPacketCache.addToCache(packetA.signature, packetA)
        
        // 2. Phone B initiates Anti-Entropy Request
        // Phone B has an empty cache, so it generates an empty Bloom Filter
        val phoneBBloomFilter = KMCompactRefinedBloomFilter()
        
        // 3. Phone A receives the Bloom Filter and checks against its cache
        val allEntries = UserPacketCache.getAllEntries()
        var foundMissingPacket = false
        
        for (entry in allEntries) {
            val signature = entry.key.toByteArray()
            if (!phoneBBloomFilter.isAvailable(signature)) {
                foundMissingPacket = true
                assertArrayEquals(packetA.signature, signature)
            }
        }
        
        assertTrue("Phone A should detect that Phone B is missing the packet", foundMissingPacket)
    }

    @Test
    fun testAntiEntropyRespondPackaging() {
        // 1. Phone A creates a packet
        val originalPayload = "Secret Payload to Recover".encodeToByteArray()
        val packetA = PacketFactory.createPackets(
            type = MessageType.USER_MESSAGE_ONE_TO_ONE.value,
            senderID = 999uL,
            receiverID = 888uL,
            payload = originalPayload,
            inputTimeStamp = 1800000000000uL
        ).single()
        
        // 2. Phone A realizes B is missing the packet, so A wraps it in ANTI_ENTROPY_RESPOND
        val encodedPacketA = Packet.encode(packetA)!!
        val respondPackets = PacketFactory.createPackets(
            type = MessageType.ANTI_ENTROPY_RESPOND.value,
            senderID = 999uL,
            receiverID = 888uL,
            payload = encodedPacketA,
            inputTimeStamp = 1800000000000uL
        )
        
        // 3. Phone B receives the ANTI_ENTROPY_RESPOND packet via Epidemic Flooding
        // Since it might be fragmented, we push it through ReassemblyQueue
        var extractedBinary: ByteArray? = null
        for (fragment in respondPackets) {
            val wire = Packet.encode(fragment)!!
            val decoded = Packet.decode(wire)!!
            assertTrue("Fragment signature must be valid", PacketSigner.verifyDirectProtocolKey(decoded))
            
            val key = ReassemblyQueue.getKeyFragment(decoded)
            extractedBinary = ReassemblyQueue.addToQueue(
                key,
                decoded.payload,
                decoded.header.totalFragments.toInt(),
                decoded.header.fragmentID.toInt()
            ) ?: extractedBinary
        }
        
        // Ensure we fully reassembled the packet
        assertTrue("Extracted payload should not be null", extractedBinary != null)
        
        // B extracts the payload, which is the serialized Packet A
        val restoredPacketA = Packet.decode(extractedBinary!!)!!
        
        // 4. Verify the restored packet matches the original packet
        assertEquals(packetA.header.type, restoredPacketA.header.type)
        assertEquals(packetA.header.senderID, restoredPacketA.header.senderID)
        assertArrayEquals(packetA.payload, restoredPacketA.payload)
        assertEquals(String(originalPayload), String(restoredPacketA.payload))
        
        // 5. Verify the signature is still valid
        assertTrue("Restored packet signature must be valid", PacketSigner.verifyDirectProtocolKey(restoredPacketA))
    }
}
