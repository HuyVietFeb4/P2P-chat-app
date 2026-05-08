package com.meshenger.backend.session

import android.util.Log
import com.meshenger.backend.network.EpidemicFlooding
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.transport2.MPAddress
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import com.meshenger.backend.network.GlobalMessageListener
import com.meshenger.backend.network.ListenerRegistry
import kotlinx.serialization.json.put


object GlobalChatSession : Session(), GlobalMessageListener {
    /**
     * Optional hook for the app layer to persist display names when a mesh peer
     * announces via bootstrap (e.g. upgrade SQLite rows that still use `mp:` placeholders).
     */
    @Volatile
    var onMeshPeerAnnounced: ((mpAddress: ULong, displayName: String) -> Unit)? = null

    private val TAG_LENGTH = 128
    private val ALGORITHM = "AES/GCM/NoPadding"
    init {
        // Register this session as the listener for the network layer
        ListenerRegistry.setGlobalListener(this)
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
        offerMessageBus(jsonResult)
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

            offerMessageBus(jsonResult)
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
        // CRITICAL: encryption IV is derived from this timestamp; the receiver re-derives the IV
        // from the packet header timestamp, so both must be the SAME value. (Previous code
        // generated two separate currentTimeMillis() calls, which silently broke AES-GCM auth.)
        val timeStamp = System.currentTimeMillis()
        val encryptMsg = globalChatEncrypt(GlobalChatKey, payLoad.encodeToByteArray(), timeStamp)
        EpidemicFlooding.onBootstrapSend(encryptMsg, timeStamp)
    }

    override fun onBootStrapReceived(senderID: ULong, payload: ByteArray, timeStamp: ULong) {
        if (senderID == MPAddress.getMyMPAddressULong()) {
            return
        }
        val GlobalChatKey = getFixedKey(NativeCredentials.getGlobalChatKey())
        val decryptMsg = try {
            globalChatDecrypt(GlobalChatKey, payload, timeStamp.toLong())
        } catch (e: Exception) {
            Log.w(
                "GlobalChatSession",
                "Bootstrap decrypt failed (sender=$senderID ts=$timeStamp): ${e.message}",
            )
            return
        }
        val decoded = String(decryptMsg, Charsets.UTF_8)
        val parts = decoded.split("|", limit = 2)
        if (parts.size < 2) {
            Log.w("GlobalChatSession", "Bootstrap payload missing MP address: $decoded")
            return
        }
        val userName = parts[0]
        val mpRaw = parts[1].trim()
        // Bootstrap encodes MP address with Base64 (see sendBootstrap), so decode then read 8 BE bytes.
        val peerMpAddres = try {
            mpRaw.toULongOrNull()
                ?: MPAddress.MPAddressByteArrayToULong(MPAddress.MPAddressStringToByteArray(mpRaw))
        } catch (e: Exception) {
            Log.w("GlobalChatSession", "Bootstrap MP address unparseable: '$mpRaw' (${e.message})")
            return
        }
        PeerInMeshRegistry.addOrUpdatePeer(Peer(userName, peerMpAddres))
        Log.d("GlobalChatSession", "Bootstrap received: $userName ($peerMpAddres)")
        try {
            onMeshPeerAnnounced?.invoke(peerMpAddres, userName.trim())
        } catch (e: Exception) {
            Log.w("GlobalChatSession", "onMeshPeerAnnounced failed: ${e.message}")
        }
    }
}