package com.meshenger.backend.application.messaging

enum class MessageStatus {
    PENDING,
    SENT,
    FAILED
}

/**
 * Model for a single chat message based on the updated schema.
 */
data class Message(
    val id: String,
    val sessionId: String,
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val nonce: String,
    val status: MessageStatus,
    val encryptedPayload: String,
    /** Local plaintext for UI history (two-party ciphertext alone cannot be decrypted after a new Noise session). */
    val bodyText: String? = null,
)
