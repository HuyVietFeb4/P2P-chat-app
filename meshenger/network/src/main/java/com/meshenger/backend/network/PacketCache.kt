// LRUCache android
// Maybe store as Key: signature, Value: The packet object
package com.meshenger.backend.network

import android.util.LruCache
import okio.ByteString.Companion.toByteString
val maxCacheSize = 10000
object PacketCache {
    val cache = LruCache<String, Packet>(maxCacheSize)
    // Bytes per entry: 56 + 24 + 80 + 412 = 572 bytes
    // Total bytes for the cache = maxCacheSize * 572
    // 10000 entries = 5.5 mb
    private fun ByteArray.toHex() = this.joinToString("") { "%02x".format(it) }
    fun checkMembership(signature: ByteArray): Boolean {
        return cache.get(signature.toHex()) != null
    }

    fun addToCache(signature: ByteArray, packet: Packet) {
        cache.put(signature.toHex(), packet)
    }

    fun removeFromCache(signature: ByteArray) {
        cache.remove(signature.toHex())
    }

    fun getPacketInCache(signature: ByteArray): Packet? {
        return cache.get(signature.toHex())
    }
}