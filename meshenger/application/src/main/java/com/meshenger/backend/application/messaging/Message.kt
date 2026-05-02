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
    val encryptedPayload: String
)
