package com.meshenger.backend.application

import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.messaging.MessagingStore
import com.meshenger.backend.application.user.UserProfile
import com.meshenger.backend.application.user.UserStore

/**
 * React Native native module for the Application layer.
 */
class MeshengerApplicationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val dbHelper = MeshengerDbHelper(reactContext.applicationContext)

    init {
        // Initialize persistent stores (messages + local profile).
        MessagingStore.init(dbHelper)
        UserStore.init(dbHelper)

        // Lắng nghe sự thay đổi trạng thái từ MessagingStore để báo cho React Native (Phần 4 + 5)
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

    @ReactMethod
    fun addPeer(id: String, displayName: String, avatarUrl: String?, promise: Promise) {
        try {
            if (id.isBlank() || displayName.isBlank()) {
                promise.reject("INVALID_INPUT", "ID and Display Name cannot be empty")
                return
            }

            val newPeer = UserProfile(
                id = id,
                displayName = displayName,
                avatarUrl = avatarUrl
            )

            dbHelper.upsertUserProfile(newPeer)
            promise.resolve("Peer $displayName saved successfully")
        } catch (e: Exception) {
            e.printStackTrace()
            promise.reject("DB_ERROR", "Could not save to database: ${e.message}")
        }
    }

    @ReactMethod
    fun myQR(promise: Promise) {
        try {
            val profile = dbHelper.getUserProfile("local-device")
            if (profile != null) {
                promise.resolve(profile.displayName)
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

    @ReactMethod
    fun sendMessage(peerId: String, text: String, promise: Promise) {
        try {
            if (peerId.isBlank() || text.isBlank()) {
                promise.reject("INVALID_INPUT", "Inputs cannot be empty")
                return
            }
            val message = MessagingStore.sendMessage(peerId, text, fromMe = true)
            val result: WritableMap = Arguments.createMap().apply {
                putString("id", message.id)
                putString("peerId", message.peerId)
                putString("text", message.text)
                putBoolean("fromMe", true)
                putString("status", message.status.name)
                putDouble("timestamp", message.timestamp.toDouble())
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("SEND_FAILED", e.message)
        }
    }

    @ReactMethod
    fun pushIncomingMessage(peerId: String, text: String, promise: Promise) {
        try {
            val message = MessagingStore.addIncomingMessage(peerId, text)
            val result = Arguments.createMap().apply {
                putString("id", message.id)
                putString("peerId", message.peerId)
                putString("text", message.text)
                putBoolean("fromMe", false)
                putString("status", message.status.name)
                putDouble("timestamp", message.timestamp.toDouble())
            }
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
                val map = Arguments.createMap().apply {
                    putString("id", msg.id)
                    putString("text", msg.text)
                    putBoolean("fromMe", msg.fromMe)
                    putString("status", msg.status.name)
                    putDouble("timestamp", msg.timestamp.toDouble())
                }
                array.pushMap(map)
            }
            promise.resolve(array)
        } catch (e: Exception) {
            promise.reject("GET_CONVERSATION_FAILED", e.message)
        }
    }

    @ReactMethod
    fun getMyIdentity(promise: Promise) {
        val result = Arguments.createMap().apply {
            putString("id", "local-device")
            putString("displayName", "Local User")
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
                    putString("displayName", peer.displayName)
                    peer.avatarUrl?.let { putString("avatarUrl", it) }
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
                putString("displayName", profile.displayName)
                profile.avatarUrl?.let { putString("avatarUrl", it) }
            }
            promise.resolve(profileMap)
        } catch (e: Exception) {
            promise.reject("GET_PROFILE_FAILED", e.message)
        }
    }

    @ReactMethod
    fun updateMyProfile(newDisplayName: String, newAvatarUrl: String?, promise: Promise) {
        try {
            val updated = UserStore.updateProfile(newDisplayName, newAvatarUrl)
            val profile = Arguments.createMap().apply {
                putString("id", updated.id)
                putString("displayName", updated.displayName)
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
