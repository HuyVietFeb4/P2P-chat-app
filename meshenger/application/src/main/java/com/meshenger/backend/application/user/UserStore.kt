package com.meshenger.backend.application.user

import com.meshenger.backend.application.db.MeshengerDbHelper

/**
 * Local profile is persisted in [users]. Peer list comes from the same table (excluding local id).
 */
object UserStore {

    private const val LOCAL_ID = "local-device"
    private const val PLACEHOLDER_HASH = "-"

    /** Default profile name on first install; must change for mesh so peers stay distinguishable. */
    const val DEFAULT_PROFILE_USER_NAME = "Local User"

    private var profile: UserProfile = UserProfile(
        id = LOCAL_ID,
        publicKeyHash = PLACEHOLDER_HASH,
        userName = DEFAULT_PROFILE_USER_NAME
    )

    private val favoritePeers: MutableSet<String> = mutableSetOf()
    private val blockedPeers: MutableSet<String> = mutableSetOf()

    @Volatile
    private var db: MeshengerDbHelper? = null

    fun init(dbHelper: MeshengerDbHelper) {
        db = dbHelper
        val loaded = dbHelper.getUserProfile(LOCAL_ID)
        profile = loaded ?: profile
        if (loaded == null) {
            dbHelper.upsertUserProfile(profile)
        }
    }

    fun getProfile(): UserProfile = profile

    fun getAllPeers(): List<UserProfile> {
        val allUsers = db?.getAllUserProfiles() ?: emptyList()
        return allUsers.filter { it.id != LOCAL_ID }
    }

    fun updateProfile(userName: String, publicKeyHash: String? = null): UserProfile {
        profile = profile.copy(
            userName = userName,
            publicKeyHash = publicKeyHash ?: profile.publicKeyHash
        )
        db?.upsertUserProfile(profile)
        return profile
    }

    fun setFavorite(peerId: String, isFavorite: Boolean) {
        if (peerId.isBlank()) return
        if (isFavorite) favoritePeers.add(peerId) else favoritePeers.remove(peerId)
    }

    fun setBlocked(peerId: String, isBlocked: Boolean) {
        if (peerId.isBlank()) return
        if (isBlocked) blockedPeers.add(peerId) else blockedPeers.remove(peerId)
    }

    /** True for the factory-default name — too ambiguous for mesh bootstrap / chat lists. */
    fun isGenericMeshDisplayName(name: String): Boolean =
        name.trim().equals(DEFAULT_PROFILE_USER_NAME, ignoreCase = true)
}
