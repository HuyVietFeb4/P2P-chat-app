package com.meshenger.backend.session
data class Peer(
    var userName: String,
    var MPAddress: ULong,
    var avatarId: String? = null
) {
    override fun equals(other: Any?): Boolean {
        // 1. Referential Equality (Check if the same memory address)
        if (this === other) return true

        // 2. Type Check
        if (other !is Peer) return false
        return this.MPAddress == other.MPAddress
    }
    override fun hashCode(): Int { // WTF
        return MPAddress.hashCode()
    }
    fun updateUserName(newName: String) {
        this.userName = newName
    }
    fun updateMPAddress(newAddress: ULong) {
        this.MPAddress = newAddress
    }
    fun updateAvatarId(id: String?) {
        this.avatarId = id
    }
}