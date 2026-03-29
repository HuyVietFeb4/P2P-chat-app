package com.meshenger.backend.application.messaging

/**
 * Model for a single chat message.
 * Used by MessagingStore and exposed to JS via getConversation.
 */
data class Message(
    val id: String,
    val peerId: String,
    val text: String,
    val fromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
