package com.meshenger.backend.application.user

import com.meshenger.backend.application.db.MeshengerDbHelper

/**
 * Simple in-memory user/peer store for the application layer.
 * This is a placeholder; in a real app it should be backed by persistent storage.
 */
object UserStore {

    private const val LOCAL_ID = "local-device"

    // Fallback for when DB is not initialized (or during very early app lifecycle).
    private var profile: UserProfile = UserProfile(
        id = LOCAL_ID,
        displayName = "Local User",
        avatarUrl = null
    )

    private val favoritePeers: MutableSet<String> = mutableSetOf()
    private val blockedPeers: MutableSet<String> = mutableSetOf()

    @Volatile
    private var db: MeshengerDbHelper? = null

    fun init(dbHelper: MeshengerDbHelper) {
        db = dbHelper
        // Load once; later reads use cached value.
        val loaded = dbHelper.getUserProfile(LOCAL_ID)
        profile = loaded ?: profile
        // Ensure default row exists.
        if (loaded == null) {
            dbHelper.upsertUserProfile(profile)
        }
    }

    fun getProfile(): UserProfile = profile

    fun getAllPeers(): List<UserProfile> {
        val allUsers = db?.getAllUserProfiles() ?: emptyList()
        // Filter out the local user to only return actual peers.
        return allUsers.filter { it.id != LOCAL_ID }
    }

    fun updateProfile(displayName: String, avatarUrl: String?): UserProfile {
        profile = profile.copy(displayName = displayName, avatarUrl = avatarUrl)
        db?.upsertUserProfile(profile)
        return profile
    }

    fun setFavorite(peerId: String, isFavorite: Boolean) {
        if (peerId.isBlank()) return
        if (isFavorite) {
            favoritePeers.add(peerId)
        } else {
            favoritePeers.remove(peerId)
        }
    }

    fun setBlocked(peerId: String, isBlocked: Boolean) {
        if (peerId.isBlank()) return
        if (isBlocked) {
            blockedPeers.add(peerId)
        } else {
            blockedPeers.remove(peerId)
        }
    }
}
