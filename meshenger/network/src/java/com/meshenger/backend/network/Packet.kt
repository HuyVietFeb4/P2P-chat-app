package com.meshenger.backend.network

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

val maxPayloadLength = 396

enum class MessageType(val value: UInt) {
    BOOTSTRAP(0x0000u),
    NOISE_HANDSHAKE(0x0001u),
    ANTI_ENTROPY(0x0002u),
    ANTI_ENTROPY_REPLY(0x0003u),
    JOIN_REQUEST(0x0004u),
    JOIN_TABLE(0x0005u),
    CREATE_GROUP(0x0006u),
    UPDATE_GROUP(0x0007u),
    REMOVE_USER_GROUP(0x0008u),
    ADD_USER_GROUP(0x0009u),
    WELCOME_GROUP(0x000Au),
    GENERATE_SEED_GROUP(0x000Bu),

    USER_MESSAGE(0x0101u),
    SEEN(0x0102u),
    REACTION(0x0103u),
    FILE(0x0104u),
    REPLY_QUOTE(0x0105u),
    AUDIO(0x0106u);

    companion object {
        fun fromValue(value: UInt): MessageType? {
            return values().find { it.value == value }
        }
    }
}

object SpecialRecipients {
    val BROADCAST = ByteArray(8) { 0xFF.toByte() }  // All 0xFF = broadcast
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
 * - RecieverID: 8 bytes
 * - SenderID: 8 bytes
 *
 * Payload sections: 0-396 bytes
 */

 @Parcelize
data class Packet(
    val version: UShort,
    val flags: UShort,
    val type: UInt,
    val TTL: UShort,
    val totalFragments: UShort,
    val fragmentID: Ushort,
    val timeStamp: ULong,
    val recieverID: ULong,
    val senderID: ULong,
    val payload: ByteArray
): Parcelable {
    val payloadLength: UShort 
        get() = payload.size.toUShort()

    init {
        // Validation happens here, ensuring NO Packet can ever be created 
        // that is too large, regardless of which constructor is used.
        // will always be used No matter how a class is created (Primary constructor, Secondary constructor, or even by the @Parcelize internal code) 
        require(payload.size <= maxPayloadSize) {
            "Payload is too large! Max is $maxPayloadSize bytes, but got ${payload.size}."
        }
    }

    constructor(
        flags: UShort,
        type: UInt,
        TTL: UShort,
        totalFragments: UShort,
        fragmentID: Ushort,
        recieverID: ULong,
        senderID: ULong,
        payload: ByteArray
    ): this(
        version = 1u,
        flags = flags,
        type = type,
        TTL = TTL,
        totalFragments = totalFragments,
        fragmentID = fragmentID,
        timeStamp = (System.currentTimeMillis()).toULong(),
        recieverID = recieverID,
        senderID = senderID,
        payload = payload
    )
}

object BinaryUtil {
    object Flags {
        const val NEED_ACK: UByte = 0x01u
        const val IS_COMPRESSED: UByte = 0x02u
    }

    fun decode(raw: ByteArray): Packet? {

    }

    fun encode(packet: Packet): ByteArray {
        
    }
}