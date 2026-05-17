package com.meshenger.backend.application

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.util.Base64
import android.util.Log
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.messaging.Message
import com.meshenger.backend.application.messaging.MessageStatus
import com.meshenger.backend.application.messaging.MessagingStore
import com.meshenger.backend.application.security.RemotePeerCryptoStore
import com.meshenger.backend.application.user.PeerSecurity
import com.meshenger.backend.application.user.UserProfile
import com.meshenger.backend.application.user.UserStore
import com.meshenger.backend.application.notification.NotificationHelper
import com.meshenger.backend.network.DirectChatNegotiationListener
import com.meshenger.backend.network.EpidemicFlooding
import com.meshenger.backend.network.ListenerRegistry
import com.meshenger.backend.network.MessageType
import com.meshenger.backend.network.TwoPartyHandshakeFallback
import com.meshenger.backend.security_native.NativeCredentials
import com.meshenger.backend.session.GlobalChatSession
import com.meshenger.backend.session.NoisePattern
import com.meshenger.backend.session.Peer as MeshPeer
import com.meshenger.backend.session.PeerInMeshRegistry
import com.meshenger.backend.session.TwoPartySession
import com.meshenger.backend.transport2.MPAddress
import com.meshenger.backend.transport2.MeshConnectionRegistry
import com.meshenger.backend.transport2.StaticKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
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

    /** peerId ("mp:<dec>") -> active TwoPartySession */
    private val activeSessions = ConcurrentHashMap<String, TwoPartySession>()
    /** peerId -> coroutine collecting TwoPartySession._messageBus */
    private val sessionBusJobs = ConcurrentHashMap<String, Job>()
    /**
     * peerId -> sessionId of the *currently active* TwoPartySession. Each new handshake gets a
     * fresh random UUID (no embedded peer id) so `messages.sessionId` distinguishes successive
     * Noise sessions with the same peer. The legacy [MeshengerDbHelper.directSessionId] row stays
     * as a fallback when inserting a new session row fails; older code paths can still use it.
     */
    private val currentSessionIdByPeer = ConcurrentHashMap<String, String>()
    /**
     * Lazily-loaded persistent X25519 static keypair for this device, used for Noise
     * handshakes with all peers. Persisted via [RemotePeerCryptoStore] so we keep the same
     * static key across app restarts; this is what lets a KK reconnect work after the
     * in-memory [TwoPartySession] from the initial XX bootstrap has been killed.
     */
    @Volatile
    private var localStaticKeypair: Pair<ByteArray, ByteArray>? = null
    private val localStaticLock = Any()

    /** Outgoing 1:1 invites we sent (mp:…); cleared on accept/reject from peer. */
    private val outgoingDirectChatInvites = ConcurrentHashMap<String, Long>()

    /** Invites awaiting user action on this device (peerId mp:…). */
    private data class PendingIncomingInvite(
        val peerId: String,
        val displayName: String,
        val avatarId: String?,
        val timestamp: Long,
    )

    private val pendingIncomingInviteByPeer = ConcurrentHashMap<String, PendingIncomingInvite>()

    private companion object {
        const val LOCAL_ID = "local-device"
        const val GLOBAL_CHAT_ID = "global-chat"
        const val GLOBAL_SESSION_ID = "global-session"
        const val GLOBAL_BROADCAST_ID = "global-broadcast"
        private const val GLOBAL_AEAD = "AES/GCM/NoPadding"
        private const val GLOBAL_TAG_LENGTH = 128
        private val TWO_PARTY_PROLOGUE = "meshenger-twoparty-v1".encodeToByteArray()
        private const val MP_PREFIX = "mp:"
        private const val PRESENCE_INTERVAL_MS = 25_000L
        /** Dùng filter Logcat: `MeshengerChat` — theo dõi gửi tin 1–1 và emit JS */
        private const val CHAT_DIAG = "MeshengerChat"
        /**
         * Filter Logcat: `MeshengerOTO` — toàn bộ luồng 1-1 (one-to-one):
         * device scan / bootstrap → invite → Noise handshake (XX lần đầu, KK lần 2+)
         * → gửi/nhận message qua tunnel.
         *
         * adb logcat -s MeshengerOTO:*
         */
        const val OTO_TAG = "MeshengerOTO"
        /**
         * Owner key under which this device's persistent X25519 static keypair is stored in
         * `peer_remote_keys`. Distinct from [LOCAL_ID] so it never collides with a real peer row.
         */
        private const val LOCAL_STATIC_OWNER = "@local-static-x25519"
    }

    init {
        MessagingStore.init(dbHelper)
        UserStore.init(dbHelper)
        ensureGlobalChatStorage()
        observeGlobalChatBus()
        registerHandshakeFallback()
        registerDirectChatNegotiationListener()
        registerMeshPeerAnnouncedHook()
        startPresenceLoop()

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
        val reactCtx = reactApplicationContext
        val active = reactCtx.hasActiveReactInstance()
        val msgIdForLog =
            params?.let { map ->
                try {
                    map.getString("id")
                } catch (_: Throwable) {
                    null
                }
            }

        Log.d(
            CHAT_DIAG,
            "sendEvent queued: $eventName id=$msgIdForLog hasActiveReactInstance=$active callerThread=${Thread.currentThread().name}",
        )

        reactCtx.runOnUiQueueThread {
            try {
                if (!reactCtx.hasActiveReactInstance()) {
                    Log.w(CHAT_DIAG, "sendEvent SKIP (no React): $eventName id=$msgIdForLog")
                    return@runOnUiQueueThread
                }
                reactCtx
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit(eventName, params)
                Log.d(CHAT_DIAG, "sendEvent OK: $eventName id=$msgIdForLog thread=ui-queue")
            } catch (e: Exception) {
                Log.w("MeshengerApplication", "sendEvent($eventName) skipped: ${e.message}")
                Log.e(CHAT_DIAG, "sendEvent EXCEPTION: $eventName", e)
            }
        }
    }

    private fun ensureGlobalChatStorage() {
        val globalKeyId = buildGlobalKeyId()
        dbHelper.upsertUserProfile(
            UserProfile(
                GLOBAL_BROADCAST_ID,
                "-",
                "Global Chat",
                security = PeerSecurity.WEAK,
            ),
        )
        dbHelper.ensureGlobalChat(GLOBAL_CHAT_ID, GLOBAL_SESSION_ID, globalKeyId)
    }

