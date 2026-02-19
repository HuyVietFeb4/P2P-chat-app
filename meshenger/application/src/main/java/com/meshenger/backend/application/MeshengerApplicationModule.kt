package com.meshenger.backend.application

import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableMap

import com.meshenger.backend.application.messaging.MessagingStore
import com.meshenger.backend.session.HelloWorldBridge

/**
 * React Native native module for the Application layer.
 * Exposes APIs to the JS frontend (getMessage, getMessageSession, sendMessage, getConversation).
 */
class MeshengerApplicationModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val bridge = HelloWorldBridge()

    override fun getName(): String = "MeshengerApplicationModule"

    @ReactMethod
    fun getMessage(promise: Promise) {
        promise.resolve("Application layer says: ${AppInfo.getMessage()}")
    }

    @ReactMethod
    fun getMessageSession(promise: Promise) {
        promise.resolve(bridge.getMessage())
    }

    // --- Messaging API ---

    @ReactMethod
    fun sendMessage(peerId: String, text: String, promise: Promise) {
        try {
            if (peerId.isBlank()) {
                promise.reject("INVALID_PEER", "peerId cannot be empty")
                return
            }
            if (text.isBlank()) {
                promise.reject("INVALID_TEXT", "text cannot be empty")
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
            promise.reject("SEND_FAILED", e.message ?: "Failed to send message")
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
                    putString("peerId", msg.peerId)
                    putString("text", msg.text)
                    putBoolean("fromMe", msg.fromMe)
                    putDouble("timestamp", msg.timestamp.toDouble())
                }
                array.pushMap(map)
            }
            promise.resolve(array)
        } catch (e: Exception) {
            promise.reject("GET_CONVERSATION_FAILED", e.message ?: "Failed to get conversation")
        }
    }
}
