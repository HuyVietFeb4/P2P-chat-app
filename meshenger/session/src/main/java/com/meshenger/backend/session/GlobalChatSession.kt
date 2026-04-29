package com.meshenger.backend.session

import android.util.Log
import com.meshenger.backend.network.EpidemicFlooding
import com.meshenger.backend.network.NetworkMessageListener
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.transport2.MPAddress
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import kotlinx.serialization.json.put


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

    override fun sendMessageStr(receiverMPAddress: ULong, message: String) {
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        val timeStamp =  System.currentTimeMillis()
        val encryptMsg = globalChatEncrypt(GlobalChatKey, message.encodeToByteArray(), timeStamp)
        val jsonResult = buildJsonObject {
            put("PeerID", receiverMPAddress.toLong())
            put("Payload", Base64.encodeToString(encryptMsg, Base64.NO_WRAP))
            put("Message", message)
            put("Nonce", timeStamp)
            put("SessionType", "GlobalChat")
            put("Action", "Send")
        }
        _messageBus.tryEmit(jsonResult)
        EpidemicFlooding.onGlobalChatMessageSend(encryptMsg, timeStamp)
    }

    override fun receiveMessageStr(senderMPAddress: ULong, encryptedData: ByteArray, nonceTimeStamp: ULong) {
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        try {
            val decryptMsg = globalChatDecrypt(GlobalChatKey, encryptedData, nonceTimeStamp.toLong())
            val plainText = String(decryptMsg, Charsets.UTF_8)
            Log.d("GlobalChatSession", "${senderMPAddress} said: ${plainText}")
            val jsonResult = buildJsonObject {
                put("PeerID", senderMPAddress.toLong())
                put("Payload", Base64.encodeToString(encryptedData, Base64.NO_WRAP))
                put("Message", plainText)
                put("Nonce", nonceTimeStamp.toLong())
                put("SessionType", "GlobalChat")
                put("Action", "Receive")
            }

            _messageBus.tryEmit(jsonResult)
        } catch (e: Exception) {
            Log.e("GlobalChatSession", "Failed to decrypt global message: ${e.message}")
        }
    }

    override fun onGlobalMessageReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {
        // This effectively calls your existing logic
        this.receiveMessageStr(senderID, payload, timeStamp)
    }

    fun sendBootstrap(userName: String) {
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        val payLoad = "$userName|${MPAddress.getMyMPAddressString()}"
        val timeStamp =  System.currentTimeMillis()
        val encryptMsg = globalChatEncrypt(GlobalChatKey, payLoad.encodeToByteArray(), timeStamp)
        EpidemicFlooding.onBootstrapSend(encryptMsg, System.currentTimeMillis())
    }

    override fun onBootStrapReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        val decryptMsg = globalChatDecrypt(GlobalChatKey, payload, timeStamp.toLong())
        val decoded = String(decryptMsg, Charsets.UTF_8)
        val parts = decoded.split("|")
        val userName = parts[0]
        val peerMpAddres = parts[1].toULong()
        PeerInMeshRegistry.addOrUpdatePeer(Peer(userName, peerMpAddres))
    }
}