//    private fun initLocalKeys() {
//        try {
//            // Ed25519
//            if (remotePeerCrypto.loadRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_ED25519_RAW) == null) {
//                val keyPair = StaticKeyManager.getOrCreateIdentityKey()
//                val raw = StaticKeyManager.getRawPublicIdentityKey(keyPair.public)
//                remotePeerCrypto.saveRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_ED25519_RAW, raw)
//            }
//            // X25519 - generate and save if missing
//            if (remotePeerCrypto.loadRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW) == null) {
//                val (pub, _) = StaticKeyManager.generateX25519KeyPair()
//                remotePeerCrypto.saveRemoteRawKey(LOCAL_ID, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW, pub)
//            }
//        } catch (e: Exception) {
//            Log.e("MeshengerApplication", "Failed to init local keys", e)
//        }
//    }

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
                    val existing = dbHelper.getUserProfile(senderId)
                    dbHelper.upsertUserProfile(
                        UserProfile(
                            id = senderId,
                            publicKeyHash = "-",
                            userName = existing?.userName ?: senderId,
                            userAvtId = existing?.userAvtId,
                            security = existing?.security ?: PeerSecurity.MEDIUM,
                        )
                    )
                }

                val msg = Message(
                    id = UUID.randomUUID().toString(),
                    sessionId = GLOBAL_SESSION_ID,
                    senderId = senderId,
                    nonce = nonce,
                    status = if (action == "Send") MessageStatus.PENDING else MessageStatus.SENT,
                    encryptedPayload = payload,
                    bodyText = plaintext.takeIf { it.isNotEmpty() },
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
                if (action != "Send") {
                    val rawName = dbHelper.getUserProfile(senderId)?.userName ?: senderId
                    val senderName = if (rawName.startsWith("mp:")) "Ai đó" else rawName
                    NotificationHelper.showNewMessageNotification(
                        reactApplicationContext,
                        "Global Chat: $senderName",
                        plaintext.takeIf { it.isNotEmpty() } ?: "Tin nhắn mới"
                    )
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
            val existingPeer = dbHelper.getUserProfile(id)
            val security = when (existingPeer?.security) {
                PeerSecurity.STRONG -> PeerSecurity.STRONG
                else -> PeerSecurity.MEDIUM
            }
            val peer = UserProfile(
                id = id,
                publicKeyHash = "-",
                userName = displayName,
                userAvtId = avatarUrl,
                security = security,
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

    private fun sanitizeDisplayName(raw: String?): String {
        val name = raw?.trim().orEmpty()
        if (name.isEmpty()) return ""
        return if (name.startsWith("mp:")) "" else name
    }

    private fun messageToWritableMap(
        msg: Message,
        fromMe: Boolean,
        sessionKey: ByteArray? = null,
        plaintextOverride: String? = null
    ): WritableMap {
        val senderName = when (msg.senderId) {
            LOCAL_ID -> sanitizeDisplayName(UserStore.getProfile().userName)
            else -> sanitizeDisplayName(dbHelper.getUserProfile(msg.senderId)?.userName)
        }
        val senderAvatarId = when (msg.senderId) {
            LOCAL_ID -> UserStore.getProfile().userAvtId
            else -> dbHelper.getUserProfile(msg.senderId)?.userAvtId
        }
        val plaintext = plaintextOverride
            ?: msg.bodyText?.takeIf { it.isNotBlank() }
            ?: if (sessionKey != null) {
                decryptGlobalPayload(msg.encryptedPayload, msg.nonce, sessionKey)
            } else {
                ""
            }
        return Arguments.createMap().apply {
            putString("id", msg.id)
            putString("sessionId", msg.sessionId)
            putString("senderId", msg.senderId)
            putString("senderName", senderName)
            putString("senderAvatarId", senderAvatarId)
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
            val avatarId = UserStore.getProfile().userAvtId
            GlobalChatSession.sendBootstrap(name, avatarId)
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
            val avatarId = UserStore.getProfile().userAvtId
            GlobalChatSession.sendBootstrap(name, avatarId)
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
     * @param mpAddress decimal string (e.g. from [meshPeerToWritableMap] mpAddress) or MP address Base64 from bootstrap.
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
            val existingPeer = dbHelper.getUserProfile(peerId)
            val security = when (existingPeer?.security) {
                PeerSecurity.STRONG -> PeerSecurity.STRONG
                else -> PeerSecurity.MEDIUM
            }
            dbHelper.upsertUserProfile(
                UserProfile(
                    id = peerId,
                    publicKeyHash = "-",
                    userName = displayName.trim(),
                    security = security,
                )
            )
            dbHelper.ensureDirectChatForPeer(peerId, displayName.trim())
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
        val seen = HashSet<ULong>()
        var n = 0
        for (peer in PeerInMeshRegistry.getAllPeers()) {
            if (peer.MPAddress == self) continue
            if (!seen.add(peer.MPAddress)) continue
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
            peer.avatarId?.let { putString("avatarId", it) }
        }
    }

    // -----------------------------------------------------------------
    // 1:1 (TwoPartySession): Noise XX (mesh), XK (QR first pairing), KK (reconnect).
    // -----------------------------------------------------------------

    /**
     * Opens (or returns the existing) Noise XX two-party session with the peer.
     * Mesh note: Noise XX supports exactly **one** initiator. [isInitiator] from JS is
     * intentionally ignored — the role is chosen deterministically via MP address
     * comparison so both devices agree regardless of tap order (`myMp < peerMp` ⇒ initiator).
     * The handshake fallback keeps working for the responder when the peer opens first.
     */
    @ReactMethod
    fun openTwoPartySession(
        peerId: String,
        displayName: String,
        @Suppress("UNUSED_PARAMETER") isInitiator: Boolean,
        promise: Promise,
    ) {
        try {
            val mp = parseMeshPeerId(peerId)
            val myMp = MPAddress.getMyMPAddressULong()
            // Noise role must be identical on both peers. For KK we used to force
            // `effectiveInitiator = true` whenever the remote static was persisted; that made
            // *both phones initiator* whenever they both opened the chat (each run
            // openTwoPartySession locally). Two KK first messages then collide and "lần 2"
            // chats fail. Use the same MP tie-break as XX: lower mesh id is initiator.
            //
            // If only the larger-MP device restarts while the peer still holds an old
            // transport session, that peer may need to re-enter the chat once so it can
            // receive KK and hit the post-handshake recreate path; the smaller-MP side is
            // the one that sends KK msg1 after a full reconnect.
            val hasStoredKey = loadStoredRemoteStatic(peerId) != null
            val qrPaired = dbHelper.isDirectChatPairedViaQr(peerId)
            val effectiveInitiator = effectiveTwoPartyInitiator(peerId, mp)
            Log.d(
                OTO_TAG,
                "openTwoPartySession peer=$peerId requested displayName='$displayName' " +
                    "myMp=$myMp peerMp=$mp hasStoredKey=$hasStoredKey qrPaired=$qrPaired " +
                    "effectiveInitiator=$effectiveInitiator " +
                    "alreadyOpen=${activeSessions.containsKey(peerId)}",
            )

            val existing = activeSessions[peerId]
            if (existing != null) {
                Log.d(OTO_TAG, "openTwoPartySession peer=$peerId already has active session, returning")
                promise.resolve(twoPartySessionInfo(peerId, isInitiator = effectiveInitiator, justOpened = false))
                return
            }
            if (mp == myMp) {
                Log.w(OTO_TAG, "openTwoPartySession peer=$peerId is self, rejecting")
                promise.reject("INVALID_INPUT", "Cannot open a session with this device (self)")
                return
            }
            val name = displayName.trim().ifBlank { peerId }
            ensurePeerRow(peerId, name)
            createAndTrackSession(peerId, mp, name, effectiveInitiator)
            promise.resolve(twoPartySessionInfo(peerId, isInitiator = effectiveInitiator, justOpened = true))
        } catch (e: Exception) {
            Log.e(OTO_TAG, "openTwoPartySession peer=$peerId FAILED: ${e.message}", e)
            promise.reject("OPEN_SESSION_FAILED", e.message, e)
        }
    }

    /**
     * Persists the peer's **long-term Noise X25519 static public** from a QR scan and prepares
     * [openTwoPartySessionWithBootstrap] with `bootstrap = "qr_scanner"` (Noise **XK**: scanner
     * is initiator and knows responder static from QR).
     */
    @ReactMethod
    fun savePeerNoisePublicFromQr(
        peerId: String,
        displayName: String,
        noisePublicKeyBase64: String,
        promise: Promise,
    ) {
        try {
            val trimmedPeer = peerId.trim()
            require(trimmedPeer.startsWith(MP_PREFIX)) {
                "peerId must be '${MP_PREFIX}<decimal>'"
            }
            val raw = try {
                Base64.decode(noisePublicKeyBase64.trim(), Base64.DEFAULT)
            } catch (e: Exception) {
                promise.reject("INVALID_INPUT", "Invalid Base64: ${e.message}")
                return
            }
            if (raw.size != 32) {
                promise.reject("INVALID_INPUT", "Noise static public must be 32 bytes, got ${raw.size}")
                return
            }
            remotePeerCrypto.saveRemoteRawKey(
                trimmedPeer,
                RemotePeerCryptoStore.KEY_TYPE_X25519_QR_IMPORT,
                raw,
            )
            val name = displayName.trim().ifBlank { trimmedPeer }
            ensurePeerRow(trimmedPeer, name)
            dbHelper.setDirectChatPairedViaQr(trimmedPeer, true)
            Log.i(OTO_TAG, "QR import peer=$trimmedPeer noisePub saved (32 bytes)")
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(OTO_TAG, "savePeerNoisePublicFromQr FAILED: ${e.message}", e)
            promise.reject("QR_SAVE_FAILED", e.message, e)
        }
    }

    /**
     * Identity payload for "My QR" using the same Noise static as [TwoPartySession] / KK reconnect
     * ([LOCAL_STATIC_OWNER]), not the legacy `local-device` profile key row.
     */
    @ReactMethod
    fun getIdentityForQr(promise: Promise) {
        try {
            val mp = MPAddress.getMyMPAddressULong()
            val (pub, _) = getOrCreateLocalStaticKeypair()
            val profile = UserStore.getProfile()
            promise.resolve(
                Arguments.createMap().apply {
                    putString("peerId", meshPeerId(mp))
                    putString("mpAddress", mp.toString())
                    putString(
                        "username",
                        profile.userName.trim().ifBlank { meshPeerId(mp) },
                    )
                    profile.userAvtId?.let { putString("avatarId", it) }
                    putString("noisePublicKeyBase64", Base64.encodeToString(pub, Base64.NO_WRAP))
                },
            )
        } catch (e: Exception) {
            Log.e(OTO_TAG, "getIdentityForQr FAILED: ${e.message}", e)
            promise.reject("QR_IDENTITY_FAILED", e.message, e)
        }
    }

    /**
     * Opens a 1:1 session with explicit bootstrap semantics:
     * - **mesh** — same as [openTwoPartySession] (XX or KK from DB; initiator = `myMp < peerMp`).
     * - **qr_scanner** — device scanned the peer's QR: must have called [savePeerNoisePublicFromQr];
     *   initiator = true, Noise **XK** using imported static.
     * - **qr_display** — device that **showed** the QR: initiator = false, Noise **XK** responder
     *   (wait for scanner's first handshake message). Use when opening chat on A before/while B connects.
     */
    @ReactMethod
    fun openTwoPartySessionWithBootstrap(
        peerId: String,
        displayName: String,
        bootstrap: String,
        promise: Promise,
    ) {
        try {
            val mode = bootstrap.trim().lowercase()
            require(mode in setOf("mesh", "qr_scanner", "qr_display")) {
                "bootstrap must be mesh | qr_scanner | qr_display"
            }
            val mp = parseMeshPeerId(peerId)
            val myMp = MPAddress.getMyMPAddressULong()
            if (mp == myMp) {
                promise.reject("INVALID_INPUT", "Cannot open a session with self")
                return
            }
            val name = displayName.trim().ifBlank { peerId }

            val (effectiveInitiator, forcedPat) = when (mode) {
                "mesh" -> effectiveTwoPartyInitiator(peerId, mp) to null
                "qr_scanner" -> {
                    if (loadQrImportedRemoteStatic(peerId) == null) {
                        promise.reject(
                            "QR_IMPORT_MISSING",
                            "Call savePeerNoisePublicFromQr before open (qr_scanner)",
                        )
                        return
                    }
                    true to null
                }
                "qr_display" -> false to NoisePattern.XK
                else -> error("unreachable")
            }

            Log.d(
                OTO_TAG,
                "openTwoPartySessionWithBootstrap peer=$peerId mode=$mode " +
                    "effectiveInitiator=$effectiveInitiator forcedPattern=$forcedPat",
            )

            if (activeSessions.containsKey(peerId)) {
                promise.resolve(
                    twoPartySessionInfo(peerId, isInitiator = effectiveInitiator, justOpened = false),
                )
                return
            }
            ensurePeerRow(peerId, name)
            createAndTrackSession(peerId, mp, name, effectiveInitiator, forcedPat)
            promise.resolve(twoPartySessionInfo(peerId, isInitiator = effectiveInitiator, justOpened = true))
        } catch (e: Exception) {
            Log.e(OTO_TAG, "openTwoPartySessionWithBootstrap FAILED: ${e.message}", e)
            promise.reject("OPEN_SESSION_FAILED", e.message, e)
        }
    }

    /**
     * Ask a mesh peer for a 1:1 chat. Receiver must accept on Pending before Noise handshake starts.
     */
    @ReactMethod
    fun sendDirectChatInvite(peerId: String, promise: Promise) {
        try {
            if (peerId.isBlank()) {
                promise.reject("INVALID_INPUT", "peerId cannot be empty")
                return
            }
            val receiverMp = parseMeshPeerId(peerId)
            if (receiverMp == MPAddress.getMyMPAddressULong()) {
                promise.reject("INVALID_INPUT", "Cannot invite self")
                return
            }
            val json = JSONObject().apply {
                put("name", UserStore.getProfile().userName.trim())
                UserStore.getProfile().userAvtId?.let { put("avatarId", it) }
            }
            val payload = json.toString().toByteArray(StandardCharsets.UTF_8)
            Log.d(
                OTO_TAG,
                "INVITE send peer=$peerId mp=$receiverMp myName='${UserStore.getProfile().userName.trim()}'",
            )
            EpidemicFlooding.onDirectChatNegotiationSend(
                MessageType.DIRECT_CHAT_INVITE,
                receiverMp,
                payload,
            )
            outgoingDirectChatInvites[peerId] = System.currentTimeMillis()
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(OTO_TAG, "INVITE send peer=$peerId FAILED: ${e.message}", e)
            promise.reject("INVITE_SEND_FAILED", e.message, e)
        }
    }

    /**
     * @param inviterDisplayName display name from invite payload (recommended when accept==true).
     */
    @ReactMethod
    fun respondDirectChatInvite(
        fromPeerId: String,
        accept: Boolean,
        inviterDisplayName: String?,
        promise: Promise,
    ) {
        try {
            if (fromPeerId.isBlank()) {
                promise.reject("INVALID_INPUT", "fromPeerId cannot be empty")
                return
            }
            val otherMp = parseMeshPeerId(fromPeerId)
            if (otherMp == MPAddress.getMyMPAddressULong()) {
                promise.reject("INVALID_INPUT", "Invalid peer")
                return
            }
            // Grab avatar before removing from pending map
            val pendingInvite = pendingIncomingInviteByPeer.remove(fromPeerId.trim())
            val emptyAck = "{}".toByteArray(StandardCharsets.UTF_8)
            Log.d(
                OTO_TAG,
                "INVITE respond peer=$fromPeerId accept=$accept inviterName='${inviterDisplayName ?: ""}'",
            )
            if (accept) {
                EpidemicFlooding.onDirectChatNegotiationSend(
                    MessageType.DIRECT_CHAT_INVITE_ACCEPT,
                    otherMp,
                    emptyAck,
                )
                val name = inviterDisplayName?.trim()?.takeIf { it.isNotEmpty() }
                    ?: resolveMeshPeerDisplayName(otherMp)
                val avatarId = pendingInvite?.avatarId
                ensurePeerRow(fromPeerId, name, avatarId)
                openOrEnsureTwoPartySession(fromPeerId, otherMp, name)
            } else {
                EpidemicFlooding.onDirectChatNegotiationSend(
                    MessageType.DIRECT_CHAT_INVITE_REJECT,
                    otherMp,
                    emptyAck,
                )
            }
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(OTO_TAG, "INVITE respond peer=$fromPeerId FAILED: ${e.message}", e)
            promise.reject("INVITE_RESPOND_FAILED", e.message, e)
        }
    }

    /** Snapshot of invitations not yet accepted/rejected (for Pending screen refresh). */
    @ReactMethod
    fun getPendingIncomingInvites(promise: Promise) {
        try {
            val arr = Arguments.createArray()
            for (inv in pendingIncomingInviteByPeer.values.sortedBy { it.timestamp }) {
                arr.pushMap(
                    Arguments.createMap().apply {
                        putString("peerId", inv.peerId)
                        putString("displayName", inv.displayName)
                        inv.avatarId?.let { putString("avatarId", it) }
                        putDouble("timestamp", inv.timestamp.toDouble())
                    },
                )
            }
            promise.resolve(arr)
        } catch (e: Exception) {
            promise.reject("PENDING_INVITES_FAILED", e.message, e)
        }
    }

    /**
     * Encrypts [plaintext] with the active two-party session's Noise transport keys,
     * floods it via EpidemicFlooding, and persists ciphertext to the local DB.
     * If the session is still handshaking, the message is queued by the session.
     */
    @ReactMethod
    fun sendDirectMessage(peerId: String, plaintext: String, promise: Promise) {
        try {
            if (peerId.isBlank() || plaintext.isBlank()) {
                promise.reject("INVALID_INPUT", "peerId and plaintext cannot be empty")
                return
            }
            Log.i(
                CHAT_DIAG,
                "sendDirectMessage START peer=$peerId len=${plaintext.length} hasSession=${activeSessions.containsKey(peerId)}",
            )
            val session = activeSessions[peerId]
                ?: run {
                    Log.w(OTO_TAG, "MSG send peer=$peerId NO_SESSION (chưa mở two-party session)")
                    promise.reject("NO_SESSION", "Open a two-party session before sending")
                    return
                }
            val mp = parseMeshPeerId(peerId)
            Log.d(
                OTO_TAG,
                "MSG send peer=$peerId len=${plaintext.length} pattern=${session.chosenPattern} " +
                    "handshakeFinished=${session.isHandshakeFinished} sessionId=${currentSessionIdByPeer[peerId]}",
            )
            session.sendMessageStr(mp, plaintext)
            Log.i(CHAT_DIAG, "sendDirectMessage DONE peer=$peerId plaintextLen=${plaintext.length}")
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(OTO_TAG, "MSG send peer=$peerId FAILED: ${e.message}", e)
            promise.reject("SEND_DIRECT_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun closeTwoPartySession(peerId: String, promise: Promise) {
        try {
            val hadSession = activeSessions.containsKey(peerId)
            sessionBusJobs.remove(peerId)?.cancel()
            activeSessions.remove(peerId)?.close()
            // Forget the per-session id so a follow-up open allocates a fresh row.
            currentSessionIdByPeer.remove(peerId)
            Log.d(OTO_TAG, "CLOSE peer=$peerId hadSession=$hadSession")
            promise.resolve(null)
        } catch (e: Exception) {
            Log.e(OTO_TAG, "CLOSE peer=$peerId FAILED: ${e.message}", e)
            promise.reject("CLOSE_SESSION_FAILED", e.message, e)
        }
    }

    @ReactMethod
    fun isTwoPartySessionOpen(peerId: String, promise: Promise) {
        promise.resolve(activeSessions.containsKey(peerId))
    }

    private fun parseMeshPeerId(peerId: String): ULong {
        val trimmed = peerId.trim()
        require(trimmed.startsWith(MP_PREFIX)) {
            "peerId must be in '$MP_PREFIX<dec>' form for mesh sessions"
        }
        return trimmed.removePrefix(MP_PREFIX).toULong()
    }

    private fun ensurePeerRow(peerId: String, displayName: String, avatarId: String? = null) {
        val existing = dbHelper.getUserProfile(peerId)
        val finalAvatar = avatarId ?: existing?.userAvtId
        val security = when (existing?.security) {
            PeerSecurity.STRONG -> PeerSecurity.STRONG
            else -> PeerSecurity.MEDIUM
        }
        dbHelper.upsertUserProfile(UserProfile(peerId, "-", displayName, finalAvatar, security))
        dbHelper.ensureDirectChatForPeer(peerId, displayName, finalAvatar)
    }

    private data class InvitePayload(val displayName: String, val avatarId: String?)

    private fun parseInvitePayload(payload: ByteArray): InvitePayload {
        return try {
            val o = JSONObject(String(payload, Charsets.UTF_8))
            val name = o.optString("name", "").trim()
            val avt = o.optString("avatarId", "").trim().takeIf { it.isNotEmpty() }
            InvitePayload(name, avt)
        } catch (_: Exception) {
            InvitePayload("", null)
        }
    }

    private fun resolveMeshPeerDisplayName(remoteMp: ULong): String {
        val peerIdStr = meshPeerId(remoteMp)
        val prof = dbHelper.getUserProfile(peerIdStr)
        val u = prof?.userName?.trim()
        if (!u.isNullOrEmpty() && u != peerIdStr) return u
        val mesh = PeerInMeshRegistry.getAllPeers()
            .firstOrNull { it.MPAddress == remoteMp }?.userName?.trim()
        return mesh?.takeIf { it.isNotEmpty() } ?: peerIdStr
    }

    private fun openOrEnsureTwoPartySession(peerIdStr: String, remoteMp: ULong, displayName: String) {
        if (activeSessions.containsKey(peerIdStr)) return
        val effectiveInitiator = effectiveTwoPartyInitiator(peerIdStr, remoteMp)
        createAndTrackSession(peerIdStr, remoteMp, displayName, effectiveInitiator)
    }

    /**
     * KK reconnect initiator: mesh-only pairs use `myMp < peerMp` so both sides never stick as
     * dual initiator when opening together. QR-bonded direct chats use **opener = initiator**
     * (`true` here for every local open) so a restarted phone can drive KK even when it has the
     * larger mesh id — the peer with a stale tunnel will hit post-handshake recreate; simultaneous
     * open on both QR phones is handled by the existing Noise simultaneous-initiate path.
     */
    private fun effectiveTwoPartyInitiator(peerId: String, remoteMp: ULong): Boolean {
        val myMp = MPAddress.getMyMPAddressULong()
        val hasKk = loadStoredRemoteStatic(peerId) != null
        if (!hasKk) return myMp < remoteMp
        return if (dbHelper.isDirectChatPairedViaQr(peerId)) true else myMp < remoteMp
    }

    /**
     * @param forcedPattern When non-null, skips the auto-detection (KK iff peer's static is
     * already persisted) and uses this pattern instead. The responder fallback uses this to
     * mirror whatever pattern the inbound first handshake packet announced.
     */
    private fun createAndTrackSession(
        peerId: String,
        mpAddress: ULong,
        displayName: String,
        isInitiator: Boolean,
        forcedPattern: NoisePattern? = null,
    ): TwoPartySession {
        // If anything forgot to remove the previous session, a new TwoPartySession ctor would
        // overwrite ListenerRegistry without unregistering the old instance — routing would
        // point at the new listener, but the leak / ordering bugs are avoided by closing first.
        sessionBusJobs.remove(peerId)?.cancel()
        activeSessions.remove(peerId)?.close()
        currentSessionIdByPeer.remove(peerId)

        // Persistent X25519 static keypair shared across all sessions / app restarts. This is
        // what makes Noise KK work after the in-memory session has been killed: both sides keep
        // the same long-term static key, so once they have learnt each other's static during the
        // initial XX bootstrap, they can KK directly on subsequent reconnects.
        val (pub, priv) = getOrCreateLocalStaticKeypair()

        val handshakeStatic = loadStoredRemoteStatic(peerId)
        val qrImportedStatic = loadQrImportedRemoteStatic(peerId)
        val pattern = forcedPattern ?: when {
            handshakeStatic != null -> NoisePattern.KK
            qrImportedStatic != null && isInitiator -> NoisePattern.XK
            else -> NoisePattern.XX
        }
        var effectivePattern = pattern
        if (pattern == NoisePattern.XK && isInitiator && qrImportedStatic == null) {
            Log.w(
                OTO_TAG,
                "SESSION downgrade peer=$peerId XK initiator without QR-import static -> XX",
            )
            effectivePattern = NoisePattern.XX
        }
        if (pattern == NoisePattern.KK && handshakeStatic == null) {
            Log.w(
                OTO_TAG,
                "SESSION downgrade peer=$peerId KK without handshake-learned static -> XX",
            )
            effectivePattern = NoisePattern.XX
        }

        val remoteStaticForCtor = when (effectivePattern) {
            NoisePattern.XX -> null
            NoisePattern.KK -> handshakeStatic
            NoisePattern.XK -> if (isInitiator) qrImportedStatic else null
        }

        Log.d(
            OTO_TAG,
            "SESSION decide peer=$peerId initiator=$isInitiator forcedPattern=$forcedPattern " +
                "hasHandshakeStatic=${handshakeStatic != null} hasQrImport=${qrImportedStatic != null} " +
                "-> pattern=$effectivePattern",
        )

        val session = TwoPartySession(
            isInitiator = isInitiator,
            prologue = TWO_PARTY_PROLOGUE,
            staticKey = pub to priv,
            peerId = mpAddress,
            userName = displayName,
            receiverPublicKey = if (effectivePattern == NoisePattern.XX) null else remoteStaticForCtor,
            chosenPattern = effectivePattern,
            // Defer the initiator's first handshake msg until the mesh transport is actually
            // ready (see deferHandshakeUntilMeshReady below). Right after an app restart BLE
            // hasn't reconnected to any peer yet, so sending immediately would silently drop
            // the packet to an empty outbound map.
            autoStartHandshake = false,
        )

        // Stale-session recovery: when our existing handshake state can't process an inbound
        // handshake packet because the peer picked a different Noise pattern (typically because
        // the peer restarted and reopened the chat with KK while we still hold an XX session
        // in memory), tear this session down, create a fresh one as responder with the
        // requested pattern, and feed the original packet into it so the handshake can run.
        session.onPatternMismatch = { senderId, message, requestedPattern ->
            try {
                Log.w(
                    OTO_TAG,
                    "Recreating session for peer=$peerId: wasPattern=$effectivePattern incomingPattern=$requestedPattern",
                )
                sessionBusJobs.remove(peerId)?.cancel()
                activeSessions.remove(peerId)?.close()
                currentSessionIdByPeer.remove(peerId)
                ensurePeerRow(peerId, displayName)
                val newSession = createAndTrackSession(
                    peerId = peerId,
                    mpAddress = mpAddress,
                    displayName = displayName,
                    isInitiator = false,
                    forcedPattern = requestedPattern,
                )
                newSession.onReceiveMessageHandShake(senderId, message)
            } catch (e: Exception) {
                Log.e(OTO_TAG, "Pattern-mismatch recreate failed peer=$peerId: ${e.message}", e)
            }
        }

        // Simultaneous-initiate conflict (both sides opened chat at the same time, both sent
        // msg 0 of the same Noise pattern). Resolve via MP tie-break — the larger MP backs
        // down to responder so the smaller MP keeps driving the handshake.
        session.onSimultaneousInitiate = { senderId, message, pattern ->
            try {
                val myMp = MPAddress.getMyMPAddressULong()
                if (myMp > senderId) {
                    Log.w(
                        OTO_TAG,
                        "Simultaneous initiate detected peer=$peerId — backing down to responder " +
                            "(myMp=$myMp > peerMp=$senderId, pattern=$pattern)",
                    )
                    sessionBusJobs.remove(peerId)?.cancel()
                    activeSessions.remove(peerId)?.close()
                    currentSessionIdByPeer.remove(peerId)
                    ensurePeerRow(peerId, displayName)
                    val newSession = createAndTrackSession(
                        peerId = peerId,
                        mpAddress = mpAddress,
                        displayName = displayName,
                        isInitiator = false,
                        forcedPattern = pattern,
                    )
                    newSession.onReceiveMessageHandShake(senderId, message)
                } else {
                    Log.d(
                        OTO_TAG,
                        "Simultaneous initiate detected peer=$peerId — keeping initiator role " +
                            "(myMp=$myMp <= peerMp=$senderId), waiting for peer to back down",
                    )
                }
            } catch (e: Exception) {
                Log.e(OTO_TAG, "Simultaneous-initiate handler failed peer=$peerId: ${e.message}", e)
            }
        }

        // After Noise completes we know the peer's static (XX/XK learn it in-band; KK already
        // had it). Persist it so the next app start can jump straight to KK.
        session.onHandshakeCompleted = { remoteStatic ->
            Log.d(
                OTO_TAG,
                "HS complete peer=$peerId pattern=$effectivePattern role=${if (isInitiator) "initiator" else "responder"} " +
                    "remoteStaticBytes=${remoteStatic?.size ?: 0}",
            )
            if (remoteStatic != null) {
                try {
                    remotePeerCrypto.saveRemoteRawKey(
                        peerUserId = peerId,
                        keyType = RemotePeerCryptoStore.KEY_TYPE_X25519_RAW,
                        rawKeyMaterial = remoteStatic,
                    )
                    try {
                        remotePeerCrypto.deleteRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_QR_IMPORT)
                    } catch (e: Exception) {
                        Log.w(OTO_TAG, "KEY delete QR import peer=$peerId: ${e.message}")
                    }
                    Log.d(OTO_TAG, "KEY persist remoteStatic peer=$peerId OK")
                } catch (e: Exception) {
                    Log.w(OTO_TAG, "KEY persist remoteStatic peer=$peerId FAILED: ${e.message}")
                }
            } else {
                Log.w(OTO_TAG, "HS complete peer=$peerId but remoteStatic was null (cannot persist)")
            }
            // XK (QR bond): mark chat + user Strong on both sides. Do not tie this to key-store
            // persistence — that can fail while Noise still completed successfully.
            if (effectivePattern == NoisePattern.XK) {
                try {
                    ensurePeerRow(peerId, displayName)
                    dbHelper.setDirectChatPairedViaQr(peerId, true)
                    val event = Arguments.createMap().apply {
                        putString("peerId", peerId)
                        putString("security", PeerSecurity.STRONG)
                    }
                    sendEvent("onPeerSecurityUpdated", event)
                } catch (e: Exception) {
                    Log.w(OTO_TAG, "XK setDirectChatPairedViaQr peer=$peerId: ${e.message}")
                }
            }
        }

        // Allocate a fresh session row: random UUID only (no peer id in the string). The legacy
        // `directSessionId(peerId)` row stays around so old messages still load via the chat
        // join, and any unmigrated code path (e.g. group/global) keeps using its own id.
        val newSessionId = UUID.randomUUID().toString()
        val inserted = try {
            dbHelper.ensureDirectSessionRow(
                sessionId = newSessionId,
                peerId = peerId,
                chachaKey = "noise-${effectivePattern.name}",
            )
        } catch (e: Exception) {
            Log.w(OTO_TAG, "SESSION row insert THREW peerId=$peerId: ${e.message}")
            false
        }
        if (inserted) {
            currentSessionIdByPeer[peerId] = newSessionId
            Log.d(OTO_TAG, "SESSION row inserted peerId=$peerId sessionId=$newSessionId")
        } else {
            // Fallback to the legacy deterministic session id so downstream message inserts
            // still satisfy the FK constraint instead of silently dropping rows.
            val legacyId = dbHelper.directSessionId(peerId)
            currentSessionIdByPeer[peerId] = legacyId
            Log.w(
                OTO_TAG,
                "SESSION row insert FAILED peerId=$peerId, falling back to legacy id=$legacyId",
            )
        }

        activeSessions[peerId] = session
        sessionBusJobs.remove(peerId)?.cancel()
        sessionBusJobs[peerId] = startObservingTwoPartyBus(peerId, session)
        Log.i(
            OTO_TAG,
            "SESSION opened peer=$peerId mp=$mpAddress initiator=$isInitiator " +
                "pattern=$effectivePattern sessionId=$newSessionId " +
                "(handshake will start ${if (isInitiator) "after mesh ready" else "on incoming packet"})",
        )
        if (isInitiator) {
            deferHandshakeUntilMeshReady(peerId, session)
        }
        return session
    }

    /**
     * Waits (with timeout) for at least one mesh neighbor to be present in
     * [MeshConnectionRegistry] before triggering the initiator's first Noise message.
     *
     * Right after an app restart BLE has not yet rediscovered any peer, so the outbound
     * map is empty for a few seconds. Calling `EpidemicFlooding.onTwoPartyMessageSend`
     * during that window silently drops the packet (no neighbors to forward to) and the
     * handshake gets stuck forever — user types a message, it queues, and nothing ever
     * leaves the device.
     */
    private fun deferHandshakeUntilMeshReady(peerId: String, session: TwoPartySession) {
        moduleScope.launch {
            val maxWaitMs = 15_000L
            val pollMs = 200L
            var waited = 0L
            while (
                waited < maxWaitMs &&
                MeshConnectionRegistry.getOutboundMap().isEmpty() &&
                activeSessions[peerId] === session &&
                !session.isHandshakeFinished
            ) {
                delay(pollMs)
                waited += pollMs
            }
            if (activeSessions[peerId] !== session) {
                Log.d(OTO_TAG, "HS deferred-start aborted peer=$peerId (session was replaced)")
                return@launch
            }
            if (session.isHandshakeFinished) {
                Log.d(OTO_TAG, "HS deferred-start aborted peer=$peerId (handshake already finished)")
                return@launch
            }
            val neighbors = MeshConnectionRegistry.getOutboundMap().size
            if (neighbors == 0) {
                Log.w(
                    OTO_TAG,
                    "HS deferred-start peer=$peerId no neighbors after ${maxWaitMs}ms — sending anyway",
                )
            } else {
                Log.d(
                    OTO_TAG,
                    "HS deferred-start peer=$peerId neighbors=$neighbors waited=${waited}ms — triggering",
                )
            }
            session.startHandshakeIfNeeded()
        }
    }

    /** Loads (or generates and persists on first call) the device's long-term X25519 static keypair. */
    private fun getOrCreateLocalStaticKeypair(): Pair<ByteArray, ByteArray> {
        localStaticKeypair?.let {
            Log.d(OTO_TAG, "KEY local static cached (in-memory)")
            return it
        }
        synchronized(localStaticLock) {
            localStaticKeypair?.let {
                Log.d(OTO_TAG, "KEY local static cached (in-memory, after lock)")
                return it
            }
            val existingPub = try {
                remotePeerCrypto.loadRemoteRawKey(LOCAL_STATIC_OWNER, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW)
            } catch (e: Exception) {
                Log.w(OTO_TAG, "KEY local static read pub FAILED: ${e.message}")
                null
            }
            val existingPriv = try {
                remotePeerCrypto.loadRemoteRawKey(LOCAL_STATIC_OWNER, RemotePeerCryptoStore.KEY_TYPE_X25519_PRIV)
            } catch (e: Exception) {
                Log.w(OTO_TAG, "KEY local static read priv FAILED: ${e.message}")
                null
            }
            if (existingPub != null && existingPriv != null) {
                Log.d(OTO_TAG, "KEY local static loaded from DB (pubBytes=${existingPub.size})")
                val pair = existingPub to existingPriv
                localStaticKeypair = pair
                return pair
            }
            val (pub, priv) = StaticKeyManager.generateX25519KeyPair()
            try {
                remotePeerCrypto.saveRemoteRawKey(
                    LOCAL_STATIC_OWNER, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW, pub,
                )
                remotePeerCrypto.saveRemoteRawKey(
                    LOCAL_STATIC_OWNER, RemotePeerCryptoStore.KEY_TYPE_X25519_PRIV, priv,
                )
                Log.i(OTO_TAG, "KEY local static GENERATED and persisted (first run)")
            } catch (e: Exception) {
                Log.w(OTO_TAG, "KEY local static persist FAILED (will regenerate next start): ${e.message}")
            }
            val pair = pub to priv
            localStaticKeypair = pair
            return pair
        }
    }

    private fun loadStoredRemoteStatic(peerId: String): ByteArray? {
        return try {
            val raw = remotePeerCrypto.loadRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_RAW)
            Log.d(OTO_TAG, "KEY load remoteStatic peer=$peerId found=${raw != null} bytes=${raw?.size ?: 0}")
            raw
        } catch (e: Exception) {
            Log.w(OTO_TAG, "KEY load remoteStatic peer=$peerId FAILED: ${e.message}")
            null
        }
    }

    private fun loadQrImportedRemoteStatic(peerId: String): ByteArray? {
        return try {
            remotePeerCrypto.loadRemoteRawKey(peerId, RemotePeerCryptoStore.KEY_TYPE_X25519_QR_IMPORT)
        } catch (e: Exception) {
            Log.w(OTO_TAG, "KEY load QR import peer=$peerId FAILED: ${e.message}")
            null
        }
    }

    private fun startObservingTwoPartyBus(peerId: String, session: TwoPartySession): Job {
        return moduleScope.launch {
            session.getMessageBus().collect { json: JsonObject ->
                handleTwoPartyBusEvent(peerId, json)
            }
        }
    }

    private fun handleTwoPartyBusEvent(peerId: String, json: JsonObject) {
        val action = json["Action"]?.toString()?.trim('"').orEmpty()
        val payload = json["Payload"]?.toString()?.trim('"').orEmpty()
        val nonce = json["Nonce"]?.toString()?.trim('"').orEmpty()
        val plaintext = json["Plaintext"]?.toString()?.trim('"').orEmpty()
        if (payload.isEmpty()) {
            Log.w(CHAT_DIAG, "twoPartyBus DROP empty payload peer=$peerId action=$action")
            return
        }

        Log.d(CHAT_DIAG, "twoPartyBus peer=$peerId action=$action payloadLen=${payload.length} nonce=$nonce")

        val activeSessionId = currentSessionIdByPeer[peerId]
        Log.d(
            OTO_TAG,
            "MSG bus peer=$peerId action=$action sessionId=$activeSessionId plaintextLen=${plaintext.length}",
        )
        val msg = try {
            when (action) {
                "Send" -> MessagingStore.sendMessage(
                    peerId = peerId,
                    encryptedPayload = payload,
                    nonce = nonce,
                    bodyText = plaintext,
                    sessionId = activeSessionId,
                )
                "Receive" -> MessagingStore.addIncomingMessage(
                    peerId = peerId,
                    senderId = peerId,
                    encryptedPayload = payload,
                    nonce = nonce,
                    bodyText = plaintext,
                    sessionId = activeSessionId,
                )
                else -> return
            }
        } catch (e: Exception) {
            Log.e("MeshengerApplication", "Failed to persist 1-1 message: ${e.message}", e)
            Log.e(CHAT_DIAG, "twoParty persist FAILED peer=$peerId action=$action", e)
            return
        }

        Log.i(CHAT_DIAG, "twoParty persisted id=${msg.id} action=$action peer=$peerId")

        val map = messageToWritableMap(
            msg = msg,
            fromMe = action == "Send",
            sessionKey = null,
            plaintextOverride = plaintext,
        ).apply {
            putString("chatId", dbHelper.directChatId(peerId))
            putString("peerId", peerId)
            putString("sessionType", "TwoPartyChat")
            putString("action", action)
        }
        if (action == "Receive") {
            val rawName = dbHelper.getUserProfile(peerId)?.userName ?: peerId
            val senderName = if (rawName.startsWith("mp:")) "Ai đó" else rawName
            NotificationHelper.showNewMessageNotification(
                reactApplicationContext,
                senderName,
                plaintext.takeIf { it.isNotEmpty() } ?: "Tin nhắn mới"
            )
        }
        sendEvent("onNewMessage", map)
    }

    private fun registerMeshPeerAnnouncedHook() {
        GlobalChatSession.onMeshPeerAnnounced = { mp, rawName, avatarId ->
            moduleScope.launch(Dispatchers.IO) {
                try {
                    applyBootstrapToStoredMeshPeer(mp, rawName, avatarId)
                } catch (e: Exception) {
                    Log.e("MeshengerApplication", "applyBootstrapToStoredMeshPeer failed", e)
                }
            }
        }
    }

    /**
     * Updates an *already-known* peer's display name / avatar from their bootstrap announcement
     * (e.g. upgrade SQLite rows that still use `mp:` placeholders).
     *
     * Intentionally does NOT create a new row for unknown peers — the in-memory
     * [PeerInMeshRegistry] is sufficient for the scan list, and we only want a DB record
     * once the user has explicitly invited or accepted a chat with this peer.
     */
    private fun applyBootstrapToStoredMeshPeer(mp: ULong, displayNameRaw: String, avatarIdRaw: String?) {
        val displayName = displayNameRaw.trim()
        if (displayName.isBlank()) return
        val avatarId = avatarIdRaw?.trim()?.takeIf { it.isNotEmpty() }
        val peerId = meshPeerId(mp)
        val existing = dbHelper.getUserProfile(peerId) ?: return // not a known peer yet — skip DB write

        val finalName = if (existing.userName == peerId) displayName else existing.userName
        dbHelper.upsertUserProfile(
            existing.copy(
                userName = finalName,
                userAvtId = avatarId ?: existing.userAvtId,
            )
        )
        if (existing.userName == peerId) {
            dbHelper.updateDirectPeerDisplayName(peerId, displayName)
            Log.d("MeshengerApplication", "Upgraded mesh peer display name: $peerId -> $displayName")
            val event = Arguments.createMap().apply {
                putString("peerId", peerId)
                putString("displayName", displayName)
            }
            sendEvent("onPeerDisplayNameUpdated", event)
        }
        dbHelper.prunePlaceholderMeshPeersWithNoMessages()
        if (UserStore.isGenericMeshDisplayName(displayName)) {
            dbHelper.pruneEmptyMeshPeersWithSameDisplayName(peerId, displayName)
        }
    }

    /**
     * Periodically floods a signed bootstrap packet so peers in DeviceScan can see this device
     * even when the user is not on the scan screen. PeerInMeshRegistry entries expire after 2
     * minutes, so this cadence (every 25s) keeps registrations alive once the mesh is up.
     */
    private fun startPresenceLoop() {
        moduleScope.launch {
            while (isActive) {
                try {
                    val profile = UserStore.getProfile()
                    val name = profile.userName.trim()
                    val hasNeighbors = MeshConnectionRegistry.getOutboundMap().isNotEmpty()
                    if (name.isNotBlank() && hasNeighbors) {
                        GlobalChatSession.sendBootstrap(name, profile.userAvtId)
                        Log.d("MeshengerApplication", "Presence bootstrap sent as '$name' avatar=${profile.userAvtId}")
                    }
                } catch (e: Exception) {
                    Log.w("MeshengerApplication", "Presence bootstrap failed: ${e.message}")
                }
                delay(PRESENCE_INTERVAL_MS)
            }
        }
    }

    /**
     * Installs the responder-side handshake fallback. When a NOISE_HANDSHAKE packet arrives
     * and there is no [TwoPartySession] yet for that sender, this creates one (isInitiator=false)
     * and feeds the packet into it so the handshake can complete.
     */
    private fun registerHandshakeFallback() {
        ListenerRegistry.setTwoPartyHandshakeFallback(
            TwoPartyHandshakeFallback { senderId, message ->
                try {
                    if (senderId == MPAddress.getMyMPAddressULong()) return@TwoPartyHandshakeFallback
                    if (message.isEmpty()) return@TwoPartyHandshakeFallback
                    // Inbound NOISE_HANDSHAKE wire format: [1 byte pattern tag][noise bytes].
                    // We must construct our session with the *same* pattern as the initiator,
                    // otherwise HandshakeState wire formats won't line up.
                    val incomingPattern = NoisePattern.fromTag(message[0])
                        ?: run {
                            Log.w(OTO_TAG, "HS inbound DROP: unknown pattern tag=${message[0]} from $senderId")
                            return@TwoPartyHandshakeFallback
                        }
                    val peerId = "$MP_PREFIX$senderId"
                    val displayName = PeerInMeshRegistry.getAllPeers()
                        .firstOrNull { it.MPAddress == senderId }
                        ?.userName
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                        ?: peerId
                    Log.d(
                        OTO_TAG,
                        "HS inbound peer=$peerId pattern=$incomingPattern bytes=${message.size} " +
                            "existing=${activeSessions[peerId]?.let { "pattern=${it.chosenPattern} finished=${it.isHandshakeFinished}" } ?: "none"}",
                    )

                    val existing = activeSessions[peerId]
                    val session = when {
                        existing == null -> {
                            Log.d(OTO_TAG, "HS inbound peer=$peerId -> create fresh session as responder")
                            ensurePeerRow(peerId, displayName)
                            createAndTrackSession(
                                peerId = peerId,
                                mpAddress = senderId,
                                displayName = displayName,
                                isInitiator = false,
                                forcedPattern = incomingPattern,
                            )
                        }
                        existing.isHandshakeFinished -> {
                            // Peer is restarting a handshake (e.g. they were killed and now
                            // reopened the chat) but we still have a finished session in memory.
                            // Tear our side down and accept the new handshake instead of dropping.
                            Log.i(
                                OTO_TAG,
                                "HS inbound peer=$peerId -> recreate finished session (peer restarted), pattern=$incomingPattern",
                            )
                            sessionBusJobs.remove(peerId)?.cancel()
                            activeSessions.remove(peerId)?.close()
                            currentSessionIdByPeer.remove(peerId)
                            ensurePeerRow(peerId, displayName)
                            createAndTrackSession(
                                peerId = peerId,
                                mpAddress = senderId,
                                displayName = displayName,
                                isInitiator = false,
                                forcedPattern = incomingPattern,
                            )
                        }
                        existing.chosenPattern != incomingPattern -> {
                            // We had an old / mismatched session for this peer (e.g. we expected
                            // KK because we still had their static, but they restarted with a
                            // wiped DB and downgraded to XX). Tear it down and start fresh with
                            // the pattern the initiator actually picked.
                            Log.w(
                                OTO_TAG,
                                "HS inbound peer=$peerId pattern mismatch (existing=${existing.chosenPattern}, " +
                                    "incoming=$incomingPattern) -> recreate",
                            )
                            sessionBusJobs.remove(peerId)?.cancel()
                            activeSessions.remove(peerId)?.close()
                            currentSessionIdByPeer.remove(peerId)
                            ensurePeerRow(peerId, displayName)
                            createAndTrackSession(
                                peerId = peerId,
                                mpAddress = senderId,
                                displayName = displayName,
                                isInitiator = false,
                                forcedPattern = incomingPattern,
                            )
                        }
                        else -> existing.also { ensurePeerRow(peerId, displayName) }
                    }
                    session.onReceiveMessageHandShake(senderId, message)

                    val event = Arguments.createMap().apply {
                        putString("peerId", peerId)
                        putString("displayName", displayName)
                        putString("mpAddress", senderId.toString())
                    }
                    sendEvent("onIncomingHandshake", event)
                } catch (e: Exception) {
                    Log.e(OTO_TAG, "HS inbound FAILED: ${e.message}", e)
                }
            }
        )
    }

    private fun registerDirectChatNegotiationListener() {
        ListenerRegistry.setDirectChatNegotiationListener(
            object : DirectChatNegotiationListener {
                override fun onInviteReceived(senderId: ULong, payload: ByteArray, timeStamp: ULong) {
                    try {
                        if (senderId == MPAddress.getMyMPAddressULong()) return
                        val peerIdStr = meshPeerId(senderId)
                        val inv = parseInvitePayload(payload)
                        Log.i(
                            OTO_TAG,
                            "INVITE recv from $peerIdStr name='${inv.displayName}' avatar='${inv.avatarId ?: ""}'",
                        )
                        // Do NOT write to DB here — peer is stored only in memory until user accepts.
                        pendingIncomingInviteByPeer[peerIdStr] = PendingIncomingInvite(
                            peerId = peerIdStr,
                            displayName = inv.displayName.ifBlank { peerIdStr },
                            avatarId = inv.avatarId,
                            timestamp = timeStamp.toLong(),
                        )
                        val rawName = inv.displayName.ifBlank { peerIdStr }
                        val displayNameForNotif = if (rawName.startsWith("mp:")) "Ai đó" else rawName
                        NotificationHelper.showPendingInviteNotification(
                            reactApplicationContext,
                            "Yêu cầu kết nối mới",
                            "Bạn có một yêu cầu kết nối từ $displayNameForNotif"
                        )
                        sendEvent(
                            "onIncomingDirectChatInvite",
                            Arguments.createMap().apply {
                                putString("peerId", peerIdStr)
                                putString("displayName", inv.displayName.ifBlank { peerIdStr })
                                inv.avatarId?.let { putString("avatarId", it) }
                                putDouble("timestamp", timeStamp.toLong().toDouble())
                            },
                        )
                    } catch (e: Exception) {
                        Log.e("MeshengerApplication", "onInviteReceived failed: ${e.message}", e)
                    }
                }

                override fun onInviteAccepted(senderId: ULong, payload: ByteArray, timeStamp: ULong) {
                    try {
                        if (senderId == MPAddress.getMyMPAddressULong()) return
                        val peerIdStr = meshPeerId(senderId)
                        outgoingDirectChatInvites.remove(peerIdStr)
                        val displayName = resolveMeshPeerDisplayName(senderId)
                        // Resolve avatar from in-memory registry (populated by bootstrap)
                        val avatarId = PeerInMeshRegistry.getAllPeers()
                            .firstOrNull { it.MPAddress == senderId }?.avatarId
                        Log.i(
                            OTO_TAG,
                            "INVITE accepted by peer=$peerIdStr displayName='$displayName' -> opening session",
                        )
                        ensurePeerRow(peerIdStr, displayName, avatarId)
                        openOrEnsureTwoPartySession(peerIdStr, senderId, displayName)
                        sendEvent(
                            "onDirectChatInviteAccepted",
                            Arguments.createMap().apply {
                                putString("peerId", peerIdStr)
                                putString("displayName", displayName)
                                avatarId?.let { putString("avatarId", it) }
                            },
                        )
                    } catch (e: Exception) {
                        Log.e("MeshengerApplication", "onInviteAccepted failed: ${e.message}", e)
                    }
                }

                override fun onInviteRejected(senderId: ULong, payload: ByteArray, timeStamp: ULong) {
                    try {
                        if (senderId == MPAddress.getMyMPAddressULong()) return
                        val peerIdStr = meshPeerId(senderId)
                        outgoingDirectChatInvites.remove(peerIdStr)
                        Log.i(OTO_TAG, "INVITE rejected by peer=$peerIdStr")
                        sendEvent(
                            "onDirectChatInviteRejected",
                            Arguments.createMap().apply {
                                putString("peerId", peerIdStr)
                            },
                        )
                    } catch (e: Exception) {
                        Log.e("MeshengerApplication", "onInviteRejected failed: ${e.message}", e)
                    }
                }
            },
        )
    }

    private fun twoPartySessionInfo(peerId: String, isInitiator: Boolean, justOpened: Boolean): WritableMap {
        return Arguments.createMap().apply {
            putString("peerId", peerId)
            putBoolean("isInitiator", isInitiator)
            putBoolean("justOpened", justOpened)
            putBoolean("isOpen", activeSessions.containsKey(peerId))
        }
    }

    @ReactMethod
    fun getMyIdentity(promise: Promise) {
        val profile = UserStore.getProfile()
        val result = Arguments.createMap().apply {
            putString("id", LOCAL_ID)
            putString("displayName", profile.userName)
            if (profile.userAvtId != null) {
                putString("userAvtId", profile.userAvtId)
            }
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
            val peersList = UserStore.getAllPeers().filter { peer ->
                when {
                    peer.id == GLOBAL_BROADCAST_ID -> true
                    peer.id.startsWith(MP_PREFIX) -> dbHelper.hasDirectChatForPeer(peer.id)
                    else -> false
                }
            }
            val array: WritableArray = Arguments.createArray()
            for (peer in peersList) {
                val map: WritableMap = Arguments.createMap().apply {
                    putString("id", peer.id)
                    putString("displayName", peer.userName)
                    peer.userAvtId?.let { putString("avatarId", it) }
                    putString("security", peer.security)
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
            putString("sessionId", UUID.randomUUID().toString())
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
                putString("avatarId", profile.userAvtId)
            }
            promise.resolve(profileMap)
        } catch (e: Exception) {
            promise.reject("GET_PROFILE_FAILED", e.message)
        }
    }

    @ReactMethod
    fun updateMyProfile(newDisplayName: String, newAvatarUrl: String?, promise: Promise) {
        try {
            val trimmed = newDisplayName.trim()
            if (trimmed.isBlank()) {
                promise.reject("INVALID_INPUT", "Display name cannot be empty")
                return
            }
            if (UserStore.isGenericMeshDisplayName(trimmed)) {
                promise.reject(
                    "INVALID_INPUT",
                    "Choose a unique name (not \"${UserStore.DEFAULT_PROFILE_USER_NAME}\") for mesh discovery",
                )
                return
            }
            val avatarId = newAvatarUrl?.trim()?.takeIf { it.isNotEmpty() }
            val updated = UserStore.updateProfile(userName = trimmed, userAvtId = avatarId)
            val profile = Arguments.createMap().apply {
                putString("id", updated.id)
                putString("displayName", updated.userName)
                updated.userAvtId?.let { putString("avatarId", it) }
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
//    @ReactMethod
//    fun saveRemotePeerRawKey(peerUserId: String, keyType: String, rawKeyMaterialBase64: String, promise: Promise) {
//        try {
//            if (peerUserId.isBlank() || keyType.isBlank() || rawKeyMaterialBase64.isBlank()) {
//                promise.reject("INVALID_INPUT", "peerUserId, keyType and rawKeyMaterialBase64 are required")
//                return
//            }
//            val normalizedType = keyType.trim()
//            if (!RemotePeerCryptoStore.allowedKeyTypes().contains(normalizedType)) {
//                promise.reject(
//                    "INVALID_KEY_TYPE",
//                    "keyType must be ${RemotePeerCryptoStore.KEY_TYPE_ED25519_RAW} or ${RemotePeerCryptoStore.KEY_TYPE_X25519_RAW}",
//                )
//                return
//            }
//            val raw = android.util.Base64.decode(rawKeyMaterialBase64.trim(), android.util.Base64.NO_WRAP)
//            remotePeerCrypto.saveRemoteRawKey(peerUserId.trim(), normalizedType, raw)
//            promise.resolve(null)
//        } catch (e: Exception) {
//            promise.reject("SAVE_REMOTE_KEY_FAILED", e.message, e)
//        }
//    }
//
//    /** Returns Base64(raw bytes) or a map of {type: Base64} if keyType is "ALL". */
//    @ReactMethod
//    fun loadRemotePeerRawKey(peerUserId: String, keyType: String, promise: Promise) {
//        try {
//            if (keyType == "ALL") {
//                val map = Arguments.createMap()
//                for (type in RemotePeerCryptoStore.allowedKeyTypes()) {
//                    val bytes = remotePeerCrypto.loadRemoteRawKey(peerUserId.trim(), type)
//                    if (bytes != null) {
//                        map.putString(type, android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
//                    }
//                }
//                promise.resolve(map)
//                return
//            }
//            val bytes = remotePeerCrypto.loadRemoteRawKey(peerUserId.trim(), keyType.trim())
//                ?: run {
//                    promise.reject("NOT_FOUND", "No stored key for this peer and keyType")
//                    return
//                }
//            promise.resolve(android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
//        } catch (e: Exception) {
//            promise.reject("LOAD_REMOTE_KEY_FAILED", e.message, e)
//        }
//    }
//
//    @ReactMethod
//    fun deleteRemotePeerRawKey(peerUserId: String, keyType: String, promise: Promise) {
//        try {
//            remotePeerCrypto.deleteRemoteRawKey(peerUserId.trim(), keyType.trim())
//            promise.resolve(null)
//        } catch (e: Exception) {
//            promise.reject("DELETE_REMOTE_KEY_FAILED", e.message, e)
//        }
//    }
}