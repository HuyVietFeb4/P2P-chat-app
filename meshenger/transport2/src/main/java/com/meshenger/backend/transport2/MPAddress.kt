package com.meshenger.backend.transport2
import java.nio.ByteBuffer
import com.google.crypto.tink.subtle.Hkdf

import com.meshenger.backend.security_native.NativeCredentials
import java.io.ByteArrayOutputStream
import java.nio.ByteOrder
import android.util.Base64

object MPAddress {
    private val SALT = "This-is-salt-value-to-generate-routing-id".encodeToByteArray()
    private val INFO_PREFIX = "Meshenger-v1-generate-routing-id".encodeToByteArray()
    fun getMyMPAddress(
        epochHours: Int = 2,
        length: Int = 8
    ): ByteArray {
        val publicKeyEncoded = StaticKeyManager.getRawPublicKey(StaticKeyManager.getOrCreateAgreementKey().public)
        val epochSeconds = epochHours * 3600
        val currentTimeStamp = System.currentTimeMillis() / 1000
        val epochIndex = currentTimeStamp / epochSeconds

        val epochBytes = ByteBuffer.allocate(8).putLong(epochIndex).array()
        val stream = ByteArrayOutputStream()
        stream.write(INFO_PREFIX)
        stream.write(epochBytes)
        stream.write(NativeCredentials.getAppSecretKey().encodeToByteArray())

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

    fun getMyMPAddressString(): String {
        val MPAddressByteArray = this.getMyMPAddress()
        val addrStr = Base64.encodeToString(MPAddressByteArray, Base64.NO_WRAP)
        return addrStr
    }

    fun MPAddressStringToByteArray(address: String): ByteArray {
        return Base64.decode(address, Base64.NO_WRAP)
    }

    fun MPAddressByteArrayToString(address: ByteArray): String {
        return Base64.encodeToString(address, Base64.NO_WRAP)
    }

    fun MPAddressByteArrayToULong(address: ByteArray): ULong {
        val buffer = ByteBuffer.wrap(address)

        // 2. Set the byte order (Big Endian is default, Little Endian is common in BLE)
        buffer.order(ByteOrder.BIG_ENDIAN)

        // 3. Get as Long, then convert to ULong
        return buffer.long.toULong()
    }

    fun MPAddressBeautifulcation(address: ByteArray): String {
        TODO("Implement eautifulcation format")
    }
}