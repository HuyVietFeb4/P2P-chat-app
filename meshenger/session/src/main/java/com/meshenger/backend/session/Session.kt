package com.meshenger.backend.session

import android.util.Log
import com.google.crypto.tink.tinkkey.SecretKeyAccess
import kotlinx.serialization.json.*
import com.meshenger.backend.network.BasicFlooding
import com.meshenger.backend.network.NetworkMessageListener
import com.meshenger.backend.network.SpecialRecipients
import com.meshenger.backend.security_native.NativeCredentials
import kotlinx.coroutines.flow.MutableSharedFlow
import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

abstract class Session {
    protected val peers = mutableListOf<Peer>()
    protected fun getFixedKey(rawKey: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(rawKey.toByteArray(Charsets.UTF_8))
    }
    open fun addMember(newMember: Peer) {
        if (!peers.contains(newMember)) {
            peers.add(newMember)
        }
    }
    open fun removeMember(toRemove: Peer) {
        if(peers.contains(toRemove)) {
            peers.remove(toRemove)
        }
    }
    open fun getPeerUsername(addressToFind: ULong): String? {
        return peers.find { it.MPAddress == addressToFind }?.userName
    }
    abstract fun sendMessageStr(
        receiverMPAddress: ULong = SpecialRecipients.BROADCAST,
        message: String
    )
    abstract fun recieveMessageStr(senderMPAddress: ULong, encryptedData: ByteArray, nonceTimeStamp: ULong): JsonObject
//    abstract fun sendFile(dest: Peer, file: ByteArray)
//    abstract fun recieveFile(senderMPAddress: ULong, file: ByteArray)
}

object GlobalChatSession : Session(), NetworkMessageListener {
    private val TAG_LENGTH = 128
    private val ALGORITHM = "AES/GCM/NoPadding"
    init {
        // Register this session as the listener for the network layer
        BasicFlooding.setListener(this)
    }
    // Only for UI testing, will be deleted later
    private fun globalChatEncrypt(secretKey: ByteArray, message: ByteArray, timeStamp: Long): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteBuffer.allocate(12).putLong(timeStamp).putInt(0).array()
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        val keySpec = SecretKeySpec(secretKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
        return cipher.doFinal(message)
    }

    private fun globalChatDecrypt(secretKey: ByteArray, encryptedData: ByteArray, timeStamp: Long): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteBuffer.allocate(12).putLong(timeStamp).putInt(0).array()
        val spec = GCMParameterSpec(TAG_LENGTH, iv)
        val keySpec = SecretKeySpec(secretKey, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
        return cipher.doFinal(encryptedData)
    }
    override fun sendMessageStr(recieverMPAddress: ULong, message: String) {
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        val timeStamp =  System.currentTimeMillis()
        val encryptMsg = globalChatEncrypt(GlobalChatKey, message.toByteArray(), timeStamp)
        BasicFlooding.onUserMessageSend(encryptMsg, timeStamp)
    }

    override fun recieveMessageStr(senderMPAddress: ULong, encryptedData: ByteArray, nonceTimeStamp: ULong): JsonObject {
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        val decryptMsg = globalChatDecrypt(GlobalChatKey, encryptedData, nonceTimeStamp.toLong())

        // Save message operation
        // Push to UI
        val jsonResult = buildJsonObject {
            put("Sender Address", senderMPAddress.toString())
            put("Message", String(decryptMsg, Charsets.UTF_8))
        }
        Log.d("GlobalChatSession", "${senderMPAddress} said: ${String(decryptMsg, Charsets.UTF_8)}")
        return jsonResult
    }

    override fun onUserMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {
        // This effectively calls your existing logic
        this.recieveMessageStr(senderID, payload, timeStamp)
    }
}