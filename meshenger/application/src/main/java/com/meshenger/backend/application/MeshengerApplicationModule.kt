package com.meshenger.backend.application

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.util.Log
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.messaging.Message
import com.meshenger.backend.application.messaging.MessageStatus
import com.meshenger.backend.application.messaging.MessagingStore
import com.meshenger.backend.application.security.RemotePeerCryptoStore
import com.meshenger.backend.application.user.UserProfile
import com.meshenger.backend.application.user.UserStore
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.session.GlobalChatSession
import com.meshenger.backend.session.Peer as MeshPeer
import com.meshenger.backend.session.PeerInMeshRegistry
import com.meshenger.backend.transport2.MPAddress
import com.meshenger.backend.transport2.StaticKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
    private val remotePeerCrypto = RemotePeerCryptoStore(dbHelper)
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
        initLocalKeys()

        MessagingStore.onStatusChanged = { messageId: String, status: MessageStatus ->
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
    }

    private fun initLocalKeys() {
        try {
            // Ed25519
            if (remotePeerCrypto.loadRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_ED25519_RAW) == null) {
                val keyPair = StaticKeyManager.getOrCreateIdentityKey()
                val raw = StaticKeyManager.getRawPublicIdentityKey(keyPair.public)
                remotePeerCrypto.saveRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_ED25519_RAW, raw)
            }
            // X25519 - generate and save if missing
            if (remotePeerCrypto.loadRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW) == null) {
                val (pub, _) = StaticKeyManager.generateX25519KeyPair()
                remotePeerCrypto.saveRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW, pub)
            }
        } catch (e: Exception) {
            Log.e("MeshengerApplication", "Failed to init local keys", e)
        }
    }

    private fun observeGlobalChatBus() {
        moduleScope.launch {
            val sessionKey = getGlobalKey()
            GlobalChatSession.getMessageBus().collect { json: JsonObject ->
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
            val sessionKey = getGlobalKey()
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

    private fun getGlobalKey(): ByteArray {
        return getFixedKey(NativeCredentials.getGlobalChatKey())
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

    /**
     * Floods a signed bootstrap packet (session/network): display name + MP address for mesh peers.
     * Call after mesh transport is up; repeat periodically because mesh registry entries expire (~2 min).
     */
    @ReactMethod
    fun sendMeshBootstrap(promise: Promise) {
        try {
            val name = UserStore.getProfile().userName.trim()
            if (name.isBlank()) {
                promise.reject("INVALID_INPUT", "Set a display name before announcing on the mesh")
                return
            }
            GlobalChatSession.sendBootstrap(name)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("MESH_BOOTSTRAP_FAILED", e.message, e)
        }
    }

    /** Peers discovered via bootstrap (in-memory registry), not the manual SQLite peer list. */
    @ReactMethod
    fun listMeshPeers(promise: Promise) {
        try {
            val array = Arguments.createArray()
            for (peer in PeerInMeshRegistry.getAllPeers()) {
                array.pushMap(meshPeerToWritableMap(peer))
            }
            promise.resolve(array)
        } catch (e: Exception) {
            promise.reject("LIST_MESH_PEERS_FAILED", e.message, e)
        }
    }

    /**
     * DeviceScan flow: announce on mesh, then return other devices (excludes this device).
     * UI should call [refreshMeshScanPeers] on an interval (e.g. 1.5–2s) while the scan screen is open.
     */
    @ReactMethod
    fun startMeshDeviceScan(promise: Promise) {
        try {
            val name = UserStore.getProfile().userName.trim()
            if (name.isBlank()) {
                promise.reject("INVALID_INPUT", "Set a display name before scanning")
                return
            }
            GlobalChatSession.sendBootstrap(name)
            val (peers, count) = meshPeersArrayExcludingSelf()
            promise.resolve(
                Arguments.createMap().apply {
                    putArray("peers", peers)
                    putInt("peerCount", count)
                }
            )
        } catch (e: Exception) {
            promise.reject("MESH_DEVICE_SCAN_FAILED", e.message, e)
        }
    }

    /**
     * Same peer list as [startMeshDeviceScan] but does not re-send bootstrap (for polling while scanning).
     */
    @ReactMethod
    fun refreshMeshScanPeers(promise: Promise) {
        try {
            val (peers, count) = meshPeersArrayExcludingSelf()
            promise.resolve(
                Arguments.createMap().apply {
                    putArray("peers", peers)
                    putInt("peerCount", count)
                }
            )
        } catch (e: Exception) {
            promise.reject("MESH_SCAN_REFRESH_FAILED", e.message, e)
        }
    }

    /**
     * Registers the chosen mesh peer in the app DB so chat/navigation can use [peerId].
     * Does not start BLE or [com.meshenger.backend.session.TwoPartySession] — session team wires crypto/handshake later.
     *
     * @param mpAddress decimal string (e.g. from [meshPeerToWritableMap] `mpAddress`) or MP address Base64 from bootstrap.
     */

    @ReactMethod
    fun connectToMeshPeer(mpAddress: String, displayName: String, promise: Promise) {
        try {
            if (displayName.isBlank()) {
                promise.reject("INVALID_INPUT", "displayName cannot be empty")
                return
            }
            val mp = parseMeshPeerMpAddress(mpAddress)
            if (mp == MPAddress.getMyMPAddressULong()) {
                promise.reject("INVALID_INPUT", "Cannot connect to this device (self)")
                return
            }
            val peerId = meshPeerId(mp)
            dbHelper.upsertUserProfile(
                UserProfile(
                    id = peerId,
                    publicKeyHash = "-",
                    userName = displayName.trim()
                )
            )
            val addrBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(mp.toLong()).array()
            promise.resolve(
                Arguments.createMap().apply {
                    putString("peerId", peerId)
                    putString("displayName", displayName.trim())
                    putString("mpAddress", mp.toString())
                    putString("mpAddressBase64", MPAddress.MPAddressByteArrayToString(addrBytes))
                }
            )
        } catch (e: Exception) {
            promise.reject("CONNECT_MESH_PEER_FAILED", e.message, e)
        }
    }

    private fun meshPeerId(mp: ULong): String = "mp:$mp"

    private fun parseMeshPeerMpAddress(raw: String): ULong {
        val t = raw.trim()
        if (t.isEmpty()) throw IllegalArgumentException("mpAddress is empty")
        t.toULongOrNull()?.let { return it }
        val bytes = MPAddress.MPAddressStringToByteArray(t)
        return MPAddress.MPAddressByteArrayToULong(bytes)
    }

    private fun meshPeersArrayExcludingSelf(): Pair<WritableArray, Int> {
        val self = MPAddress.getMyMPAddressULong()
        val array = Arguments.createArray()
        var n = 0
        for (peer in PeerInMeshRegistry.getAllPeers()) {
            if (peer.MPAddress == self) continue
            array.pushMap(meshPeerToWritableMap(peer))
            n++
        }
        return array to n
    }

    private fun meshPeerToWritableMap(peer: MeshPeer): WritableMap {
        val mp = peer.MPAddress
        val addrBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(mp.toLong()).array()
        return Arguments.createMap().apply {
            putString("id", "mp:$mp")
            putString("displayName", peer.userName)
            putString("mpAddress", mp.toString())
            putString("mpAddressBase64", MPAddress.MPAddressByteArrayToString(addrBytes))
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
    fun getMyMPAddress(promise: Promise) {
        promise.resolve(MPAddress.getMyMPAddressString())
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
            putInt("meshPeersCount", PeerInMeshRegistry.getAllPeers().size)
            putInt("peersCount", UserStore.getAllPeers().size)
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

    /**
     * Persists remote raw key material (32-byte Ed25519 or X25519 public encoding as received).
     * Uses software AES master → encrypt → SQLite; master imported under SHA-256 alias in Keystore.
     */
    @ReactMethod
    fun saveRemotePeerRawKey(peerUserId: String, keyType: String, rawKeyMaterialBase64: String, promise: Promise) {
        try {
            if (peerUserId.isBlank() || keyType.isBlank() || rawKeyMaterialBase64.isBlank()) {
                promise.reject("INVALID_INPUT", "peerUserId, keyType and rawKeyMaterialBase64 are required")
                return
            }
            val normalizedType = keyType.trim()
            if (!RemotePeerCryptoStore.allowedKeyTypes().contains(normalizedType)) {
                promise.reject(
                    "INVALID_KEY_TYPE",
                    "keyType must be ${RemotePeerCryptoStore.KEY_TYPE_ED25519_RAW} or ${RemotePeerCryptoStore.KEY_TYPE_X25519_RAW}",
                )
                return
            }
            val raw = android.util.Base64.decode(rawKeyMaterialBase64.trim(), android.util.Base64.NO_WRAP)
            remotePeerCrypto.saveRemoteRawKey(peerUserId.trim(), normalizedType, raw)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("SAVE_REMOTE_KEY_FAILED", e.message, e)
        }
    }

    /** Returns Base64(raw bytes) or a map of {type: Base64} if keyType is "ALL". */
    @ReactMethod
    fun loadRemotePeerRawKey(peerUserId: String, keyType: String, promise: Promise) {
        try {
            if (keyType == "ALL") {
                val map = Arguments.createMap()
                for (type in RemotePeerCryptoStore.allowedKeyTypes()) {
                    val bytes = remotePeerCrypto.loadRemoteRawKey(peerUserId.trim(), type)
                    if (bytes != null) {
                        map.putString(type, android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                    }
                }
                promise.resolve(map)
                return
            }
            val bytes = remotePeerCrypto.loadRemoteRawKey(peerUserId.trim(), keyType.trim())
                ?: run {
                    promise.reject("NOT_FOUND", "No stored key for this peer and keyType")
                    return
                }
            promise.resolve(android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
        } catch (e: Exception) {
            promise.reject("LOAD_REMOTE_KEY_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun deleteRemotePeerRawKey(peerUserId: String, keyType: String, promise: Promise) {
        try {
            remotePeerCrypto.deleteRemoteRawKey(peerUserId.trim(), keyType.trim())
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("DELETE_REMOTE_KEY_FAILED", e.message, e)
        }
    }
}