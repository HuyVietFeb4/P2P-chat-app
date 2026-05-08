package com.meshenger.backend.session

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import android.util.Log

object PeerInMeshRegistry {
    private const val TAG = "PeerRegistry"
    private const val EXPIRY_DURATION_MS = 5_000L // 5 Minutes
    private const val CHECK_INTERVAL_MS = 5_000L   // 5 Seconds

    private val peerMap = ConcurrentHashMap<ULong, Peer>()
    // Instead of Jobs, we store the "Last Seen" timestamp
    private val lastSeenMap = ConcurrentHashMap<ULong, Long>()

    private val registryScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    init {
        // One single loop for the entire application lifetime
        registryScope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                cleanExpiredPeers()
            }
        }
    }

    private fun cleanExpiredPeers() {
        val now = System.currentTimeMillis()
        val iterator = lastSeenMap.entries.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val address = entry.key
            val lastSeen = entry.value

            if (now - lastSeen > EXPIRY_DURATION_MS) {
                // Remove from both maps
                iterator.remove()
                val removedPeer = peerMap.remove(address)

                Log.d(TAG, "Sweep: Peer ${removedPeer?.userName} ($address) expired.")
            }
        }
    }

    fun addOrUpdatePeer(peer: Peer) {
        peerMap[peer.MPAddress] = peer
        refreshDeadline(peer.MPAddress)
    }

    fun refreshDeadline(address: ULong) {
        // Simply update the timestamp. O(1) operation, very fast.
        lastSeenMap[address] = System.currentTimeMillis()
    }

    fun getAllPeers(): Collection<Peer> {
        return peerMap.values
    }

}