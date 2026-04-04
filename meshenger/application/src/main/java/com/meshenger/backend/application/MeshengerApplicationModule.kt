package com.meshenger.backend.application

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap
import com.meshenger.backend.application.db.MeshengerDbHelper
import com.meshenger.backend.application.messaging.MessagingStore
import com.meshenger.backend.application.user.UserProfile
import com.meshenger.backend.application.user.UserStore
import com.meshenger.backend.session.HelloWorldBridge

/**
 * React Native native module for the Application layer.
 */
class MeshengerApplicationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val bridge = HelloWorldBridge()
    private val dbHelper = MeshengerDbHelper(reactContext.applicationContext)

    init {
        // Initialize persistent stores (messages + local profile).
        MessagingStore.init(dbHelper)
        UserStore.init(dbHelper)
    }

    override fun getName(): String = "MeshengerApplicationModule"

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
    fun getMessage(promise: Promise) {
        promise.resolve("Application layer is active")
    }

    @ReactMethod
    fun getMessageSession(promise: Promise) {
        promise.resolve(bridge.getMessage())
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
            val result: WritableMap = Arguments.createMap().apply {
                putString("id", message.id)
                putString("peerId", message.peerId)
                putString("text", message.text)
                putBoolean("fromMe", message.fromMe)
                putDouble("timestamp", message.timestamp.toDouble())
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("PUSH_INCOMING_FAILED", e.message)
        }
    }

    @ReactMethod
    fun getConversation(peerId: String, promise: Promise) {
        try {
            val messages = MessagingStore.getConversation(peerId)
            val array: WritableArray = Arguments.createArray()
            for (msg in messages) {
                val map: WritableMap = Arguments.createMap().apply {
                    putString("id", msg.id)
                    putString("text", msg.text)
                    putBoolean("fromMe", msg.fromMe)
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
