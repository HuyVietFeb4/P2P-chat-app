package com.meshenger.backend.session

import android.util.Log
import com.meshenger.backend.network.EpidemicFlooding
import com.meshenger.backend.network.NetworkMessageListener
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.session.Session
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object GlobalChatSession : Session(), NetworkMessageListener {
    private val TAG_LENGTH = 128
    private val ALGORITHM = "AES/GCM/NoPadding"
    init {
        // Register this session as the listener for the network layer
        EpidemicFlooding.setListener(this)
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
        val encryptMsg = globalChatEncrypt(GlobalChatKey, message.encodeToByteArray(), timeStamp)
        EpidemicFlooding.onGlobalChatMessageSend(encryptMsg, timeStamp)
    }

    override fun recieveMessageStr(senderMPAddress: ULong, encryptedData: ByteArray, nonceTimeStamp: ULong) {
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        val decryptMsg = globalChatDecrypt(GlobalChatKey, encryptedData, nonceTimeStamp.toLong())

        // Save message operation
        // Push to UI
        val jsonResult = buildJsonObject {
            put("Sender Address", senderMPAddress.toString())
            put("Message", String(decryptMsg, Charsets.UTF_8))
            put("SessionType", "GlobalChat")
        }
        Log.d("GlobalChatSession", "${senderMPAddress} said: ${String(decryptMsg, Charsets.UTF_8)}")
        messageBus.tryEmit(jsonResult)
    }

    override fun onGlobalMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {
        // This effectively calls your existing logic
        this.recieveMessageStr(senderID, payload, timeStamp)
    }
}