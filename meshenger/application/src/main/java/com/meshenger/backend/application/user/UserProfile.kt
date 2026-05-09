package com.meshenger.backend.application.user

/** Stored in [users.security]; surfaced in the chat list as a trust / pairing hint. */
object PeerSecurity {
    const val WEAK = "weak"
    const val MEDIUM = "medium"
    const val STRONG = "strong"
}

/**
 * User/peer row in SQLite [users].
 */
data class UserProfile(
    val id: String,
    val publicKeyHash: String,
    val userName: String,
    val userAvtId: String? = null,
    val security: String = PeerSecurity.MEDIUM,
)
