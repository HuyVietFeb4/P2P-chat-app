package com.meshenger.backend.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.meshenger.backend.transport2.MPAddress
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MPAddressTest {

    @Test
    fun testGetMyMPAddress_ReturnsCorrectLength() {
        // Arrange
        val expectedLength = 8

        // Act
        val address = MPAddress.getMyMPAddress(length = expectedLength)

        // Assert
        assertNotNull(address)
        assertEquals(expectedLength, address.size)
    }

    @Test
    fun testAddressConsistency_WithinSameEpoch() {
        // Generating the address twice rapidly should yield the same result
        val address1 = MPAddress.getMyMPAddress()
        val address2 = MPAddress.getMyMPAddress()

        assertArrayEquals("Addresses should be identical within the same epoch", address1, address2)
    }
}