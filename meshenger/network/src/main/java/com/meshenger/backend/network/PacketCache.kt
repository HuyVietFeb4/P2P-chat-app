// LRUCache android
// Maybe store as Key: signature, Value: The packet object
package com.meshenger.backend.network

import android.util.LruCache
import okio.ByteString
import okio.ByteString.Companion.toByteString

open class PacketCache(val maxCacheSize: Int) {
    // 2. Initialize the LruCache with the provided size
    protected val cache = LruCache<ByteString, Packet>(maxCacheSize)

    fun checkMembership(signature: ByteArray): Boolean =
        cache.get(signature.toByteString()) != null // return true if already present

    fun addToCache(signature: ByteArray, packet: Packet) {
        if(!checkMembership(signature)) {
            cache.put(signature.toByteString(), packet)
        }
    }

    fun addIfNew(signature: ByteArray, packet: Packet): Boolean {
        val key = signature.toByteString()
        if (cache.get(key) != null) return false
        cache.put(key, packet)
        return true
    }

    fun removeFromCache(signature: ByteArray) {
        cache.remove(signature.toByteString())
    }

    fun getPacketInCache(signature: ByteArray): Packet? {
        return cache.get(signature.toByteString())
    }

    fun getAllKeys(): List<ByteString> = cache.snapshot().keys.toList()

    fun getAllPackets(): List<Packet> = cache.snapshot().values.toList()

    fun getAllEntries(): Map<ByteString, Packet> = cache.snapshot()
}

// 3. Define the specialized caches with different sizes
object UserPacketCache : PacketCache(5000)

// Renamed to avoid confusion with the concept of a 'Protocol'
object ProtocolPacketCache : PacketCache(500)