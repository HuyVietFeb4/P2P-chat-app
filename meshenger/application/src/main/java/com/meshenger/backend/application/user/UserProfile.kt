package com.meshenger.backend.application.user

/**
 * Updated UserProfile model based on the new schema.
 */
data class UserProfile(
    val id: String,
    val publicKeyHash: String,
    val userName: String,
    val userAvtId: String? = null
)
