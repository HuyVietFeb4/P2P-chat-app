package com.meshenger.backend.transport2

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.nio.ByteBuffer

@RunWith(Parameterized::class)
class MPAddressParameterizedTest {

    @Parameter(0)
    @JvmField
    var description: String = ""

    @Parameter(1)
    @JvmField
    var mockPublicKey: ByteArray = ByteArray(0)

    @Parameter(2)
    @JvmField
    var inputEpochHours: Int = 24

    companion object {
        @JvmStatic
        @Parameters(name = "{index}: {0}")
        fun generateAddressTestData(): Collection<Array<Any>> {
            return listOf(
                // Scenario A: Standard Node Identity Tracking
                arrayOf("Standard Node Identity - 24h Epoch", ByteArray(32) { 0xA.toByte() }, 24),

                // Scenario B: Alternative Identity Matrix
                arrayOf("Alternative Node Identity - 24h Epoch", ByteArray(32) { 0xB.toByte() }, 24),

                // Scenario C: Alternative Identity Matrix
                arrayOf("Alternative Node Identity - 24h Epoch", ByteArray(32) { 0xC.toByte() }, 24),

                // Scenario D: Alternative Identity Matrix
                arrayOf("Alternative Node Identity - 24h Epoch", ByteArray(32) { 0xD.toByte() }, 24)
            )
        }
    }

    @Test
    fun verifyIdentityAndCodecRoundTrips() {
        // 1. Generate baseline address via input parameters
        val addressBytes = MPAddress.calculateCurrentMPAddress(
            identityPublicKey = mockPublicKey,
            epochHours = inputEpochHours,
            length = 8
        )

        // 2. Validate Type Cast Round-Trip Symmetry (Black-Box Assertion)
        val addressString = MPAddress.MPAddressByteArrayToString(addressBytes)
        val recoveredBytesFromString = MPAddress.MPAddressStringToByteArray(addressString)
        assertArrayEquals("String-to-ByteArray translation chain corrupted identity data!", addressBytes, recoveredBytesFromString)

        // 3. Validate ULong Bit Alignment Symmetry
        val addressULong = MPAddress.MPAddressByteArrayToULong(addressBytes)
        val reconstructedBuffer = ByteBuffer.allocate(8).putLong(addressULong.toLong()).array()
        assertArrayEquals("ULong bit conversion tracking introduced byte displacement!", addressBytes, reconstructedBuffer)
    }

    @Test
    fun verifyIdentityDivergence() {
        // Two unique nodes calculating addresses within identical execution time brackets MUST diverge
        val nodeABytes = MPAddress.calculateCurrentMPAddress(ByteArray(32) { 0x1.toByte() }, inputEpochHours, 8)
        val nodeBBytes = MPAddress.calculateCurrentMPAddress(ByteArray(32) { 0x2.toByte() }, inputEpochHours, 8)

        assertFalse("Cryptographic address collision occurred across distinct nodes!", nodeABytes.contentEquals(nodeBBytes))
    }

    @Test
    fun verifySameIdentity() {
        // The same node must produce the same MP Address in the same time epoch
        val nodeABytes = MPAddress.calculateCurrentMPAddress(ByteArray(32) { 0x1.toByte() }, inputEpochHours, 8)
        val stillNodeABytes = MPAddress.calculateCurrentMPAddress(ByteArray(32) { 0x1.toByte() }, inputEpochHours, 8)

        assertTrue("Cryptographic address is not euqal for the same node!", nodeABytes.contentEquals(stillNodeABytes))
    }
}