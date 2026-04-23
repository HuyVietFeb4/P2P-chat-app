package com.meshenger.backend.application.messaging

enum class MessageStatus {
    PENDING,
    SENT,
    FAILED
}

data class Message(
    val id: String,
    val peerId: String,
    val text: String,
    val fromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)