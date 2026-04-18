package com.meshenger.backend.network
import java.nio.ByteBuffer
import com.google.crypto.tink.subtle.Hkdf

import com.meshenger.backend.security_native.NativeCredentials
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder

object MPAddress {
    private val SALT = "This-is-salt-value-to-generate-routing-id".toByteArray()
    private val INFO_PREFIX = "Meshenger-v1-generate-routing-id".toByteArray()
    fun getMyMPAddress(
        epochHours: Int = 2,
        length: Int = 8
    ): ByteArray {
        val publicKeyEncoded = StaticKeyManager.getRawIdentityPublicKey(StaticKeyManager.getOrCreateIdentityKey().public)
        val epochSeconds = epochHours * 3600
        val currentTimeStamp = System.currentTimeMillis() / 1000
        val epochIndex = currentTimeStamp / epochSeconds

        val epochBytes = ByteBuffer.allocate(8).putLong(epochIndex).array()
        val stream = ByteArrayOutputStream()
        stream.write(INFO_PREFIX)
        stream.write(epochBytes)
        stream.write(NativeCredentials.getAppSecretKey().toByteArray())

        val info = stream.toByteArray()
        return Hkdf.computeHkdf(
            "HmacSHA256",
            publicKeyEncoded, // IKM
            SALT,           // Salt
            info,           // Info
            length          // Output Length
        )
    }

    fun getMyMPAddressULong(): ULong {
        val MPAddressByteArray = this.getMyMPAddress()
        val buffer = ByteBuffer.wrap(MPAddressByteArray)

        // 2. Set the byte order (Big Endian is default, Little Endian is common in BLE)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // 3. Get as Long, then convert to ULong
        return buffer.long.toULong()
    }
}