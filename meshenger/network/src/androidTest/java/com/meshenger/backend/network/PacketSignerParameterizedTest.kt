package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class PacketSignerParameterizedTest {

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
    var expectedVerificationPath: Int = 0 // 1: GlobalChat, 2: GlobalProtocol, 3: DirectProtocol

    companion object {
        @JvmStatic
        @Parameters(name = "{index}: {0}")
        fun generateSignerData(): Collection<Array<Any>> {
            val standardPayload = "Secure Mesh Data".encodeToByteArray()
            val largePayload = ByteArray(10000) { it.toByte() } // Triggers packet fragmentation
            val broadcastLong = SpecialRecipients.BROADCAST.toLong()

            return listOf(
                // --- Standard Payloads ---
                arrayOf("Global Chat Path Alignment", MessageType.USER_MESSAGE_ALL.value.toInt(), standardPayload, broadcastLong, 1),
                arrayOf("Global App Protocol Alignment", MessageType.BOOTSTRAP.value.toInt(), standardPayload, broadcastLong, 2),
                arrayOf("Direct Communication Path Alignment", MessageType.USER_MESSAGE_ONE_TO_ONE.value.toInt(), standardPayload, 7777777L, 3),
                arrayOf("Noise Handshake Protocol Alignment Verification", MessageType.NOISE_HANDSHAKE.value.toInt(), standardPayload, 8888888L, 3),

                // --- Fragmented Large Payloads ---
                arrayOf("Global Chat Large Payload (Fragmented)", MessageType.USER_MESSAGE_ALL.value.toInt(), largePayload, broadcastLong, 1),
                arrayOf("Direct 1-to-1 Large Payload (Fragmented)", MessageType.USER_MESSAGE_ONE_TO_ONE.value.toInt(), largePayload, 9999999L, 3)
            )
        }
    }

    @Test
    fun verifySignatureAndVerificationSymmetry() {
        // 1. Generate the packets via PacketFactory to accurately replicate real network fragments
        val generatedPackets = PacketFactory.createPackets(
            type = type.toUInt(),
            payload = payload,
            receiverID = receiverID.toULong()
        )

        assertNotNull("Packet factory returned null collection for: $description", generatedPackets)
        assertTrue("Packet factory generated an empty list for: $description", generatedPackets.isNotEmpty())

        // If the payload was large, this runs through each fragmented block
        for ((index, packetFragment) in generatedPackets.withIndex()) {

            // 2. Generate the expected signature dynamically based on the specific fragment's headers
            val signature = when (expectedVerificationPath) {
                1 -> PacketSigner.getSignatureGlobalChat(
                    version = packetFragment.header.version,
                    flags = packetFragment.header.flags,
                    type = packetFragment.header.type,
                    payload = packetFragment.payload,
                    fragmentID = packetFragment.header.fragmentID,
                    totalFragments = packetFragment.header.totalFragments,
                    timeStamp = packetFragment.header.timeStamp,
                    senderID = packetFragment.header.senderID
                )
                2 -> PacketSigner.getSignatureGlobalProtocol(
                    version = packetFragment.header.version,
                    flags = packetFragment.header.flags,
                    type = packetFragment.header.type,
                    payload = packetFragment.payload,
                    fragmentID = packetFragment.header.fragmentID,
                    totalFragments = packetFragment.header.totalFragments,
                    timeStamp = packetFragment.header.timeStamp,
                    senderID = packetFragment.header.senderID
                )
                3 -> PacketSigner.getSignatureDirectProtocol(
                    version = packetFragment.header.version,
                    flags = packetFragment.header.flags,
                    type = packetFragment.header.type,
                    payload = packetFragment.payload,
                    fragmentID = packetFragment.header.fragmentID,
                    totalFragments = packetFragment.header.totalFragments,
                    timeStamp = packetFragment.header.timeStamp,
                    senderID = packetFragment.header.senderID,
                    receiverID = packetFragment.header.receiverID
                )
                else -> fail("Invalid verification path defined in data rows") as ByteArray
            }

            // 3. Inject signature back into our test block clone to assert verification consistency
            val signedTestPacket = Packet(
                version = packetFragment.header.version,
                flags = packetFragment.header.flags,
                type = packetFragment.header.type,
                TTL = packetFragment.header.TTL,
                totalFragments = packetFragment.header.totalFragments,
                fragmentID = packetFragment.header.fragmentID,
                timeStamp = packetFragment.header.timeStamp,
                receiverID = packetFragment.header.receiverID,
                senderID = packetFragment.header.senderID,
                signature = signature,
                payload = packetFragment.payload
            )

            // 4. Validate through the PacketSigner's verification engines
            val isVerified = when (expectedVerificationPath) {
                1 -> PacketSigner.verifyGlobalChatKey(signedTestPacket)
                2 -> PacketSigner.verifyGlobalProtocolKey(signedTestPacket)
                3 -> PacketSigner.verifyDirectProtocolKey(signedTestPacket)
                else -> false
            }

            val ContextMsg = "$description [Fragment ${index + 1}/${generatedPackets.size}]"
            assertTrue("Signature verification asymmetrical layout failed on: $ContextMsg", isVerified)
        }
    }
}