package com.meshenger.backend.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.util.Log

import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.transport2.MPAddress
import javax.crypto.spec.SecretKeySpec

enum class MessageType(val value: UInt) {
    BOOTSTRAP(0x0000u),
    NOISE_HANDSHAKE(0x0001u),
    ANTI_ENTROPY_REQUEST(0x0002u),
    ANTI_ENTROPY_RESPOND(0x0003u),
    JOIN_REQUEST(0x0004u),
    JOIN_TABLE(0x0005u),
    CREATE_GROUP(0x0006u),
    UPDATE_GROUP(0x0007u),
    REMOVE_USER_GROUP(0x0008u),
    ADD_USER_GROUP(0x0009u),
    WELCOME_GROUP(0x000Au),
    GENERATE_SEED_GROUP(0x000Bu),

    USER_MESSAGE_ALL(0x0101u),
    USER_MESSAGE_ONE_TO_ONE(0x0102u),
    USER_MESSAGE_GROUP(0x0103u),
    SEEN(0x0104u),
    REACTION(0x0105u),
    FILE(0x0106u),
    REPLY_QUOTE(0x0107u),
    AUDIO(0x0108u),

    /** 1:1 chat invite — sender asks receiver (must accept before Noise handshake). */
    DIRECT_CHAT_INVITE(0x0109u),
    DIRECT_CHAT_INVITE_ACCEPT(0x010Au),
    DIRECT_CHAT_INVITE_REJECT(0x010Bu);

    companion object {
        fun fromValue(value: UInt): MessageType? {
            return values().find { it.value == value }
        }
    }
}

object SpecialRecipients {
    val BROADCAST_BYTE = ByteArray(8) { 0xFF.toByte() }
    val BROADCAST: ULong by lazy {
        ByteBuffer.wrap(BROADCAST_BYTE).getLong().toULong()
    }
}

/**
 * Packet format
 *
 * Header:
 * - Version: 2 bytes
 * - Flags: 2 bytes (bit 0: needAck, bit 1: isCompressed)
 * - Types: 4 bytes
 * - TTL: 2 bytes
 * - Payload Length: 2 bytes (big-endian: left to right)
 * - Total fragments: 2 bytes 
 * - Fragment ID: 2 bytes 
 * - Timestamp: 8 bytes
 * - ReceiverID: 8 bytes
 * - SenderID: 8 bytes
 *
 * Payload sections: 0-396 bytes
 */
@Parcelize
data class Header(
    val version: UShort,
    val flags: UShort,
    val type: UInt,
    val TTL: UShort,
    val totalFragments: UShort,
    val fragmentID: UShort,
    val timeStamp: ULong,
    val receiverID: ULong,
    val senderID: ULong,

): Parcelable

@Parcelize
data class Packet(
    val header: Header,
    val signature: ByteArray,
    val payload: ByteArray,
): Parcelable {
    val payloadLength: UShort 
        get() = payload.size.toUShort()

    init {
        // Validation happens here, ensuring NO Packet can ever be created 
        // that is too large, regardless of which constructor is used.
        // will always be used No matter how a class is created (Primary constructor, Secondary constructor, or even by the @Parcelize internal code) 
        require(payload.size <= PacketLimitConfig.MAX_PAYLOAD_LENGTH) {
            "Payload is too large! Max is $PacketLimitConfig.MAX_PAYLOAD_LENGTH bytes, but got ${payload.size}."
        }
        require(signature.size == PacketLimitConfig.SIGNATURE_SIZE) {
            "Signature size must be exactly $PacketLimitConfig.SIGNATURE_SIZE bytes!"
        }
    }

    constructor(
        version: UShort,
        flags: UShort,
        type: UInt,
        TTL: UShort,
        totalFragments: UShort,
        fragmentID: UShort,
        timeStamp: ULong,
        receiverID: ULong,
        senderID: ULong,
        signature: ByteArray,
        payload: ByteArray
    ): this(
        header = Header(
            version = version,
            flags = flags,
            type = type,
            TTL = TTL,
            totalFragments = totalFragments,
            fragmentID = fragmentID,
            timeStamp = timeStamp,
            receiverID = receiverID,
            senderID = senderID,
        ),
        signature = signature,
        payload = payload
    )

    companion object {
        const val NEED_ACK: UShort = 0x0001u
        const val IS_COMPRESSED: UShort = 0x0002u
        private const val HEADER_SIZE = 102

        fun decode(rawData: ByteArray): Packet? {
            try {
                val buffer = ByteBuffer.wrap(rawData).order(ByteOrder.BIG_ENDIAN)
                val version = buffer.short.toUShort()
                val flags = buffer.short.toUShort()
                val type = buffer.int.toUInt()
                val ttl = buffer.short.toUShort()
                val payloadLength = buffer.short.toUShort()
                val totalFrags = buffer.short.toUShort()
                val fragID = buffer.short.toUShort()
                val time = buffer.long.toULong()
                val receiver = buffer.long.toULong()
                val sender = buffer.long.toULong()
                val signature = ByteArray(PacketLimitConfig.SIGNATURE_SIZE)
                buffer.get(signature)
                val payload = ByteArray(payloadLength.toInt())
                buffer.get(payload)

                return Packet(
                    header = Header(version, flags, type, ttl, totalFrags, fragID, time, receiver, sender),
                    signature = signature,
                    payload = payload
                )
            } catch(e: Exception) {
                Log.e("BinaryUtil", "Error decoding rawData into Packet object: ${e.message}")
                return null
            }
        }

        fun encode(packet: Packet): ByteArray? {
            try {
                val buffer = ByteBuffer.allocate(PacketLimitConfig.BLE_MAX_SIZE).order(ByteOrder.BIG_ENDIAN)
                
                buffer.putShort(packet.header.version.toShort())
                buffer.putShort(packet.header.flags.toShort())
                buffer.putInt(packet.header.type.toInt())
                buffer.putShort(packet.header.TTL.toShort())
                buffer.putShort(packet.payloadLength.toShort())
                buffer.putShort(packet.header.totalFragments.toShort())
                buffer.putShort(packet.header.fragmentID.toShort())
                buffer.putLong(packet.header.timeStamp.toLong())
                buffer.putLong(packet.header.receiverID.toLong())
                buffer.putLong(packet.header.senderID.toLong())
                buffer.put(packet.signature)
                val payload = PaddingUtil.pad(packet.payload, PacketLimitConfig.MAX_PAYLOAD_LENGTH)
                buffer.put(payload)
                return buffer.array()
            } catch(e: Exception) {
                Log.e("BinaryUtil", "Error encoding packet type ${packet.header.type}: ${e.message}")
                return null
            }
        }
    }
}

