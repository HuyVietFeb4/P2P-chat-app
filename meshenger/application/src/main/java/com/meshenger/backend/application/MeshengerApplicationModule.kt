package com.meshenger.backend.application

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.util.Log
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.messaging.Message
import com.meshenger.backend.application.messaging.MessageStatus
import com.meshenger.backend.application.messaging.MessagingStore
import com.meshenger.backend.application.security.SessionKeyVault
import com.meshenger.backend.application.user.UserProfile
import com.meshenger.backend.application.user.UserStore
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.session.GlobalChatSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * React Native native module for the Application layer.
 */
class MeshengerApplicationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val dbHelper = MeshengerDbHelper(reactContext.applicationContext)
    private val keyVault = SessionKeyVault(reactContext.applicationContext)
    private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private companion object {
        const val LOCAL_ID = "local-device"
        const val GLOBAL_CHAT_ID = "global-chat"
        const val GLOBAL_SESSION_ID = "global-session"
        const val GLOBAL_BROADCAST_ID = "global-broadcast"
        private const val GLOBAL_AEAD = "AES/GCM/NoPadding"
        private const val GLOBAL_TAG_LENGTH = 128
    }

    init {
        MessagingStore.init(dbHelper)
        UserStore.init(dbHelper)
        ensureGlobalChatStorage()
        observeGlobalChatBus()

        MessagingStore.onStatusChanged = { messageId, status ->
            val event = Arguments.createMap().apply {
                putString("id", messageId)
                putString("status", status.name)
            }
            sendEvent("onMessageStatusChanged", event)
        }
    }

    override fun getName(): String = "MeshengerApplicationModule"

    private fun sendEvent(eventName: String, params: WritableMap?) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun ensureGlobalChatStorage() {
        val globalKeyId = buildGlobalKeyId()
        dbHelper.upsertUserProfile(UserProfile(LOCAL_ID, "-", "Local User"))
        dbHelper.upsertUserProfile(UserProfile(GLOBAL_BROADCAST_ID, "-", "Global Chat"))
        dbHelper.ensureGlobalChat(GLOBAL_CHAT_ID, GLOBAL_SESSION_ID, globalKeyId)
        if (keyVault.getSessionKey(globalKeyId) == null) {
            keyVault.putSessionKey(globalKeyId, getFixedKey(NativeCredentials.getGlobalChatKey()))
        }
    }

    private fun observeGlobalChatBus() {
        moduleScope.launch {
            val keyId = dbHelper.getSessionKeyId(GLOBAL_SESSION_ID)
            val sessionKey = keyId?.let { keyVault.getSessionKey(it) }
            GlobalChatSession.getMessageBus().collect { json ->
                val payload = json["Payload"]?.toString()?.trim('"').orEmpty()
                if (payload.isEmpty()) return@collect

                val action = json["Action"]?.toString()?.trim('"').orEmpty()
                val peerId = json["PeerID"]?.toString()?.trim('"').orEmpty()
                val nonce = json["Nonce"]?.toString()?.trim('"').orEmpty()
                val plaintext = json["Message"]?.toString()?.trim('"').orEmpty()
                val senderId = if (action == "Send") LOCAL_ID else "mp:$peerId"
                val receiverIds = if (action == "Send") listOf(GLOBAL_BROADCAST_ID) else listOf(LOCAL_ID)

                if (action != "Send") {
                    dbHelper.upsertUserProfile(
                        UserProfile(
                            id = senderId,
                            publicKeyHash = "-",
                            userName = senderId
                        )
                    )
                }

                val msg = Message(
                    id = UUID.randomUUID().toString(),
                    sessionId = GLOBAL_SESSION_ID,
                    senderId = senderId,
                    nonce = nonce,
                    status = if (action == "Send") MessageStatus.PENDING else MessageStatus.SENT,
                    encryptedPayload = payload
                )
                dbHelper.insertMessage(msg, receiverIds)

                val uiMap = messageToWritableMap(
                    msg = msg,
                    fromMe = senderId == LOCAL_ID,
                    sessionKey = sessionKey,
                    plaintextOverride = plaintext
                ).apply {
                    putString("chatId", GLOBAL_CHAT_ID)
                    putString("sessionType", "GlobalChat")
                    putString("action", action)
                }
                sendEvent("onNewMessage", uiMap)
                Log.d("MeshengerApplication", "Persisted global bus event action=$action sender=$senderId")
            }
        }
    }

    @ReactMethod
    fun addPeer(id: String, displayName: String, avatarUrl: String?, promise: Promise) {
        try {
            if (id.isBlank() || displayName.isBlank()) {
                promise.reject("INVALID_INPUT", "ID and Display Name cannot be empty")
                return
            }
            val peer = UserProfile(
                id = id,
                publicKeyHash = "-",
                userName = displayName
            )
            dbHelper.upsertUserProfile(peer)
            promise.resolve("Peer $displayName saved successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            promise.reject("DB_ERROR", "Could not save to database: ${e.message}")
        }
    }

    @ReactMethod
    fun myQR(promise: Promise) {
        try {
            val profile = dbHelper.getUserProfile(LOCAL_ID)
            if (profile != null) {
                promise.resolve(profile.userName)
            } else {
                promise.reject("NOT_FOUND", "Local profile not found")
            }
        } catch (e: Exception) {
            promise.reject("DB_ERROR", e.message)
        }
    }

    @ReactMethod
    fun getMessage(promise: Promise) {
        promise.resolve("Application layer is active")
    }

    /**
     * Persists ciphertext only. [encryptedPayload] should match what you send on the wire (e.g. Base64).
     * [nonce] is stored for later decryption.
     */
    @ReactMethod
    fun sendMessage(peerId: String, encryptedPayload: String, nonce: String, promise: Promise) {
        try {
            if (peerId.isBlank() || encryptedPayload.isBlank() || nonce.isBlank()) {
                promise.reject("INVALID_INPUT", "peerId, encryptedPayload and nonce cannot be empty")
                return
            }
            val message = MessagingStore.sendMessage(peerId, encryptedPayload, nonce)
            promise.resolve(messageToWritableMap(message, fromMe = true))
        } catch (e: Exception) {
            promise.reject("SEND_FAILED", e.message)
        }
    }

    @ReactMethod
    fun pushIncomingMessage(
        peerId: String,
        senderId: String,
        encryptedPayload: String,
        nonce: String,
        promise: Promise,
    ) {
        try {
            if (peerId.isBlank() || senderId.isBlank() || encryptedPayload.isBlank() || nonce.isBlank()) {
                promise.reject("INVALID_INPUT", "All string arguments must be non-empty")
                return
            }
            val message = MessagingStore.addIncomingMessage(peerId, senderId, encryptedPayload, nonce)
            val result = messageToWritableMap(message, fromMe = false)
            sendEvent("onNewMessage", result)
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("PUSH_INCOMING_FAILED", e.message)
        }
    }

    @ReactMethod
    fun getConversation(peerId: String, promise: Promise) {
        try {
            val messages = MessagingStore.getConversation(peerId)
            val array = Arguments.createArray()
            for (msg in messages) {
                val fromMe = msg.senderId == LOCAL_ID
                array.pushMap(messageToWritableMap(msg, fromMe))
            }
            promise.resolve(array)
        } catch (e: Exception) {
            promise.reject("GET_CONVERSATION_FAILED", e.message)
        }
    }

    @ReactMethod
    fun getGlobalConversation(promise: Promise) {
        try {
            val messages = dbHelper.getConversation(GLOBAL_CHAT_ID)
            val keyId = dbHelper.getSessionKeyId(GLOBAL_SESSION_ID)
            val sessionKey = keyId?.let { keyVault.getSessionKey(it) }
            val array = Arguments.createArray()
            for (msg in messages) {
                val fromMe = msg.senderId == LOCAL_ID
                array.pushMap(messageToWritableMap(msg, fromMe, sessionKey))
            }
            promise.resolve(array)
        } catch (e: Exception) {
            promise.reject("GET_GLOBAL_CONVERSATION_FAILED", e.message)
        }
    }

    @ReactMethod
    fun globalChatSendMessageStr(message: String, promise: Promise) {
        try {
            if (message.isBlank()) {
                promise.reject("INVALID_INPUT", "message cannot be empty")
                return
            }
            GlobalChatSession.sendMessageStr(message = message)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("GLOBAL_SEND_FAILED", e.message)
        }
    }

    private fun buildGlobalKeyId(): String {
        val seed = "v1|$GLOBAL_CHAT_ID|$GLOBAL_SESSION_ID"
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.encodeToByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun getFixedKey(rawKey: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(rawKey.encodeToByteArray())
    }

    private fun decryptGlobalPayload(
        encryptedPayload: String,
        nonceString: String,
        sessionKey: ByteArray?
    ): String {
        if (sessionKey == null) return ""
        return try {
            val nonceLong = nonceString.toLongOrNull() ?: return ""
            val encryptedData = android.util.Base64.decode(encryptedPayload, android.util.Base64.NO_WRAP)
            val cipher = Cipher.getInstance(GLOBAL_AEAD)
            val iv = ByteBuffer.allocate(12).putLong(nonceLong).putInt(0).array()
            val spec = GCMParameterSpec(GLOBAL_TAG_LENGTH, iv)
            val keySpec = SecretKeySpec(sessionKey, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec)
            String(cipher.doFinal(encryptedData), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    private fun messageToWritableMap(
        msg: Message,
        fromMe: Boolean,
        sessionKey: ByteArray? = null,
        plaintextOverride: String? = null
    ): WritableMap {
        val plaintext = plaintextOverride ?: if (sessionKey != null) {
            decryptGlobalPayload(msg.encryptedPayload, msg.nonce, sessionKey)
        } else {
            ""
        }
        return Arguments.createMap().apply {
            putString("id", msg.id)
            putString("sessionId", msg.sessionId)
            putString("senderId", msg.senderId)
            putString("status", msg.status.name)
            putDouble("timestamp", msg.timestamp.toDouble())
            putBoolean("fromMe", fromMe)
            putString("text", plaintext)
        }
    }

    @ReactMethod
    fun getMyIdentity(promise: Promise) {
        val result = Arguments.createMap().apply {
            putString("id", LOCAL_ID)
            putString("displayName", UserStore.getProfile().userName)
        }
        promise.resolve(result)
    }

    @ReactMethod
    fun listPeers(promise: Promise) {
        try {
            val peersList = UserStore.getAllPeers()
            val array: WritableArray = Arguments.createArray()
            for (peer in peersList) {
                val map: WritableMap = Arguments.createMap().apply {
                    putString("id", peer.id)
                    putString("displayName", peer.userName)
                }
                array.pushMap(map)
            }
            promise.resolve(array)
        } catch (e: Exception) {
            promise.reject("LIST_PEERS_FAILED", e.message)
        }
    }

    @ReactMethod
    fun openSession(peerId: String, promise: Promise) {
        val session = Arguments.createMap().apply {
            putString("sessionId", "session-$peerId")
            putString("status", "open")
        }
        promise.resolve(session)
    }

    @ReactMethod
    fun closeSession(sessionId: String, promise: Promise) {
        promise.resolve(null)
    }

    @ReactMethod
    fun getAppStatus(promise: Promise) {
        val status = Arguments.createMap().apply {
            putBoolean("isScanning", false)
            putInt("peersCount", 0)
        }
        promise.resolve(status)
    }

    @ReactMethod
    fun getMyProfile(promise: Promise) {
        try {
            val profile = UserStore.getProfile()
            val profileMap = Arguments.createMap().apply {
                putString("id", profile.id)
                putString("displayName", profile.userName)
            }
            promise.resolve(profileMap)
        } catch (e: Exception) {
            promise.reject("GET_PROFILE_FAILED", e.message)
        }
    }

    @ReactMethod
    fun updateMyProfile(newDisplayName: String, newAvatarUrl: String?, promise: Promise) {
        try {
            val updated = UserStore.updateProfile(userName = newDisplayName)
            val profile = Arguments.createMap().apply {
                putString("id", updated.id)
                putString("displayName", updated.userName)
            }
            promise.resolve(profile)
        } catch (e: Exception) {
            promise.reject("UPDATE_PROFILE_FAILED", e.message)
        }
    }

    @ReactMethod
    fun setPeerFavorite(peerId: String, isFavorite: Boolean, promise: Promise) {
        UserStore.setFavorite(peerId, isFavorite)
        promise.resolve(null)
    }

    @ReactMethod
    fun setPeerBlocked(peerId: String, isBlocked: Boolean, promise: Promise) {
        UserStore.setBlocked(peerId, isBlocked)
        promise.resolve(null)
    }
}