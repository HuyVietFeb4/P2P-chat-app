package com.meshenger.backend.network
import android.util.Log
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.transport2.StaticKeyManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object PacketSigner {
    private val globalKeySpec by lazy { SecretKeySpec(NativeCredentials.getGlobalChatKey().encodeToByteArray(), "HmacSHA512") }
    private val directKeySpec by lazy { SecretKeySpec(NativeCredentials.getTwoPartyChatKey().encodeToByteArray(), "HmacSHA512") }
    private val appKeySpec by lazy { SecretKeySpec(NativeCredentials.getAppSecretKey().encodeToByteArray(), "HmacSHA512") }

    // --- Verification Logic ---

    fun verifyGlobalChatKey(packet: Packet): Boolean = verifyHmac(packet, globalKeySpec, false)
    fun verifyGlobalProtocolKey(packet: Packet): Boolean = verifyHmac(packet, appKeySpec, false)
    fun verifyDirectProtocolKey(packet: Packet): Boolean = verifyHmac(packet, directKeySpec, true)

    private fun verifyHmac(packet: Packet, key: SecretKeySpec, isDirect: Boolean): Boolean {
        val expected = if (isDirect) {
            getSignatureDirectProtocol(
                packet.header.version, packet.header.flags, packet.header.type, packet.payload,
                packet.header.fragmentID, packet.header.totalFragments, packet.header.timeStamp,
                packet.header.senderID, packet.header.receiverID
            )
        } else {
            getSignatureGlobalChat(
                packet.header.version, packet.header.flags, packet.header.type, packet.payload,
                packet.header.fragmentID, packet.header.totalFragments, packet.header.timeStamp,
                packet.header.senderID
            )
        }
        return MessageDigest.isEqual(expected, packet.signature)
    }

    // --- Ed25519 Identity Logic ---

    fun signTwoPartySession(version: UShort, flags: UShort, type: UInt,
                            payload: ByteArray, fragmentID: UShort, totalFragments: UShort,
                            timeStamp: ULong, senderID: ULong, receiverID: ULong): ByteArray
    {
        val buffer = buildBaseBuffer(version, flags, type, payload,
            fragmentID, totalFragments, timeStamp, senderID, true)
        buffer.putLong(receiverID.toLong())
        buffer.put(payload)
        val identityKeyPair = StaticKeyManager.getOrCreateIdentityKey()
        return Signature.getInstance("Ed25519").run {
            initSign(identityKeyPair.private)
            update(buffer.array())
            sign()
        }
    }

    fun verifyTwoPartySession(data: ByteArray, signature: ByteArray, remotePublicKey: PublicKey): Boolean {
        return try {
            Signature.getInstance("Ed25519").run {
                initVerify(remotePublicKey)
                update(data)
                verify(signature)
            }
        } catch (e: Exception) {
            Log.e("PacketSigner", "Ed25519 Verification failed: ${e.message}")
            false
        }
    }

    fun reassembleSignedData(packet: Packet): ByteArray {
        // 44 bytes for header fields + payload size
        val size = 44 + packet.payload.size
        val buffer = ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN)

        buffer.putShort(packet.header.version.toShort())
        buffer.putShort(packet.header.flags.toShort())
        buffer.putInt(packet.header.type.toInt())
        buffer.putShort(packet.payload.size.toShort())
        buffer.putShort(packet.header.fragmentID.toShort())
        buffer.putShort(packet.header.totalFragments.toShort())
        buffer.putLong(packet.header.timeStamp.toLong())
        buffer.putLong(packet.header.senderID.toLong())
        buffer.putLong(packet.header.receiverID.toLong())
        buffer.put(packet.payload)

        return buffer.array()
    }

    // --- Signing Logic (Buffer Helpers) ---

    fun getSignatureGlobalChat(version: UShort, flags: UShort, type: UInt, payload: ByteArray, fragmentID: UShort, totalFragments: UShort, timeStamp: ULong, senderID: ULong): ByteArray {
        val buffer = buildBaseBuffer(version, flags, type, payload, fragmentID, totalFragments, timeStamp, senderID)
        return calculateHmac(buffer.array(), globalKeySpec)
    }
    fun getSignatureGlobalProtocol(version: UShort, flags: UShort, type: UInt, payload: ByteArray, fragmentID: UShort, totalFragments: UShort, timeStamp: ULong, senderID: ULong): ByteArray {
        val buffer = buildBaseBuffer(version, flags, type, payload, fragmentID, totalFragments, timeStamp, senderID)
        return calculateHmac(buffer.array(), appKeySpec)
    }

    fun getSignatureDirectProtocol(version: UShort, flags: UShort, type: UInt, payload: ByteArray, fragmentID: UShort, totalFragments: UShort, timeStamp: ULong, senderID: ULong, receiverID: ULong): ByteArray {
        val buffer = buildBaseBuffer(version, flags, type, payload, fragmentID, totalFragments, timeStamp, senderID, true)
        buffer.putLong(receiverID.toLong())
        buffer.put(payload)
        return calculateHmac(buffer.array(), directKeySpec)
    }

    private fun buildBaseBuffer(version: UShort, flags: UShort, type: UInt, payload: ByteArray, fragmentID: UShort, totalFragments: UShort, timeStamp: ULong, senderID: ULong, isDirect: Boolean = false): ByteBuffer {
        val size = (if (isDirect) 44 else 36) + payload.size
        return ByteBuffer.allocate(size).order(ByteOrder.BIG_ENDIAN).apply {
            putShort(version.toShort())
            putShort(flags.toShort())
            putInt(type.toInt())
            putShort(payload.size.toShort())
            putShort(fragmentID.toShort())
            putShort(totalFragments.toShort())
            putLong(timeStamp.toLong())
            putLong(senderID.toLong())
            if (!isDirect) put(payload) // Put payload now if no receiverID follows
        }
    }

    private fun calculateHmac(data: ByteArray, key: SecretKeySpec): ByteArray {
        return Mac.getInstance("HmacSHA512").apply {
            init(key)
        }.doFinal(data)
    }
}