object PacketFactory {
    private const val DEFAULT_VERSION: UShort = 1u
    private const val DEFAULT_TTL: UShort = 20u
    private val globalKeySpec by lazy {
        SecretKeySpec(NativeCredentials.getGlobalChatKey().encodeToByteArray(), "HmacSHA512")
    }
    private val directKeySpec by lazy {
        SecretKeySpec(NativeCredentials.getTwoPartyChatKey().encodeToByteArray(), "HmacSHA512")
    }

    fun createPackets( // For all type of packet
        type: UInt,
        payload: ByteArray,
        senderID: ULong = MPAddress.getMyMPAddressULong(),
        receiverID: ULong = SpecialRecipients.BROADCAST,
        inputTimeStamp: ULong = System.currentTimeMillis().toULong(),
        needAck: Boolean = false,
        isCompressed: Boolean = false,
        version: UShort = DEFAULT_VERSION,
        TTL: UShort = DEFAULT_TTL
    ): List<Packet> {
        val fragments = FragmentUtil.toFragments(payload)
        val packetList = mutableListOf<Packet>()
        var flags: UShort = 0u
        if(needAck) {
            flags = flags or Packet.NEED_ACK
        }
        if(isCompressed) {
            flags = flags or Packet.IS_COMPRESSED
        }
        // For signing in broadcast mode
        for ((index, fragment) in fragments.withIndex()) {
            // Signature:
                // If for all chat: using native hiding secret key to hmac payload and other field
            // The payload must be encrypted from session layer above
            var signature: ByteArray? = null
            when (type) {
                MessageType.USER_MESSAGE_ALL.value -> {
                    signature = PacketSigner.getSignatureGlobalChat(version, flags, type, fragment,
                        index.toUShort(), fragments.size.toUShort(), inputTimeStamp, senderID
                    )
                }
                MessageType.BOOTSTRAP.value -> {
                    signature = PacketSigner.getSignatureGlobalProtocol(
                        version, flags, type, fragment,
                        index.toUShort(), fragments.size.toUShort(), inputTimeStamp, senderID
                    )
                }

                MessageType.ANTI_ENTROPY_REQUEST.value,
                MessageType.ANTI_ENTROPY_RESPOND.value,
                MessageType.NOISE_HANDSHAKE.value -> {
                    signature = PacketSigner.getSignatureDirectProtocol(
                        version, flags, type, fragment,
                        index.toUShort(), fragments.size.toUShort(), inputTimeStamp,
                        senderID, receiverID
                    )
                }
                MessageType.USER_MESSAGE_ONE_TO_ONE.value,
                MessageType.DIRECT_CHAT_INVITE.value,
                MessageType.DIRECT_CHAT_INVITE_ACCEPT.value,
                MessageType.DIRECT_CHAT_INVITE_REJECT.value -> {
                    // Keep 1:1 signing consistent with current receiver verification path
                    // (verifyDirectProtocolKey = HMAC direct key). A mismatch here causes
                    // packets to be dropped even when transport is healthy.
                    signature = PacketSigner.getSignatureDirectProtocol(
                        version, flags, type, fragment,
                        index.toUShort(), fragments.size.toUShort(), inputTimeStamp,
                        senderID, receiverID
                    )
                }
                else -> {
                    Log.e("PacketFactory", "Unhandled message type: $type")
                }
            }
            if (signature == null) {
                Log.e("PacketFactory", "Error creating signature: Result was null")
                return emptyList()
            }
            val packet = Packet(version, flags, type, TTL, fragments.size.toUShort(), index.toUShort(), inputTimeStamp,
                receiverID, senderID, signature,fragment)
            packetList.add(packet)
        }
        return packetList
    }
}