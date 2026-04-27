package com.meshenger.backend.network

import android.util.LruCache
import com.meshenger.backend.security_native.NativeCredentials
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

val maxQSize = 5000
// For the purpose of storing the fragment for reassembly.
// Type LruCache with key is hash value of timeStamp, type, totalFragments, senderID, recieverID and sign by app secret key
object ReassemblyQueue {
    val queue = LruCache<ByteString, Array<ByteArray?>>(maxQSize)
    private val appKeySpec by lazy {
        SecretKeySpec(NativeCredentials.getAppSecretKey().encodeToByteArray(), "HmacSHA512")
    }
    fun getKeyFragment(packet: Packet): ByteArray {
        val sha512HMAC = Mac.getInstance("HmacSHA512")
        sha512HMAC.init(appKeySpec)

        val buffer = ByteBuffer.allocate(22)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(packet.header.type.toInt())
        buffer.putShort(packet.header.totalFragments.toShort())
        buffer.putLong(packet.header.timeStamp.toLong())
        buffer.putLong(packet.header.senderID.toLong())

        return sha512HMAC.doFinal(buffer.array())
    }
    // Add fragment to queue. First check if there are packets that
    fun addToQueue(signature: ByteArray, fragment: ByteArray, totalFragments: Int, fragmentId: Int): ByteArray? {
        val messageKey = signature.toByteString()
        var fragments = queue.get(signature.toByteString())
        if (fragments == null) {
            fragments = arrayOfNulls<ByteArray>(totalFragments)
            queue.put(messageKey, fragments)
        }
        fragments[fragmentId] = fragment
        if(fragments.all{it != null}) {
            val completeMessage = FragmentUtil.reassembly(fragments.filterNotNull())
            queue.remove(messageKey)
            return completeMessage
        }
        return null
    }
}